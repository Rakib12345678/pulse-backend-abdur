package com.zendot.pulseLive.media.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class RelativePathUtils {
    @Value("${cloudfront.url}")
    private  String cloudfrontUrl; // Inject directly from config

    public String extractKey(String value) {

        if (value == null || value.isBlank()) return null;

        try {
            String path = value;

            if (value.startsWith("http")) {
                URI uri = URI.create(value);
                path = uri.getPath();
            }

            // Remove leading slash
            if (path.startsWith("/")) path = path.substring(1);

            return path;

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid profile image URL or key");
        }
    }



    public  String buildCdnUrl(String key) {
        if (key == null || key.isBlank()) return null;

        String url = cloudfrontUrl;
        // Remove trailing slash if exists
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        return url + "/" + key;
    }
}
