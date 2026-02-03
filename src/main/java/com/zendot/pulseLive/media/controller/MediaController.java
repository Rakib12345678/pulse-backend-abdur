package com.zendot.pulseLive.media.controller;

import com.zendot.pulseLive.media.model.MediaPath;
import com.zendot.pulseLive.media.model.PresignedUploadResponse;
import com.zendot.pulseLive.media.service.MediaService;
import com.zendot.pulseLive.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/media")
public class MediaController {


    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /**
     * Generate a presigned URL for uploading a file to S3.
     *
     * @param path The folder/path where the file should be stored (e.g., "/media/image/profile-images").
     * @param fileExtension The file extension (e.g., "jpg", "mp4").
     * @return PresignedUploadResponse containing presigned URL, S3 key, CDN URL, and expiration.
     */
    @Operation(
            summary = "Generate presigned upload URL",
            description = "Generates a presigned URL for uploading a file to S3. " +
                    "The URL is valid for a limited time (e.g., 10 minutes)."
    )

    @PostMapping("/upload")
    public ResponseEntity<PresignedUploadResponse> getPresignedUploadUrl(
            @RequestParam MediaPath path,
            @RequestParam String fileExtension
    ) {
        String userId = SecurityUtils.requireUserId().toString();
        PresignedUploadResponse response = mediaService.generateUploadUrl( path.getPath(), userId,fileExtension);
        return ResponseEntity.ok(response);
    }




    /**
     * Delete a file from S3 given its key.
     *
     * @param key The S3 key of the file to delete.
     */
    @Operation(
            summary = "Delete file from S3",
            description = "Deletes the file from S3 given its key. "
    )
    @DeleteMapping
    public ResponseEntity<Void> deleteFile(@RequestParam String key) {
        mediaService.deleteFile(key);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
