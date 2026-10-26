package vn.iotstar.service.impl;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSystemStorageServiceImplTest {
    private final FileSystemStorageServiceImpl storage = new FileSystemStorageServiceImpl();

    @ParameterizedTest
    @ValueSource(strings = {"photo.jpg", "photo.JPEG", "photo.png", "photo.webp"})
    void imageEvidenceKeepsItsExtension(String name) {
        String stored = storage.buildStorageFilename(new MockMultipartFile("evidence", name, null, new byte[]{1}), "delivery-evidence");
        assertThat(stored).startsWith("delivery-evidence_").endsWith(name.substring(name.lastIndexOf('.')).toLowerCase());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page.html", "image.svg", "script.js", "noextension", "photo.jpg.html"})
    void activeOrUnknownContentIsRejected(String name) {
        assertThatThrownBy(() -> storage.buildStorageFilename(new MockMultipartFile("evidence", name, null, new byte[]{1}), "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
