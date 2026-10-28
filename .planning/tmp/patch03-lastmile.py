from pathlib import Path
p=Path('src/main/java/vn/iotstar/service/impl/LastMileDeliveryServiceImpl.java');s=p.read_text(encoding='utf-8')
s=s.replace('    private final StorageService storageService;', '    private final StorageService storageService;\n    private final LastMileAccess access;\n    private final ShipmentFeeService fees;')
s=s.replace('StorageService storageService) {','StorageService storageService, LastMileAccess access, ShipmentFeeService fees) {')
s=s.replace('        this.storageService = storageService;','        this.storageService = storageService;\n        this.access=access; this.fees=fees;')
# Forward scan: physical inventory and actual assignee profile before mutation, no client hub trust.
a=s.index('        // Idempotent check');b=s.index('        shipment.transitionTo(ShipmentStatus.OUT_FOR_DELIVERY);',a)
s=s[:a]+'''        var profile = access.require(UserRole.ROLE_SHIPPER);
        if (!profile.getHubId().equals(shipment.getDestinationHubId()) || shipment.isOnHold() || !"FORWARD".equals(shipment.getDirection()))
            throw new AccessDeniedException("Shipment is not assigned to this delivery hub");
        if (ShipmentStatus.OUT_FOR_DELIVERY.equals(shipment.getCurrentStatus())) {
            if (!"IN_PROGRESS".equals(activeAssignment.getStatus()) || shipment.getCurrentHubId()!=null) throw new IllegalStateException("Invalid outbound custody");
            return toTaskResponse(shipment, activeAssignment);
        }
        access.requireAt(shipment,profile);
        if (shipment.getCurrentStatus()!=ShipmentStatus.AT_HUB || !"ASSIGNED".equals(activeAssignment.getStatus()))
            throw new IllegalStateException("Parcel is not ready for outbound scan");
        if(request.getHubId()!=null && !profile.getHubId().equals(request.getHubId())) throw new AccessDeniedException("Wrong scan hub");
        shipment.setCurrentHubId(null);
'''+s[b:]
# Entire collection rewritten to validate all obligations before mutation.
a=s.index('        // 1. Process COD');b=s.index('        return toTaskResponse(shipment, activeAssignment);',a)
s=s[:a]+'''        requireDeliveryCustody(shipment,activeAssignment,actor);
        String method=request.getPaymentMethod();
        if (!Set.of("CASH","VNPAY").contains(method)) throw new IllegalArgumentException("Unsupported collection method");
        BigDecimal codDue=zero(shipment.getCodAmount());
        BigDecimal feeDue="RECEIVER".equals(shipment.getFeePayer())?zero(shipment.getShippingFee()):BigDecimal.ZERO;
        exactAmount(request.getCodAmountCollected(),codDue);
        exactAmount(request.getShippingFeeCollected(),feeDue);
        var cod=codTransactionRepository.findByShipmentIdForUpdate(shipment.getId()).orElse(null);
        var payment=paymentRepository.findFirstByShipmentIdAndPayerTypeAndPaymentType(shipment.getId(),"RECEIVER","SHIPPING_FEE").orElse(null);
        if(codDue.signum()>0 && cod!=null) {
            if(cod.getAmount()==null || cod.getAmount().compareTo(codDue)!=0 || !actor.userId().equals(cod.getCollectedByShipper().getId()))
                throw new IllegalStateException("COD source does not match this task");
            if(!Set.of("WAITING_COLLECTION","COLLECTED").contains(cod.getStatus())
                    || "COLLECTED".equals(cod.getStatus()) && (!method.equals(cod.getMethod()) || cod.getCollectedAt()==null))
                throw new IllegalStateException("COD collection conflicts with recorded evidence");
        }
        if(feeDue.signum()>0 && payment!=null) {
            if(payment.getAmount()==null || payment.getAmount().compareTo(feeDue)!=0 || !Set.of("UNPAID","PENDING","PAID").contains(payment.getStatus()))
                throw new IllegalStateException("Shipping collection does not match obligation");
            if("PAID".equals(payment.getStatus()) && (!method.equals(payment.getMethod()) || !actor.userId().equals(payment.getCollectedByUserId()) || payment.getPaidAt()==null))
                throw new IllegalStateException("Shipping collection conflicts with recorded evidence");
        }
        if(codDue.signum()>0 && (cod==null || "WAITING_COLLECTION".equals(cod.getStatus()))) {
            if(cod==null) cod=CodTransaction.createWaiting(shipment,shipper,codDue,method);
            cod.collect(method,shipper); codTransactionRepository.save(cod);
            shipmentEventRepository.save(ShipmentEvent.create(shipment,shipper,"COD_COLLECTED","COD collection recorded"));
        }
        if(feeDue.signum()>0 && (payment==null || !"PAID".equals(payment.getStatus()))) {
            if(payment==null) payment=Payment.create(shipment,"RECEIVER","SHIPPING_FEE",feeDue,method,"UNPAID");
            payment.collect(method,actor.userId(),"DELIVERY-FEE-"+shipment.getId()); paymentRepository.save(payment);
            shipmentEventRepository.save(ShipmentEvent.create(shipment,shipper,"RECEIVER_FEE_COLLECTED","Shipping collection recorded"));
        }

'''+s[b:]
# Confirm before mutation must check assignment/custody; replay only completed same task.
s=s.replace('''        if (ShipmentStatus.DELIVERED.equals(shipment.getCurrentStatus())) {
            return toTaskResponse(shipment, activeAssignment);
        }''','''        if (ShipmentStatus.DELIVERED.equals(shipment.getCurrentStatus())) {
            if (!"COMPLETED".equals(activeAssignment.getStatus())) throw new AccessDeniedException("Invalid completed task");
            return toTaskResponse(shipment, activeAssignment);
        }
        requireDeliveryCustody(shipment,activeAssignment,actor);''')
