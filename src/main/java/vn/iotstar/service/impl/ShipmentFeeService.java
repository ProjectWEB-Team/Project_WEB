package vn.iotstar.service.impl;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.dto.returnflow.ShipmentFeeResponse;
import vn.iotstar.entity.*;
import vn.iotstar.repository.*;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.WalletLedgerService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Authoritative fee obligations; a client amount is never payment evidence. */
@Service
@Transactional
public class ShipmentFeeService {
    private final ShipmentRepository shipments;
    private final PaymentRepository payments;
    private final ShipmentAssignmentRepository assignments;
    private final ShipmentEventRepository events;
    private final UserRepository users;
    private final WalletLedgerService wallet;
    private final LastMileAccess access;
    public ShipmentFeeService(ShipmentRepository shipments, PaymentRepository payments,
            ShipmentAssignmentRepository assignments, ShipmentEventRepository events,
            UserRepository users, WalletLedgerService wallet, LastMileAccess access) {
        this.shipments=shipments;this.payments=payments;this.assignments=assignments;
        this.events=events;this.users=users;this.wallet=wallet;this.access=access;
    }
    @Transactional(readOnly=true)
    public ShipmentFeeResponse view(Long shipmentId) {
        var s=shipments.findById(shipmentId).orElseThrow(()->new AccessDeniedException("Shipment unavailable"));
        requireOwner(s);
        boolean returning="RETURN".equals(s.getDirection());
        return new ShipmentFeeResponse(s.getId(),s.getTrackingNumber(),returning?forwardOutstanding(s):BigDecimal.ZERO,
                returning?outstanding(s,"RETURN_FEE",nonnegative(s.getReturnFee())):BigDecimal.ZERO,
                payments.findFirstByShipmentIdAndPaymentType(s.getId(),"WEIGHT_SURCHARGE")
                        .filter(p->!"PAID".equals(p.getStatus())).map(Payment::getAmount).orElse(BigDecimal.ZERO));
    }
    public void payReturn(Long shipmentId, String method, boolean demoConfirmed) {
        if(method==null) throw new IllegalArgumentException("Payment method required");
        Shipment s=lock(shipmentId);
        if (!"RETURN".equals(s.getDirection()) || s.isOnHold()
                || !(s.getCurrentStatus()==ShipmentStatus.RETURN_TO_SENDER || s.getCurrentStatus()==ShipmentStatus.OUT_FOR_DELIVERY))
            throw new IllegalStateException("Return fees cannot be settled in this state");
        Long collector=authorizeSettlement(s,method,demoConfirmed);
        BigDecimal forward=forwardOutstanding(s);
        BigDecimal fee=nonnegative(s.getReturnFee());
        // Validate both obligations before any ledger mutation; failure rolls back the whole settlement.
        validateExisting(s,"FORWARD_FEE_ON_RETURN",forwardLiability(s));
        validateExisting(s,"RETURN_FEE",fee);
        for(String type:List.of("FORWARD_FEE_ON_RETURN","RETURN_FEE"))
            payments.findFirstByShipmentIdAndPaymentType(s.getId(),type).filter(p->"PAID".equals(p.getStatus()))
                    .ifPresent(p->requireMatchingReplay(p,method,collector));
        if(forward.signum()>0) settle(s,"FORWARD_FEE_ON_RETURN",forward,method,collector);
        if(outstanding(s,"RETURN_FEE",fee).signum()>0) settle(s,"RETURN_FEE",fee,method,collector);
    }
    public void paySurcharge(Long shipmentId, String method, boolean demoConfirmed) {
        if(method==null) throw new IllegalArgumentException("Payment method required");
        Shipment s=lock(shipmentId); requireOwner(s);
        if (s.getCurrentStatus()!=ShipmentStatus.WAITING_FOR_HANDOVER || s.isOnHold()
                || !"SELF_DROPOFF".equals(s.getPickupMethod())) throw new IllegalStateException("Surcharge not payable now");
        if (!List.of("E_WALLET","VNPAY").contains(method) || "VNPAY".equals(method)&&!demoConfirmed)
            throw new IllegalArgumentException("Use wallet or the explicitly confirmed demo VNPay action");
        Payment p=payments.findFirstByShipmentIdAndPaymentType(s.getId(),"WEIGHT_SURCHARGE")
                .orElseThrow(()->new IllegalStateException("Hub must assess the surcharge first"));
        if(!"SENDER".equals(p.getPayerType()) || p.getAmount()==null || p.getAmount().signum()<=0)
            throw new IllegalStateException("Invalid surcharge obligation");
        if("PAID".equals(p.getStatus())) { requireMatchingReplay(p,method,AuthenticatedActor.requireCurrent().userId()); return; }
        settle(s,"WEIGHT_SURCHARGE",p.getAmount(),method,AuthenticatedActor.requireCurrent().userId());
    }
    public void requireReturnSettled(Shipment s) {
        if(s.getReturnFee()==null) throw new IllegalStateException("Return fee has not been priced");
        if(forwardOutstanding(s).signum()>0 || outstanding(s,"RETURN_FEE",nonnegative(s.getReturnFee())).signum()>0)
            throw new IllegalStateException("Sender must settle all forward and return fees before release");
    }
    public BigDecimal returnOutstanding(Shipment s) {
        return forwardOutstanding(s).add(outstanding(s,"RETURN_FEE",nonnegative(s.getReturnFee())));
    }
    private BigDecimal forwardLiability(Shipment s) {
        BigDecimal expected=nonnegative(s.getShippingFee());
        if(expected.signum()==0) return BigDecimal.ZERO;
        Payment original=payments.findFirstByShipmentIdAndPaymentType(s.getId(),"SHIPPING_FEE")
                .orElseThrow(()->new IllegalStateException("Original shipping obligation is missing"));
        if(original.getAmount()==null || original.getAmount().compareTo(expected)!=0)
            throw new IllegalStateException("Original shipping obligation does not match shipment");
        if(!Objects.equals(original.getPayerType(),s.getFeePayer())) throw new IllegalStateException("Original payer does not match shipment");
        if("PAID".equals(original.getStatus())) { requirePaidEvidence(original); return BigDecimal.ZERO; }
        if(!List.of("UNPAID","PENDING").contains(original.getStatus())) throw new IllegalStateException("Invalid forward fee status");
        return expected;
    }
    public BigDecimal forwardOutstanding(Shipment s) { return outstanding(s,"FORWARD_FEE_ON_RETURN",forwardLiability(s)); }
    private BigDecimal outstanding(Shipment s,String type,BigDecimal expected) {
        var p=payments.findFirstByShipmentIdAndPaymentType(s.getId(),type);
        if(p.isEmpty()) return expected;
        validate(p.get(),expected);
        if("PAID".equals(p.get().getStatus())) { requirePaidEvidence(p.get()); return BigDecimal.ZERO; }
        if(!List.of("UNPAID","PENDING").contains(p.get().getStatus())) throw new IllegalStateException("Fee is not settleable");
        return expected;
    }
    private void validateExisting(Shipment s,String type,BigDecimal amount) {
        payments.findFirstByShipmentIdAndPaymentType(s.getId(),type).ifPresent(p->validate(p,amount));
    }
    private void validate(Payment p,BigDecimal expected) {
        if(!"SENDER".equals(p.getPayerType()) || p.getAmount()==null || p.getAmount().compareTo(expected)!=0)
            throw new IllegalStateException("Stored fee does not match authoritative obligation");
    }
    private void requireMatchingReplay(Payment p,String method,Long collector) {
        requirePaidEvidence(p);
        if(!method.equals(p.getMethod()) || !Objects.equals(collector,p.getCollectedByUserId()))
            throw new IllegalStateException("Settled payment does not match replay");
    }
    private void settle(Shipment s,String type,BigDecimal amount,String method,Long collector) {
        Payment p=payments.findFirstByShipmentIdAndPaymentType(s.getId(),type)
                .orElseGet(()->payments.save(Payment.create(s,"SENDER",type,amount,method,"UNPAID")));
        validate(p,amount);
        if("PAID".equals(p.getStatus())) { requirePaidEvidence(p); return; }
        if("E_WALLET".equals(method)) wallet.recordFeeDebit(s.getSender().getId(),amount,type+"_PAYMENT",s.getId(),"Sender shipment fee");
        p.collect(method,collector,("VNPAY".equals(method)?"DEMO-VNPAY-":"FEE-")+type+"-"+s.getId());
        payments.save(p);
        events.save(ShipmentEvent.of(s,users.getReferenceById(AuthenticatedActor.requireCurrent().userId()),type+"_PAID","Shipment fee settled"));
    }
    private Long authorizeSettlement(Shipment s,String method,boolean demoConfirmed) {
        var actor=AuthenticatedActor.requireCurrent();
        if(List.of("E_WALLET","VNPAY").contains(method)) {
            requireOwner(s);
            if("VNPAY".equals(method)&&!demoConfirmed) throw new IllegalArgumentException("Demo VNPay confirmation required");
            return actor.userId();
        }
        if(!"CASH".equals(method)) throw new IllegalArgumentException("Unsupported fee payment method");
        if(actor.hasRole(UserRole.ROLE_HUB_STAFF)) {
            var profile=access.require(UserRole.ROLE_HUB_STAFF,"INBOUND","GENERAL_STAFF"); access.requireAt(s,profile);
            if(!profile.getHubId().equals(s.getOriginHubId()) || s.getReturnArrivedAtOriginAt()==null)
                throw new AccessDeniedException("Return has not reached its origin");
        } else {
            var p=access.require(UserRole.ROLE_SHIPPER);
            if(!p.getHubId().equals(s.getOriginHubId()) || s.getCurrentStatus()!=ShipmentStatus.OUT_FOR_DELIVERY
                    || s.getCurrentHubId()!=null || assignments.findByShipmentIdOrderByAssignedAtDesc(s.getId()).stream()
                    .noneMatch(a->"RETURN_DELIVERY".equals(a.getAssignmentType())&&"IN_PROGRESS".equals(a.getStatus())&&actor.userId().equals(a.getAssignee().getId())))
                throw new AccessDeniedException("Return collection requires outbound assigned custody");
        }
        return actor.userId();
    }
    private void requireOwner(Shipment s) {
        var a=AuthenticatedActor.requireCurrent();
        if(!a.hasRole(UserRole.ROLE_USER)||!a.userId().equals(s.getSender().getId())) throw new AccessDeniedException("Sender ownership required");
    }
    private Shipment lock(Long id) { return shipments.findByIdForUpdate(id).orElseThrow(()->new AccessDeniedException("Shipment unavailable")); }
    private BigDecimal nonnegative(BigDecimal amount) {
        if(amount==null) return BigDecimal.ZERO;
        if(amount.signum()<0) throw new IllegalStateException("Negative fee policy"); return amount;
    }
    public static void requirePaidEvidence(Payment p) {
        if(!"PAID".equals(p.getStatus()) || p.getPaidAt()==null || p.getTransactionRef()==null
                || !List.of("CASH","VNPAY","E_WALLET").contains(p.getMethod())) throw new IllegalStateException("Settled payment evidence missing");
    }
}
