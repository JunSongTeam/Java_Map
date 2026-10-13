package com.example.mapjava.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.example.mapjava.common.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AvatarStorageService {

    private static final long MAX_AVATAR_BYTES = 2L * 1024L * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Path storageDir;

    public AvatarStorageService(@Value("${app.avatar.storage-dir:data/avatars}") String storageDir) {
        this.storageDir = Path.of(storageDir);
    }

    public void save(UUID userId, MultipartFile file) {
        validate(file);

        try {
            Files.createDirectories(storageDir);
            Files.copy(file.getInputStream(), imagePath(userId), StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(contentTypePath(userId), file.getContentType(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new BadRequestException("头像上传失败，请稍后重试");
        }
    }

    public Optional<AvatarImage> find(UUID userId) {
        Path imagePath = imagePath(userId);
        Path contentTypePath = contentTypePath(userId);
        if (!Files.exists(imagePath) || !Files.exists(contentTypePath)) {
            return Optional.empty();
        }

        try {
            return Optional.of(new AvatarImage(
                    Files.readAllBytes(imagePath),
                    Files.readString(contentTypePath, StandardCharsets.UTF_8).trim()
            ));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("头像文件不能为空");
        }

        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new BadRequestException("头像不能超过 2MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("头像格式只支持 JPG、PNG、WEBP");
        }
    }

    private Path imagePath(UUID userId) {
        return storageDir.resolve(userId + ".avatar");
    }

    private Path contentTypePath(UUID userId) {
        return storageDir.resolve(userId + ".content-type");
    }
}
