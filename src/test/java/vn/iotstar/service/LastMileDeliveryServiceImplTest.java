package vn.iotstar.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.iotstar.dto.delivery.DeliveryCollectionRequest;
import vn.iotstar.dto.delivery.DeliveryScanRequest;
import vn.iotstar.dto.delivery.DeliveryTaskResponse;
import vn.iotstar.entity.CodTransaction;
import vn.iotstar.entity.DeliveryAttempt;
import vn.iotstar.entity.Payment;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentAssignment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.realtime.NotificationEventPublisher;
import vn.iotstar.repository.CodTransactionRepository;
import vn.iotstar.repository.LastMileDeliveryRepository;
import vn.iotstar.repository.PaymentRepository;
import vn.iotstar.repository.ShipmentAssignmentRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.impl.LastMileDeliveryServiceImpl;
import vn.iotstar.config.LogisticsProperties;
import vn.iotstar.service.StorageService;
import vn.iotstar.service.WalletLedgerService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LastMileDeliveryServiceImplTest {

    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private ShipmentAssignmentRepository shipmentAssignmentRepository;
    @Mock
    private LastMileDeliveryRepository lastMileDeliveryRepository;
    @Mock
    private CodTransactionRepository codTransactionRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ShipmentEventRepository shipmentEventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private NotificationEventPublisher notificationEventPublisher;
    @Mock
    private WalletLedgerService walletLedgerService;
    @Mock
    private StorageService storageService;

    private final LogisticsProperties logisticsProperties = new LogisticsProperties();

    private LastMileDeliveryServiceImpl deliveryService;

    private User shipper;
    private User sender;
    private Shipment shipment;
    private ShipmentAssignment assignment;

    @BeforeEach
    void setUp() {
        deliveryService = new LastMileDeliveryServiceImpl(
                shipmentRepository,
                shipmentAssignmentRepository,
                lastMileDeliveryRepository,
                codTransactionRepository,
                paymentRepository,
                shipmentEventRepository,
                userRepository,
                eventPublisher,
                notificationEventPublisher,
                walletLedgerService,
                logisticsProperties,
                storageService
        );

        shipper = new User(10L, "shipper_test", "hash", UserRole.ROLE_SHIPPER, "ACTIVE");
        sender = new User(1L, "sender_test", "hash", UserRole.ROLE_USER, "ACTIVE");

        shipment = Shipment.create("TRK123456", sender, "Sender", "0901", "Address 1", "Receiver", "0902", "Address 2");
        shipment.setId(1L);
        shipment.transitionTo(ShipmentStatus.AT_HUB);
        shipment.setCurrentHubId(100L);
        shipment.setDestinationHubId(100L);
        shipment.setCodAmount(new BigDecimal("200000.00"));
        shipment.setShippingFee(new BigDecimal("30000.00"));
        shipment.setFeePayer("RECEIVER");

        assignment = ShipmentAssignment.create(shipment, shipper, "DELIVERY");

        // Set security context
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(shipper, null, shipper.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Role check: Non-shipper role is denied scanning")
    void nonShipperRoleIsDenied() {
        User user = new User(2L, "user_regular", "hash", UserRole.ROLE_USER, "ACTIVE");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));

        DeliveryScanRequest request = new DeliveryScanRequest(1L, 100L);
        assertThatThrownBy(() -> deliveryService.scanForDelivery(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ROLE_SHIPPER required");
    }

    @Test
    @DisplayName("Scan delivery: AT_HUB transitions to OUT_FOR_DELIVERY with event audit")
    void scanDeliverySuccess() {
        when(shipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(shipment));
        when(userRepository.findById(10L)).thenReturn(Optional.of(shipper));
        when(shipmentAssignmentRepository.findByShipmentIdOrderByAssignedAtDesc(any())).thenReturn(List.of(assignment));
        when(codTransactionRepository.findByShipmentId(any())).thenReturn(Optional.empty());
        when(paymentRepository.findFirstByShipmentIdAndPayerTypeAndPaymentType(any(), any(), any())).thenReturn(Optional.empty());

        DeliveryScanRequest request = new DeliveryScanRequest(1L, 100L);
        DeliveryTaskResponse response = deliveryService.scanForDelivery(request);

        assertThat(response.getCurrentStatus()).isEqualTo("OUT_FOR_DELIVERY");
        assertThat(shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.OUT_FOR_DELIVERY);
        assertThat(assignment.getStatus()).isEqualTo("IN_PROGRESS");
        verify(shipmentEventRepository).save(any(ShipmentEvent.class));
    }

    @Test
    @DisplayName("Scan delivery: Fails if shipment is at a different hub")
    void scanDeliveryWrongHubFails() {
        when(shipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(shipment));
        when(userRepository.findById(10L)).thenReturn(Optional.of(shipper));
        when(shipmentAssignmentRepository.findByShipmentIdOrderByAssignedAtDesc(any())).thenReturn(List.of(assignment));

        DeliveryScanRequest request = new DeliveryScanRequest(1L, 999L); // Wrong hub ID
        assertThatThrownBy(() -> deliveryService.scanForDelivery(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not at the scanning hub");
    }

    @Test
    @DisplayName("Collect COD and receiver fee: Creates distinct records and events")
    void recordCollectionSuccess() {
        shipment.transitionTo(ShipmentStatus.OUT_FOR_DELIVERY);
        when(shipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(shipment));
        when(userRepository.findById(10L)).thenReturn(Optional.of(shipper));
        when(shipmentAssignmentRepository.findFirstByShipmentIdAndAssigneeIdAndStatus(any(), any(), eq("IN_PROGRESS")))
                .thenReturn(Optional.of(assignment));
        when(codTransactionRepository.findByShipmentIdForUpdate(any())).thenReturn(Optional.empty());
        when(paymentRepository.findFirstByShipmentIdAndPayerTypeAndPaymentType(any(), any(), any())).thenReturn(Optional.empty());

        DeliveryCollectionRequest request = new DeliveryCollectionRequest(1L, new BigDecimal("200000.00"), new BigDecimal("30000.00"), "CASH");
        deliveryService.recordCollection(request);

        verify(codTransactionRepository).save(any(CodTransaction.class));
        verify(paymentRepository).save(any(Payment.class));
        verify(shipmentEventRepository, times(2)).save(any(ShipmentEvent.class));
    }

    @Test
    @DisplayName("Confirm delivery: Fails if COD obligation is uncollected")
    void confirmDeliveryFailsWhenCodUncollected() {
        shipment.transitionTo(ShipmentStatus.OUT_FOR_DELIVERY);
        when(shipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(shipment));
        when(userRepository.findById(10L)).thenReturn(Optional.of(shipper));
        when(shipmentAssignmentRepository.findByShipmentIdOrderByAssignedAtDesc(any())).thenReturn(List.of(assignment));
        when(codTransactionRepository.findByShipmentId(any())).thenReturn(Optional.empty()); // COD not collected

        assertThatThrownBy(() -> deliveryService.confirmDelivery(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COD collection required before delivery confirmation");
    }

    @Test
    @DisplayName("Confirm delivery: Succeeds after collections, records attempt and event")
    void confirmDeliverySuccess() {
        shipment.transitionTo(ShipmentStatus.OUT_FOR_DELIVERY);
        CodTransaction codTx = CodTransaction.createCollected(shipment, shipper, new BigDecimal("200000.00"), "CASH");
        Payment feePayment = Payment.create(shipment, "RECEIVER", "SHIPPING_FEE", new BigDecimal("30000.00"), "CASH", "PAID");

        when(shipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(shipment));
        when(userRepository.findById(10L)).thenReturn(Optional.of(shipper));
        when(shipmentAssignmentRepository.findByShipmentIdOrderByAssignedAtDesc(any())).thenReturn(List.of(assignment));
        when(codTransactionRepository.findByShipmentId(any())).thenReturn(Optional.of(codTx));
        when(paymentRepository.findFirstByShipmentIdAndPayerTypeAndPaymentType(any(), any(), any())).thenReturn(Optional.of(feePayment));
        when(lastMileDeliveryRepository.countByShipmentId(any())).thenReturn(0L);

        DeliveryTaskResponse response = deliveryService.confirmDelivery(1L);

        assertThat(shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(assignment.getStatus()).isEqualTo("COMPLETED");
        verify(lastMileDeliveryRepository).save(any(DeliveryAttempt.class));
        verify(shipmentEventRepository).save(any(ShipmentEvent.class));
    }
}
