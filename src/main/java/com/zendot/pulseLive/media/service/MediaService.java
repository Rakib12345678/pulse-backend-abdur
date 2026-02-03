package com.zendot.pulseLive.media.service;

import com.zendot.pulseLive.media.model.PresignedUploadResponse;

/**
 * MediaService defines operations for handling media files in S3.

 * This interface provides methods to:
 * - Generate a presigned URL for uploading files to S3.
 * - Delete existing files from S3.

 * The generateUploadUrl method takes a folder path, a reference ID, and the file extension,
 * then returns a PresignedUploadResponse containing the presigned URL, the S3 key, CDN URL, and expiration time.

 * The deleteFile method removes the file associated with the given S3 key.

 * This interface is intended to be implemented by a service that manages media storage and CDN URLs.

 @Author: Abdur
 @Date: 2026-02-03
 */

public interface MediaService {


     PresignedUploadResponse generateUploadUrl(
            String path,
            String referenceId,
            String fileExtension
    );


    void deleteFile(String key);

}
