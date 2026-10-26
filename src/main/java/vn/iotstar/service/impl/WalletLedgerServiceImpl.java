package vn.iotstar.service.impl;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.dto.wallet.WalletResponse;
import vn.iotstar.entity.Wallet;
import vn.iotstar.entity.UserRole;
import vn.iotstar.entity.WalletTransaction;
import vn.iotstar.repository.WalletRepository;
import vn.iotstar.repository.WalletTransactionRepository;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.WalletLedgerService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WalletLedgerServiceImpl implements WalletLedgerService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    public WalletLedgerServiceImpl(WalletRepository walletRepository,
                                   WalletTransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> walletRepository.save(Wallet.createForUser(userId)));
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWalletResponse(Long userId) {
        requireOwnerOrAdmin(userId);
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> Wallet.createForUser(userId));
        return new WalletResponse(wallet.getUserId(), wallet.getBalance(), wallet.getLockedBalance(), wallet.getStatus());
    }

    @Override
    public WalletTransaction recordWithdrawalLock(Long userId, BigDecimal amount, Long withdrawalRequestId) {
        Wallet wallet = getOrCreateWallet(userId);
        BigDecimal before = wallet.getBalance();
        wallet.lockFunds(amount);
        BigDecimal after = wallet.getBalance();
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.create(
                userId, "WITHDRAWAL_LOCK", amount, before, after,
                withdrawalRequestId, "WITHDRAWAL", "Locked for withdrawal #" + withdrawalRequestId);
        return transactionRepository.save(tx);
    }

    @Override
    public WalletTransaction recordWithdrawalDeduct(Long userId, BigDecimal amount, Long withdrawalRequestId) {
        Wallet wallet = getOrCreateWallet(userId);
        BigDecimal before = wallet.getBalance();
        wallet.deductLocked(amount);
        BigDecimal after = wallet.getBalance();
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.create(
                userId, "WITHDRAWAL_DEDUCT", amount, before, after,
                withdrawalRequestId, "WITHDRAWAL", "Deducted on approval for withdrawal #" + withdrawalRequestId);
        return transactionRepository.save(tx);
    }

    @Override
    public WalletTransaction recordWithdrawalRefund(Long userId, BigDecimal amount, Long withdrawalRequestId) {
        Wallet wallet = getOrCreateWallet(userId);
        BigDecimal before = wallet.getBalance();
        wallet.refundLocked(amount);
        BigDecimal after = wallet.getBalance();
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.create(
                userId, "WITHDRAWAL_REFUND", amount, before, after,
                withdrawalRequestId, "WITHDRAWAL", "Refunded locked balance on rejection for withdrawal #" + withdrawalRequestId);
        return transactionRepository.save(tx);
    }

    @Override
    public WalletTransaction creditRefund(Long userId, BigDecimal amount, Long paymentId, Long shipmentId, String description) {
        // Enforce reference uniqueness
        Optional<WalletTransaction> existing = transactionRepository
                .findFirstByTransactionTypeAndReferenceTypeAndReferenceId("REFUND", "PAYMENT", paymentId);
        if (existing.isPresent()) {
            throw new IllegalStateException("Refund already credited for payment: " + paymentId);
        }

        Wallet wallet = getOrCreateWallet(userId);
        BigDecimal before = wallet.getBalance();
        wallet.credit(amount);
        BigDecimal after = wallet.getBalance();
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.create(
                userId, "REFUND", amount, before, after,
                paymentId, "PAYMENT", description != null ? description : "Refund for payment #" + paymentId);
        return transactionRepository.save(tx);
    }

    @Override
    public WalletTransaction creditCompensation(Long userId, BigDecimal amount, Long incidentId, String description) {
        // Enforce reference uniqueness
        Optional<WalletTransaction> existing = transactionRepository
                .findFirstByTransactionTypeAndReferenceTypeAndReferenceId("COMPENSATION", "INCIDENT", incidentId);
        if (existing.isPresent()) {
            throw new IllegalStateException("Compensation already credited for incident: " + incidentId);
        }

        Wallet wallet = getOrCreateWallet(userId);
        BigDecimal before = wallet.getBalance();
        wallet.credit(amount);
        BigDecimal after = wallet.getBalance();
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.create(
                userId, "COMPENSATION", amount, before, after,
                incidentId, "INCIDENT", description != null ? description : "Compensation for incident #" + incidentId);
        return transactionRepository.save(tx);
    }

    @Override
    public WalletTransaction recordFeeDebit(Long userId, BigDecimal amount, String transactionType,
                                            Long shipmentId, String description) {
        // Enforce reference uniqueness so the same fee is never debited twice.
        Optional<WalletTransaction> existing = transactionRepository
                .findFirstByTransactionTypeAndReferenceTypeAndReferenceId(transactionType, "SHIPMENT", shipmentId);
        if (existing.isPresent()) {
            throw new IllegalStateException("Fee already debited for shipment: " + shipmentId);
        }

        Wallet wallet = getOrCreateWallet(userId);
        BigDecimal before = wallet.getBalance();
        wallet.debit(amount);
        BigDecimal after = wallet.getBalance();
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.create(
                userId, transactionType, amount, before, after,
                shipmentId, "SHIPMENT", description != null ? description : "Fee payment for shipment #" + shipmentId);
        return transactionRepository.save(tx);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransaction> getTransactions(Long userId) {
        requireOwnerOrAdmin(userId);
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private static void requireOwnerOrAdmin(Long userId) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        if (!actor.hasRole(UserRole.ROLE_ADMIN) && !actor.userId().equals(userId)) {
            throw new AccessDeniedException("Wallet owner required");
        }
    }
}
