package com.zendot.pulseLive.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zendot.pulseLive.auth.dto.AuthResult;
import com.zendot.pulseLive.auth.dto.AuthUserDto;
import com.zendot.pulseLive.auth.dto.GoogleLoginRequest;
import com.zendot.pulseLive.auth.service.AuthService;
import com.zendot.pulseLive.media.util.RelativePathUtils;
import com.zendot.pulseLive.security.jwt.JwtAuthFilter;
import com.zendot.pulseLive.user.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, OAuth2ClientAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RelativePathUtils relativePathUtils;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    private AuthUserDto mockUser(Long id) {
        return new AuthUserDto(
                id,
                "test@email.com",
                "testuser",
                "Test User",
                "bio",
                "India",
                null, // RelativePath
                UserStatus.ACTIVE,
                true,
                true,
                Instant.parse("2024-01-01T10:00:00Z")
        );
    }

    // =====================================================
    // GOOGLE LOGIN
    // =====================================================

    @Test
    void googleLogin_success() throws Exception {

        AuthResult result = new AuthResult(
                "access-token",
                "refresh-token",
                mockUser(1L),
                true
        );

        when(authService.loginGoogle(anyString(), anyBoolean()))
                .thenReturn(result);

        GoogleLoginRequest request =
                new GoogleLoginRequest("google-token", true);

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.newUser").value(true))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.status").value("ACTIVE"));
    }

    @Test
    void googleLogin_validationFailure_shouldReturn400() throws Exception {

        // Assuming idToken has @NotBlank
        GoogleLoginRequest request =
                new GoogleLoginRequest(null, true);

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void googleLogin_missingBody_shouldReturn500() throws Exception {

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void googleLogin_invalidJson_shouldReturn500() throws Exception {

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void googleLogin_serviceThrowsException_shouldReturn500() throws Exception {

        when(authService.loginGoogle(anyString(), anyBoolean()))
                .thenThrow(new RuntimeException("Invalid token"));

        GoogleLoginRequest request =
                new GoogleLoginRequest("bad-token", true);

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    // =====================================================
    // REFRESH
    // =====================================================

    @Test
    void refresh_success() throws Exception {

        AuthResult result = new AuthResult(
                "new-access",
                "new-refresh",
                mockUser(2L),
                false
        );

        when(authService.refresh(anyString()))
                .thenReturn(result);

        mockMvc.perform(post("/api/auth/refresh")
                        .param("refreshToken", "valid-refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"))
                .andExpect(jsonPath("$.newUser").value(false))
                .andExpect(jsonPath("$.user.id").value(2));
    }

    @Test
    void refresh_missingParam_shouldReturn500() throws Exception {

        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void refresh_serviceThrowsException_shouldReturn500() throws Exception {

        when(authService.refresh(anyString()))
                .thenThrow(new RuntimeException("Expired token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .param("refreshToken", "expired"))
                .andExpect(status().isInternalServerError());
    }
}
