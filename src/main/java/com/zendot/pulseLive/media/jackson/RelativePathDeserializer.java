package com.zendot.pulseLive.media.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.zendot.pulseLive.media.model.RelativePath;
import com.zendot.pulseLive.media.util.RelativePathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;
/**
 * RelativePathDeserializer is a Jackson deserializer for the {@link RelativePath} type.

 * This deserializer converts incoming JSON values (which may be full CDN URLs or simple keys)
 * into a {@link RelativePath} object containing only the S3 key.
 * For example, given a value like:
 *   "https://cdn.myapp.com/profile-images/123/file_170000.jpg"
 * it will extract the key:
 *   "profile-images/123/file_170000.jpg"

 * This is useful when frontend sends full CDN URLs but the backend only needs the S3 key
 * to store in the database or perform operations like delete or re-generate presigned URLs.

 * Note: This class is currently not actively used, but retained here for reference and possible
 * future usage in JSON deserialization of profile image fields.
 *
 * @Author: Abdur
 * @Date: 2026-02-03
 */

@JsonComponent
@RequiredArgsConstructor
public class RelativePathDeserializer extends JsonDeserializer<RelativePath> {

    private final RelativePathUtils utils;

    @Override
    public RelativePath deserialize(JsonParser parser,
                                    DeserializationContext ctx) throws IOException {

        String value = parser.getText();

        if (value == null || value.isBlank()) {
            return null;
        }

        String key = utils.extractKey(value);

        return new RelativePath(key);
    }
}

