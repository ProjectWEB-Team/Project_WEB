package vn.iotstar.exception;

/** Safe staff-facing outcome for a missing or ambiguous static route rule. */
public class RouteConfigurationException extends RuntimeException {
    public RouteConfigurationException() { super("Sorting is temporarily unavailable"); }
}
