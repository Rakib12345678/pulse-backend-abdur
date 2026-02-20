package com.zendot.pulseLive.media.controller;

import com.zendot.pulseLive.media.model.PresignedUploadResponse;
import com.zendot.pulseLive.media.service.MediaService;
import com.zendot.pulseLive.media.util.RelativePathUtils;
import com.zendot.pulseLive.security.SecurityConfig;
import com.zendot.pulseLive.security.jwt.JwtAuthFilter;
import com.zendot.pulseLive.security.jwt.JwtService;
import com.zendot.pulseLive.user.model.User;
import com.zendot.pulseLive.user.model.UserStatus;
import com.zendot.pulseLive.user.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MediaController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtAuthFilter.class})
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaService mediaService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RelativePathUtils relativePathUtils;

    @Test
    void shouldReturnPresignedUploadUrl() throws Exception {
        PresignedUploadResponse response = new PresignedUploadResponse(
                "https://s3.example.com/upload",
                "media/image/profile-image/1/file_123.jpg",
                "https://cdn.example.com/media/image/profile-image/1/file_123.jpg",
                Instant.parse("2024-01-01T00:00:00Z")
        );

        when(jwtService.parseUserId("token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser()));
        when(mediaService.generateUploadUrl(
                eq("media/image/profile-image"),
                eq("1"),
                eq("jpg")
        )).thenReturn(response);

        mockMvc.perform(post("/api/media/upload")
                        .param("path", "PROFILE_IMAGE")
                        .param("fileExtension", "jpg")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").value("https://s3.example.com/upload"))
                .andExpect(jsonPath("$.key").value("media/image/profile-image/1/file_123.jpg"));
    }

    @Test
    void deleteFile_requiresAuth() throws Exception {
        mockMvc.perform(delete("/api/media")
                        .param("key", "media/image/profile-image/1/file_123.jpg"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteFile_returnsNoContentWhenAuthenticated() throws Exception {
        when(jwtService.parseUserId("token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser()));

        mockMvc.perform(delete("/api/media")
                        .param("key", "media/image/profile-image/1/file_123.jpg")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNoContent());

        verify(mediaService).deleteFile("media/image/profile-image/1/file_123.jpg");
    }

    private User activeUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
