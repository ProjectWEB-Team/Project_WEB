package vn.iotstar.service;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.entity.*;
import vn.iotstar.repository.ManagerOperationsRepository;
import vn.iotstar.service.impl.ShipmentReturnServiceImpl;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShipmentReturnServiceImplTest {
    LastMileTestFixture f;ShipmentReturnServiceImpl service;ManagerOperationsRepository managers;
    final LocalDateTime now=LocalDateTime.of(2026,10,25,12,0);
    final User manager=new User(20L,"manager","hash",UserRole.ROLE_MANAGER,"ACTIVE");
    @BeforeEach void setup(){
        f=new LastMileTestFixture();f.returnAtOrigin();managers=mock(ManagerOperationsRepository.class);
        when(managers.findActiveProfileByUserId(20L)).thenReturn(Optional.of(EmployeeProfile.active(manager,f.hub,"MANAGER")));
        when(managers.findHubSubtreeIds(100L)).thenReturn(List.of(100L));
        service=new ShipmentReturnServiceImpl(f.shipments,f.events,f.users,f.config,f.assignments,managers,f.access,Clock.fixed(now.atZone(ZoneId.systemDefault()).toInstant(),ZoneOffset.UTC));
        LastMileTestFixture.login(manager);when(f.shipments.findByCurrentStatus(ShipmentStatus.RETURN_TO_SENDER)).thenReturn(List.of(f.shipment));
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    void arrived(LocalDateTime t){ReflectionTestUtils.setField(f.shipment,"returnArrivedAtOriginAt",t);}
    @Test void userCannotReceiveOrDispose(){LastMileTestFixture.login(f.sender);assertThatThrownBy(()->service.markReturnArrivedAtOriginHub(10L)).isInstanceOf(AccessDeniedException.class);assertThatThrownBy(()->service.disposeOverdueReturn(10L,"x")).isInstanceOf(AccessDeniedException.class);}
    @Test void realArrivalEventIsIdempotent(){arrived(now);service.markReturnArrivedAtOriginHub(10L);service.markReturnArrivedAtOriginHub(10L);assertThat(f.history).hasSize(1);assertThat(f.shipment.getReturnArrivedAtOriginAt()).isEqualTo(now);}
    @Test void wrongStatusCannotClaimOriginArrival(){f.shipment.transitionTo(ShipmentStatus.OUT_FOR_DELIVERY);assertThatThrownBy(()->service.markReturnArrivedAtOriginHub(10L)).isInstanceOf(IllegalStateException.class);}
    @Test void oldCreationWithoutReceiptNeverOverdue(){arrived(null);ReflectionTestUtils.setField(f.shipment,"createdAt",now.minusYears(1));assertThat(service.findOverdueReturnedShipments(now)).isEmpty();}
    @Test void actualOldArrivalIsOverdue(){arrived(now.minusDays(10));assertThat(service.findOverdueReturnedShipments(now)).containsExactly(f.shipment);}
    @Test void recentReceiptIsNotOverdue(){arrived(now.minusHours(2));assertThat(service.findOverdueReturnedShipments(now)).isEmpty();assertThatThrownBy(()->service.disposeOverdueReturn(10L,"Too early")).isInstanceOf(IllegalStateException.class);}
    @Test void configuredDemoGraceWorks(){arrived(now.minusMinutes(5));f.config.getTimeout().setDisposedGraceMinutes(2);assertThat(service.findOverdueReturnedShipments(now)).containsExactly(f.shipment);}
    @Test void overdueDisposalIsScopedAndIdempotent(){arrived(now.minusDays(10));service.disposeOverdueReturn(10L,"Uncollected");service.disposeOverdueReturn(10L,"Uncollected");assertThat(f.shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.CLOSED_EXCEPTION);assertThat(f.shipment.getClosureReason()).isEqualTo("DISPOSED");assertThat(f.history).hasSize(1);}
    @Test void longestAcceptedDisposalReasonFitsAuditColumnWithoutTruncation(){
        arrived(now.minusDays(10));String reason="R".repeat(255);
        service.disposeOverdueReturn(10L,reason);
        assertThat(f.history).hasSize(1);
        assertThat(f.history.getFirst().getDescription()).hasSize(255).isEqualTo(reason);
        assertThat(f.history.getFirst().getEventType()).isEqualTo("RETURN_DISPOSED");
    }
    @Test void crossHubHeldAndOutboundCannotDispose(){arrived(now.minusDays(10));when(managers.findHubSubtreeIds(100L)).thenReturn(List.of(999L));assertThatThrownBy(()->service.disposeOverdueReturn(10L,"x")).isInstanceOf(AccessDeniedException.class);when(managers.findHubSubtreeIds(100L)).thenReturn(List.of(100L));f.shipment.setOnHold(true);assertThatThrownBy(()->service.disposeOverdueReturn(10L,"x")).isInstanceOf(IllegalStateException.class);f.shipment.setOnHold(false);f.shipment.setCurrentHubId(null);assertThatThrownBy(()->service.disposeOverdueReturn(10L,"x")).isInstanceOf(IllegalStateException.class);}
    @Test void assignedOutboundTaskPreventsDisposal(){arrived(now.minusDays(10));f.tasks.add(ShipmentAssignment.create(f.shipment,f.shipper,"RETURN_DELIVERY"));assertThat(service.findOverdueReturnedShipments(now)).isEmpty();}
    @Test void sealedContainerCustodyPreventsDisposalEvenIfLocationIsOrigin(){arrived(now.minusDays(10));when(f.shipments.hasActiveContainerCustody(10L)).thenReturn(true);assertThat(service.findOverdueReturnedShipments(now)).isEmpty();assertThatThrownBy(()->service.disposeOverdueReturn(10L,"x")).isInstanceOf(IllegalStateException.class);}
    @Test void schedulerReportsWithoutDisposalOrAuthentication(){
        ShipmentReturnServiceImpl reporting=mock(ShipmentReturnServiceImpl.class);when(reporting.findOverdueReturnedShipments(now)).thenReturn(List.of(f.shipment));SecurityContextHolder.clearContext();
        new vn.iotstar.service.impl.ReturnGracePeriodScheduler(reporting,Clock.fixed(now.atZone(ZoneId.systemDefault()).toInstant(),ZoneOffset.UTC)).reportOverdueReturns();
        verify(reporting).findOverdueReturnedShipments(now);verify(reporting,never()).disposeOverdueReturn(anyLong(),anyString());
    }
}
