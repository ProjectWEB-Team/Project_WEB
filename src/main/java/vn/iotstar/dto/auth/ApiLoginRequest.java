package vn.iotstar.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record ApiLoginRequest(@NotBlank String username, @NotBlank String password) { }
