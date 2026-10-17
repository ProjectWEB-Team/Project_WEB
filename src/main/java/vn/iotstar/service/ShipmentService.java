package vn.iotstar.service;

import vn.iotstar.dto.shipment.CreateShipmentRequest;
import vn.iotstar.dto.shipment.ShipmentDetailResponse;

public interface ShipmentService {
    Long create(CreateShipmentRequest request);
    ShipmentDetailResponse getDetail(Long shipmentId);
}
