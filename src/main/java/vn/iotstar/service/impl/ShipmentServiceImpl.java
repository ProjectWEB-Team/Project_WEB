package vn.iotstar.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.dto.realtime.ShipmentCreatedEventResponse;
import vn.iotstar.dto.shipment.CreateShipmentRequest;
import vn.iotstar.dto.shipment.QuoteResponse;
import vn.iotstar.dto.shipment.ShipmentDetailResponse;
import vn.iotstar.dto.shipment.ShipmentEventResponse;
import vn.iotstar.dto.shipment.ShipmentSummaryResponse;
import vn.iotstar.entity.Payment;
import vn.iotstar.entity.PricingPolicy;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.RegistrationException;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.exception.ShipmentAccessDeniedException;
import vn.iotstar.realtime.ShipmentEventPublisher;
import vn.iotstar.repository.PaymentRepository;
import vn.iotstar.repository.PricingPolicyRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.ShipmentService;
import vn.iotstar.service.auth.VietnamPhoneNormalizer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/** Intake, server-side quote and permitted payment timing (no wallet, COD settlement or real provider). */
@Service
public class ShipmentServiceImpl implements ShipmentService {
    private static final String SHIPPING_FEE = "SHIPPING_FEE";
    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final UserRepository userRepository;
    private final ShipmentEventPublisher shipmentEventPublisher;
    private final PricingPolicyRepository pricingPolicyRepository;
    private final PaymentRepository paymentRepository;
    private final Clock clock;
    private final ShipmentCancellation cancellation;

