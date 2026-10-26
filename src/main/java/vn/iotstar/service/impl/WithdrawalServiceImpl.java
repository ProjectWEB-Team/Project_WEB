package vn.iotstar.service.impl;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.dto.realtime.NotificationEventResponse;
import vn.iotstar.dto.wallet.WithdrawalRequestDto;
import vn.iotstar.dto.wallet.WithdrawalReviewDto;
import vn.iotstar.dto.wallet.WithdrawalSummaryResponse;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.entity.WithdrawalRequest;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.realtime.NotificationEventPublisher;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.repository.WithdrawalRequestRepository;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.WalletLedgerService;
import vn.iotstar.service.WithdrawalService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WithdrawalServiceImpl implements WithdrawalService {

    private final WithdrawalRequestRepository withdrawalRepository;
    private final WalletLedgerService walletLedgerService;
    private final NotificationEventPublisher notificationEventPublisher;
    private final UserRepository userRepository;

    public WithdrawalServiceImpl(WithdrawalRequestRepository withdrawalRepository,
                                 WalletLedgerService walletLedgerService,
                                 NotificationEventPublisher notificationEventPublisher,
                                 UserRepository userRepository) {
        this.withdrawalRepository = withdrawalRepository;
        this.walletLedgerService = walletLedgerService;
        this.notificationEventPublisher = notificationEventPublisher;
        this.userRepository = userRepository;
    }

    @Override
    public WithdrawalRequest requestWithdrawal(WithdrawalRequestDto dto) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        Long userId = actor.userId();

        // Idempotency check with request_key
        if (dto.getRequestKey() != null && !dto.getRequestKey().isBlank()) {
            Optional<WithdrawalRequest> existing = withdrawalRepository.findByUserIdAndRequestKey(userId, dto.getRequestKey());
            if (existing.isPresent()) {
                WithdrawalRequest req = existing.get();
                // If matching request payload, return existing
                if (req.getAmount().compareTo(dto.getAmount()) == 0
                        && req.getAccountNumber().equals(dto.getAccountNumber())) {
                    return req;
                }
                throw new IllegalStateException("Conflicting withdrawal request with the same key");
            }
        }

        // Create request
        WithdrawalRequest request = WithdrawalRequest.create(
                userId, dto.getAmount(), dto.getBankName(),
                dto.getAccountNumber(), dto.getAccountHolderName(), dto.getRequestKey());
        request = withdrawalRepository.save(request);

        // Lock funds in wallet and record WITHDRAWAL_LOCK ledger entry
        walletLedgerService.recordWithdrawalLock(userId, dto.getAmount(), request.getId());

        return request;
    }

    @Override
    public WithdrawalRequest approveWithdrawal(WithdrawalReviewDto dto) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        if (!actor.hasRole(UserRole.ROLE_ADMIN)) {
            throw new AccessDeniedException("ROLE_ADMIN required to approve withdrawals");
        }

        WithdrawalRequest request = withdrawalRepository.findByIdForUpdate(dto.getRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal request not found: " + dto.getRequestId()));

        if (!"PENDING".equals(request.getStatus())) {
            if ("COMPLETED".equals(request.getStatus())) {
                return request; // Idempotent repeat
            }
            throw new IllegalStateException("Only PENDING withdrawals can be approved. Current: " + request.getStatus());
        }

        // Deduct locked balance and record WITHDRAWAL_DEDUCT ledger entry
        walletLedgerService.recordWithdrawalDeduct(request.getUserId(), request.getAmount(), request.getId());

        request.setStatus("COMPLETED");
        request.setApprovedAt(LocalDateTime.now());
        request.setCompletedAt(LocalDateTime.now());
        WithdrawalRequest approved = withdrawalRepository.save(request);

        notifyOwner(request, "WITHDRAWAL_APPROVED", "Your withdrawal was approved");

        return approved;
    }

    @Override
    public WithdrawalRequest rejectWithdrawal(WithdrawalReviewDto dto) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        if (!actor.hasRole(UserRole.ROLE_ADMIN)) {
            throw new AccessDeniedException("ROLE_ADMIN required to reject withdrawals");
        }

        if (dto.getRejectionReason() == null || dto.getRejectionReason().isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }

        WithdrawalRequest request = withdrawalRepository.findByIdForUpdate(dto.getRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal request not found: " + dto.getRequestId()));

        if (!"PENDING".equals(request.getStatus())) {
            if ("REJECTED".equals(request.getStatus())) {
                return request; // Idempotent repeat
            }
            throw new IllegalStateException("Only PENDING withdrawals can be rejected. Current: " + request.getStatus());
        }

        // Refund locked balance back to available balance and record WITHDRAWAL_REFUND
        walletLedgerService.recordWithdrawalRefund(request.getUserId(), request.getAmount(), request.getId());

        request.setStatus("REJECTED");
        request.setRejectionReason(dto.getRejectionReason());
        request.setRejectedAt(LocalDateTime.now());
        WithdrawalRequest rejected = withdrawalRepository.save(request);

        notifyOwner(request, "WITHDRAWAL_REJECTED", "Your withdrawal was rejected. Review its details in your wallet.");

        return rejected;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WithdrawalRequest> getUserWithdrawals(Long userId) {
        AuthenticatedActor actor=AuthenticatedActor.requireCurrent();
        if (!actor.hasRole(UserRole.ROLE_ADMIN) && !actor.userId().equals(userId)) throw new AccessDeniedException("Withdrawal owner required");
        return withdrawalRepository.findByUserIdOrderByRequestedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WithdrawalRequest> getPendingWithdrawals() {
        if (!AuthenticatedActor.requireCurrent().hasRole(UserRole.ROLE_ADMIN)) throw new AccessDeniedException("Administrator required");
        return withdrawalRepository.findByStatusOrderByRequestedAtAsc("PENDING");
    }

    @Override
    @Transactional(readOnly = true)
    public List<WithdrawalSummaryResponse> getPendingWithdrawalSummaries() {
        return getPendingWithdrawals().stream()
                .map(request -> {
                    String username = userRepository.findById(request.getUserId())
                            .map(User::getUsername)
                            .orElse("user#" + request.getUserId());
                    return new WithdrawalSummaryResponse(
                            request.getId(), request.getUserId(), username, request.getAmount(), request.getBankName(),
                            request.getAccountNumber(), request.getAccountHolderName(), request.getStatus(),
                            request.getRejectionReason(), request.getRequestedAt());
                })
                .toList();
    }

    private void notifyOwner(WithdrawalRequest request, String type, String message) {
        User owner=userRepository.findById(request.getUserId()).orElseThrow(() -> new ResourceNotFoundException("Withdrawal owner"));
        notificationEventPublisher.publishToUserAfterCommit(owner.getUsername(),
                new NotificationEventResponse(type, "WITHDRAWAL#"+request.getId(), message));
    }
}

