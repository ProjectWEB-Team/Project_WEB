package vn.iotstar.service;

import vn.iotstar.entity.User;
import vn.iotstar.security.VerifiedGoogleIdentity;

import java.util.Optional;

/** Identity-to-account bridge used only by the server authentication flow. */
public interface GoogleAccountService {
    Optional<User> findActiveAccount(VerifiedGoogleIdentity identity);
    User register(VerifiedGoogleIdentity identity);
}
