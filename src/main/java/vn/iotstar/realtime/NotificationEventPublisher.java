package vn.iotstar.realtime;

import vn.iotstar.dto.realtime.NotificationEventResponse;
import vn.iotstar.security.AuthenticatedActor;

/**
 * Publishes private, after-commit notifications for the last-mile and finance
 * slice. Implementations must never broadcast shipment or financial details.
 */
public interface NotificationEventPublisher {

    void publishAfterCommit(AuthenticatedActor actor, NotificationEventResponse event);
}