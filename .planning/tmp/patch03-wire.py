from pathlib import Path
root=Path('src/main')
p=root/'java/vn/iotstar/service/impl/HubOperationsServiceImpl.java';s=p.read_text(encoding='utf-8')
s=s.replace('        if (senderPays && surcharge.signum() > 0) {','''        Payment surchargePayment = paymentRepository.findFirstByShipmentIdAndPaymentType(shipment.getId(), "WEIGHT_SURCHARGE").orElse(null);
        boolean surchargePaid = false;
        if (senderPays && surcharge.signum() > 0) {
            if (surchargePayment != null) {
                if (!"SENDER".equals(surchargePayment.getPayerType()) || surchargePayment.getAmount().compareTo(surcharge) != 0)
                    throw new IllegalStateException("Assessed surcharge differs from measured obligation");
                if ("PAID".equals(surchargePayment.getStatus())) { ShipmentFeeService.requirePaidEvidence(surchargePayment); surchargePaid = true; }
                else if (!"UNPAID".equals(surchargePayment.getStatus()) && !"PENDING".equals(surchargePayment.getStatus()))
                    throw new IllegalStateException("Surcharge cannot be received");
            }''')
s=s.replace('''            if (!"ACCEPT".equals(command.surchargeDecision())) throw new IllegalArgumentException("Sender must accept the weight surcharge");
            if (paymentRepository.findFirstByShipmentIdAndPaymentType(shipment.getId(), "WEIGHT_SURCHARGE").isPresent())
                throw new IllegalStateException("A weight surcharge already exists; reconcile it before receipt");''','''            if ("PAY_ONLINE".equals(command.surchargeDecision()) && !surchargePaid) {
                if (command.cashReceived() != null && command.cashReceived().signum()!=0) throw new IllegalArgumentException("Do not collect cash while requesting online surcharge payment");
                if (surchargePayment==null) paymentRepository.save(Payment.create(shipment,"SENDER","WEIGHT_SURCHARGE",surcharge,"VNPAY","PENDING"));
                // No custody is accepted yet. The sender pays this server-assessed obligation on their own fee page.
                return;
            }
            if (!"ACCEPT".equals(command.surchargeDecision()) && !surchargePaid) throw new IllegalArgumentException("Sender must accept the weight surcharge");''')
s=s.replace('.add(senderPays ? surcharge : BigDecimal.ZERO);','.add(senderPays && !surchargePaid ? surcharge : BigDecimal.ZERO);')
s=s.replace('payment.markPaid("HUB-" + hub.getId() + "-" + shipment.getId());','payment.collect("CASH", actor.userId(), "HUB-" + hub.getId() + "-" + shipment.getId());')
s=s.replace('''        if (surcharge.signum() > 0 && senderPays) {
            Payment extra = Payment.create(shipment, "SENDER", "WEIGHT_SURCHARGE", surcharge, "CASH", "UNPAID");
            extra.markPaid("HUB-SURCHARGE-" + shipment.getId());
            paymentRepository.save(extra);''','''        if (surcharge.signum() > 0 && senderPays) {
            if (!surchargePaid) {
                Payment extra = surchargePayment != null ? surchargePayment : Payment.create(shipment, "SENDER", "WEIGHT_SURCHARGE", surcharge, "CASH", "UNPAID");
                extra.collect("CASH", actor.userId(), "HUB-SURCHARGE-" + shipment.getId());
                paymentRepository.save(extra);
            }''')
p.write_text(s,encoding='utf-8')
p=root/'java/vn/iotstar/service/impl/DestinationReceiptServiceImpl.java';s=p.read_text(encoding='utf-8');s=s.replace('''        if (returning) item.getShipment().transitionTo(ShipmentStatus.RETURN_TO_SENDER);''','''        if (returning) {
            item.getShipment().transitionTo(ShipmentStatus.RETURN_TO_SENDER);
            if (item.getShipment().getReturnArrivedAtOriginAt()!=null
                    && !shipmentEventRepository.existsByShipmentIdAndEventType(item.getShipment().getId(), "RETURNED_AT_ORIGIN_HUB"))
                shipmentEventRepository.save(ShipmentEvent.of(item.getShipment(),userRepository.getReferenceById(AuthenticatedActor.requireCurrent().userId()),
                        "RETURNED_AT_ORIGIN_HUB","Return parcel physically received at origin"));
        }''');p.write_text(s,encoding='utf-8')
p=root/'java/vn/iotstar/controller/LastMileDeliveryController.java';s=p.read_text(encoding='utf-8');s=s.replace('@PostMapping("/user/return-collection")','@PostMapping({"/user/return-collection", "/hub/return-collection"})');s=s.replace('@GetMapping("/user/return-collection")','@GetMapping({"/user/return-collection", "/hub/return-collection"})');i=s.index('    @PostMapping("/shipper/return-tasks/confirm")');s=s[:i]+'''    @PostMapping("/shipper/return-tasks/scan")
    public String scanReturn(@RequestParam Long shipmentId) {
        lastMileDeliveryService.scanReturnDelivery(shipmentId);return "redirect:/shipper/return-tasks";
    }
    @PostMapping("/hub/delivery-returns/receive")
    public String receiveFailed(@RequestParam Long shipmentId,@RequestParam String trackingNumber) {
        lastMileDeliveryService.receiveFailedDelivery(shipmentId,trackingNumber);return "redirect:/hub/delivery-assignments";
    }
'''+s[i:];p.write_text(s,encoding='utf-8')
p=root/'java/vn/iotstar/service/impl/TrackingServiceImpl.java';s=p.read_text(encoding='utf-8');s=s.replace('Map.entry("DELIVERY_ASSIGNED",', 'Map.entry("RETURN_DELIVERY_ASSIGNED", "Đã phân công giao trả người gửi"),\n            Map.entry("RETURN_OUT_FOR_DELIVERY", "Shipper đang giao trả người gửi"),\n            Map.entry("DELIVERY_ASSIGNED",');p.write_text(s,encoding='utf-8')
p=root/'webapp/WEB-INF/views/hub/sorter-queue.jsp';s=p.read_text(encoding='utf-8');s=s.replace('<option value="REJECT">','<option value="PAY_ONLINE">Yêu cầu thanh toán phụ thu bằng ví / VNPay demo</option><option value="REJECT">');p.write_text(s,encoding='utf-8')
p=root/'webapp/WEB-INF/views/hub/delivery-assignments.jsp';s=p.read_text(encoding='utf-8');s=s.replace('</body>', '''<h2>Nhận lại kiện giao thất bại từ shipper</h2><form method="post" action="${pageContext.request.contextPath}/hub/delivery-returns/receive">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><label>Mã ID vận đơn<input name="shipmentId" type="number" required></label>
<label>Quét mã vận đơn<input name="trackingNumber" required></label><button>Xác nhận nhận kiện vật lý</button></form>
<a href="${pageContext.request.contextPath}/hub/return-collection">Bàn giao kiện hoàn tại hub</a></body>''');p.write_text(s,encoding='utf-8')
p=root/'webapp/WEB-INF/views/layout/sidebar.jsp';s=p.read_text(encoding='utf-8');s=s.replace('/hub/pickup','/hub/pickup',1);s += '''\n<sec:authorize access="hasRole('HUB_STAFF')"><a href="${pageContext.request.contextPath}/hub/delivery-assignments">Phân công giao / hoàn hàng</a><a href="${pageContext.request.contextPath}/hub/return-collection">Nhận hàng hoàn</a></sec:authorize>\n'''
# Existing sidebar uses security tags only if declared; use its prefix inventory below before final compile/render.
p.write_text(s,encoding='utf-8')
