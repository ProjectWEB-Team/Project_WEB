package vn.iotstar.exception;

import org.springframework.security.access.AccessDeniedException;

/** Service-boundary ownership denial for shipment resources. */
public class ShipmentAccessDeniedException extends AccessDeniedException {
    public ShipmentAccessDeniedException() {
        super("Shipment is outside the current actor scope");
    }
}
