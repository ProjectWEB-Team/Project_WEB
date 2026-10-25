package vn.iotstar.service;

import vn.iotstar.entity.Shipment;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles the return line of the Workflow once a parcel travels back to the
 * sender, including the grace period for an uncollected parcel.
 */
public interface ShipmentReturnService {

    /**
     * Marks a returned parcel as arrived at the origin hub and waiting for the
     * sender. This starts the grace period of the Workflow.
     */
    Shipment markReturnArrivedAtOriginHub(Long shipmentId);

    /**
     * Returns the shipments whose grace period expired without being collected.
     */
    List<Shipment> findOverdueReturnedShipments(LocalDateTime referenceTime);

    /**
     * Closes an overdue returned parcel as a disposed exception, as decided by
     * the hub manager.
     */
    Shipment disposeOverdueReturn(Long shipmentId, String reason);
}