package vn.iotstar.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.config.LogisticsProperties;
import vn.iotstar.dto.hub.InboundShipmentCommand;
import vn.iotstar.dto.hub.SortShipmentCommand;
import vn.iotstar.dto.hub.SortingDecisionResponse;
import vn.iotstar.dto.hub.RouteDiagnosticResponse;
import vn.iotstar.dto.realtime.OperationalActionNotification;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.Hub;
import vn.iotstar.entity.HubRoutingRule;
import vn.iotstar.entity.Handover;
import vn.iotstar.entity.ParcelContainer;
import vn.iotstar.entity.ContainerItem;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.RoutingExceptionRecord;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.HubOperationAccessDeniedException;
import vn.iotstar.exception.RouteConfigurationException;
import vn.iotstar.repository.HubOperationsRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.RoutingExceptionRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.impl.HubOperationsServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HubRoutingServiceImplTest {
    @Mock private HubOperationsRepository hubOperationsRepository;
    @Mock private ShipmentEventRepository shipmentEventRepository;
    @Mock private RoutingExceptionRepository routingExceptionRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    private final LogisticsProperties logisticsProperties = new LogisticsProperties();
    private HubOperationsServiceImpl service;

    @BeforeEach
    void createService() {
        service = new HubOperationsServiceImpl(hubOperationsRepository, shipmentEventRepository, routingExceptionRepository,
                userRepository, applicationEventPublisher, null, null, null, null, logisticsProperties);
    }

    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void inboundAtOwnHubSetsAtHubAndRecordsArrivalEvent() {
        Fixture fixture = fixture();
        authenticate(fixture.staff());
        when(hubOperationsRepository.findActiveProfileByUserId(7L)).thenReturn(Optional.of(fixture.inboundProfile()));
        when(hubOperationsRepository.findShipmentByIdForUpdate(101L)).thenReturn(Optional.of(fixture.shipment()));
        when(userRepository.getReferenceById(7L)).thenReturn(fixture.staff());

        service.receiveAtHub(InboundShipmentCommand.pickupReceipt(101L));

        assertThat(fixture.shipment().getCurrentStatus()).isEqualTo(ShipmentStatus.AT_HUB);
        assertThat(fixture.shipment().getCurrentHub()).isEqualTo(fixture.currentHub());
        ArgumentCaptor<ShipmentEvent> event = ArgumentCaptor.forClass(ShipmentEvent.class);
        verify(shipmentEventRepository).save(event.capture());
        assertThat(event.getValue().getEventType()).isEqualTo("ARRIVED_AT_HUB");
        assertThat(event.getValue().getHub()).isEqualTo(fixture.currentHub());
    }

    @Test
    void genericInboundCannotReceiveInTransitShipmentEvenAtTheExpectedDestinationHub() {
        Fixture fixture = fixture();
        ReflectionTestUtils.setField(fixture.shipment(), "currentStatus", ShipmentStatus.IN_TRANSIT);
        authenticate(fixture.staff());
        when(hubOperationsRepository.findActiveProfileByUserId(7L)).thenReturn(Optional.of(fixture.inboundProfile()));
        when(hubOperationsRepository.findShipmentByIdForUpdate(101L)).thenReturn(Optional.of(fixture.shipment()));

        assertThatThrownBy(() -> service.receiveAtHub(InboundShipmentCommand.pickupReceipt(101L)))
                .isInstanceOf(HubOperationAccessDeniedException.class);

        verifyNoInteractions(shipmentEventRepository);
    }

    @Test
    void genericInboundCannotBypassMissingItemDiscrepancyReceipt() {
        Fixture fixture = fixture();
        User driver = new User(10L, "driver", "hash", UserRole.ROLE_LINEHAUL_DRIVER, "ACTIVE");
        ParcelContainer container = ParcelContainer.open("BAG-MISSING", fixture.currentHub(), fixture.currentHub(), fixture.staff());
        ContainerItem missingItem = ContainerItem.scanned(container, fixture.shipment());
        missingItem.markMissing(fixture.staff());
        Handover discrepancy = Handover.pending(fixture.currentHub(), fixture.currentHub(), fixture.inboundProfile(), driver, 1, null);
        discrepancy.recordContainerScan();
        discrepancy.depart();
        discrepancy.completeDestinationReceipt(true);
        ReflectionTestUtils.setField(fixture.shipment(), "currentStatus", ShipmentStatus.IN_TRANSIT);
        authenticate(fixture.staff());
        when(hubOperationsRepository.findActiveProfileByUserId(7L)).thenReturn(Optional.of(fixture.inboundProfile()));
        when(hubOperationsRepository.findShipmentByIdForUpdate(101L)).thenReturn(Optional.of(fixture.shipment()));

        assertThat(missingItem.isMissing()).isTrue();
        assertThat(discrepancy.getStatus()).isEqualTo("COMPLETED_WITH_DISCREPANCY");
        assertThatThrownBy(() -> service.receiveAtHub(InboundShipmentCommand.pickupReceipt(101L)))
                .isInstanceOf(HubOperationAccessDeniedException.class);

        assertThat(fixture.shipment().getCurrentStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        verifyNoInteractions(shipmentEventRepository);
    }

    @Test
    void inboundCannotReceiveInitialIntakeAssignedToAnotherHub() {
        Fixture fixture = fixture();
        Hub assignedOrigin = Hub.active("HUB-ORIGIN", "Origin Hub", 1, "DELIVERY_HUB", "SOUTH", "Can Tho");
        ReflectionTestUtils.setField(assignedOrigin, "id", 45L);
        ReflectionTestUtils.setField(fixture.shipment(), "currentHub", assignedOrigin);
        authenticate(fixture.staff());
        when(hubOperationsRepository.findActiveProfileByUserId(7L)).thenReturn(Optional.of(fixture.inboundProfile()));
        when(hubOperationsRepository.findShipmentByIdForUpdate(101L)).thenReturn(Optional.of(fixture.shipment()));

        assertThatThrownBy(() -> service.receiveAtHub(InboundShipmentCommand.pickupReceipt(101L)))
                .isInstanceOf(HubOperationAccessDeniedException.class);

        verifyNoInteractions(shipmentEventRepository);
    }

    @Test
    void sorterDerivesOneStaticNextHopAndRepeatReturnsExistingDecision() {
        Fixture fixture = fixture();
        ReflectionTestUtils.setField(fixture.shipment(), "currentStatus", ShipmentStatus.AT_HUB);
        ReflectionTestUtils.setField(fixture.shipment(), "currentHub", fixture.currentHub());
        authenticate(fixture.staff());
        when(hubOperationsRepository.findActiveProfileByUserId(7L)).thenReturn(Optional.of(fixture.sorterProfile()));
        when(hubOperationsRepository.findShipmentByIdForUpdate(101L)).thenReturn(Optional.of(fixture.shipment()));
        when(hubOperationsRepository.findByRouteDecisionKey("shipment:101:hub:11:FORWARD"))
                .thenReturn(Optional.empty(), Optional.of(fixture.route()));
        when(hubOperationsRepository.findActiveRules(11L, "SOUTH", "Can Tho")).thenReturn(List.of(fixture.rule()));
        when(userRepository.getReferenceById(7L)).thenReturn(fixture.staff());
        when(hubOperationsRepository.saveRoute(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SortingDecisionResponse first = service.sortAtHub(new SortShipmentCommand(101L));
        SortingDecisionResponse repeated = service.sortAtHub(new SortShipmentCommand(101L));

        assertThat(first.nextHubCode()).isEqualTo("SOC-LA");
        assertThat(first.reused()).isFalse();
        assertThat(repeated.reused()).isTrue();
        verify(hubOperationsRepository, times(1)).saveRoute(any());
        verify(shipmentEventRepository, times(1)).save(any());
    }

    @Test
    void unmatchedStaticRuleCreatesOneDurableExceptionAndSafeOperationalEvidence() {
        Fixture fixture = fixture();
        ReflectionTestUtils.setField(fixture.shipment(), "currentStatus", ShipmentStatus.AT_HUB);
        ReflectionTestUtils.setField(fixture.shipment(), "currentHub", fixture.currentHub());
        authenticate(fixture.staff());
        when(hubOperationsRepository.findActiveProfileByUserId(7L)).thenReturn(Optional.of(fixture.sorterProfile()));
        when(hubOperationsRepository.findShipmentByIdForUpdate(101L)).thenReturn(Optional.of(fixture.shipment()));
        when(hubOperationsRepository.findByRouteDecisionKey("shipment:101:hub:11:FORWARD")).thenReturn(Optional.empty());
        when(hubOperationsRepository.findActiveRules(11L, "SOUTH", "Can Tho")).thenReturn(List.of());
        RoutingExceptionRecord existing = RoutingExceptionRecord.openRouteLookupBlocked(fixture.shipment(), fixture.currentHub(), fixture.staff());
        when(routingExceptionRepository.findOpenRouteLookupBlockedForUpdate(101L, 11L))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(routingExceptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.getReferenceById(7L)).thenReturn(fixture.staff());

        assertThatThrownBy(() -> service.sortAtHub(new SortShipmentCommand(101L)))
                .isInstanceOf(RouteConfigurationException.class)
                .hasMessage("Sorting is temporarily unavailable");
        assertThatThrownBy(() -> service.sortAtHub(new SortShipmentCommand(101L)))
                .isInstanceOf(RouteConfigurationException.class);

        verify(hubOperationsRepository, never()).saveRoute(any());
        ArgumentCaptor<ShipmentEvent> event = ArgumentCaptor.forClass(ShipmentEvent.class);
        verify(shipmentEventRepository).save(event.capture());
        assertThat(event.getValue().getEventType()).isEqualTo("ROUTE_LOOKUP_BLOCKED");
        verify(routingExceptionRepository).save(any(RoutingExceptionRecord.class));
        ArgumentCaptor<OperationalActionNotification> notification = ArgumentCaptor.forClass(OperationalActionNotification.class);
        verify(applicationEventPublisher).publishEvent(notification.capture());
        assertThat(notification.getValue().type()).isEqualTo("BLOCKED_ROUTE");
        assertThat(notification.getValue().currentHubId()).isEqualTo(11L);
        assertThat(notification.getValue().diagnosticReference()).startsWith("routing-exception:");
    }

    @Test
    void successfulStaticRetryResolvesTheOpenBlockedRouteException() {
        Fixture fixture = fixture();
        ReflectionTestUtils.setField(fixture.shipment(), "currentStatus", ShipmentStatus.AT_HUB);
        ReflectionTestUtils.setField(fixture.shipment(), "currentHub", fixture.currentHub());
        RoutingExceptionRecord open = RoutingExceptionRecord.openRouteLookupBlocked(fixture.shipment(), fixture.currentHub(), fixture.staff());
        authenticate(fixture.staff());
        when(hubOperationsRepository.findActiveProfileByUserId(7L)).thenReturn(Optional.of(fixture.sorterProfile()));
        when(hubOperationsRepository.findShipmentByIdForUpdate(101L)).thenReturn(Optional.of(fixture.shipment()));
        when(hubOperationsRepository.findByRouteDecisionKey("shipment:101:hub:11:FORWARD")).thenReturn(Optional.empty());
        when(hubOperationsRepository.findActiveRules(11L, "SOUTH", "Can Tho")).thenReturn(List.of(fixture.rule()));
        when(hubOperationsRepository.saveRoute(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(routingExceptionRepository.findOpenRouteLookupBlockedForUpdate(101L, 11L)).thenReturn(Optional.of(open));
        when(userRepository.getReferenceById(7L)).thenReturn(fixture.staff());

        service.sortAtHub(new SortShipmentCommand(101L));

        assertThat(open.getStatus()).isEqualTo("RESOLVED");
        assertThat(open.getResolvedBy()).isSameAs(fixture.staff());
    }

    @Test
    void sorterCannotOperateShipmentOutsideTheirCurrentHub() {
        Fixture fixture = fixture();
        Hub differentHub = Hub.active("HUB-OTHER", "Other Hub", 1, "DELIVERY_HUB", "SOUTH", "Can Tho");
        ReflectionTestUtils.setField(fixture.shipment(), "currentStatus", ShipmentStatus.AT_HUB);
        ReflectionTestUtils.setField(fixture.shipment(), "currentHub", differentHub);
        authenticate(fixture.staff());
        when(hubOperationsRepository.findActiveProfileByUserId(7L)).thenReturn(Optional.of(fixture.sorterProfile()));
        when(hubOperationsRepository.findShipmentByIdForUpdate(101L)).thenReturn(Optional.of(fixture.shipment()));

        assertThatThrownBy(() -> service.sortAtHub(new SortShipmentCommand(101L)))
                .isInstanceOf(HubOperationAccessDeniedException.class);
        verify(hubOperationsRepository, never()).findActiveRules(anyLong(), anyString(), anyString());
    }

    @Test
    void scopedManagerCanReadConfigurationDiagnosisButHubStaffCannot() {
        Fixture fixture = fixture();
        ReflectionTestUtils.setField(fixture.shipment(), "currentHub", fixture.currentHub());
        when(hubOperationsRepository.findShipmentByIdWithHub(101L)).thenReturn(Optional.of(fixture.shipment()));
        when(hubOperationsRepository.findActiveRules(11L, "SOUTH", "Can Tho")).thenReturn(List.of());
        User manager = new User(9L, "manager", "hash", UserRole.ROLE_MANAGER, "ACTIVE");
        EmployeeProfile managerProfile = EmployeeProfile.active(manager, fixture.currentHub(), null);
        authenticate(manager);
        when(hubOperationsRepository.findActiveProfileByUserId(9L)).thenReturn(Optional.of(managerProfile));
        when(hubOperationsRepository.isHubWithinSubtree(11L, 11L)).thenReturn(1);

        RouteDiagnosticResponse diagnostic = service.getRouteDiagnostic(101L);
        assertThat(diagnostic.diagnosticCode()).isEqualTo("NO_ACTIVE_STATIC_RULE");

        authenticate(fixture.staff());
        assertThatThrownBy(() -> service.getRouteDiagnostic(101L))
                .isInstanceOf(HubOperationAccessDeniedException.class);
    }

    @Test
    void managerOutsideTheCurrentHubSubtreeCannotReadConfigurationDiagnosis() {
        Fixture fixture = fixture();
        ReflectionTestUtils.setField(fixture.shipment(), "currentHub", fixture.currentHub());
        User manager = new User(9L, "manager", "hash", UserRole.ROLE_MANAGER, "ACTIVE");
        Hub otherHub = Hub.active("HUB-OTHER", "Other Hub", 1, "DELIVERY_HUB", "NORTH", "Ha Noi");
        ReflectionTestUtils.setField(otherHub, "id", 19L);
        EmployeeProfile managerProfile = EmployeeProfile.active(manager, otherHub, null);
        authenticate(manager);
        when(hubOperationsRepository.findShipmentByIdWithHub(101L)).thenReturn(Optional.of(fixture.shipment()));
        when(hubOperationsRepository.findActiveProfileByUserId(9L)).thenReturn(Optional.of(managerProfile));
        when(hubOperationsRepository.isHubWithinSubtree(19L, 11L)).thenReturn(0);

        assertThatThrownBy(() -> service.getRouteDiagnostic(101L))
                .isInstanceOf(HubOperationAccessDeniedException.class);
        verify(hubOperationsRepository, never()).findActiveRules(anyLong(), anyString(), anyString());
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void destinationHubStopsLinehaulRoutingWithoutCreatingAnotherRoute() {
        Fixture f = fixture();
        f.shipment().receiveAt(f.currentHub());
        f.shipment().setDestinationHubId(f.currentHub().getId());
        authenticate(f.staff());
        when(hubOperationsRepository.findActiveProfileByUserId(7L)).thenReturn(Optional.of(f.sorterProfile()));
        when(hubOperationsRepository.findShipmentByIdForUpdate(101L)).thenReturn(Optional.of(f.shipment()));
        assertThat(service.sortAtHub(new SortShipmentCommand(101L)).containerReady()).isFalse();
        assertThat(service.sortAtHub(new SortShipmentCommand(101L)).nextHubCode()).isEqualTo("HUB-A");
        verify(hubOperationsRepository, never()).findActiveRules(anyLong(), anyString(), anyString());
        verify(hubOperationsRepository, never()).saveRoute(any());
        verifyNoInteractions(shipmentEventRepository);
    }

    @Test void returnUsesRealOriginGeographyAndDirectionSpecificDecision() {
        Fixture f=fixture();f.shipment().receiveAt(f.currentHub());f.shipment().setDirection("RETURN");
        f.shipment().transitionTo(ShipmentStatus.RETURN_TO_SENDER);f.shipment().setOriginHubId(99L);
        Hub origin=Hub.active("ORIGIN","Origin",1,"DELIVERY_HUB","NORTH","Ha Noi");ReflectionTestUtils.setField(origin,"id",99L);
        authenticate(f.staff());
        when(hubOperationsRepository.findActiveProfileByUserId(7L)).thenReturn(Optional.of(f.sorterProfile()));
        when(hubOperationsRepository.findShipmentByIdForUpdate(101L)).thenReturn(Optional.of(f.shipment()));
        when(hubOperationsRepository.findActiveHub(99L)).thenReturn(Optional.of(origin));
        when(hubOperationsRepository.findActiveRules(11L,"NORTH","Ha Noi")).thenReturn(List.of(HubRoutingRule.active(f.currentHub(),"NORTH","Ha Noi",origin)));
        when(hubOperationsRepository.saveRoute(any())).thenAnswer(i->i.getArgument(0));when(userRepository.getReferenceById(7L)).thenReturn(f.staff());
        assertThat(service.sortAtHub(new SortShipmentCommand(101L)).nextHubCode()).isEqualTo("ORIGIN");
        verify(hubOperationsRepository).findByRouteDecisionKey("shipment:101:hub:11:RETURN");
        verify(hubOperationsRepository,never()).findByRouteDecisionKey("shipment:101:hub:11:FORWARD");
        verify(hubOperationsRepository,never()).findActiveRules(11L,"SOUTH","Can Tho");
    }
    @Test void actualOriginStopsReturnRouting() {
        Fixture f=fixture();f.shipment().setDirection("RETURN");f.shipment().setOriginHubId(11L);f.shipment().transitionTo(ShipmentStatus.RETURN_TO_SENDER);f.shipment().receiveAt(f.currentHub());
        authenticate(f.staff());when(hubOperationsRepository.findActiveProfileByUserId(7L)).thenReturn(Optional.of(f.sorterProfile()));when(hubOperationsRepository.findShipmentByIdForUpdate(101L)).thenReturn(Optional.of(f.shipment()));
        assertThat(service.sortAtHub(new SortShipmentCommand(101L)).containerReady()).isFalse();verify(hubOperationsRepository,never()).saveRoute(any());
    }
    // --- Demo fast-track route bypass (Workflow section 101, D-08) ---

    @Test
    void demoProfileBypassRoutesToThePersistedDestinationWithTheNormalDecisionAndEvent() throws Exception {
        new Binder(ConfigurationPropertySources.from(new PropertiesPropertySourceLoader()
                .load("demo", new ClassPathResource("application-demo.properties"))))
                .bind("uteexpress", Bindable.ofInstance(logisticsProperties));
        Fixture f = sortable();
        Hub destination = destination(f);
        when(hubOperationsRepository.findActiveRules(11L, "SOUTH", "Can Tho")).thenReturn(List.of());

        SortingDecisionResponse decision = service.sortAtHub(new SortShipmentCommand(101L));

        assertThat(logisticsProperties.getDemo().isFastTrackRouteBypass()).isTrue();
        assertThat(decision.nextHubCode()).isEqualTo(destination.getCode());
        assertThat(decision.containerReady()).isTrue();
        assertThat(decision.reused()).isFalse();
        ArgumentCaptor<vn.iotstar.entity.ShipmentRoute> route = ArgumentCaptor.forClass(vn.iotstar.entity.ShipmentRoute.class);
        verify(hubOperationsRepository).saveRoute(route.capture());
        assertThat(route.getValue().getHub()).isSameAs(destination);
        assertThat(route.getValue().getRouteDecisionKey()).isEqualTo("shipment:101:hub:11:FORWARD");
        ArgumentCaptor<ShipmentEvent> event = ArgumentCaptor.forClass(ShipmentEvent.class);
        verify(shipmentEventRepository).save(event.capture());
        assertThat(event.getValue().getEventType()).isEqualTo("SORTING_COMPLETED");
        verify(routingExceptionRepository, never()).save(any());
        verifyNoInteractions(applicationEventPublisher);
        assertThat(f.shipment().getCurrentStatus()).isEqualTo(ShipmentStatus.AT_HUB);
    }

    @Test
    void bypassNeverOverridesASingleValidStaticRule() {
        Fixture f = sortable();
        logisticsProperties.getDemo().setFastTrackRouteBypass(true);
        when(hubOperationsRepository.findActiveRules(11L, "SOUTH", "Can Tho")).thenReturn(List.of(f.rule()));

        assertThat(service.sortAtHub(new SortShipmentCommand(101L)).nextHubCode()).isEqualTo("SOC-LA");
        verify(hubOperationsRepository, never()).findActiveHub(anyLong());
    }

    @Test
    void ambiguousRulesUseTheBypassOnlyWhenTheFlagIsOn() {
        Fixture f = sortable();
        Hub destination = destination(f);
        when(hubOperationsRepository.findActiveRules(11L, "SOUTH", "Can Tho")).thenReturn(List.of(f.rule(), f.rule()));
        when(routingExceptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThatThrownBy(() -> service.sortAtHub(new SortShipmentCommand(101L))).isInstanceOf(RouteConfigurationException.class);
        verify(hubOperationsRepository, never()).saveRoute(any());

        logisticsProperties.getDemo().setFastTrackRouteBypass(true);
        assertThat(service.sortAtHub(new SortShipmentCommand(101L)).nextHubCode()).isEqualTo(destination.getCode());
    }

    @Test
    void bypassStillBlocksWithoutAUsablePersistedTarget() {
        Fixture f = sortable();
        logisticsProperties.getDemo().setFastTrackRouteBypass(true);
        when(hubOperationsRepository.findActiveRules(11L, "SOUTH", "Can Tho")).thenReturn(List.of());
        when(routingExceptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // No destination hub persisted.
        assertThatThrownBy(() -> service.sortAtHub(new SortShipmentCommand(101L))).isInstanceOf(RouteConfigurationException.class);
        // A destination that is not an active hub (findActiveHub stays empty).
        f.shipment().setDestinationHubId(30L);
        assertThatThrownBy(() -> service.sortAtHub(new SortShipmentCommand(101L))).isInstanceOf(RouteConfigurationException.class);

        verify(hubOperationsRepository, never()).saveRoute(any());
        verify(routingExceptionRepository, atLeastOnce()).save(any(RoutingExceptionRecord.class));
    }

    @Test
    void bypassOnAReturnTargetsTheOriginHubWithTheReturnKey() {
        Fixture f = sortable();
        f.shipment().setDirection("RETURN");
        f.shipment().transitionTo(ShipmentStatus.RETURN_TO_SENDER);
        f.shipment().setOriginHubId(99L);
        Hub origin = Hub.active("ORIGIN", "Origin", 1, "DELIVERY_HUB", "NORTH", "Ha Noi");
        ReflectionTestUtils.setField(origin, "id", 99L);
        when(hubOperationsRepository.findActiveHub(99L)).thenReturn(Optional.of(origin));
        when(hubOperationsRepository.findActiveRules(11L, "NORTH", "Ha Noi")).thenReturn(List.of());
        logisticsProperties.getDemo().setFastTrackRouteBypass(true);

        assertThat(service.sortAtHub(new SortShipmentCommand(101L)).nextHubCode()).isEqualTo("ORIGIN");
        verify(hubOperationsRepository).saveRoute(argThat(r -> "shipment:101:hub:11:RETURN".equals(r.getRouteDecisionKey())));
    }

    @Test
    void bypassKeepsTheSorterAuthorizationRules() {
        Fixture f = fixture();
        ReflectionTestUtils.setField(f.shipment(), "currentStatus", ShipmentStatus.AT_HUB);
        logisticsProperties.getDemo().setFastTrackRouteBypass(true);
        authenticate(f.staff());
        when(hubOperationsRepository.findActiveProfileByUserId(7L)).thenReturn(Optional.of(f.inboundProfile()));

        assertThatThrownBy(() -> service.sortAtHub(new SortShipmentCommand(101L))).isInstanceOf(HubOperationAccessDeniedException.class);
        verify(hubOperationsRepository, never()).saveRoute(any());
    }

    @Test
    void successfulBypassResolvesTheOpenBlockedRouteException() {
        Fixture f = sortable();
        destination(f);
        logisticsProperties.getDemo().setFastTrackRouteBypass(true);
        RoutingExceptionRecord open = RoutingExceptionRecord.openRouteLookupBlocked(f.shipment(), f.currentHub(), f.staff());
        when(hubOperationsRepository.findActiveRules(11L, "SOUTH", "Can Tho")).thenReturn(List.of());
        when(routingExceptionRepository.findOpenRouteLookupBlockedForUpdate(101L, 11L)).thenReturn(Optional.of(open));

        service.sortAtHub(new SortShipmentCommand(101L));

        assertThat(open.getStatus()).isEqualTo("RESOLVED");
    }

    private Fixture sortable() {
        Fixture f = fixture();
        ReflectionTestUtils.setField(f.shipment(), "currentStatus", ShipmentStatus.AT_HUB);
        authenticate(f.staff());
        when(hubOperationsRepository.findActiveProfileByUserId(7L)).thenReturn(Optional.of(f.sorterProfile()));
        when(hubOperationsRepository.findShipmentByIdForUpdate(101L)).thenReturn(Optional.of(f.shipment()));
        lenient().when(hubOperationsRepository.saveRoute(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(userRepository.getReferenceById(7L)).thenReturn(f.staff());
        return f;
    }

    private Hub destination(Fixture f) {
        Hub destination = Hub.active("HUB-CT", "Can Tho Hub", 1, "DELIVERY_HUB", "SOUTH", "Can Tho");
        ReflectionTestUtils.setField(destination, "id", 31L);
        f.shipment().setDestinationHubId(31L);
        when(hubOperationsRepository.findActiveHub(31L)).thenReturn(Optional.of(destination));
        return destination;
    }

    private Fixture fixture() {
        User staff = new User(7L, "sorter", "hash", UserRole.ROLE_HUB_STAFF, "ACTIVE");
        User sender = new User(8L, "sender", "hash", UserRole.ROLE_USER, "ACTIVE");
        Hub current = Hub.active("HUB-A", "Hub A", 1, "DELIVERY_HUB", "SOUTH", "Binh Duong");
        ReflectionTestUtils.setField(current, "id", 11L);
        Hub next = Hub.active("SOC-LA", "Long An SOC", 3, "SOC", "SOUTH", "Long An");
        ReflectionTestUtils.setField(next, "id", 12L);
        Shipment shipment = Shipment.create("UTX-001", sender, "Sender", "0900000001", "Address", "Receiver", "0900000002", "Can Tho");
        ReflectionTestUtils.setField(shipment, "id", 101L);
        ReflectionTestUtils.setField(shipment, "currentStatus", ShipmentStatus.PICKED_UP);
        ReflectionTestUtils.setField(shipment, "pickupMethod", "HOME_PICKUP");
        ReflectionTestUtils.setField(shipment, "currentHub", current);
        shipment.setDestinationGrouping("SOUTH", "Can Tho");
        EmployeeProfile inbound = EmployeeProfile.active(staff, current, "INBOUND");
        EmployeeProfile sorter = EmployeeProfile.active(staff, current, "SORTER");
        HubRoutingRule rule = HubRoutingRule.active(current, "SOUTH", "Can Tho", next);
        return new Fixture(staff, current, shipment, inbound, sorter, rule,
                vn.iotstar.entity.ShipmentRoute.decided(shipment, current, next, "shipment:101:hub:11:FORWARD"));
    }

    private record Fixture(User staff, Hub currentHub, Shipment shipment, EmployeeProfile inboundProfile,
                           EmployeeProfile sorterProfile, HubRoutingRule rule, vn.iotstar.entity.ShipmentRoute route) { }
}
