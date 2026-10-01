package vn.iotstar.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.dto.shipment.CreateShipmentRequest;
import vn.iotstar.dto.shipment.ShipmentDetailResponse;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.exception.ShipmentAccessDeniedException;
import vn.iotstar.realtime.ShipmentEventPublisher;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.impl.ShipmentServiceImpl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private ShipmentEventRepository shipmentEventRepository;
    @Mock private UserRepository userRepository;
    @Mock private ShipmentEventPublisher shipmentEventPublisher;
    @InjectMocks private ShipmentServiceImpl shipmentService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsCanonicalShipmentAndSeparateCreationEventForCurrentActor() {
        User user = authenticate(7L, "owner");
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
        assertThat(shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.CREATED);
        assertThat(shipment.getTrackingNumber()).startsWith("UTX-");
        assertThat(eventCaptor.getValue().getShipment()).isSameAs(shipment);
        assertThat(eventCaptor.getValue().getActor()).isSameAs(user);
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("SHIPMENT_CREATED");
        verify(shipmentEventPublisher).publishAfterCommit(any(), any());
        verifyNoMoreInteractions(shipmentRepository, shipmentEventRepository, userRepository, shipmentEventPublisher);
    }

    @Test
    void ownerReadsDetailAndChronologicalEvents() {
        User owner = authenticate(7L, "owner");
        Shipment shipment = Shipment.create("UTX-001", owner, "Owner", "0900000001", "A", "Receiver", "0900000002", "B");
        ReflectionTestUtils.setField(shipment, "id", 42L);
        ReflectionTestUtils.setField(shipment, "createdAt", LocalDateTime.of(2026, 10, 1, 10, 0));
        ShipmentEvent event = ShipmentEvent.shipmentCreated(shipment, owner);
        ReflectionTestUtils.setField(event, "createdAt", LocalDateTime.of(2026, 10, 1, 10, 1));
        when(shipmentRepository.findById(42L)).thenReturn(Optional.of(shipment));
        when(shipmentEventRepository.findByShipmentIdOrderByCreatedAtAsc(42L)).thenReturn(List.of(event));

        ShipmentDetailResponse detail = shipmentService.getDetail(42L);

        assertThat(detail.id()).isEqualTo(42L);
        assertThat(detail.currentStatus()).isEqualTo(ShipmentStatus.CREATED);
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
        when(userRepository.getReferenceById(7L)).thenReturn(user);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shipmentEventRepository.save(any(ShipmentEvent.class))).thenThrow(new IllegalStateException("persistence failed"));

        assertThatThrownBy(() -> shipmentService.create(request())).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(shipmentEventPublisher);
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
        return request;
    }
}
