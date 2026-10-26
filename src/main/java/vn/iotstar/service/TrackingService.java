package vn.iotstar.service;

import org.springframework.data.domain.Page;
import vn.iotstar.dto.tracking.IncomingShipmentResponse;
import vn.iotstar.dto.tracking.PublicTrackingResponse;
import vn.iotstar.dto.tracking.RecipientShipmentResponse;

/** Read-only tracking projections for guests and linked recipients. */
public interface TrackingService {
    PublicTrackingResponse track(String trackingNumber);
    Page<IncomingShipmentResponse> incoming(int page);
    RecipientShipmentResponse incomingDetail(Long shipmentId, boolean unlocked);
    /** True only when the linked recipient typed the exact full tracking number. */
    boolean unlock(Long shipmentId, String trackingNumber);
}
