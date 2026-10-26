package vn.iotstar.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.dto.shipment.CreateShipmentRequest;
import vn.iotstar.dto.shipment.ShipmentDetailResponse;
import vn.iotstar.entity.Payment;
import vn.iotstar.entity.PricingPolicy;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.exception.ShipmentAccessDeniedException;
import vn.iotstar.realtime.ShipmentEventPublisher;
import vn.iotstar.repository.PaymentRepository;
import vn.iotstar.repository.PricingPolicyRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.repository.UserRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceImplTest {
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private ShipmentEventRepository shipmentEventRepository;
    @Mock private UserRepository userRepository;
    @Mock private ShipmentEventPublisher shipmentEventPublisher;
    @Mock private PricingPolicyRepository pricingPolicyRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private Clock clock;
    @Mock private ShipmentCancellation cancellation;
    @Mock private vn.iotstar.repository.HubRepository hubRepository;
    @InjectMocks private ShipmentServiceImpl shipmentService;
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 10, 23, 23, 59, 59);

    @BeforeEach
    void useServerTime() {
        lenient().when(clock.instant()).thenReturn(CREATED_AT.atZone(ZoneId.systemDefault()).toInstant().plusNanos(999_999_999));
        var destination = vn.iotstar.entity.Hub.active("HCM-Q1", "Q1", 1, "DELIVERY_HUB", "SOUTH", "HCM");
        ReflectionTestUtils.setField(destination, "id", 91L);
        ReflectionTestUtils.setField(destination, "district", "Q1");
        lenient().when(hubRepository.findByStatusAndTypeAndProvinceAndDistrict("ACTIVE", "DELIVERY_HUB", "HCM", "Q1"))
                .thenReturn(List.of(destination));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsCanonicalShipmentAndSeparateCreationEventForCurrentActor() {
        User user = authenticate(7L, "owner");
        stubPolicy();
        when(userRepository.getReferenceById(7L)).thenReturn(user);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> {
            Shipment shipment = invocation.getArgument(0);
            ReflectionTestUtils.setField(shipment, "id", 42L);
            return shipment;
        });
        when(shipmentEventRepository.save(any(ShipmentEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Long shipmentId = shipmentService.create(request());

        ArgumentCaptor<Shipment> shipmentCaptor = ArgumentCaptor.forClass(Shipment.class);
        ArgumentCaptor<ShipmentEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentEvent.class);
        verify(shipmentRepository).save(shipmentCaptor.capture());
        verify(shipmentEventRepository).save(eventCaptor.capture());
        Shipment shipment = shipmentCaptor.getValue();
        assertThat(shipmentId).isEqualTo(42L);
        assertThat(shipment.getSender()).isSameAs(user);
        assertThat(shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.WAITING_FOR_PAYMENT);
        assertThat(shipment.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(shipment.getWaitingForHandoverAt()).isNull();
        assertThat(shipment.getTrackingNumber()).startsWith("UTX-");
        assertThat(shipment.getDestinationHubId()).isEqualTo(91L);
        assertThat(shipment.getDestinationRegion()).isEqualTo("SOUTH");
        assertThat(shipment.getDestinationProvince()).isEqualTo("HCM");
        assertThat(eventCaptor.getValue().getShipment()).isSameAs(shipment);
        assertThat(eventCaptor.getValue().getActor()).isSameAs(user);
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("SHIPMENT_CREATED");
        verify(shipmentEventPublisher).publishAfterCommit(any(), any());
        verify(userRepository).findByPhone(anyString());
        verify(paymentRepository).save(any(Payment.class));
        verifyNoMoreInteractions(shipmentRepository, shipmentEventRepository, userRepository, shipmentEventPublisher);
    }

    @Test
    void quotesBasePlusExtraWeightStepsInsuranceAndCodFee() {
        stubPolicy();
        // 20000 + ceil(800/500)=2 steps * 5000 + 1% of 1,000,000 + 2% of 500,000 = 50000
        CreateShipmentRequest request = request();
        request.setWeightGrams(1300);
        request.setDeclaredValue(new BigDecimal("1000000"));
        request.setCodAmount(new BigDecimal("500000"));

        assertThat(shipmentService.quote(request).shippingFee()).isEqualByComparingTo("50000");
    }

    @Test void modernCreationPersistsFullAddressesAndDerivesRegionsBeforePricing() {
        User user = authenticate(7L, "owner");
        stubPolicy();
        when(userRepository.getReferenceById(7L)).thenReturn(user);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shipmentEventRepository.save(any(ShipmentEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        var destination = vn.iotstar.entity.Hub.active("LOCAL2025-P79", "Demo HCM", 1, "DELIVERY_HUB", "SOUTH", "Thành phố Hồ Chí Minh");
        ReflectionTestUtils.setField(destination, "id", 92L);
        when(hubRepository.findByStatusAndTypeAndProvinceAndDistrict("ACTIVE", "DELIVERY_HUB", "Thành phố Hồ Chí Minh", null)).thenReturn(List.of(destination));
        var request = request();
        request.setSenderProvinceCode("79"); request.setSenderWardCode("26740");
        request.setReceiverProvinceCode("79"); request.setReceiverWardCode("26743");
        request.setFromRegion("NORTH"); request.setToRegion("CENTRAL");
        shipmentService.create(request);
        var captured = ArgumentCaptor.forClass(Shipment.class);
        verify(shipmentRepository).save(captured.capture());
        assertThat(captured.getValue().getSenderAddress()).isEqualTo("1 Owner Street, Phường Sài Gòn, Thành phố Hồ Chí Minh");
        assertThat(captured.getValue().getReceiverAddress()).isEqualTo("2 Receiver Street, Phường Bến Thành, Thành phố Hồ Chí Minh");
        assertThat(captured.getValue().getReceiverDistrict()).isNull();
        assertThat(captured.getValue().getDestinationHubId()).isEqualTo(92L);
        verify(pricingPolicyRepository).findFirstByServiceCodeAndFromRegionAndToRegionAndStatusOrderByIdDesc("STANDARD", "SOUTH", "SOUTH", "ACTIVE");
    }

    @Test void missingModernCoverageIsRejectedBeforeOfferingAQuote() {
        var request = request();
        request.setSenderProvinceCode("79"); request.setSenderWardCode("26740");
        request.setReceiverProvinceCode("01"); request.setReceiverWardCode("00004");
        assertThatThrownBy(() -> shipmentService.quote(request)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(pricingPolicyRepository, shipmentRepository, paymentRepository, shipmentEventRepository);
    }

    @Test
    void rejectsChangedPriceBeforeAnyShipmentOrPaymentIsWritten() {
        authenticate(7L, "owner");
        stubPolicy();

        assertThatThrownBy(() -> shipmentService.create(request(), new BigDecimal("19000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bảng giá đã thay đổi. Vui lòng báo giá lại trước khi tạo đơn.");

        verifyNoInteractions(shipmentRepository, paymentRepository, shipmentEventRepository, shipmentEventPublisher, userRepository);
    }

    @Test
    void quotedCreationRequiresAcceptedFee() {
        assertThatThrownBy(() -> shipmentService.create(request(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Vui lòng báo giá lại trước khi tạo đơn.");
        verifyNoInteractions(shipmentRepository, paymentRepository, pricingPolicyRepository);
    }

    @Test
    void acceptedFeeComparisonIgnoresDecimalScale() {
        User user = authenticate(7L, "owner");
        stubPolicy();
        when(userRepository.getReferenceById(7L)).thenReturn(user);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shipmentEventRepository.save(any(ShipmentEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        shipmentService.create(request(), new BigDecimal("20000.00"));

        verify(shipmentRepository).save(any(Shipment.class));
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void senderOnlinePaymentWaitsForPaymentThenDemoConfirmIsIdempotent() {
        User user = authenticate(7L, "owner");
        stubPolicy();
        when(userRepository.getReferenceById(7L)).thenReturn(user);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shipmentEventRepository.save(any(ShipmentEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

        shipmentService.create(request());

        ArgumentCaptor<Shipment> shipmentCaptor = ArgumentCaptor.forClass(Shipment.class);
        verify(shipmentRepository).save(shipmentCaptor.capture());
        verify(paymentRepository).save(paymentCaptor.capture());
        Shipment shipment = shipmentCaptor.getValue();
        Payment payment = paymentCaptor.getValue();
        assertThat(shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.WAITING_FOR_PAYMENT);
        assertThat(shipment.getWaitingForHandoverAt()).isNull();
        assertThat(payment.getStatus()).isEqualTo("PENDING");

        ReflectionTestUtils.setField(shipment, "id", 42L);
        when(shipmentRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(shipment));
        when(paymentRepository.findFirstByShipmentIdAndPaymentType(42L, "SHIPPING_FEE")).thenReturn(Optional.of(payment));

        LocalDateTime confirmedAt = CREATED_AT.plusDays(2).plusSeconds(1);
        when(clock.instant()).thenReturn(confirmedAt.atZone(ZoneId.systemDefault()).toInstant().plusNanos(999_999_999));
        shipmentService.confirmPayment(42L);
        assertThat(shipment.getWaitingForHandoverAt()).isEqualTo(confirmedAt);
        lenient().when(clock.instant()).thenReturn(confirmedAt.plusDays(1).atZone(ZoneId.systemDefault()).toInstant());
        shipmentService.confirmPayment(42L);

        assertThat(shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.WAITING_FOR_HANDOVER);
        assertThat(shipment.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(shipment.getWaitingForHandoverAt()).isEqualTo(confirmedAt);
        assertThat(shipment.getPaymentStatus()).isEqualTo("PAID");
        assertThat(payment.getStatus()).isEqualTo("PAID");
        verify(shipmentEventRepository, times(2)).save(any(ShipmentEvent.class)); // created + one PAYMENT_CONFIRMED
    }

    @ParameterizedTest
    @ValueSource(strings = {"SENDER", "RECEIVER"})
    void cashPaymentSkipsOnlineWaitAndReceiverPayerCannotUseOnlineMethod(String payer) {
        User user = authenticate(7L, "owner");
        stubPolicy();
        when(userRepository.getReferenceById(7L)).thenReturn(user);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shipmentEventRepository.save(any(ShipmentEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateShipmentRequest cash = request();
        cash.setPaymentMethod("CASH");
        cash.setFeePayer(payer);

        shipmentService.create(cash);

        ArgumentCaptor<Shipment> captor = ArgumentCaptor.forClass(Shipment.class);
        verify(shipmentRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentStatus()).isEqualTo(ShipmentStatus.WAITING_FOR_HANDOVER);
        assertThat(captor.getValue().getPaymentStatus()).isEqualTo("UNPAID");
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(captor.getValue().getWaitingForHandoverAt()).isEqualTo(CREATED_AT);

        CreateShipmentRequest invalid = request();
        invalid.setFeePayer("RECEIVER");
        assertThatThrownBy(() -> shipmentService.quote(invalid)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nonOwnerCannotConfirmPayment() {
        User owner = new User(7L, "owner", "hash", UserRole.ROLE_USER, "ACTIVE");
        Shipment shipment = Shipment.create("UTX-001", owner, "Owner", "0900000001", "A", "Receiver", "0900000002", "B");
        authenticate(8L, "other");
        when(shipmentRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> shipmentService.confirmPayment(42L)).isInstanceOf(ShipmentAccessDeniedException.class);
    }

    @Test
    void ownerReadsDetailAndChronologicalEvents() {
        User owner = authenticate(7L, "owner");
        Shipment shipment = Shipment.create("UTX-001", owner, "Owner", "0900000001", "A", "Receiver", "0900000002", "B");
        ReflectionTestUtils.setField(shipment, "id", 42L);
        ReflectionTestUtils.setField(shipment, "createdAt", LocalDateTime.of(2026, 10, 1, 10, 0));
        ReflectionTestUtils.setField(shipment, "currentStatus", ShipmentStatus.WAITING_FOR_HANDOVER);
        ReflectionTestUtils.setField(shipment, "waitingForHandoverAt", CREATED_AT);
        ShipmentEvent event = ShipmentEvent.shipmentCreated(shipment, owner);
        ReflectionTestUtils.setField(event, "createdAt", LocalDateTime.of(2026, 10, 1, 10, 1));
        when(shipmentRepository.findById(42L)).thenReturn(Optional.of(shipment));
        when(shipmentEventRepository.findByShipmentIdOrderByCreatedAtAsc(42L)).thenReturn(List.of(event));

        ShipmentDetailResponse detail = shipmentService.getDetail(42L);

        assertThat(detail.id()).isEqualTo(42L);
        assertThat(detail.currentStatus()).isEqualTo(ShipmentStatus.WAITING_FOR_HANDOVER);
        assertThat(detail.waitingForHandoverAt()).isEqualTo(CREATED_AT);
        assertThat(detail.events()).singleElement().extracting("eventType").isEqualTo("SHIPMENT_CREATED");
    }

    @Test
    void nonOwnerIsDeniedAtServiceBoundaryBeforeEventsAreRead() {
        User owner = new User(7L, "owner", "hash", UserRole.ROLE_USER, "ACTIVE");
        Shipment shipment = Shipment.create("UTX-001", owner, "Owner", "0900000001", "A", "Receiver", "0900000002", "B");
        authenticate(8L, "other");
        when(shipmentRepository.findById(42L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> shipmentService.getDetail(42L)).isInstanceOf(ShipmentAccessDeniedException.class);

        verify(shipmentRepository).findById(42L);
        verifyNoInteractions(shipmentEventRepository);
    }

    @Test
    void missingShipmentIsSafelyReportedAsNotFound() {
        authenticate(7L, "owner");
        when(shipmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shipmentService.getDetail(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void declaresExactlyTheThirteenCanonicalStatuses() {
        assertThat(Arrays.stream(ShipmentStatus.values()).collect(java.util.stream.Collectors.toSet()))
                .isEqualTo(Set.of(ShipmentStatus.CREATED, ShipmentStatus.WAITING_FOR_PAYMENT,
                        ShipmentStatus.WAITING_FOR_HANDOVER, ShipmentStatus.PICKED_UP, ShipmentStatus.AT_HUB,
                        ShipmentStatus.IN_TRANSIT, ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.FAILED,
                        ShipmentStatus.DELIVERED, ShipmentStatus.RETURN_TO_SENDER, ShipmentStatus.RETURNED_TO_SENDER,
                        ShipmentStatus.CANCELLED, ShipmentStatus.CLOSED_EXCEPTION));
    }

    @Test
    void failedCreationDoesNotStageARealtimePublication() {
        User user = authenticate(7L, "owner");
        stubPolicy();
        when(userRepository.getReferenceById(7L)).thenReturn(user);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shipmentEventRepository.save(any(ShipmentEvent.class))).thenThrow(new IllegalStateException("persistence failed"));

        assertThatThrownBy(() -> shipmentService.create(request())).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(shipmentEventPublisher);
    }

    @Test
    void senderCancelsOwnWaitingShipmentThroughSharedCancellation() {
        User owner = authenticate(7L, "owner");
        Shipment shipment = waitingShipment(owner, ShipmentStatus.WAITING_FOR_HANDOVER);
        when(shipmentRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(shipment));
        when(userRepository.getReferenceById(7L)).thenReturn(owner);

        shipmentService.cancel(42L);

        verify(cancellation).cancel(shipment, "SENDER_REQUEST", owner);
    }

    @Test
    void senderCannotCancelAfterPickupOrSomeoneElsesShipment() {
        User owner = authenticate(7L, "owner");
        when(shipmentRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(waitingShipment(owner, ShipmentStatus.PICKED_UP)));
        assertThatThrownBy(() -> shipmentService.cancel(42L)).isInstanceOf(IllegalArgumentException.class);

        authenticate(8L, "intruder");
        assertThatThrownBy(() -> shipmentService.cancel(42L)).isInstanceOf(ShipmentAccessDeniedException.class);
        verifyNoInteractions(cancellation);
    }

    private Shipment waitingShipment(User owner, ShipmentStatus status) {
        Shipment shipment = Shipment.create("UTX-1", owner, "Owner", "0900000001", "a", "Receiver", "+84900000002", "b");
        shipment.applyIntake(null, "HCM", "Q1", "HOME_PICKUP", "SENDER", "NO_INSPECTION", 500, 1, 1, 1, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.TEN, "UNPAID", status, CREATED_AT);
        ReflectionTestUtils.setField(shipment, "id", 42L);
        return shipment;
    }

    private void stubPolicy() {
        PricingPolicy policy = mock(PricingPolicy.class);
        when(policy.getBasePrice()).thenReturn(new BigDecimal("20000"));
        when(policy.getBaseWeightG()).thenReturn(500);
        lenient().when(policy.getExtraWeightStepG()).thenReturn(500);
        lenient().when(policy.getExtraWeightPrice()).thenReturn(new BigDecimal("5000"));
        lenient().when(policy.getInsuranceRatePercent()).thenReturn(new BigDecimal("1"));
        lenient().when(policy.getCodFeePercent()).thenReturn(new BigDecimal("2"));
        when(pricingPolicyRepository.findFirstByServiceCodeAndFromRegionAndToRegionAndStatusOrderByIdDesc(
                "STANDARD", "SOUTH", "SOUTH", "ACTIVE")).thenReturn(Optional.of(policy));
    }

    @Test
    void missingOrAmbiguousDeliveryCoverageCannotCreateFinancialOrShipmentRecords() {
        authenticate(7L, "owner");
        stubPolicy();
        when(hubRepository.findByStatusAndTypeAndProvinceAndDistrict("ACTIVE", "DELIVERY_HUB", "HCM", "Q1"))
                .thenReturn(List.of(), List.of(mock(vn.iotstar.entity.Hub.class), mock(vn.iotstar.entity.Hub.class)));
        assertThatThrownBy(() -> shipmentService.create(request())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> shipmentService.create(request())).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(shipmentRepository, paymentRepository, shipmentEventRepository, shipmentEventPublisher);
    }

    @Test
    void realIntakeOutputFeedsSelfDropoffAndTerminalSortingWithoutFixtureRoutingFields() {
        User owner = authenticate(7L, "owner");
        stubPolicy();
        when(userRepository.getReferenceById(7L)).thenReturn(owner);
        var persisted = new java.util.concurrent.atomic.AtomicReference<Shipment>();
        var fee = new java.util.concurrent.atomic.AtomicReference<Payment>();
        when(shipmentRepository.save(any())).thenAnswer(inv -> { Shipment s = inv.getArgument(0); s.setId(42L); persisted.set(s); return s; });
        when(paymentRepository.save(any())).thenAnswer(inv -> { Payment p = inv.getArgument(0); fee.set(p); return p; });
        when(shipmentEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CreateShipmentRequest request = request(); request.setPaymentMethod("CASH");
        shipmentService.create(request);

        var operations = mock(vn.iotstar.repository.HubOperationsRepository.class);
        var hub = hubRepository.findByStatusAndTypeAndProvinceAndDistrict("ACTIVE", "DELIVERY_HUB", "HCM", "Q1").getFirst();
        User staff = new User(11L, "general", "hash", UserRole.ROLE_HUB_STAFF, "ACTIVE");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(staff, null, staff.getAuthorities()));
        when(operations.findActiveProfileByUserId(11L)).thenReturn(Optional.of(vn.iotstar.entity.EmployeeProfile.active(staff, hub, "GENERAL_STAFF")));
        when(operations.findShipmentByIdForUpdate(42L)).thenReturn(Optional.of(persisted.get()));
        when(userRepository.getReferenceById(11L)).thenReturn(staff);
        when(paymentRepository.findFirstByShipmentIdAndPaymentType(42L, "SHIPPING_FEE")).thenAnswer(inv -> Optional.of(fee.get()));
        var hubService = new HubOperationsServiceImpl(operations, shipmentEventRepository,
                mock(vn.iotstar.repository.RoutingExceptionRepository.class), userRepository,
                mock(org.springframework.context.ApplicationEventPublisher.class), paymentRepository, pricingPolicyRepository,
                cancellation, shipmentEventPublisher);
        hubService.receiveAtHub(new vn.iotstar.dto.hub.InboundShipmentCommand(42L, persisted.get().getTrackingNumber(),
                500, 10, 10, 10, new BigDecimal("20000"), null));
        assertThat(hubService.sortAtHub(new vn.iotstar.dto.hub.SortShipmentCommand(42L)).containerReady()).isFalse();
        assertThat(persisted.get().getDestinationHubId()).isEqualTo(persisted.get().getCurrentHubId());
        assertThat(persisted.get().getCurrentStatus()).isEqualTo(ShipmentStatus.AT_HUB);
        assertThat(fee.get().getStatus()).isEqualTo("PAID");
        verify(operations, never()).saveRoute(any());
    }

    private User authenticate(Long id, String username) {
        User user = new User(id, username, "hash", UserRole.ROLE_USER, "ACTIVE");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        return user;
    }

    private CreateShipmentRequest request() {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setSenderName("Owner");
        request.setSenderPhone("0900000001");
        request.setSenderAddress("1 Owner Street");
        request.setReceiverName("Receiver");
        request.setReceiverPhone("0900000002");
        request.setReceiverAddress("2 Receiver Street");
        request.setReceiverProvince("HCM");
        request.setReceiverDistrict("Q1");
        request.setFromRegion("SOUTH");
        request.setToRegion("SOUTH");
        request.setWeightGrams(500);
        request.setLengthCm(10);
        request.setWidthCm(10);
        request.setHeightCm(10);
        return request;
    }
}
