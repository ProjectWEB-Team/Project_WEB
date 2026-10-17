package vn.iotstar.dto.auth;

public record ApiLoginResponse(String accessToken, String tokenType, long expiresInSeconds) { }
