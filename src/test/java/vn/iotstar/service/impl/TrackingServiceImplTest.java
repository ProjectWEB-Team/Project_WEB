package vn.iotstar.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.dto.tracking.PublicTrackingResponse;
import vn.iotstar.dto.tracking.RecipientShipmentResponse;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.exception.ShipmentAccessDeniedException;
import vn.iotstar.repository.HubRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.ShipmentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingServiceImplTest {
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private ShipmentEventRepository eventRepository;
    @Mock private HubRepository hubRepository;
    @InjectMocks private TrackingServiceImpl trackingService;
    private final User sender = new User(7L, "sender", "hash", UserRole.ROLE_USER, "ACTIVE");
    private final User receiver = new User(8L, "receiver", "hash", UserRole.ROLE_USER, "ACTIVE");
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        shipment = Shipment.create("UTX-ABCDEF1234", sender, "Nguyen Van A", "0900000001", "1 Secret St", "Tran Thi B",
                "+84900000002", "2 Private Rd");
        shipment.applyIntake(receiver, "HCM", "Q1", "HOME_PICKUP", "RECEIVER", "NO_INSPECTION", 500, 1, 1, 1,
                BigDecimal.ZERO, new BigDecimal("100000"), new BigDecimal("25000"), "UNPAID",
                ShipmentStatus.WAITING_FOR_HANDOVER, LocalDateTime.of(2026, 10, 22, 9, 0));
        ReflectionTestUtils.setField(shipment, "id", 42L);
        lenient().when(eventRepository.findByShipmentIdOrderByCreatedAtAsc(42L)).thenReturn(List.of(
                ShipmentEvent.of(shipment, sender, "SHIPMENT_CREATED", "Shipment created"),
                ShipmentEvent.of(shipment, null, "PICKUP_SERVICE_FAILURE", "Internal staff note: truck broke")));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publicTrackingShowsOnlyAllowlistedLabels() {
        when(shipmentRepository.findByTrackingNumber("UTX-ABCDEF1234")).thenReturn(Optional.of(shipment));

        PublicTrackingResponse response = trackingService.track(" utx-abcdef1234 ");

        assertThat(response.events()).extracting("label").containsExactly("Đơn hàng đã được tạo");
        assertThat(response.toString()).doesNotContain("Secret", "Private", "0900000001", "truck");
    }

    @Test
    void unknownCodeIsNotFound() {
        when(shipmentRepository.findByTrackingNumber("NOPE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> trackingService.track("nope")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void recipientSeesMaskedUntilExactTrackingNumberUnlocks() {
        login(receiver);
        when(shipmentRepository.findById(42L)).thenReturn(Optional.of(shipment));

        RecipientShipmentResponse masked = trackingService.incomingDetail(42L, false);
        assertThat(masked.trackingNumber()).isEqualTo("****1234");
        assertThat(masked.senderName()).isEqualTo("N***");
        assertThat(masked.receiverAddress()).isNull();

        assertThat(trackingService.unlock(42L, "UTX-ABCDEF123")).isFalse();
        assertThat(trackingService.unlock(42L, "utx-abcdef1234")).isTrue();
        RecipientShipmentResponse full = trackingService.incomingDetail(42L, true);
        assertThat(full.receiverAddress()).isEqualTo("2 Private Rd");
        assertThat(full.amountToPay()).isEqualByComparingTo("125000");
    }

    @Test
    void otherUsersCannotReadOrUnlockSomeoneElsesIncomingShipment() {
        login(sender);
        when(shipmentRepository.findById(42L)).thenReturn(Optional.of(shipment));
        assertThatThrownBy(() -> trackingService.incomingDetail(42L, false)).isInstanceOf(ShipmentAccessDeniedException.class);
        assertThatThrownBy(() -> trackingService.unlock(42L, "UTX-ABCDEF1234")).isInstanceOf(ShipmentAccessDeniedException.class);
    }

    private static void login(User user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void journeyHistoryUsesPersistedOrderAndSafeLabelsForPublicAndRecipientViews() {
        when(shipmentRepository.findByTrackingNumber("UTX-ABCDEF1234")).thenReturn(Optional.of(shipment));
        when(shipmentRepository.findById(42L)).thenReturn(Optional.of(shipment));
        when(eventRepository.findByShipmentIdOrderByCreatedAtAsc(42L)).thenReturn(List.of(
                ShipmentEvent.of(shipment, sender, "ARRIVED_AT_HUB", "private address and COD 12345"),
                ShipmentEvent.of(shipment, sender, "LINEHAUL_TRIP_STARTED", "driver phone"),
                ShipmentEvent.of(shipment, sender, "OUT_FOR_DELIVERY", "private assignment"),
                ShipmentEvent.of(shipment, sender, "DELIVERED", "private recipient signature"),
                ShipmentEvent.of(shipment, sender, "MISSING_PARCEL", "internal incident"),
                ShipmentEvent.of(shipment, sender, "UNKNOWN_INTERNAL_NOTE", "sensitive investigation")));
        var publicView = trackingService.track("UTX-ABCDEF1234");
        assertThat(publicView.events()).hasSize(5);
        assertThat(publicView.events()).extracting("label").containsExactly(
                "Kiện hàng đã đến bưu cục", "Kiện hàng đang được vận chuyển", "Shipper đang giao hàng", "Giao hàng thành công",
                "Đơn hàng đang được kiểm tra hành trình");
        assertThat(publicView.toString()).doesNotContain("private", "12345", "phone", "incident", "sensitive", "UNKNOWN_INTERNAL_NOTE");
        login(receiver);
        assertThat(trackingService.incomingDetail(42L, false).events()).isEqualTo(publicView.events());
        assertThat(trackingService.incomingDetail(42L, true).events()).isEqualTo(publicView.events());
    }
}