    public ShipmentServiceImpl(ShipmentRepository shipmentRepository, ShipmentEventRepository shipmentEventRepository,
                               UserRepository userRepository, ShipmentEventPublisher shipmentEventPublisher,
                               PricingPolicyRepository pricingPolicyRepository, PaymentRepository paymentRepository,
                               Clock clock, ShipmentCancellation cancellation) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentEventRepository = shipmentEventRepository;
        this.userRepository = userRepository;
        this.shipmentEventPublisher = shipmentEventPublisher;
        this.pricingPolicyRepository = pricingPolicyRepository;
        this.paymentRepository = paymentRepository;
        this.clock = clock;
        this.cancellation = cancellation;
    }

    @Override
    @Transactional(readOnly = true)
    public QuoteResponse quote(CreateShipmentRequest request) {
        receiverPhone(request);
        checkPayerAndMethod(request);
        return new QuoteResponse(price(request));
    }

    @Override
    @Transactional
    public Long create(CreateShipmentRequest request) {
        return createShipment(request, null);
    }

    @Override
    @Transactional
    public Long create(CreateShipmentRequest request, BigDecimal acceptedFee) {
        if (acceptedFee == null) {
            throw new IllegalArgumentException("Vui lòng báo giá lại trước khi tạo đơn.");
        }
        return createShipment(request, acceptedFee);
    }

    private Long createShipment(CreateShipmentRequest request, BigDecimal acceptedFee) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        String receiverPhone = receiverPhone(request);
        checkPayerAndMethod(request);
        BigDecimal fee = price(request);
        if (acceptedFee != null && fee.compareTo(acceptedFee) != 0) {
            throw new IllegalArgumentException("Bảng giá đã thay đổi. Vui lòng báo giá lại trước khi tạo đơn.");
        }
        User sender = userRepository.getReferenceById(actor.userId());
        // Only a verified customer account owns a receiver link; otherwise ReceiverLinkService links it later.
        User receiver = userRepository.findByPhone(receiverPhone)
                .filter(u -> u.getRole() == UserRole.ROLE_USER && u.isEnabled() && u.isPhoneVerified()).orElse(null);
        boolean onlinePrepaid = "SENDER".equals(request.getFeePayer()) && "VNPAY".equals(request.getPaymentMethod());

        Shipment shipment = Shipment.create(newTrackingNumber(), sender, request.getSenderName(), request.getSenderPhone(),
                request.getSenderAddress(), request.getReceiverName(), receiverPhone, request.getReceiverAddress());
        shipment.applyIntake(receiver, request.getReceiverProvince(), request.getReceiverDistrict(),
                request.getPickupMethod(), request.getFeePayer(), request.getInspectionPolicy(),
                request.getWeightGrams(), request.getLengthCm(), request.getWidthCm(), request.getHeightCm(),
                request.getDeclaredValue(), request.getCodAmount(), fee,
                onlinePrepaid ? "PENDING" : "UNPAID",
                onlinePrepaid ? ShipmentStatus.WAITING_FOR_PAYMENT : ShipmentStatus.WAITING_FOR_HANDOVER,
                serverTime());
        Shipment savedShipment = shipmentRepository.save(shipment);
        paymentRepository.save(Payment.shippingFee(savedShipment, request.getFeePayer(), fee,
                request.getPaymentMethod(), onlinePrepaid ? "PENDING" : "UNPAID"));
        ShipmentEvent savedEvent = shipmentEventRepository.save(ShipmentEvent.shipmentCreated(savedShipment, sender));
        shipmentEventPublisher.publishAfterCommit(actor, new ShipmentCreatedEventResponse(savedShipment.getTrackingNumber(),
                savedShipment.getCurrentStatus(), savedEvent.getEventType(), savedEvent.getCreatedAt()));
        return savedShipment.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentDetailResponse getDetail(Long shipmentId) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment"));
        if (!shipment.getSender().getId().equals(actor.userId())) {
            throw new ShipmentAccessDeniedException();
        }
        List<ShipmentEventResponse> events = shipmentEventRepository.findByShipmentIdOrderByCreatedAtAsc(shipmentId).stream()
                .map(event -> new ShipmentEventResponse(event.getEventType(), event.getDescription(), event.getCreatedAt()))
                .toList();
        String method = paymentRepository.findFirstByShipmentIdAndPaymentType(shipmentId, SHIPPING_FEE)
                .map(Payment::getMethod).orElse(null);
        return new ShipmentDetailResponse(shipment.getId(), shipment.getTrackingNumber(), shipment.getCurrentStatus(),
                shipment.getSenderName(), shipment.getSenderPhone(), shipment.getSenderAddress(), shipment.getReceiverName(),
                shipment.getReceiverPhone(), shipment.getReceiverAddress(), shipment.getCreatedAt(), events,
                shipment.getShippingFee(), shipment.getPaymentStatus(), method, shipment.getPickupMethod(),
                shipment.getFeePayer(), shipment.getWaitingForHandoverAt());
    }

    @Override
    @Transactional
    public void confirmPayment(Long shipmentId) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        // Serialize confirmation before reading payment state so retries preserve one reference and event.
        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment"));
        if (!shipment.getSender().getId().equals(actor.userId())) {
            throw new ShipmentAccessDeniedException();
        }
        Payment payment = paymentRepository.findFirstByShipmentIdAndPaymentType(shipmentId, SHIPPING_FEE)
                .orElseThrow(() -> new ResourceNotFoundException("Payment"));
        if (!"SENDER".equals(payment.getPayerType()) || !"VNPAY".equals(payment.getMethod())) {
            throw new IllegalArgumentException("Đơn này không thanh toán trực tuyến.");
        }
        if ("PAID".equals(payment.getStatus())) {
            return;
        }
        if (shipment.getCurrentStatus() != ShipmentStatus.WAITING_FOR_PAYMENT) {
            throw new IllegalArgumentException("Đơn không còn ở trạng thái chờ thanh toán.");
        }
        payment.markPaid("DEMO-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        shipment.confirmOnlinePayment(serverTime());
        shipmentEventRepository.save(ShipmentEvent.of(shipment, userRepository.getReferenceById(actor.userId()),
                "PAYMENT_CONFIRMED", "Online payment confirmed (demo)"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentSummaryResponse> listMine(ShipmentStatus status, int page) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        PageRequest pageable = PageRequest.of(Math.max(page, 0), 10);
        Page<Shipment> shipments = status == null
                ? shipmentRepository.findBySenderIdOrderByCreatedAtDesc(actor.userId(), pageable)
                : shipmentRepository.findBySenderIdAndCurrentStatusOrderByCreatedAtDesc(actor.userId(), status, pageable);
        return shipments.map(s -> new ShipmentSummaryResponse(s.getId(), s.getTrackingNumber(), s.getReceiverName(),
                s.getCurrentStatus(), s.getShippingFee(), s.getPaymentStatus(), s.getCreatedAt()));
    }

    @Override
    @Transactional
    public void cancel(Long shipmentId) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        // Same row lock as pickup completion: only one of cancel / PICKED_UP can win.
        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment"));
        if (!shipment.getSender().getId().equals(actor.userId())) {
            throw new ShipmentAccessDeniedException();
        }
        if (!EnumSet.of(ShipmentStatus.CREATED, ShipmentStatus.WAITING_FOR_HANDOVER).contains(shipment.getCurrentStatus())) {
            throw new IllegalArgumentException("Đơn đã được bàn giao hoặc đã đóng, không thể hủy.");
        }
        cancellation.cancel(shipment, "SENDER_REQUEST", userRepository.getReferenceById(actor.userId()));
    }

    private String receiverPhone(CreateShipmentRequest request) {
        try {
            return VietnamPhoneNormalizer.normalize(request.getReceiverPhone());
        } catch (RegistrationException exception) {
            throw new IllegalArgumentException(exception.getMessage());
        }
    }

    private LocalDateTime serverTime() {
        return ServerTime.now(clock);
    }

    private void checkPayerAndMethod(CreateShipmentRequest request) {
        if ("RECEIVER".equals(request.getFeePayer()) && !"CASH".equals(request.getPaymentMethod())) {
            throw new IllegalArgumentException("Người nhận trả cước chỉ thanh toán tiền mặt khi nhận hàng.");
        }
    }

    /** Actual weight only (no volumetric rule), rounded to whole VND with HALF_UP. */
    private BigDecimal price(CreateShipmentRequest request) {
        PricingPolicy policy = pricingPolicyRepository
                .findFirstByServiceCodeAndFromRegionAndToRegionAndStatusOrderByIdDesc(
                        request.getServiceCode(), request.getFromRegion(), request.getToRegion(), "ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException("Chưa có bảng giá cho dịch vụ và tuyến này."));
        BigDecimal fee = policy.getBasePrice();
        int extraGrams = request.getWeightGrams() - policy.getBaseWeightG();
        if (extraGrams > 0) {
            int steps = (extraGrams + policy.getExtraWeightStepG() - 1) / policy.getExtraWeightStepG();
            fee = fee.add(policy.getExtraWeightPrice().multiply(BigDecimal.valueOf(steps)));
        }
        fee = fee.add(percent(request.getDeclaredValue(), policy.getInsuranceRatePercent()))
                .add(percent(request.getCodAmount(), policy.getCodFeePercent()));
        return fee.setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(BigDecimal amount, BigDecimal rate) {
        return rate == null ? BigDecimal.ZERO : amount.multiply(rate).divide(BigDecimal.valueOf(100));
    }

    private String newTrackingNumber() {
        return "UTX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
