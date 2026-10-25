package vn.iotstar.realtime;

import vn.iotstar.dto.realtime.ShipmentCreatedEventResponse;
import vn.iotstar.security.AuthenticatedActor;

/** Stages safe realtime publication to a server-chosen user's private destination. */
public interface ShipmentEventPublisher {
    default void publishAfterCommit(AuthenticatedActor actor, ShipmentCreatedEventResponse event) {
        publishToUserAfterCommit(actor.username(), event);
    }

    void publishToUserAfterCommit(String username, ShipmentCreatedEventResponse event);
}
