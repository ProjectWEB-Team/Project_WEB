package vn.iotstar.controller.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.iotstar.dto.auth.ApiLoginRequest;
import vn.iotstar.dto.auth.ApiLoginResponse;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.AuthService;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthApiController {
    private final AuthService authService;
    public AuthApiController(AuthService authService) { this.authService = authService; }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody ApiLoginRequest request) {
        try {
            ApiLoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }
    }

    /** Minimal protected endpoint proving the API filter supplies a server-derived actor. */
    @GetMapping("/security/actor")
    public Map<String, Object> currentActor() {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        return Map.of("userId", actor.userId(), "username", actor.username(), "roles", actor.roles());
    }
}
