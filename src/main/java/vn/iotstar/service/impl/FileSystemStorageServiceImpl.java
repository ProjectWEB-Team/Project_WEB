package vn.iotstar.service.impl;

import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.iotstar.service.StorageService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Stores uploaded evidence files under the local {@code uploads} directory. */
@Service
public class FileSystemStorageServiceImpl implements StorageService {

    private static final String STORAGE_DIRECTORY = "uploads";
    private static final int MAX_FILENAME_LENGTH = 60;

    private final Path rootLocation = Paths.get(STORAGE_DIRECTORY);

    public FileSystemStorageServiceImpl() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not initialize the upload storage directory", exception);
        }
    }

    @Override
    public String buildStorageFilename(MultipartFile file, String prefix) {
        // Uploads are served same-origin from /uploads/**, so only image types may be stored (no .html/.svg).
        String extension = String.valueOf(FilenameUtils.getExtension(file.getOriginalFilename())).toLowerCase(java.util.Locale.ROOT);
        if (!java.util.Set.of("jpg", "jpeg", "png", "webp").contains(extension)) {
            throw new IllegalArgumentException("Evidence must be a JPG, PNG or WEBP image");
        }
        String safeExtension = "." + extension;
        String uniquePart = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String safePrefix = prefix == null || prefix.isBlank() ? "evidence" : prefix;
        if (safePrefix.length() > MAX_FILENAME_LENGTH) {
            safePrefix = safePrefix.substring(0, MAX_FILENAME_LENGTH);
        }
        return safePrefix + "_" + uniquePart + safeExtension;
    }

    @Override
    public String store(MultipartFile file, String storedFilename) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("The uploaded file is empty");
        }

        Path destination = rootLocation.resolve(Paths.get(storedFilename)).normalize().toAbsolutePath();
        if (!destination.startsWith(rootLocation.toAbsolutePath())) {
            throw new IllegalArgumentException("The uploaded file name resolves outside the storage directory");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not store the uploaded file", exception);
        }
        return storedFilename;
    }
}