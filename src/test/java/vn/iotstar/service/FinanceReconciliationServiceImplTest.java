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
import vn.iotstar.dto.finance.ReconciliationSubmissionRequest;
import vn.iotstar.entity.CodTransaction;
import vn.iotstar.entity.Incident;
import vn.iotstar.entity.Payment;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.ShipperReconciliation;
import vn.iotstar.entity.ShipperReconciliationItem;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.entity.Wallet;
import vn.iotstar.entity.WalletTransaction;
import vn.iotstar.realtime.NotificationEventPublisher;
import vn.iotstar.repository.CodTransactionRepository;
import vn.iotstar.repository.FinanceReconciliationRepository;
import vn.iotstar.repository.IncidentRepository;
import vn.iotstar.repository.PaymentRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.repository.ShipperReconciliationItemRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.repository.WalletRepository;
import vn.iotstar.repository.WalletTransactionRepository;
import vn.iotstar.service.impl.FinanceReconciliationServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceReconciliationServiceImplTest {

    @Mock
    private FinanceReconciliationRepository reconciliationRepository;
    @Mock
    private ShipperReconciliationItemRepository itemRepository;
    @Mock
    private CodTransactionRepository codTransactionRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private WalletTransactionRepository walletTransactionRepository;
    @Mock
    private IncidentRepository incidentRepository;
    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ShipmentEventRepository shipmentEventRepository;
    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    private FinanceReconciliationServiceImpl reconciliationService;

    private User manager;
    private User shipper;
    private User sender;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        reconciliationService = new FinanceReconciliationServiceImpl(
                reconciliationRepository,
                itemRepository,
                codTransactionRepository,
                paymentRepository,
                walletRepository,
                walletTransactionRepository,
                incidentRepository,
                shipmentRepository,
                userRepository,
                shipmentEventRepository,
                notificationEventPublisher
        );

        manager = new User(100L, "manager_test", "hash", UserRole.ROLE_MANAGER, "ACTIVE");
        shipper = new User(10L, "shipper_test", "hash", UserRole.ROLE_SHIPPER, "ACTIVE");
        sender = new User(1L, "sender_test", "hash", UserRole.ROLE_USER, "ACTIVE");

        shipment = Shipment.create("TRK111222", sender, "Sender", "0901", "Address 1", "Receiver", "0902", "Address 2");
        shipment.setId(1L);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(manager, null, manager.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Role check: Non-manager / non-admin cannot reconcile")
    void nonManagerIsDenied() {
        User regularUser = new User(2L, "user_reg", "hash", UserRole.ROLE_USER, "ACTIVE");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(regularUser, null, regularUser.getAuthorities()));

        ReconciliationSubmissionRequest request = new ReconciliationSubmissionRequest(10L, LocalDate.now(), List.of());
        assertThatThrownBy(() -> reconciliationService.reconcile(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ROLE_MANAGER or ROLE_ADMIN required");
    }

    @Test
    @DisplayName("Matched COD: Reconciles to READY_FOR_PAYOUT, then pays out to the sender wallet")
    void matchedCodPayoutSuccess() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(manager));
        when(userRepository.findById(10L)).thenReturn(Optional.of(shipper));
        when(reconciliationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(itemRepository.findBySourceTypeAndSourceId("COD_TRANSACTION", 50L)).thenReturn(Optional.empty());

        CodTransaction cod = CodTransaction.createCollected(shipment, shipper, new BigDecimal("500000.00"), "CASH");
        when(codTransactionRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(cod));

        Wallet senderWallet = Wallet.createForUser(sender.getId());
        when(walletRepository.findByUserIdForUpdate(sender.getId())).thenReturn(Optional.of(senderWallet));

        ReconciliationSubmissionRequest.ItemRequest item = new ReconciliationSubmissionRequest.ItemRequest(
                1L, "COD_TRANSACTION", 50L, new BigDecimal("500000.00"), new BigDecimal("500000.00"), "Matched COD");
        ReconciliationSubmissionRequest request = new ReconciliationSubmissionRequest(
                10L, LocalDate.now(), List.of(item));

        ShipperReconciliation result = reconciliationService.reconcile(request);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        // The reconciliation only makes the COD ready; the wallet credit is a
        // separate payout step of the Workflow.
        assertThat(cod.getStatus()).isEqualTo("READY_FOR_PAYOUT");
        assertThat(senderWallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        when(codTransactionRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(cod));
        ShipperReconciliationItem reconciledItem = mock(ShipperReconciliationItem.class);
        when(itemRepository.findBySourceTypeAndSourceId("COD_TRANSACTION", 50L))
                .thenReturn(Optional.of(reconciledItem));

        reconciliationService.payoutReadyCod(50L);

        assertThat(cod.getStatus()).isEqualTo("PAID_TO_SENDER");
        assertThat(cod.getPayoutAt()).isNotNull();
        assertThat(senderWallet.getBalance()).isEqualByComparingTo(new BigDecimal("500000.00"));

        verify(walletRepository).save(senderWallet);
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
        verify(incidentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Matched receiver cash fee: Reconciles accountability but NEVER credits wallet or creates COD_PAYOUT")
    void matchedReceiverCashFeeNoPayout() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(manager));
        when(userRepository.findById(10L)).thenReturn(Optional.of(shipper));
        when(reconciliationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(itemRepository.findBySourceTypeAndSourceId("RECEIVER_CASH_SHIPPING_FEE_PAYMENT", 60L)).thenReturn(Optional.empty());

        Payment feePayment = Payment.create(shipment, "RECEIVER", "SHIPPING_FEE", new BigDecimal("35000.00"), "CASH", "PAID");
        when(paymentRepository.findByIdForUpdate(60L)).thenReturn(Optional.of(feePayment));

        ReconciliationSubmissionRequest.ItemRequest item = new ReconciliationSubmissionRequest.ItemRequest(
                1L, "RECEIVER_CASH_SHIPPING_FEE_PAYMENT", 60L, new BigDecimal("35000.00"), new BigDecimal("35000.00"), "Fee match");
        ReconciliationSubmissionRequest request = new ReconciliationSubmissionRequest(
                10L, LocalDate.now(), List.of(item));

        ShipperReconciliation result = reconciliationService.reconcile(request);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        verify(walletRepository, never()).findByUserIdForUpdate(any());
        verify(walletTransactionRepository, never()).save(any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Discrepant COD: Item shortage holds in HOLD_DISPUTE and creates MONEY_SHORTAGE incident")
    void discrepantCodCreatesFinancialIncident() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(manager));
        when(userRepository.findById(10L)).thenReturn(Optional.of(shipper));
        when(reconciliationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(itemRepository.findBySourceTypeAndSourceId("COD_TRANSACTION", 50L)).thenReturn(Optional.empty());

        CodTransaction cod = CodTransaction.createCollected(shipment, shipper, new BigDecimal("500000.00"), "CASH");
        when(codTransactionRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(cod));

        // Actual is 450,000 instead of 500,000 -> SHORTAGE
        ReconciliationSubmissionRequest.ItemRequest item = new ReconciliationSubmissionRequest.ItemRequest(
                1L, "COD_TRANSACTION", 50L, new BigDecimal("500000.00"), new BigDecimal("450000.00"), "Shortage 50k");
        ReconciliationSubmissionRequest request = new ReconciliationSubmissionRequest(
                10L, LocalDate.now(), List.of(item));

        ShipperReconciliation result = reconciliationService.reconcile(request);

        assertThat(result.getStatus()).isEqualTo("COMPLETED_WITH_DISCREPANCY");
        assertThat(cod.getStatus()).isEqualTo("HOLD_DISPUTE");
        verify(walletRepository, never()).findByUserIdForUpdate(any());
        verify(incidentRepository).save(any(Incident.class));
    }

    @Test
    @DisplayName("Reconciliation uniqueness: Reconciling already-reconciled source throws IllegalStateException")
    void duplicateSourceReconciledFails() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(manager));
        when(userRepository.findById(10L)).thenReturn(Optional.of(shipper));
        when(reconciliationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShipperReconciliationItem existing = mock(ShipperReconciliationItem.class);
        when(itemRepository.findBySourceTypeAndSourceId("COD_TRANSACTION", 50L)).thenReturn(Optional.of(existing));

        ReconciliationSubmissionRequest.ItemRequest item = new ReconciliationSubmissionRequest.ItemRequest(
                1L, "COD_TRANSACTION", 50L, new BigDecimal("500000.00"), new BigDecimal("500000.00"), "Duplicate");
        ReconciliationSubmissionRequest request = new ReconciliationSubmissionRequest(
                10L, LocalDate.now(), List.of(item));

        assertThatThrownBy(() -> reconciliationService.reconcile(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Source already reconciled");
    }
}

