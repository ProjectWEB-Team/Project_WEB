package vn.iotstar.realtime;

import vn.iotstar.dto.realtime.ShipmentCreatedEventResponse;
import vn.iotstar.security.AuthenticatedActor;

/** Stages safe realtime publication for the authenticated shipment owner. */
public interface ShipmentEventPublisher {
    void publishAfterCommit(AuthenticatedActor actor, ShipmentCreatedEventResponse event);
}
