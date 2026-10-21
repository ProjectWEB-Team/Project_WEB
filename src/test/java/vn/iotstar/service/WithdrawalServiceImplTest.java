package vn.iotstar.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.iotstar.dto.wallet.WithdrawalRequestDto;
import vn.iotstar.dto.wallet.WithdrawalReviewDto;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.entity.WithdrawalRequest;
import vn.iotstar.realtime.NotificationEventPublisher;
import vn.iotstar.repository.WithdrawalRequestRepository;
import vn.iotstar.service.impl.WithdrawalServiceImpl;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceImplTest {

    @Mock
    private WithdrawalRequestRepository withdrawalRepository;
    @Mock
    private WalletLedgerService walletLedgerService;
    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    private WithdrawalServiceImpl withdrawalService;

    private User admin;
    private User regularUser;

    @BeforeEach
    void setUp() {
        withdrawalService = new WithdrawalServiceImpl(
                withdrawalRepository, walletLedgerService, notificationEventPublisher);

        admin = new User(100L, "admin_test", "hash", UserRole.ROLE_ADMIN, "ACTIVE");
        regularUser = new User(2L, "user_test", "hash", UserRole.ROLE_USER, "ACTIVE");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Request: creates a pending request and locks the wallet balance")
    void requestLocksWalletBalance() {
        WithdrawalRequestDto dto = new WithdrawalRequestDto(
                new BigDecimal("50000.00"), "Vietcombank", "0123456789", "Nguyen Van A", "key-1");

        when(withdrawalRepository.findByUserIdAndRequestKey(admin.getId(), "key-1")).thenReturn(Optional.empty());
        when(withdrawalRepository.save(any())).thenAnswer(inv -> {
            WithdrawalRequest request = inv.getArgument(0);
            request.setId(7L);
            return request;
        });

        WithdrawalRequest created = withdrawalService.requestWithdrawal(dto);

        assertThat(created.getId()).isEqualTo(7L);
        assertThat(created.getStatus()).isEqualTo("PENDING");
        verify(walletLedgerService).recordWithdrawalLock(admin.getId(), new BigDecimal("50000.00"), 7L);
    }

    @Test
    @DisplayName("Request: the same idempotency key returns the existing request")
    void requestIsIdempotent() {
        WithdrawalRequest existing = WithdrawalRequest.create(
                admin.getId(), new BigDecimal("50000.00"), "Vietcombank", "0123456789", "Nguyen Van A", "key-1");
        existing.setId(7L);

        WithdrawalRequestDto dto = new WithdrawalRequestDto(
                new BigDecimal("50000.00"), "Vietcombank", "0123456789", "Nguyen Van A", "key-1");

        when(withdrawalRepository.findByUserIdAndRequestKey(admin.getId(), "key-1")).thenReturn(Optional.of(existing));

        WithdrawalRequest result = withdrawalService.requestWithdrawal(dto);

        assertThat(result.getId()).isEqualTo(7L);
        verify(walletLedgerService, never()).recordWithdrawalLock(any(), any(), any());
    }

    @Test
    @DisplayName("Request: the same idempotency key with a different amount is rejected")
    void requestWithConflictingKeyIsRejected() {
        WithdrawalRequest existing = WithdrawalRequest.create(
                1L, new BigDecimal("50000.00"), "Vietcombank", "0123456789", "Nguyen Van A", "key-1");
        existing.setId(7L);

        WithdrawalRequestDto dto = new WithdrawalRequestDto(
                new BigDecimal("70000.00"), "Vietcombank", "0123456789", "Nguyen Van A", "key-1");

        when(withdrawalRepository.findByUserIdAndRequestKey(admin.getId(), "key-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> withdrawalService.requestWithdrawal(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Conflicting withdrawal request");
    }

    @Test
    @DisplayName("Approval: only an administrator can approve a withdrawal")
    void onlyAdminCanApprove() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(regularUser, null, regularUser.getAuthorities()));

        assertThatThrownBy(() -> withdrawalService.approveWithdrawal(new WithdrawalReviewDto(7L, null)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ROLE_ADMIN required");
    }

    @Test
    @DisplayName("Approval: deducts the locked balance and completes the request")
    void approvalDeductsLockedBalance() {
        WithdrawalRequest request = WithdrawalRequest.create(
                1L, new BigDecimal("50000.00"), "Vietcombank", "0123456789", "Nguyen Van A", null);
        request.setId(7L);

        when(withdrawalRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(request));
        when(withdrawalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WithdrawalRequest result = withdrawalService.approveWithdrawal(new WithdrawalReviewDto(7L, null));

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getCompletedAt()).isNotNull();
        verify(walletLedgerService).recordWithdrawalDeduct(1L, new BigDecimal("50000.00"), 7L);
    }

    @Test
    @DisplayName("Approval: a request that is not pending cannot be approved")
    void approvalRejectsNonPendingRequest() {
        WithdrawalRequest request = WithdrawalRequest.create(
                1L, new BigDecimal("50000.00"), "Vietcombank", "0123456789", "Nguyen Van A", null);
        request.setId(7L);
        request.setStatus("REJECTED");

        when(withdrawalRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> withdrawalService.approveWithdrawal(new WithdrawalReviewDto(7L, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only PENDING withdrawals can be approved");
    }

    @Test
    @DisplayName("Rejection: releases the locked balance and keeps the reason")
    void rejectionReleasesLockedBalance() {
        WithdrawalRequest request = WithdrawalRequest.create(
                1L, new BigDecimal("50000.00"), "Vietcombank", "0123456789", "Nguyen Van A", null);
        request.setId(7L);

        when(withdrawalRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(request));
        when(withdrawalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WithdrawalRequest result = withdrawalService.rejectWithdrawal(
                new WithdrawalReviewDto(7L, "Account number does not match"));

        assertThat(result.getStatus()).isEqualTo("REJECTED");
        assertThat(result.getRejectionReason()).isEqualTo("Account number does not match");
        verify(walletLedgerService).recordWithdrawalRefund(1L, new BigDecimal("50000.00"), 7L);
    }

    @Test
    @DisplayName("Rejection: a reason is mandatory")
    void rejectionRequiresReason() {
        assertThatThrownBy(() -> withdrawalService.rejectWithdrawal(new WithdrawalReviewDto(7L, "  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rejection reason is required");
    }
}