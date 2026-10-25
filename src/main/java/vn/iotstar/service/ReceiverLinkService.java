package vn.iotstar.service;

import vn.iotstar.entity.User;

public interface ReceiverLinkService {
    /** Links unlinked shipments whose receiver phone matches the user's verified phone; returns the count. */
    int linkVerifiedReceiver(User user);
}
