package vn.iotstar.dto.hub;

/** A parcel the signed-in hub staff can act on now: its ID (what the forms ask for), tracking code and what to do with it.
 *  Plain getters because the JSP expression language of this container does not read record accessors. */
public final class WaitingParcelResponse {
    private final Long id;
    private final String trackingNumber;
    private final String action;

    public WaitingParcelResponse(Long id, String trackingNumber, String action) {
        this.id = id; this.trackingNumber = trackingNumber; this.action = action;
    }

    public Long getId() { return id; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getAction() { return action; }
}
