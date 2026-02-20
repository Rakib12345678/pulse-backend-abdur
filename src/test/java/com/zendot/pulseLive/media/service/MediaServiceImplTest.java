package com.zendot.pulseLive.media.service;

import com.zendot.pulseLive.media.model.PresignedUploadResponse;
import com.zendot.pulseLive.media.serviceImpl.MediaServiceImpl;
import com.zendot.pulseLive.media.util.RelativePathUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Mock
    private S3Presigner s3Presigner;
    @Mock
    private S3Client s3Client;
    @Mock
    private RelativePathUtils utils;
    @Mock
    private PresignedPutObjectRequest presignedRequest;

    private MediaServiceImpl mediaService;

    @BeforeEach
    void setUp() {
        mediaService = new MediaServiceImpl(s3Presigner, s3Client, utils);
        ReflectionTestUtils.setField(mediaService, "bucket", "test-bucket");
    }

    @Test
    void shouldGeneratePresignedUploadUrl() throws Exception {
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenReturn(presignedRequest);
        when(presignedRequest.url()).thenReturn(new URL("https://s3.example.com/upload"));
        when(presignedRequest.expiration()).thenReturn(Instant.parse("2024-01-01T00:00:00Z"));
        when(utils.buildCdnUrl(anyString())).thenAnswer(invocation ->
                "https://cdn.example.com/" + invocation.getArgument(0));

        PresignedUploadResponse response =
                mediaService.generateUploadUrl("media/image", "123", "jpg");

        assertThat(response.uploadUrl()).isEqualTo("https://s3.example.com/upload");
        assertThat(response.key()).startsWith("media/image/123/file_");
        assertThat(response.key()).endsWith(".jpg");
        assertThat(response.fullUrl()).startsWith("https://cdn.example.com/media/image/123/file_");
    }

    @Test
    void generateUploadUrl_throwsForUnsupportedExtension() {
        assertThatThrownBy(() -> mediaService.generateUploadUrl("media/image", "123", "exe"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generateUploadUrl_throwsForMissingExtension() {
        assertThatThrownBy(() -> mediaService.generateUploadUrl("media/image", "123", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteFile_throwsForBlankKey() {
        assertThatThrownBy(() -> mediaService.deleteFile(" "))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void deleteFile_deletesObject() {
        mediaService.deleteFile("media/image/profile-image/1/file_123.jpg");

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deleteFile_swallowsDeleteErrors() {
        doThrow(new RuntimeException("s3 error"))
                .when(s3Client)
                .deleteObject(any(DeleteObjectRequest.class));

        mediaService.deleteFile("media/image/profile-image/1/file_123.jpg");
    }
}