# Failure guards and physical receipt deferred, not fabricated.
anchor='        String reason = request.getReason();'
s=s.replace(anchor,'''        requireDeliveryCustody(shipment,activeAssignment,actor);
        if (codTransactionRepository.findByShipmentId(shipment.getId()).filter(c->"COLLECTED".equals(c.getStatus())).isPresent()
                || paymentRepository.findFirstByShipmentIdAndPayerTypeAndPaymentType(shipment.getId(),"RECEIVER","SHIPPING_FEE")
                .filter(p->"PAID".equals(p.getStatus())).isPresent())
            throw new IllegalStateException("Recorded collection must be reconciled before reporting failure");
        if("RESCHEDULED".equals(request.getReason()) && (request.getRescheduledDate()==null || !request.getRescheduledDate().isAfter(LocalDate.now())))
            throw new IllegalArgumentException("Select a future rescheduled date");
'''+anchor)
a=s.index('                // Temporary failure:');b=s.index('                notificationEventPublisher.publishAfterCommit',a)
s=s[:a]+'''                // The shipper still has the parcel until inbound staff scan its physical return.
                shipment.transitionTo(ShipmentStatus.FAILED);
                shipment.setFailReason(reason);
                shipmentRepository.save(shipment);
                activeAssignment.setStatus("COMPLETED");
                activeAssignment.setCompletedAt(LocalDateTime.now());
                shipmentAssignmentRepository.save(activeAssignment);
                shipmentEventRepository.save(ShipmentEvent.create(shipment,shipper,"DELIVERY_ATTEMPT_FAILED","Delivery attempt failed; physical return pending"));
                if("RESCHEDULED".equals(reason)) shipmentEventRepository.save(ShipmentEvent.create(shipment,shipper,"DELIVERY_RESCHEDULED","Delivery rescheduled"));

'''+s[b:]
s=s.replace('        shipment.setReturnFee(returnFee);','        if(returnFee==null) returnFee=new BigDecimal("25000.00");\n        if(returnFee.signum()<0) throw new IllegalStateException("Invalid return fee policy");\n        shipment.setReturnFee(returnFee);')
# Replace unsafe handover+fee setter+return confirmation block.
a=s.index('    @Override\n    public ReturnTaskResponse processReturnHandover');b=s.index('    @Override\n    @Transactional(readOnly = true)\n    public List<DeliveryTaskResponse>',a)
s=s[:a]+'''    @Override
    public ReturnTaskResponse processReturnHandover(ReturnHandoverRequest request) {
        if(!"SELF_PICKUP".equals(request.getHandoverType())) throw new IllegalArgumentException("Use assigned return-delivery workflow");
        var actor=AuthenticatedActor.requireCurrent();
        var profile=access.require(UserRole.ROLE_HUB_STAFF,"INBOUND","GENERAL_STAFF");
        Shipment s=shipmentRepository.findByIdForUpdate(request.getShipmentId()).orElseThrow(()->new ResourceNotFoundException("Shipment"));
        if(!"RETURN".equals(s.getDirection()) || s.getOriginHubId()==null || !profile.getHubId().equals(s.getOriginHubId())) throw new AccessDeniedException("Wrong origin hub");
        if(s.getCurrentStatus()==ShipmentStatus.RETURNED_TO_SENDER) return toReturnResponse(s,true);
        access.requireAt(s,profile);
        if(s.getCurrentStatus()!=ShipmentStatus.RETURN_TO_SENDER || s.getReturnArrivedAtOriginAt()==null
                || shipmentAssignmentRepository.existsByShipmentIdAndStatusIn(s.getId(),List.of("ASSIGNED","ACCEPTED","IN_PROGRESS")))
            throw new IllegalStateException("Parcel is not available for sender pickup");
        if(request.getIdentityProof()==null || request.getIdentityProof().isBlank() || request.getIdentityProof().length()>255)
            throw new IllegalArgumentException("Record the verified sender identity reference");
        fees.requireReturnSettled(s);
        s.transitionTo(ShipmentStatus.RETURNED_TO_SENDER); s.setReturnedAt(LocalDateTime.now()); s.setCurrentHubId(null);
        shipmentEventRepository.save(ShipmentEvent.create(s,profile.getUser(),"RETURNED_TO_SENDER","Verified sender pickup at origin hub"));
        return toReturnResponse(s,true);
    }

    @Override
    public ReturnTaskResponse scanReturnDelivery(Long shipmentId) {
        var actor=AuthenticatedActor.requireCurrent(); var profile=access.require(UserRole.ROLE_SHIPPER);
        var s=shipmentRepository.findByIdForUpdate(shipmentId).orElseThrow(()->new ResourceNotFoundException("Shipment"));
        var a=returnAssignment(s,actor);
        if(s.isOnHold() || !"RETURN".equals(s.getDirection()) || !profile.getHubId().equals(s.getOriginHubId())) throw new AccessDeniedException("Wrong return task");
        if(s.getCurrentStatus()==ShipmentStatus.OUT_FOR_DELIVERY && "IN_PROGRESS".equals(a.getStatus()) && s.getCurrentHubId()==null) return toReturnResponse(s,true);
        access.requireAt(s,profile);
        if(s.getCurrentStatus()!=ShipmentStatus.RETURN_TO_SENDER || s.getReturnArrivedAtOriginAt()==null || !"ASSIGNED".equals(a.getStatus()))
            throw new IllegalStateException("Return is not ready for outbound scan");
        a.setStatus("IN_PROGRESS");a.setAcceptedAt(LocalDateTime.now());s.setCurrentHubId(null);s.transitionTo(ShipmentStatus.OUT_FOR_DELIVERY);
        shipmentEventRepository.save(ShipmentEvent.create(s,profile.getUser(),"RETURN_OUT_FOR_DELIVERY","Return parcel scanned to assigned shipper"));
        return toReturnResponse(s,true);
    }

    @Override
    public ReturnTaskResponse confirmReturnDelivery(Long shipmentId) {
        var actor=AuthenticatedActor.requireCurrent();var profile=access.require(UserRole.ROLE_SHIPPER);
        var s=shipmentRepository.findByIdForUpdate(shipmentId).orElseThrow(()->new ResourceNotFoundException("Shipment"));
        var a=returnAssignment(s,actor);
        if(!"RETURN".equals(s.getDirection()) || !profile.getHubId().equals(s.getOriginHubId())) throw new AccessDeniedException("Wrong return task");
        if(s.getCurrentStatus()==ShipmentStatus.RETURNED_TO_SENDER && "COMPLETED".equals(a.getStatus())) return toReturnResponse(s,true);
        if(s.isOnHold() || s.getCurrentHubId()!=null || s.getCurrentStatus()!=ShipmentStatus.OUT_FOR_DELIVERY || !"IN_PROGRESS".equals(a.getStatus()) || a.getAcceptedAt()==null)
            throw new IllegalStateException("Return delivery requires scanned outbound custody");
        fees.requireReturnSettled(s);
        s.transitionTo(ShipmentStatus.RETURNED_TO_SENDER);s.setReturnedAt(LocalDateTime.now());a.complete(LocalDateTime.now());
        shipmentEventRepository.save(ShipmentEvent.create(s,profile.getUser(),"RETURNED_TO_SENDER","Return delivered by assigned shipper"));
        return toReturnResponse(s,true);
    }

    @Override
    public void receiveFailedDelivery(Long shipmentId,String trackingNumber) {
        var profile=access.require(UserRole.ROLE_HUB_STAFF,"INBOUND","GENERAL_STAFF");
        var s=shipmentRepository.findByIdForUpdate(shipmentId).orElseThrow(()->new ResourceNotFoundException("Shipment"));
        if(s.isOnHold() || !profile.getHubId().equals(s.getDestinationHubId()) || !s.getTrackingNumber().equals(trackingNumber)) throw new AccessDeniedException("Invalid returned parcel scan");
        if(profile.getHubId().equals(s.getCurrentHubId()) && shipmentEventRepository.existsByShipmentIdAndEventType(s.getId(),"RETURNED_TO_HUB")) return;
        if(s.getCurrentHubId()!=null || !(s.getCurrentStatus()==ShipmentStatus.FAILED || s.getCurrentStatus()==ShipmentStatus.RETURN_TO_SENDER))
            throw new IllegalStateException("Parcel is not awaiting physical return");
        var task=shipmentAssignmentRepository.findByShipmentIdOrderByAssignedAtDesc(s.getId()).stream().filter(a->"DELIVERY".equals(a.getAssignmentType())).findFirst()
                .orElseThrow(()->new IllegalStateException("No preceding delivery custody"));
        if(!"COMPLETED".equals(task.getStatus()) || task.getAcceptedAt()==null) throw new IllegalStateException("No completed failed delivery task");
        s.receiveAt(profile.getHub());
        shipmentEventRepository.save(ShipmentEvent.arrivedAtHub(s,profile.getUser(),profile.getHub()));
        shipmentEventRepository.save(ShipmentEvent.of(s,profile.getUser(),"RETURNED_TO_HUB","Physical return from shipper received"));
        if(s.getReturnArrivedAtOriginAt()!=null && !shipmentEventRepository.existsByShipmentIdAndEventType(s.getId(),"RETURNED_AT_ORIGIN_HUB"))
            shipmentEventRepository.save(ShipmentEvent.of(s,profile.getUser(),"RETURNED_AT_ORIGIN_HUB","Return parcel received at origin"));
    }

    private ShipmentAssignment returnAssignment(Shipment s,AuthenticatedActor actor) {
        return shipmentAssignmentRepository.findByShipmentIdOrderByAssignedAtDesc(s.getId()).stream()
                .filter(a->"RETURN_DELIVERY".equals(a.getAssignmentType()) && actor.userId().equals(a.getAssignee().getId()))
                .findFirst().orElseThrow(()->new AccessDeniedException("No assigned return task"));
    }
    private void requireDeliveryCustody(Shipment s,ShipmentAssignment a,AuthenticatedActor actor) {
        var profile=access.require(UserRole.ROLE_SHIPPER);
        if(s.isOnHold() || !"FORWARD".equals(s.getDirection()) || !profile.getHubId().equals(s.getDestinationHubId())
                || s.getCurrentHubId()!=null || !"DELIVERY".equals(a.getAssignmentType()) || !"IN_PROGRESS".equals(a.getStatus()) || a.getAcceptedAt()==null
                || !actor.userId().equals(a.getAssignee().getId())) throw new AccessDeniedException("Physical delivery custody required");
    }
    private static BigDecimal zero(BigDecimal a){return a==null?BigDecimal.ZERO:a;}
    private static void exactAmount(BigDecimal supplied,BigDecimal expected){
        if(expected.signum()<0 || zero(supplied).compareTo(expected)!=0) throw new IllegalArgumentException("Collection must match authoritative amount");
    }

'''+s[b:]
# Read access is scoped to active shipper before projections.
s=s.replace('''        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        List<ShipmentAssignment> assignments''','''        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        access.require(UserRole.ROLE_SHIPPER);
        List<ShipmentAssignment> assignments''')
p.write_text(s,encoding='utf-8')
p=Path('src/main/java/vn/iotstar/service/LastMileDeliveryService.java');s=p.read_text();s=s.replace('    ReturnTaskResponse confirmReturnDelivery(Long shipmentId);','    ReturnTaskResponse confirmReturnDelivery(Long shipmentId);\n    ReturnTaskResponse scanReturnDelivery(Long shipmentId);\n    void receiveFailedDelivery(Long shipmentId, String trackingNumber);');p.write_text(s)
