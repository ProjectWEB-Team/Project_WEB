package vn.iotstar.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import vn.iotstar.dto.finance.ReconciliationSubmissionRequest;
import vn.iotstar.entity.CodTransaction;
import vn.iotstar.entity.Payment;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.ShipperReconciliation;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.repository.CodTransactionRepository;
import vn.iotstar.repository.PaymentRepository;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.repository.WalletRepository;
import vn.iotstar.service.FinanceReconciliationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIf(value = "sqlServerVerificationConfigured", disabledReason = "REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("sqlserver-verification")
class Phase04FinanceSqlServerIntegrationTest {

    @Autowired
    private FinanceReconciliationService reconciliationService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ShipmentRepository shipmentRepository;
    @Autowired
    private CodTransactionRepository codTransactionRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private WalletRepository walletRepository;

    private Long managerUserId;
    private Long shipperUserId;
    private Long senderUserId;
    private Long shipmentId;
    private Long codTxId;
    private Long paymentId;
    private Long reconciliationId;

    @BeforeEach
    void setUp() {
        String suffix = String.valueOf(System.nanoTime());

        // Create manager
        jdbcTemplate.update("INSERT INTO dbo.users (username, password, full_name, phone, email, role, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                "mgr-" + suffix, "hash", "Manager " + suffix, "09" + suffix.substring(Math.max(0, suffix.length() - 8)),
                "mgr-" + suffix + "@example.test", "ROLE_MANAGER", "ACTIVE");
        managerUserId = jdbcTemplate.queryForObject("SELECT id FROM dbo.users WHERE username = ?", Long.class, "mgr-" + suffix);

        // Create shipper
        jdbcTemplate.update("INSERT INTO dbo.users (username, password, full_name, phone, email, role, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                "shp-" + suffix, "hash", "Shipper " + suffix, "08" + suffix.substring(Math.max(0, suffix.length() - 8)),
                "shp-" + suffix + "@example.test", "ROLE_SHIPPER", "ACTIVE");
        shipperUserId = jdbcTemplate.queryForObject("SELECT id FROM dbo.users WHERE username = ?", Long.class, "shp-" + suffix);

        // Create sender
        jdbcTemplate.update("INSERT INTO dbo.users (username, password, full_name, phone, email, role, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                "snd-" + suffix, "hash", "Sender " + suffix, "07" + suffix.substring(Math.max(0, suffix.length() - 8)),
                "snd-" + suffix + "@example.test", "ROLE_USER", "ACTIVE");
        senderUserId = jdbcTemplate.queryForObject("SELECT id FROM dbo.users WHERE username = ?", Long.class, "snd-" + suffix);

        User manager = userRepository.findById(managerUserId).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(manager, null, manager.getAuthorities()));

        User sender = userRepository.findById(senderUserId).orElseThrow();
        Shipment s = Shipment.create("TRK-" + suffix, sender, "Sender", "0901", "Addr 1", "Receiver", "0902", "Addr 2");
        s.setCodAmount(new BigDecimal("300000.00"));
        s.setShippingFee(new BigDecimal("25000.00"));
        s.setFeePayer("RECEIVER");
        s = shipmentRepository.save(s);
        shipmentId = s.getId();

        User shipper = userRepository.findById(shipperUserId).orElseThrow();
        CodTransaction cod = CodTransaction.createCollected(s, shipper, new BigDecimal("300000.00"), "CASH");
        cod = codTransactionRepository.save(cod);
        codTxId = cod.getId();

        Payment payment = Payment.create(s, "RECEIVER", "SHIPPING_FEE", new BigDecimal("25000.00"), "CASH", "PAID");
        payment = paymentRepository.save(payment);
        paymentId = payment.getId();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (reconciliationId != null) {
            jdbcTemplate.update("DELETE FROM dbo.shipper_reconciliation_items WHERE reconciliation_id = ?", reconciliationId);
            jdbcTemplate.update("DELETE FROM dbo.shipper_reconciliations WHERE id = ?", reconciliationId);
        }
        if (codTxId != null) {
            jdbcTemplate.update("DELETE FROM dbo.cod_transactions WHERE id = ?", codTxId);
        }
        if (paymentId != null) {
            jdbcTemplate.update("DELETE FROM dbo.payments WHERE id = ?", paymentId);
        }
        if (shipmentId != null) {
            jdbcTemplate.update("DELETE FROM dbo.wallet_transactions WHERE reference_id = ? AND reference_type = 'SHIPMENT'", shipmentId);
            jdbcTemplate.update("DELETE FROM dbo.shipment_events WHERE shipment_id = ?", shipmentId);
            jdbcTemplate.update("DELETE FROM dbo.shipments WHERE id = ?", shipmentId);
        }
        if (senderUserId != null) {
            jdbcTemplate.update("DELETE FROM dbo.wallets WHERE user_id = ?", senderUserId);
            jdbcTemplate.update("DELETE FROM dbo.users WHERE id = ?", senderUserId);
        }
        if (shipperUserId != null) {
            jdbcTemplate.update("DELETE FROM dbo.users WHERE id = ?", shipperUserId);
        }
        if (managerUserId != null) {
            jdbcTemplate.update("DELETE FROM dbo.users WHERE id = ?", managerUserId);
        }
    }

    @Test
    void reconcilesCodAndFeeSourcesOnSqlServer() {
        ReconciliationSubmissionRequest.ItemRequest codItem = new ReconciliationSubmissionRequest.ItemRequest(
                shipmentId, "COD_TRANSACTION", codTxId, new BigDecimal("300000.00"), new BigDecimal("300000.00"), "COD Match");
        ReconciliationSubmissionRequest.ItemRequest feeItem = new ReconciliationSubmissionRequest.ItemRequest(
                shipmentId, "RECEIVER_CASH_SHIPPING_FEE_PAYMENT", paymentId, new BigDecimal("25000.00"), new BigDecimal("25000.00"), "Fee Match");

        ReconciliationSubmissionRequest request = new ReconciliationSubmissionRequest(
                shipperUserId, LocalDate.now(), List.of(codItem, feeItem));

        ShipperReconciliation rec = reconciliationService.reconcile(request);
        reconciliationId = rec.getId();

        assertThat(rec.getStatus()).isEqualTo("COMPLETED");

        // The reconciliation only makes the COD ready for payout.
        String codStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM dbo.cod_transactions WHERE id = ?", String.class, codTxId);
        assertThat(codStatus).isEqualTo("READY_FOR_PAYOUT");

        // The wallet is untouched until the payout step runs. The wallet row
        // only exists once the payout credited it.
        Integer walletRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.wallets WHERE user_id = ?", Integer.class, senderUserId);
        assertThat(walletRows).isEqualTo(0);

        reconciliationService.payoutReadyCod(codTxId);

        // Verify COD moved to PAID_TO_SENDER in SQL Server
        String paidStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM dbo.cod_transactions WHERE id = ?", String.class, codTxId);
        assertThat(paidStatus).isEqualTo("PAID_TO_SENDER");

        // Verify sender wallet credited
        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM dbo.wallets WHERE user_id = ?", BigDecimal.class, senderUserId);
        assertThat(balance).isEqualByComparingTo(new BigDecimal("300000.00"));

        // Verify the wallet ledger holds exactly one COD_PAYOUT entry.
        Integer ledgerRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.wallet_transactions WHERE user_id = ? AND transaction_type = 'COD_PAYOUT'",
                Integer.class, senderUserId);
        assertThat(ledgerRows).isEqualTo(1);
    }

    static boolean sqlServerVerificationConfigured() {
        return hasValue("UTEEXPRESS_SQLSERVER_URL") && hasValue("UTEEXPRESS_SQLSERVER_USERNAME")
                && hasValue("UTEEXPRESS_SQLSERVER_PASSWORD");
    }

    private static boolean hasValue(String name) {
        String value = System.getenv(name);
        return value != null && !value.isBlank();
    }
}

