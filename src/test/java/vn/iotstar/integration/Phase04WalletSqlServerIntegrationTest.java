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
import vn.iotstar.dto.wallet.WithdrawalRequestDto;
import vn.iotstar.dto.wallet.WithdrawalReviewDto;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.entity.Wallet;
import vn.iotstar.entity.WithdrawalRequest;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.repository.WalletRepository;
import vn.iotstar.repository.WithdrawalRequestRepository;
import vn.iotstar.service.WalletLedgerService;
import vn.iotstar.service.WithdrawalService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIf(value = "sqlServerVerificationConfigured", disabledReason = "REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("sqlserver-verification")
class Phase04WalletSqlServerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private WithdrawalRequestRepository withdrawalRepository;
    @Autowired
    private WalletLedgerService walletLedgerService;
    @Autowired
    private WithdrawalService withdrawalService;

    private Long userId;
    private Long withdrawalId;

    @BeforeEach
    void setUp() {
        String suffix = String.valueOf(System.nanoTime());

        jdbcTemplate.update("INSERT INTO dbo.users (username, password, full_name, phone, email, role, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                "wal-" + suffix, "hash", "Wallet " + suffix, "06" + suffix.substring(Math.max(0, suffix.length() - 8)),
                "wal-" + suffix + "@example.test", "ROLE_USER", "ACTIVE");
        userId = jdbcTemplate.queryForObject("SELECT id FROM dbo.users WHERE username = ?", Long.class, "wal-" + suffix);

        jdbcTemplate.update("INSERT INTO dbo.wallets (user_id, balance, locked_balance, status, created_at) VALUES (?, ?, ?, 'ACTIVE', SYSDATETIME())",
                userId, new BigDecimal("200000.00"), new BigDecimal("0.00"));

        User user = userRepository.findById(userId).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (withdrawalId != null) {
            jdbcTemplate.update("DELETE FROM dbo.wallet_transactions WHERE reference_id = ? AND reference_type = 'WITHDRAWAL'",
                    withdrawalId);
            jdbcTemplate.update("DELETE FROM dbo.withdrawal_requests WHERE id = ?", withdrawalId);
        }
        if (userId != null) {
            jdbcTemplate.update("DELETE FROM dbo.wallet_transactions WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM dbo.wallets WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM dbo.users WHERE id = ?", userId);
        }
    }

    private void actAsAdministrator() {
        User admin = new User(900L, "admin_wallet", "hash", UserRole.ROLE_ADMIN, "ACTIVE");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));
    }

    @Test
    void withdrawalLocksThenApprovesTheLockedBalance() {
        WithdrawalRequestDto request = new WithdrawalRequestDto(
                new BigDecimal("50000.00"), "Vietcombank", "0123456789", "Wallet User", null);

        WithdrawalRequest created = withdrawalService.requestWithdrawal(request);
        withdrawalId = created.getId();

        assertThat(created.getStatus()).isEqualTo("PENDING");

        BigDecimal balanceAfterLock = jdbcTemplate.queryForObject(
                "SELECT balance FROM dbo.wallets WHERE user_id = ?", BigDecimal.class, userId);
        BigDecimal lockedAfterLock = jdbcTemplate.queryForObject(
                "SELECT locked_balance FROM dbo.wallets WHERE user_id = ?", BigDecimal.class, userId);
        assertThat(balanceAfterLock).isEqualByComparingTo(new BigDecimal("150000.00"));
        assertThat(lockedAfterLock).isEqualByComparingTo(new BigDecimal("50000.00"));

        actAsAdministrator();
        withdrawalService.approveWithdrawal(new WithdrawalReviewDto(withdrawalId, null));

        BigDecimal lockedAfterApproval = jdbcTemplate.queryForObject(
                "SELECT locked_balance FROM dbo.wallets WHERE user_id = ?", BigDecimal.class, userId);
        assertThat(lockedAfterApproval).isEqualByComparingTo(BigDecimal.ZERO);

        Integer ledgerRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.wallet_transactions WHERE user_id = ? AND transaction_type IN ('WITHDRAWAL_LOCK','WITHDRAWAL_DEDUCT')",
                Integer.class, userId);
        assertThat(ledgerRows).isEqualTo(2);
    }

    @Test
    void rejectedWithdrawalReleasesTheLockedBalance() {
        WithdrawalRequestDto request = new WithdrawalRequestDto(
                new BigDecimal("30000.00"), "Vietcombank", "0123456789", "Wallet User", null);

        WithdrawalRequest created = withdrawalService.requestWithdrawal(request);
        withdrawalId = created.getId();

        actAsAdministrator();
        withdrawalService.rejectWithdrawal(new WithdrawalReviewDto(withdrawalId, "Account mismatch"));

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM dbo.wallets WHERE user_id = ?", BigDecimal.class, userId);
        BigDecimal locked = jdbcTemplate.queryForObject(
                "SELECT locked_balance FROM dbo.wallets WHERE user_id = ?", BigDecimal.class, userId);

        assertThat(balance).isEqualByComparingTo(new BigDecimal("200000.00"));
        assertThat(locked).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void withdrawalBeyondTheAvailableBalanceIsRejected() {
        WithdrawalRequestDto request = new WithdrawalRequestDto(
                new BigDecimal("500000.00"), "Vietcombank", "0123456789", "Wallet User", null);

        assertThatThrownBy(() -> withdrawalService.requestWithdrawal(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    void walletLedgerKeepsBalanceAndLockedBalanceConsistent() {
        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("200000.00"));
        assertThat(wallet.getLockedBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        walletLedgerService.recordFeeDebit(userId, new BigDecimal("25000.00"), "RETURN_FEE_PAYMENT", 1L,
                "Return fee paid from e-wallet");

        Wallet afterDebit = walletRepository.findByUserId(userId).orElseThrow();
        assertThat(afterDebit.getBalance()).isEqualByComparingTo(new BigDecimal("175000.00"));

        Integer feeRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.wallet_transactions WHERE user_id = ? AND transaction_type = 'RETURN_FEE_PAYMENT'",
                Integer.class, userId);
        assertThat(feeRows).isEqualTo(1);
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