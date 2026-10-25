package vn.iotstar.dto.auth;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class VerifyPhoneOtpRequest {
    private UUID challengeId;
    @NotBlank @Pattern(regexp = "[0-9]{6}") private String code;
    public UUID getChallengeId() { return challengeId; }
    public void setChallengeId(UUID value) { challengeId = value; }
    public String getCode() { return code; }
    public void setCode(String value) { code = value; }
}
