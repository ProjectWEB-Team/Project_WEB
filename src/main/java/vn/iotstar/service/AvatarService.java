package vn.iotstar.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.iotstar.exception.RegistrationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** One avatar per user at uploads/avatars/{id}.{jpg|png|webp}, served by the existing /uploads/** handler. */
@Service
public class AvatarService {
    static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final List<String> EXTENSIONS = List.of("jpg", "png", "webp");
    private final Path dir = Path.of("uploads", "avatars");

    public Optional<String> urlFor(Long userId) {
        for (String ext : EXTENSIONS) {
            Path file = dir.resolve(userId + "." + ext);
            if (Files.isRegularFile(file)) {
                try {
                    return Optional.of("/uploads/avatars/" + file.getFileName() + "?v=" + Files.getLastModifiedTime(file).toMillis());
                } catch (IOException ignored) { /* fall through to the next candidate */ }
            }
        }
        return Optional.empty();
    }

    public void save(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new RegistrationException("Ảnh đại diện không được trống và tối đa 2MB.");
        }
        try {
            String ext = extensionOf(file.getBytes());
            Files.createDirectories(dir);
            // The type comes from the file's own bytes, never from its name or declared content type.
            for (String old : EXTENSIONS) Files.deleteIfExists(dir.resolve(userId + "." + old));
            Files.write(dir.resolve(userId + "." + ext), file.getBytes());
        } catch (IOException exception) {
            throw new RegistrationException("Không thể lưu ảnh lúc này. Vui lòng thử lại.");
        }
    }

    static String extensionOf(byte[] b) {
        if (b.length > 12 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        throw new RegistrationException("Chỉ nhận ảnh JPG, PNG hoặc WEBP.");
    }
}
