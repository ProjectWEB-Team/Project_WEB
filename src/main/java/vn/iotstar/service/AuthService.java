package vn.iotstar.service;

import vn.iotstar.dto.auth.ApiLoginRequest;
import vn.iotstar.dto.auth.ApiLoginResponse;

public interface AuthService {
    ApiLoginResponse login(ApiLoginRequest request);
}
