package vn.iotstar.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import vn.iotstar.dto.delivery.*;
import vn.iotstar.dto.finance.ReconciliationSubmissionRequest;
import vn.iotstar.dto.hub.*;
import vn.iotstar.dto.shipment.CreateShipmentRequest;
import vn.iotstar.entity.User;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.*;
import vn.iotstar.service.impl.LastMileAssignmentService;
import vn.iotstar.service.impl.ShipmentFeeService;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/** One ownership namespace per test. Production services create ALL shipment/custody/money facts. */
@EnabledIf(value="vn.iotstar.integration.Phase05SqlServerFixtureSupport#configured",
        disabledReason="REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("sqlserver-verification")
@ContextConfiguration(initializers=Phase05SqlServerFixtureSupport.DisposableDatabaseInitializer.class)
abstract class Phase05SqlServerFixtureSupport {
    @Autowired DataSource source;
    @Autowired JdbcTemplate sql;
    @Autowired UserRepository users;
    @Autowired ShipmentService shipments;
    @Autowired HubOperationsService hubs;
    @Autowired ContainerHandoverService custody;
    @Autowired LinehaulTripService linehaul;
    @Autowired DestinationReceiptService receipts;
    @Autowired LastMileAssignmentService assignments;
    @Autowired LastMileDeliveryService delivery;
    @Autowired FinanceReconciliationService reconciliation;
    @Autowired ShipmentFinanceService finance;
    @Autowired ShipmentFeeService fees;
    @Autowired ManagerOperationsService managers;
    @Autowired ShipmentReturnService returns;
    @Autowired IntakeTimeoutService timeouts;
    @Autowired FirstMileService firstMile;
    @SpyBean Clock clock;
    String fixture;
    boolean seeded;

    /** Runs before context refresh: scheduled production beans must never see an unguarded database. */
    public static class DisposableDatabaseInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override public void initialize(ConfigurableApplicationContext context) {
            var source=new DriverManagerDataSource(System.getenv("UTEEXPRESS_SQLSERVER_URL"),
                    System.getenv("UTEEXPRESS_SQLSERVER_USERNAME"),System.getenv("UTEEXPRESS_SQLSERVER_PASSWORD"));
            try {
                SqlServerMigrationSupport.bootstrapCanonicalIfEmpty(source);
                SqlServerMigrationSupport.applyPhase05(source);
            } catch(Exception failure) {
                throw new IllegalStateException("Dedicated SQL identity and canonical manifest must succeed before application startup",failure);
            }
        }
    }

    static boolean configured() {
        return List.of("UTEEXPRESS_SQLSERVER_URL", "UTEEXPRESS_SQLSERVER_USERNAME",
                "UTEEXPRESS_SQLSERVER_PASSWORD", "UTEEXPRESS_JWT_SECRET").stream()
                .allMatch(name -> System.getenv(name) != null && !System.getenv(name).isBlank());
    }
    @BeforeEach void seed() throws Exception {
        fixture = "p05" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        SqlServerMigrationSupport.bootstrapCanonicalIfEmpty(source);
        SqlServerMigrationSupport.applyPhase05(source);
        seeded = true; // Reset can recover a failed seed using its exact namespace.
        fixtureScript("db/phase-05-integration-fixtures.sql");
    }
    @AfterEach void cleanup() throws Exception {
        SecurityContextHolder.clearContext();
        if (seeded) fixtureScript("db/phase-05-integration-reset.sql");
    }
    void fixtureScript(String path) throws Exception {
        try (Connection connection=source.getConnection()) {
            SqlServerMigrationSupport.requireDisposableDatabase(connection);
            try (var parameter=connection.prepareStatement("EXEC sys.sp_set_session_context @key=N'phase05_fixture', @value=?")) {
                parameter.setString(1,fixture); parameter.execute();
            }
            try (var statement=connection.createStatement(); var input=new ClassPathResource(path).getInputStream()) {
                statement.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch(Exception failure) {
            throw new IllegalStateException("Fixture "+fixture+" failed in "+path+"; recovery is scoped to this namespace",failure);
        }
    }
    Long hub(String suffix) { return sql.queryForObject("SELECT id FROM dbo.hubs WHERE code=?",Long.class,fixture+"_"+suffix); }
    Long user(String suffix) { return sql.queryForObject("SELECT id FROM dbo.users WHERE username=?",Long.class,fixture+"_"+suffix); }
    Long vehicle() { return sql.queryForObject("SELECT id FROM dbo.vehicles WHERE plate_number=?",Long.class,fixture); }
    void actor(String suffix) {
        User principal=users.findById(user(suffix)).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));
    }
    CreateShipmentRequest request(String payer, String method) {
        var r=new CreateShipmentRequest();
        r.setSenderName("Fixture sender"); r.setSenderPhone("0900000001"); r.setSenderAddress("Origin address");
        r.setReceiverName("Fixture receiver"); r.setReceiverPhone("0900000002"); r.setReceiverAddress("Destination address");
        r.setReceiverProvince(fixture+"_D"); r.setReceiverDistrict(fixture+"_D");
        r.setFromRegion("SOUTH"); r.setToRegion("SOUTH"); r.setServiceCode(fixture);
        r.setWeightGrams(800); r.setLengthCm(10); r.setWidthCm(10); r.setHeightCm(10);
        r.setPickupMethod("SELF_DROPOFF"); r.setFeePayer(payer); r.setPaymentMethod(method);
        r.setCodAmount(new BigDecimal("100000")); r.setDeclaredValue(new BigDecimal("200000"));
        return r;
    }
    Long create() { actor("S"); return shipments.create(request("RECEIVER","CASH")); }
    String tracking(Long id) { return sql.queryForObject("SELECT tracking_number FROM dbo.shipments WHERE id=?",String.class,id); }
    void inbound(Long id) {
        actor("O");
        hubs.receiveAtHub(new InboundShipmentCommand(id,tracking(id),800,10,10,10,BigDecimal.ZERO,null));
        assertParcel(id,"AT_HUB",hub("O"));
    }
    record Hop(Long handover, Long container, Long trip, String destination) {}
    Hop depart(List<Long> ids, String origin, String destination) {
        return depart(ids,origin,destination,vehicle());
    }
    Hop depart(List<Long> ids, String origin, String destination, Long selectedVehicle) {
        actor(origin);
        for(Long id:ids) hubs.sortAtHub(new SortShipmentCommand(id));
        Long container=custody.createContainer(new CreateContainerCommand(fixture+"-"+UUID.randomUUID().toString().substring(0,8),hub(destination)));
        for(Long id:ids) custody.loadShipment(new LoadContainerCommand(container,id));
        Long handover=custody.createHandover(new CreateHandoverCommand(hub(destination),user("L"),List.of(container)));
        assertThat(sql.queryForObject("SELECT scanned_at FROM dbo.handover_containers WHERE handover_id=?",Object.class,handover)).isNull();
        actor("L"); custody.scanAndAccept(new ScanHandoverContainerCommand(handover,container));
        Long trip=linehaul.acceptHandover(handover,selectedVehicle).tripId();
        linehaul.startTrip(trip);
        for(Long id:ids) assertThat(sql.queryForObject("SELECT current_hub_id FROM dbo.shipments WHERE id=?",Long.class,id)).isNull();
        linehaul.arriveTrip(trip);
        assertThat(sql.queryForObject("SELECT status FROM dbo.transport_trips WHERE id=?",String.class,trip)).isEqualTo("ARRIVED");
        return new Hop(handover,container,trip,destination);
    }
    void receive(Hop hop,Long shipment) {
        actor(hop.destination());
        receipts.receiveItem(new DestinationReceiptCommand(hop.handover(),hop.container(),shipment));
        assertThat(sql.queryForObject("SELECT current_hub_id FROM dbo.shipments WHERE id=?",Long.class,shipment)).isEqualTo(hub(hop.destination()));
    }
    Long atDestination() {
        Long id=create(); inbound(id);
        receive(depart(List.of(id),"O","M"),id);
        receive(depart(List.of(id),"M","D"),id);
        assertParcel(id,"AT_HUB",hub("D"));
        return id;
    }
    void outbound(Long id) {
        actor("D"); assignments.assign(new DeliveryAssignmentRequest(id,user("P")));
        actor("P"); delivery.scanForDelivery(new DeliveryScanRequest(id,hub("D")));
        assertParcel(id,"OUT_FOR_DELIVERY",null);
    }
    Long delivered() {
        Long id=atDestination(); outbound(id);
        delivery.recordCollection(new DeliveryCollectionRequest(id,new BigDecimal("100000"),new BigDecimal("30000"),"CASH"));
        delivery.confirmDelivery(id); assertParcel(id,"DELIVERED",null); return id;
    }
    ReconciliationSubmissionRequest submission(Long shipment,BigDecimal actual) {
        Long cod=sql.queryForObject("SELECT id FROM dbo.cod_transactions WHERE shipment_id=?",Long.class,shipment);
        return new ReconciliationSubmissionRequest(user("P"), LocalDate.now(),List.of(
                new ReconciliationSubmissionRequest.ItemRequest(shipment,"COD_TRANSACTION",cod,new BigDecimal("100000"),actual,"Fixture shift")));
    }
    void assertParcel(Long id,String status,Long hub) {
        Map<String,Object> row=sql.queryForMap("SELECT current_status,current_hub_id FROM dbo.shipments WHERE id=?",id);
        assertThat(row.get("current_status")).isEqualTo(status);
        assertThat(row.get("current_hub_id")).isEqualTo(hub);
    }
    List<Throwable> simultaneous(String actor,Runnable command) throws Exception {
        var start=new CountDownLatch(1); var ready=new CountDownLatch(2);
        try(var pool=Executors.newFixedThreadPool(2)) {
            List<Future<Throwable>> calls=new ArrayList<>();
            for(int i=0;i<2;i++) calls.add(pool.submit(()-> {
                actor(actor); ready.countDown();
                try { if(!start.await(20,TimeUnit.SECONDS)) throw new AssertionError("concurrency barrier timed out"); command.run(); return null; }
                catch(Throwable failure) { return failure; }
                finally { SecurityContextHolder.clearContext(); }
            }));
            assertThat(ready.await(20,TimeUnit.SECONDS)).isTrue(); start.countDown();
            var failures=new ArrayList<Throwable>();
            for(var call:calls) { Throwable failure=call.get(40,TimeUnit.SECONDS); if(failure!=null) failures.add(failure); }
            assertThat(failures.size()).isLessThan(2); // At least one real service transaction commits.
            return failures;
        }
    }
}
