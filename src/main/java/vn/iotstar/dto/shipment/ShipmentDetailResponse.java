package vn.iotstar.dto.shipment;

import vn.iotstar.entity.ShipmentStatus;

import java.time.LocalDateTime;
import java.util.List;

/** Safe shipment detail/tracking read model; it deliberately excludes owner credentials. */
public record ShipmentDetailResponse(Long id, String trackingNumber, ShipmentStatus currentStatus,
                                     String senderName, String senderPhone, String senderAddress,
                                     String receiverName, String receiverPhone, String receiverAddress,
                                     LocalDateTime createdAt, List<ShipmentEventResponse> events) {
}
