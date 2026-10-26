package vn.iotstar.exception;

/** Signals a server-side hub/profile/location scope failure without exposing operations detail. */
public class HubOperationAccessDeniedException extends RuntimeException {
    public HubOperationAccessDeniedException() { super("Access denied"); }
}
