package vn.iotstar.service;

import org.springframework.web.multipart.MultipartFile;

/** Stores uploaded evidence files on the local file system. */
public interface StorageService {

    /**
     * Builds a collision free storage name for the uploaded file. The original
     * file name is never reused directly, so a crafted name cannot escape the
     * storage directory.
     */
    String buildStorageFilename(MultipartFile file, String prefix);

    /**
     * Stores the file and returns the stored name.
     */
    String store(MultipartFile file, String storedFilename);
}