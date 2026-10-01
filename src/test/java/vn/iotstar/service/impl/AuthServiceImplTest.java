package vn.iotstar.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.iotstar.config.JwtProperties;
import vn.iotstar.dto.auth.ApiLoginRequest;
import vn.iotstar.dto.auth.ApiLoginResponse;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.JwtService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setExpirationSeconds(45);
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtService, properties);
    }

    @Test
    void nonexistentUserIsNormalizedToGenericCredentialFailure() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());
        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> authService.login(new ApiLoginRequest("missing", "password")));
        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    void wrongPasswordIsNormalizedToSameGenericCredentialFailure() {
        User user = new User(1L, "known", "stored-hash", UserRole.ROLE_USER, "ACTIVE");
        when(userRepository.findByUsername("known")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);
        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> authService.login(new ApiLoginRequest("known", "wrong")));
        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    void loginResponseUsesConfiguredExpiration() {
        User user = new User(1L, "known", "stored-hash", UserRole.ROLE_USER, "ACTIVE");
        when(userRepository.findByUsername("known")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", "stored-hash")).thenReturn(true);
        when(jwtService.issueToken(user)).thenReturn("token");

        ApiLoginResponse response = authService.login(new ApiLoginRequest("known", "correct"));
        assertEquals(45, response.expiresInSeconds());
    }
}
