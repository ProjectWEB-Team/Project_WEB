package vn.iotstar.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.UserRole;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.AccountSessionAuthenticator;
import vn.iotstar.service.PhoneSessionService;

@Service
public class PhoneSessionServiceImpl implements PhoneSessionService {
    private final UserRepository users;
    private final AccountSessionAuthenticator authenticator;

    public PhoneSessionServiceImpl(UserRepository users, AccountSessionAuthenticator authenticator) {
        this.users = users;
        this.authenticator = authenticator;
    }

    @Override
    @Transactional(readOnly = true)
    public void signIn(Long verifiedUserId, HttpServletRequest request, HttpServletResponse response) {
        var user = users.findById(verifiedUserId)
                .filter(account -> account.getRole() == UserRole.ROLE_USER && account.isPhoneVerified())
                .orElseThrow(() -> new BadCredentialsException("Account unavailable"));
        authenticator.signIn(user, request, response);
    }
}
