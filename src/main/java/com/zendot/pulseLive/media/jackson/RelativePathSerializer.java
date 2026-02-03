package com.zendot.pulseLive.media.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.zendot.pulseLive.media.model.RelativePath;
import com.zendot.pulseLive.media.util.RelativePathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;
/**
 * RelativePathSerializer is a Jackson serializer for the {@link RelativePath} type.

 * This serializer converts a {@link RelativePath} object (which contains only the S3 key)
 * into a full CDN URL when sending JSON to the frontend.
 * For example, given a RelativePath with key:
 *   "profile-images/123/file_170000.jpg"
 * it will output:
 *   "https://cdn.myapp.com/profile-images/123/file_170000.jpg"

 * This allows the frontend to always receive complete, publicly accessible URLs for
 * profile images or other media, while the backend continues to store only the S3 keys.

 * If the RelativePath object or its key is null, the serializer writes a JSON null value.
 */


@JsonComponent
@RequiredArgsConstructor
public class RelativePathSerializer extends JsonSerializer<RelativePath> {

    private final RelativePathUtils utils;

    @Override
    public void serialize(RelativePath value,
                          JsonGenerator gen,
                          SerializerProvider serializers) throws IOException {

        if (value == null || value.getPath() == null) {
            gen.writeNull();
            return;
        }

        gen.writeString(utils.buildCdnUrl(value.getPath()));
    }
}

