package vn.iotstar.service;

import org.junit.jupiter.api.Test;
import vn.iotstar.exception.RegistrationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvatarServiceTest {
    @Test
    void typeComesFromTheBytesNotTheName() {
        byte[] jpg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0, 0, 0, 0, 0};
        byte[] webp = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 0};
        assertThat(AvatarService.extensionOf(jpg)).isEqualTo("jpg");
        assertThat(AvatarService.extensionOf(png)).isEqualTo("png");
        assertThat(AvatarService.extensionOf(webp)).isEqualTo("webp");
        assertThatThrownBy(() -> AvatarService.extensionOf("<svg onload=alert(1)>".getBytes())).isInstanceOf(RegistrationException.class);
    }
}
