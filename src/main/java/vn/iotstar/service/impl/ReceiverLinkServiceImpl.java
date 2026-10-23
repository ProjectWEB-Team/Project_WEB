package vn.iotstar.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.service.ReceiverLinkService;

import java.util.List;

@Service
public class ReceiverLinkServiceImpl implements ReceiverLinkService {
    private final ShipmentRepository shipments;

    public ReceiverLinkServiceImpl(ShipmentRepository shipments) {
        this.shipments = shipments;
    }

    @Override
    @Transactional
    public int linkVerifiedReceiver(User user) {
        // Only a verified phone proves ownership; a Google contact phone never qualifies.
        if (user == null || user.getRole() != UserRole.ROLE_USER || !user.isEnabled() || !user.isPhoneVerified()) {
            return 0;
        }
        String national = "0" + user.getPhone().substring(3); // phone is stored as +84xxxxxxxxx
        // Older snapshots keep the typed format, so match the three common forms.
        return shipments.linkReceiver(user, List.of(user.getPhone(), national, user.getPhone().substring(1)));
    }
}
