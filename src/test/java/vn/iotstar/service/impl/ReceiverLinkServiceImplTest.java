package vn.iotstar.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.repository.ShipmentRepository;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiverLinkServiceImplTest {
    private static final Instant NOW = Instant.parse("2026-10-22T08:00:00Z");
    @Mock private ShipmentRepository shipments;

    private ReceiverLinkServiceImpl service() { return new ReceiverLinkServiceImpl(shipments); }

    private User pending() {
        return User.registerPending("recv01", "hash", "Receiver", "+84901234567", "r@example.test", NOW);
    }

    @Test
    void verifiedActiveCustomerLinksAllThreePhoneForms() {
        User user = pending();
        user.verifyPhone(NOW);
        when(shipments.linkReceiver(user, List.of("+84901234567", "0901234567", "84901234567"))).thenReturn(2);

        assertThat(service().linkVerifiedReceiver(user)).isEqualTo(2);
    }

    @Test
    void unverifiedPhoneLinksNothing() {
        assertThat(service().linkVerifiedReceiver(pending())).isZero();
        verifyNoInteractions(shipments);
    }

    @Test
    void googleAccountWithUnverifiedContactPhoneLinksNothing() {
        User google = User.googleAccount("g_x", "hash", "G", "+84901234567", "g@example.test", "sub", NOW);

        assertThat(service().linkVerifiedReceiver(google)).isZero();
        verifyNoInteractions(shipments);
    }

    @Test
    void internalRoleNeverLinks() {
        User staff = new User(5L, "hub", "hash", UserRole.ROLE_HUB_STAFF, "ACTIVE");

        assertThat(service().linkVerifiedReceiver(staff)).isZero();
        verifyNoInteractions(shipments);
    }

    @Test
    void nullUserLinksNothing() {
        assertThat(service().linkVerifiedReceiver(null)).isZero();
        verifyNoInteractions(shipments);
    }
}
