package vn.iotstar.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.iotstar.config.OtpProperties;
import vn.iotstar.dto.auth.RegisterUserRequest;
import vn.iotstar.dto.auth.RegistrationChallengeResponse;
import vn.iotstar.dto.auth.VerifyPhoneOtpRequest;
import vn.iotstar.entity.User;
import vn.iotstar.exception.RegistrationException;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.RegistrationService;
import vn.iotstar.service.auth.OtpDelivery;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Opt-in verification against an existing SQL Server database with the canonical schema
 * and phase-02-account-otp.sql already applied. No schema or database is created here.
 * Deliberately has no test-level transaction: each service call must really commit or roll back.
 */
@EnabledIf(value = "sqlServerVerificationConfigured", disabledReason = "REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("sqlserver-verification")
@Import(RegistrationSqlServerIntegrationTest.DeliveryConfiguration.class)
class RegistrationSqlServerIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-10-22T08:00:00Z");
    @Autowired private RegistrationService registrations;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private OtpProperties policy;
    @Autowired private CapturingDelivery delivery;
    @Autowired private TestClock clock;
    @Autowired private UserRepository users;
    @Autowired private ShipmentRepository shipments;
    private final List<TestIdentity> identities = new ArrayList<>();
    private final List<Long> shipmentIds = new ArrayList<>();

    @BeforeEach
    void resetDeliveryAndTime() {
        delivery.codes.clear();
        delivery.fail = false;
        clock.now = NOW;
    }

    @AfterEach
    void removeOnlyThisTestsRowsInForeignKeyOrder() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            for (Long id : shipmentIds) {
                jdbc.update("DELETE FROM dbo.payments WHERE shipment_id = ?", id);
                jdbc.update("DELETE FROM dbo.shipment_events WHERE shipment_id = ?", id);
                jdbc.update("DELETE FROM dbo.shipments WHERE id = ?", id);
            }
            for (TestIdentity identity : identities) {
                // Both values contain this test's UUID; never delete by a shared prefix or phone.
                List<Long> ids = jdbc.queryForList("SELECT id FROM dbo.users WHERE username = ? AND email = ?",
                        Long.class, identity.username(), identity.email());
                for (Long id : ids) {
                    jdbc.update("DELETE FROM dbo.phone_otp_challenges WHERE user_id = ?", id);
                    jdbc.update("DELETE FROM dbo.users WHERE id = ? AND username = ? AND email = ?",
                            id, identity.username(), identity.email());
                }
            }
        });
        delivery.codes.clear();
        delivery.fail = false;
    }

    @Test
    void failedAttemptCommitsThenCorrectCodeActivatesAndCannotBeReplayed() {
        RegisterUserRequest form = form();
        RegistrationChallengeResponse challenge = registrations.register(form);
        String code = delivery.codeFor(challenge.challengeId());

        assertThat(userStatus(form)).isEqualTo("INACTIVE");
        assertThat(phoneVerifiedAt(form)).isNull();
        // DATETIME2 holds the UTC wall-clock value of the Instant, regardless of the JVM timezone.
        assertThat(snapshot(challenge.challengeId()).expiresAt())
                .isEqualTo(LocalDateTime.ofInstant(challenge.expiresAt(), ZoneOffset.UTC));
        assertThat(jdbc.queryForObject("SELECT full_name FROM dbo.users WHERE username = ?",
                String.class, form.getUsername())).isEqualTo("Kiểm thử đăng ký");

        String wrongCode = "000000".equals(code) ? "000001" : "000000";
        assertThat(registrations.verify(verification(challenge.challengeId(), wrongCode))).isFalse();
        assertThat(snapshot(challenge.challengeId()).failedAttempts()).isEqualTo(1);
        assertThat(userStatus(form)).isEqualTo("INACTIVE");

        assertThat(registrations.verify(verification(challenge.challengeId(), code))).isTrue();
        assertThat(userStatus(form)).isEqualTo("ACTIVE");
        assertThat(phoneVerifiedAt(form)).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        assertThat(snapshot(challenge.challengeId()).consumedAt()).isNotNull();
        assertThat(registrations.verify(verification(challenge.challengeId(), code))).isFalse();
        assertThat(snapshot(challenge.challengeId()).failedAttempts()).isEqualTo(1);
    }

    @Test
    void deliveryFailureRollsBackNewAccountAndChallenge() {
        RegisterUserRequest form = form();
        delivery.fail = true;

        assertThatThrownBy(() -> registrations.register(form)).isInstanceOf(RegistrationException.class);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.users WHERE username = ? AND email = ?",
                Integer.class, form.getUsername(), form.getEmail())).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.phone_otp_challenges c "
                        + "JOIN dbo.users u ON u.id = c.user_id WHERE u.username = ? AND u.email = ?",
                Integer.class, form.getUsername(), form.getEmail())).isZero();
        assertThat(delivery.codes).isEmpty();
    }

    @Test
    void failedResendRestoresOriginalCodeAndRateLimitCounters() {
        RegistrationChallengeResponse challenge = registrations.register(form());
        String originalCode = delivery.codeFor(challenge.challengeId());
        ChallengeSnapshot before = snapshot(challenge.challengeId());
        clock.advance(policy.getResendCooldown());
        delivery.fail = true;

        assertThatThrownBy(() -> registrations.resend(challenge.challengeId()))
                .isInstanceOf(RegistrationException.class);

        assertThat(snapshot(challenge.challengeId())).isEqualTo(before);
        assertThat(registrations.verify(verification(challenge.challengeId(), originalCode))).isTrue();
    }

    @Test
    void activationLinksEarlierShipmentsInAllPhoneFormsWithoutChangingSnapshotsOrOtherOwners() {
        User sender = registerAndActivate(form());
        RegisterUserRequest recipient = form();
        List<Long> eligible = phoneForms(recipient.getPhone()).stream()
                .map(phone -> shipment(sender.getId(), phone, null)).toList();
        long alreadyOwned = shipment(sender.getId(), recipient.getPhone(), sender.getId());
        long mismatched = shipment(sender.getId(), form().getPhone(), null);
        Map<Long, Map<String, Object>> before = new LinkedHashMap<>();
        shipmentIds.forEach(id -> before.put(id, shipmentSnapshot(id)));

        RegistrationChallengeResponse challenge = registrations.register(recipient);
        eligible.forEach(id -> assertThat(receiverId(id)).isNull());
        assertThat(registrations.verify(verification(challenge.challengeId(), delivery.codeFor(challenge.challengeId()))))
                .isTrue();

        long recipientId = users.findByUsername(recipient.getUsername()).orElseThrow().getId();
        eligible.forEach(id -> assertThat(receiverId(id)).isEqualTo(recipientId));
        assertThat(receiverId(alreadyOwned)).isEqualTo(sender.getId());
        assertThat(receiverId(mismatched)).isNull();
        before.forEach((id, snapshot) -> assertThat(shipmentSnapshot(id)).isEqualTo(snapshot));
    }

    @Test
    void outerRollbackRestoresAccountChallengeAndReceiverLinkTogether() {
        User sender = registerAndActivate(form());
        RegisterUserRequest recipient = form();
        long shipmentId = shipment(sender.getId(), recipient.getPhone(), null);
        RegistrationChallengeResponse challenge = registrations.register(recipient);
        String code = delivery.codeFor(challenge.challengeId());
        ChallengeSnapshot beforeChallenge = snapshot(challenge.challengeId());
        Map<String, Object> beforeShipment = shipmentSnapshot(shipmentId);
        long recipientId = users.findByUsername(recipient.getUsername()).orElseThrow().getId();

        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            assertThat(registrations.verify(verification(challenge.challengeId(), code))).isTrue();
            assertThat(receiverId(shipmentId)).isEqualTo(recipientId);
            assertThat(userStatus(recipient)).isEqualTo("ACTIVE");
            status.setRollbackOnly();
        });

        assertThat(userStatus(recipient)).isEqualTo("INACTIVE");
        assertThat(phoneVerifiedAt(recipient)).isNull();
        assertThat(snapshot(challenge.challengeId())).isEqualTo(beforeChallenge);
        assertThat(receiverId(shipmentId)).isNull();
        assertThat(shipmentSnapshot(shipmentId)).isEqualTo(beforeShipment);
        assertThat(registrations.verify(verification(challenge.challengeId(), code))).isTrue();
        assertThat(receiverId(shipmentId)).isEqualTo(recipientId);
    }

    @Test
    void concurrentConditionalClaimsCannotOverwriteTheFirstCommittedReceiver() throws Exception {
        User first = registerAndActivate(form());
        User second = registerAndActivate(form());
        long shipmentId = shipment(second.getId(), first.getPhone(), null);
        Map<String, Object> before = shipmentSnapshot(shipmentId);
        List<String> phones = phoneForms(first.getPhone());
        CountDownLatch firstUpdated = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        transaction.setTimeout(15);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> winner = workers.submit(() -> transaction.execute(status -> {
                int count = shipments.linkReceiver(first, phones);
                firstUpdated.countDown();
                await(releaseFirst);
                return count;
            }));
            await(firstUpdated);
            // Compete at the repository boundary to prove that even another owner cannot overwrite a claim.
            Future<Integer> contender = workers.submit(() -> transaction.execute(status -> {
                secondStarted.countDown();
                return shipments.linkReceiver(second, phones);
            }));
            await(secondStarted);
            assertThatThrownBy(() -> contender.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            releaseFirst.countDown();

            assertThat(winner.get(20, TimeUnit.SECONDS)).isEqualTo(1);
            assertThat(contender.get(20, TimeUnit.SECONDS)).isZero();
            assertThat(receiverId(shipmentId)).isEqualTo(first.getId());
            assertThat(shipmentSnapshot(shipmentId)).isEqualTo(before);
        } finally {
            releaseFirst.countDown();
            workers.shutdownNow();
            assertThat(workers.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }
    }

    private RegisterUserRequest form() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        RegisterUserRequest form = new RegisterUserRequest();
        form.setUsername("phase02-it-" + suffix);
        form.setEmail("phase02-" + suffix + "@example.test");
        form.setFullName("Kiểm thử đăng ký");
        form.setPassword("IntegrationOnly-9!" + suffix);
        String phone;
        do {
            phone = "+849" + String.format(java.util.Locale.ROOT, "%08d",
                    ThreadLocalRandom.current().nextInt(100_000_000));
        } while (phoneAlreadyUsed(phone));
        form.setPhone(phone);
        identities.add(new TestIdentity(form.getUsername(), form.getEmail(), phone));
        return form;
    }

    private boolean phoneAlreadyUsed(String phone) {
        if (identities.stream().anyMatch(identity -> identity.phone().equals(phone))) return true;
        List<String> forms = phoneForms(phone);
        return jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM dbo.users WHERE phone IN (?, ?, ?)) "
                        + "+ (SELECT COUNT(*) FROM dbo.shipments WHERE receiver_phone IN (?, ?, ?) "
                        + "OR sender_phone IN (?, ?, ?))", Integer.class,
                forms.get(0), forms.get(1), forms.get(2), forms.get(0), forms.get(1), forms.get(2),
                forms.get(0), forms.get(1), forms.get(2)) != 0;
    }

    private List<String> phoneForms(String phone) {
        return List.of(phone, "0" + phone.substring(3), phone.substring(1));
    }

    private User registerAndActivate(RegisterUserRequest form) {
        RegistrationChallengeResponse challenge = registrations.register(form);
        assertThat(registrations.verify(verification(challenge.challengeId(), delivery.codeFor(challenge.challengeId()))))
                .isTrue();
        return users.findByUsername(form.getUsername()).orElseThrow();
    }

    private long shipment(long senderId, String receiverPhone, Long receiverId) {
        Long id = jdbc.queryForObject("INSERT INTO dbo.shipments "
                        + "(tracking_number, sender_user_id, receiver_user_id, sender_name, sender_phone, sender_address, "
                        + "receiver_name, receiver_phone, receiver_address, receiver_province, receiver_district, "
                        + "pickup_method, fee_payer, inspection_policy, payment_status, current_status) "
                        + "OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Long.class,
                "phase02-it-" + UUID.randomUUID(), senderId, receiverId, "Người gửi nguyên bản", receiverPhone,
                "Địa chỉ gửi nguyên bản", "Người nhận trên vận đơn", receiverPhone, "Địa chỉ nhận nguyên bản",
                "HCM", "Q1", "SELF_DROPOFF", "SENDER", "NO_INSPECTION", "UNPAID", "CREATED");
        shipmentIds.add(id);
        return id;
    }

    private Long receiverId(long shipmentId) {
        return jdbc.queryForObject("SELECT receiver_user_id FROM dbo.shipments WHERE id = ?", Long.class, shipmentId);
    }

    private Map<String, Object> shipmentSnapshot(long shipmentId) {
        Map<String, Object> snapshot = new LinkedHashMap<>(jdbc.queryForMap("SELECT * FROM dbo.shipments WHERE id = ?", shipmentId));
        snapshot.remove("receiver_user_id");
        return snapshot;
    }

    private void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while coordinating SQL transactions", interrupted);
        }
    }

    private VerifyPhoneOtpRequest verification(UUID id, String code) {
        VerifyPhoneOtpRequest request = new VerifyPhoneOtpRequest();
        request.setChallengeId(id);
        request.setCode(code);
        return request;
    }

    private String userStatus(RegisterUserRequest form) {
        return jdbc.queryForObject("SELECT status FROM dbo.users WHERE username = ?", String.class, form.getUsername());
    }

    private LocalDateTime phoneVerifiedAt(RegisterUserRequest form) {
        return jdbc.queryForObject("SELECT phone_verified_at FROM dbo.users WHERE username = ?",
                (result, row) -> result.getObject(1, LocalDateTime.class), form.getUsername());
    }

    private ChallengeSnapshot snapshot(UUID id) {
        return jdbc.queryForObject("SELECT purpose, code_hash, expires_at, consumed_at, failed_attempts, "
                        + "last_sent_at, send_window_started_at, send_count, version FROM dbo.phone_otp_challenges WHERE id = ?",
                (result, row) -> new ChallengeSnapshot(result.getString("purpose"), result.getString("code_hash"),
                        result.getObject("expires_at", LocalDateTime.class), result.getObject("consumed_at", LocalDateTime.class),
                        result.getInt("failed_attempts"), result.getObject("last_sent_at", LocalDateTime.class),
                        result.getObject("send_window_started_at", LocalDateTime.class), result.getInt("send_count"),
                        result.getLong("version")), id.toString());
    }

    static boolean sqlServerVerificationConfigured() {
        return hasValue("UTEEXPRESS_SQLSERVER_URL") && hasValue("UTEEXPRESS_SQLSERVER_USERNAME")
                && hasValue("UTEEXPRESS_SQLSERVER_PASSWORD");
    }

    private static boolean hasValue(String name) {
        String value = System.getenv(name);
        return value != null && !value.isBlank();
    }

    private record TestIdentity(String username, String email, String phone) { }
    private record ChallengeSnapshot(String purpose, String hash, LocalDateTime expiresAt, LocalDateTime consumedAt,
                                     int failedAttempts, LocalDateTime lastSentAt, LocalDateTime windowStartedAt,
                                     int sendCount, long version) { }

    @TestConfiguration(proxyBeanMethods = false)
    static class DeliveryConfiguration {
        @Bean @Primary CapturingDelivery capturingOtpDelivery() { return new CapturingDelivery(); }
        @Bean @Primary TestClock registrationTestClock() { return new TestClock(); }
    }

    static final class CapturingDelivery implements OtpDelivery {
        final Map<UUID, String> codes = new ConcurrentHashMap<>();
        volatile boolean fail;
        @Override public void send(UUID id, String phone, String code, Instant expiresAt) {
            if (fail) throw new RegistrationException("Simulated test delivery failure");
            codes.put(id, code);
        }
        String codeFor(UUID id) {
            String code = codes.get(id);
            assertThat(code).matches("[0-9]{6}");
            return code;
        }
    }

    static final class TestClock extends Clock {
        volatile Instant now = NOW;
        void advance(Duration duration) { now = now.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
        @Override public Instant instant() { return now; }
    }
}
