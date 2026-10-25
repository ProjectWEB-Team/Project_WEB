package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.WalletTransaction;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<WalletTransaction> findFirstByTransactionTypeAndReferenceTypeAndReferenceId(
            String transactionType, String referenceType, Long referenceId);
}

