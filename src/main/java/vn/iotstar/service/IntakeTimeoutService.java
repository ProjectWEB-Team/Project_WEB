package vn.iotstar.service;

/** Cancels HOME_PICKUP (PICKUP_TIMEOUT) and SELF_DROPOFF (DROPOFF_TIMEOUT) shipments after their handover window. */
public interface IntakeTimeoutService {
    /** @return number of shipments cancelled in this run; safe to run repeatedly */
    int cancelExpired();
}
