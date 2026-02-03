package com.zendot.pulseLive.media.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents a relative path to a media file stored in S3.

 * The RelativePath class encapsulates only the S3 key (relative path) without the full CDN URL.
 * It normalizes the path by removing a leading slash if present.

 * Example:
 * RelativePath rp = new RelativePath("/profile-images/123/file_170000.jpg");
 * rp.getPath(); // returns "profile-images/123/file_170000.jpg"
 * This class works together with RelativePathSerializer and RelativePathDeserializer
 * to convert between backend S3 keys and frontend CDN URLs.
 */


@Getter
@NoArgsConstructor
public class RelativePath {

    private String path;

    public RelativePath(String path) {
        this.path = normalize(path);
    }

    private String normalize(String p) {
        if (p == null || p.isBlank()) return null;

        if (p.startsWith("/")) {
            return p.substring(1);
        }
        return p;
    }
}

