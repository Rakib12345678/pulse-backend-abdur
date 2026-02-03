package com.zendot.pulseLive.media.serviceImpl;


import com.zendot.pulseLive.media.model.PresignedUploadResponse;
import com.zendot.pulseLive.media.service.MediaService;
import com.zendot.pulseLive.media.util.RelativePathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaServiceImpl implements MediaService {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final RelativePathUtils utils;

    @Value("${aws.s3.bucket}")
    private String bucket;
    private static final Map<String, String> EXTENSION_CONTENT_TYPE_MAP = Map.ofEntries(

            // Images
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("webp", "image/webp"),
            Map.entry("gif", "image/gif"),

            // Videos
            Map.entry("mp4", "video/mp4"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("avi", "video/x-msvideo"),
            Map.entry("mkv", "video/x-matroska"),

            // Documents
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    );


    private static final Duration PRESIGN_DURATION = Duration.ofMinutes(10);

    // =============================
    // GENERATE PRESIGNED URL
    // =============================


    @Override
    public PresignedUploadResponse generateUploadUrl(
            String path,
            String referenceId,
            String fileExtension
    ) {

        // Normalize extension
        String normalizedExtension = normalizeExtension(fileExtension);

        // Validate + Map to ContentType
        String contentType = getContentTypeFromExtension(normalizedExtension);

        // Build S3 Key
        String key = buildKey(path, referenceId, normalizedExtension);

        // Build Put Request
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        // Presign Request
        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(PRESIGN_DURATION)
                        .putObjectRequest(putObjectRequest)
                        .build();

        // Generate Presigned URL
        PresignedPutObjectRequest presigned =
                s3Presigner.presignPutObject(presignRequest);

        return new PresignedUploadResponse(
                presigned.url().toString(),
                key,
                utils.buildCdnUrl(key),
                presigned.expiration()
        );
    }



    // =============================
    // DELETE FILE
    // =============================

     @Override
    public void deleteFile(String key) {

        if (key == null || key.isBlank()) throw new RuntimeException("key cannot be null or empty");

        try {

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);

        } catch (Exception e) {
            log.error("Failed to delete S3 object: {}", key, e);
        }
    }



    // =============================
    // INTERNAL KEY BUILDER
    // =============================
    private String buildKey(String folder,
                            String referenceId,
                            String contentType) {

        String ext = extractExtension(contentType);

        return "%s/%s/file_%d.%s"
                .formatted(folder, referenceId, System.currentTimeMillis(), ext);
    }

    private String extractExtension(String contentType) {

        if (contentType == null) return "jpg";

        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/jpeg" -> "jpg";
            default -> "jpg";
        };
    }
    private String normalizeExtension(String extension) {

        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("File extension is required");
        }

        return extension.replace(".", "").trim().toLowerCase();
    }


    private String getContentTypeFromExtension(String extension) {

        String contentType = EXTENSION_CONTENT_TYPE_MAP.get(extension);

        if (contentType == null) {
            throw new IllegalArgumentException(
                    "Unsupported file extension: " + extension
            );
        }

        return contentType;
    }

}
