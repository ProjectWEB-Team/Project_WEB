package vn.iotstar.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.iotstar.entity.Shipment;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Watches returned parcels that stayed uncollected past the grace period.
 *
 * <p>The Workflow keeps the shipment in RETURN_TO_SENDER and lets the hub
 * manager decide about the disposal, so the scheduler only reports the
 * overdue parcels instead of closing them.
 */
@Component
public class ReturnGracePeriodScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReturnGracePeriodScheduler.class);

    private final ShipmentReturnServiceImpl shipmentReturnService;

    public ReturnGracePeriodScheduler(ShipmentReturnServiceImpl shipmentReturnService) {
        this.shipmentReturnService = shipmentReturnService;
    }

    @Scheduled(fixedDelayString = "${uteexpress.scheduler.timeout-check-interval-ms:30000}")
    public void reportOverdueReturns() {
        List<Shipment> overdue = shipmentReturnService.findOverdueReturnedShipments(LocalDateTime.now());
        for (Shipment shipment : overdue) {
            LOGGER.warn("Returned shipment {} exceeded the grace period and waits for the manager decision",
                    shipment.getTrackingNumber());
        }
    }
}