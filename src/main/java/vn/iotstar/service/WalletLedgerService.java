package vn.iotstar.service;

import vn.iotstar.dto.wallet.WalletResponse;
import vn.iotstar.entity.Wallet;
import vn.iotstar.entity.WalletTransaction;

import java.math.BigDecimal;
import java.util.List;

public interface WalletLedgerService {

    Wallet getOrCreateWallet(Long userId);

    WalletResponse getWalletResponse(Long userId);

    WalletTransaction recordWithdrawalLock(Long userId, BigDecimal amount, Long withdrawalRequestId);

    WalletTransaction recordWithdrawalDeduct(Long userId, BigDecimal amount, Long withdrawalRequestId);

    WalletTransaction recordWithdrawalRefund(Long userId, BigDecimal amount, Long withdrawalRequestId);

    WalletTransaction creditRefund(Long userId, BigDecimal amount, Long paymentId, Long shipmentId, String description);

    WalletTransaction creditCompensation(Long userId, BigDecimal amount, Long incidentId, String description);

    /**
     * Debits the available balance for a fee the wallet owner owes, such as a
     * return fee or a weight surcharge paid from the e-wallet.
     */
    WalletTransaction recordFeeDebit(Long userId, BigDecimal amount, String transactionType,
                                     Long shipmentId, String description);

    List<WalletTransaction> getTransactions(Long userId);
}

