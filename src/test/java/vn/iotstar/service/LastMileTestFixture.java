package vn.iotstar.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.config.LogisticsProperties;
import vn.iotstar.entity.*;
import vn.iotstar.repository.*;
import vn.iotstar.realtime.*;
import vn.iotstar.service.impl.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** In-memory repository seam; transitions, assignment production and settlement use real services. */
class LastMileTestFixture {
    final ShipmentRepository shipments=mock(ShipmentRepository.class);
    final ShipmentAssignmentRepository assignments=mock(ShipmentAssignmentRepository.class);
    final LastMileDeliveryRepository attempts=mock(LastMileDeliveryRepository.class);
    final CodTransactionRepository cods=mock(CodTransactionRepository.class);
    final PaymentRepository payments=mock(PaymentRepository.class);
    final ShipmentEventRepository events=mock(ShipmentEventRepository.class);
    final UserRepository users=mock(UserRepository.class);
    final EmployeeProfileRepository profiles=mock(EmployeeProfileRepository.class);
    final WalletLedgerService wallet=mock(WalletLedgerService.class);
    final LogisticsProperties config=new LogisticsProperties();
    final User sender=new User(1L,"sender","hash",UserRole.ROLE_USER,"ACTIVE");
    final User shipper=new User(2L,"shipper","hash",UserRole.ROLE_SHIPPER,"ACTIVE");
    final User inbound=new User(3L,"inbound","hash",UserRole.ROLE_HUB_STAFF,"ACTIVE");
    final User coordinator=new User(4L,"coordinator","hash",UserRole.ROLE_HUB_STAFF,"ACTIVE");
    final Hub hub=Hub.active("H","Hub",1,"DELIVERY_HUB","SOUTH","P");
    final Shipment shipment=Shipment.create("TRACK",sender,"Sender","0900000001","A","Receiver","0900000002","B");
    final List<ShipmentAssignment> tasks=new ArrayList<>();
    final Map<String,Payment> money=new HashMap<>();
    final List<ShipmentEvent> history=new ArrayList<>();
    final List<DeliveryAttempt> deliveries=new ArrayList<>();
    final MutableClock clock=new MutableClock();
    CodTransaction cod;
    final LastMileAccess access=new LastMileAccess(profiles,attempts,clock);
    final ShipmentFeeService fees=new ShipmentFeeService(shipments,payments,assignments,events,users,wallet,access);
    final LastMileDeliveryServiceImpl service=new LastMileDeliveryServiceImpl(shipments,assignments,attempts,cods,payments,events,users,
            mock(ApplicationEventPublisher.class),mock(NotificationEventPublisher.class),wallet,config,mock(StorageService.class),access,fees);
    final LastMileAssignmentService dispatch=new LastMileAssignmentService(access,shipments,assignments,events,profiles,mock(ShipmentEventPublisher.class));
    LastMileTestFixture(){
        ReflectionTestUtils.setField(hub,"id",100L);shipment.setId(10L);shipment.setOriginHubId(100L);shipment.setDestinationHubId(100L);
        shipment.setShippingFee(new BigDecimal("35000"));shipment.setCodAmount(BigDecimal.ZERO);shipment.setFeePayer("SENDER");shipment.receiveAt(hub);
        Payment original=Payment.create(shipment,"SENDER","SHIPPING_FEE",shipment.getShippingFee(),"CASH","UNPAID");original.collect("CASH",3L,"ORIGINAL");money.put("SHIPPING_FEE",original);
        when(profiles.findFirstByUserId(2L)).thenReturn(Optional.of(EmployeeProfile.active(shipper,hub,"SHIPPER")));
        when(profiles.findFirstByUserId(3L)).thenReturn(Optional.of(EmployeeProfile.active(inbound,hub,"INBOUND")));
        when(profiles.findFirstByUserId(4L)).thenReturn(Optional.of(EmployeeProfile.active(coordinator,hub,"COORDINATOR")));
        when(shipments.findByIdForUpdate(10L)).thenReturn(Optional.of(shipment));when(shipments.findById(10L)).thenReturn(Optional.of(shipment));
        when(shipments.save(any())).thenAnswer(i->i.getArgument(0));
        when(cods.findByShipmentId(10L)).thenAnswer(i->Optional.ofNullable(cod));
        when(cods.findByShipmentIdForUpdate(10L)).thenAnswer(i->Optional.ofNullable(cod));
        when(cods.save(any())).thenAnswer(i->{cod=i.getArgument(0);return cod;});
        when(attempts.save(any())).thenAnswer(i->{DeliveryAttempt a=i.getArgument(0);deliveries.add(a);return a;});
        when(attempts.findTopByShipmentIdOrderByAttemptNumberDesc(10L)).thenAnswer(i->deliveries.isEmpty()?Optional.empty():Optional.of(deliveries.getLast()));
        when(attempts.countByShipmentId(10L)).thenAnswer(i->(long)deliveries.size());
        when(attempts.countByShipmentIdAndAttemptDate(eq(10L),any())).thenAnswer(i->deliveries.stream().filter(a->a.getAttemptDate().equals(i.getArgument(1))).count());
        when(users.findById(anyLong())).thenAnswer(i->Optional.of(user(i.getArgument(0))));when(users.getReferenceById(anyLong())).thenAnswer(i->user(i.getArgument(0)));
        when(assignments.findByShipmentIdOrderByAssignedAtDesc(10L)).thenAnswer(i->tasks.reversed());
        when(assignments.findFirstByShipmentIdAndAssigneeIdAndStatus(eq(10L),anyLong(),anyString())).thenAnswer(i->tasks.reversed().stream().filter(a->a.getAssignee().getId().equals(i.getArgument(1))&&a.getStatus().equals(i.getArgument(2))).findFirst());
        when(assignments.existsByShipmentIdAndStatusIn(eq(10L),anyList())).thenAnswer(i->tasks.stream().anyMatch(a->((List<?>)i.getArgument(1)).contains(a.getStatus())));
        when(assignments.save(any())).thenAnswer(i->{ShipmentAssignment a=i.getArgument(0);if(!tasks.contains(a))tasks.add(a);return a;});
        when(payments.findFirstByShipmentIdAndPaymentType(eq(10L),anyString())).thenAnswer(i->Optional.ofNullable(money.get(i.getArgument(1))));
        when(payments.findFirstByShipmentIdAndPayerTypeAndPaymentType(eq(10L),anyString(),anyString())).thenAnswer(i->Optional.ofNullable(money.get(i.getArgument(2))).filter(p->p.getPayerType().equals(i.getArgument(1))));
        when(payments.findFirstByShipmentIdAndPaymentTypeAndStatus(eq(10L),anyString(),anyString())).thenAnswer(i->Optional.ofNullable(money.get(i.getArgument(1))).filter(p->p.getStatus().equals(i.getArgument(2))));
        when(payments.save(any())).thenAnswer(i->{Payment p=i.getArgument(0);money.put(p.getPaymentType(),p);return p;});
        when(events.save(any())).thenAnswer(i->{ShipmentEvent e=i.getArgument(0);history.add(e);return e;});
        when(events.existsByShipmentIdAndEventType(eq(10L),anyString())).thenAnswer(i->history.stream().anyMatch(e->e.getEventType().equals(i.getArgument(1))));
    }
    User user(Long id){return id==1?sender:id==2?shipper:id==3?inbound:coordinator;}
    static void login(User u){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u,null,u.getAuthorities()));}
    void outbound(){login(coordinator);dispatch.assign(new vn.iotstar.dto.delivery.DeliveryAssignmentRequest(10L,2L));login(shipper);service.scanForDelivery(new vn.iotstar.dto.delivery.DeliveryScanRequest(10L,100L));}
    void returnAtOrigin(){shipment.setDirection("RETURN");shipment.setReturnFee(new BigDecimal("25000"));shipment.transitionTo(ShipmentStatus.RETURN_TO_SENDER);shipment.receiveAt(hub);}
    static class MutableClock extends java.time.Clock {
        java.time.Instant instant=java.time.Instant.parse("2026-10-25T10:00:00Z");
        public java.time.ZoneId getZone(){return java.time.ZoneOffset.UTC;}
        public java.time.Clock withZone(java.time.ZoneId zone){return java.time.Clock.fixed(instant,zone);}
        public java.time.Instant instant(){return instant;}
        void advanceDays(long days){instant=instant.plus(java.time.Duration.ofDays(days));}
    }
}
