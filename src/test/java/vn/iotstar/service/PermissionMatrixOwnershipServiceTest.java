package vn.iotstar.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.iotstar.dto.wallet.WithdrawalRequestDto;
import vn.iotstar.dto.wallet.WithdrawalReviewDto;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.Hub;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.entity.WithdrawalRequest;
import vn.iotstar.realtime.NotificationEventPublisher;
import vn.iotstar.repository.HubRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.repository.WithdrawalRequestRepository;
import vn.iotstar.service.impl.TrackingServiceImpl;
import vn.iotstar.service.impl.WithdrawalServiceImpl;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HARD-01 ownership cells (D-01) not already covered by the per-service tests; see 06-MATRIX-SERVICE-OWNERSHIP.md. */
class PermissionMatrixOwnershipServiceTest {
    private final User admin = new User(100L, "admin", "hash", UserRole.ROLE_ADMIN, "ACTIVE");
    private final User manager = new User(20L, "manager", "hash", UserRole.ROLE_MANAGER, "ACTIVE");
    private final User customer = new User(1L, "customer", "hash", UserRole.ROLE_USER, "ACTIVE");

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    // --- Recipient inbox (TrackingServiceImpl.incoming) ---

    @Test void incomingListQueriesOnlyTheCallersOwnReceipts() {
        ShipmentRepository shipments = mock(ShipmentRepository.class);
        when(shipments.findByReceiverIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class))).thenReturn(Page.empty());
        var tracking = new TrackingServiceImpl(shipments, mock(ShipmentEventRepository.class), mock(HubRepository.class));
        LastMileTestFixture.login(customer);

        assertThat(tracking.incoming(0)).isEmpty();

        verify(shipments).findByReceiverIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class));
        verify(shipments, never()).findById(anyLong());
    }

    // --- Withdrawals: admin-only review, owner-bound request ---

    private final WithdrawalRequestRepository withdrawals = mock(WithdrawalRequestRepository.class);
    private final WalletLedgerService ledger = mock(WalletLedgerService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final WithdrawalServiceImpl withdrawalService =
            new WithdrawalServiceImpl(withdrawals, ledger, mock(NotificationEventPublisher.class), users);

    @Test void managerCannotRejectAWithdrawalAndNothingIsReleased() {
        LastMileTestFixture.login(manager);
        assertThatThrownBy(() -> withdrawalService.rejectWithdrawal(new WithdrawalReviewDto(7L, "reason")))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(withdrawals, ledger);
    }

    @Test void adminCanRejectAPendingWithdrawal() {
        WithdrawalRequest pending = WithdrawalRequest.create(1L, new BigDecimal("50000"), "B", "1", "H", "k");
        pending.setId(7L);
        when(withdrawals.findByIdForUpdate(7L)).thenReturn(Optional.of(pending));
        when(withdrawals.save(any())).thenAnswer(i -> i.getArgument(0));
        when(users.findById(1L)).thenReturn(Optional.of(customer));
        LastMileTestFixture.login(admin);

        assertThat(withdrawalService.rejectWithdrawal(new WithdrawalReviewDto(7L, "reason")).getStatus()).isEqualTo("REJECTED");
        verify(ledger).recordWithdrawalRefund(1L, new BigDecimal("50000"), 7L);
    }

    @Test void managerCannotReadTheAdministratorWithdrawalQueue() {
        LastMileTestFixture.login(manager);
        assertThatThrownBy(withdrawalService::getPendingWithdrawalSummaries).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(withdrawals, users);
    }

    @Test void withdrawalRequestLocksOnlyTheCallersWallet() {
        when(withdrawals.save(any())).thenAnswer(i -> { WithdrawalRequest r = i.getArgument(0); r.setId(8L); return r; });
        LastMileTestFixture.login(customer);

        withdrawalService.requestWithdrawal(new WithdrawalRequestDto(new BigDecimal("50000"), "B", "1", "H", "key"));

        verify(ledger).recordWithdrawalLock(1L, new BigDecimal("50000"), 8L);
        verify(ledger, never()).recordWithdrawalLock(eq(100L), any(), any());
    }

    // --- Return fee cash collection (ShipmentFeeService.payReturn) ---

    @Test void cashReturnFeeAtAnotherHubIsDeniedWithoutAnyPayment() {
        LastMileTestFixture f = new LastMileTestFixture();
        f.returnAtOrigin();
        User outsider = new User(7L, "outsider", "hash", UserRole.ROLE_HUB_STAFF, "ACTIVE");
        Hub otherHub = Hub.active("O", "Other", 1, "DELIVERY_HUB", "SOUTH", "P");
        ReflectionTestUtils.setField(otherHub, "id", 200L);
        when(f.profiles.findFirstByUserId(7L)).thenReturn(Optional.of(EmployeeProfile.active(outsider, otherHub, "INBOUND")));
        LastMileTestFixture.login(outsider);

        assertThatThrownBy(() -> f.fees.payReturn(10L, "CASH", false)).isInstanceOf(AccessDeniedException.class);
        assertThat(f.money).doesNotContainKey("RETURN_FEE");
        verifyNoInteractions(f.wallet);
    }

    @Test void cashReturnFeeByAShipperWithoutAnInProgressReturnTaskIsDenied() {
        LastMileTestFixture f = new LastMileTestFixture();
        f.returnAtOrigin();
        LastMileTestFixture.login(f.shipper);

        assertThatThrownBy(() -> f.fees.payReturn(10L, "CASH", false)).isInstanceOf(AccessDeniedException.class);
        assertThat(f.money).doesNotContainKey("RETURN_FEE");
    }

    @Test void cashReturnFeeByInboundStaffAtTheOriginIsRecorded() {
        LastMileTestFixture f = new LastMileTestFixture();
        f.returnAtOrigin();
        LastMileTestFixture.login(f.inbound);

        f.fees.payReturn(10L, "CASH", false);

        assertThat(f.money.get("RETURN_FEE").getCollectedByUserId()).isEqualTo(3L);
    }
}
