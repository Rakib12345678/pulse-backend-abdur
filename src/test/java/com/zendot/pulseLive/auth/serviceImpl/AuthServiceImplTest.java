package com.zendot.pulseLive.auth.serviceImpl;


import com.zendot.pulseLive.auth.dto.AuthResult;
import com.zendot.pulseLive.auth.helper.AuthHelper;
import com.zendot.pulseLive.auth.service.impl.AuthServiceImpl;
import com.zendot.pulseLive.common.exception.AppException;
import com.zendot.pulseLive.security.jwt.JwtService;
import com.zendot.pulseLive.user.model.User;
import com.zendot.pulseLive.user.model.UserStatus;
import com.zendot.pulseLive.user.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthHelper authHelper;

    @Mock
    private Executor executor;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@gmail.com");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setEighteenPlus(true);
    }

    /* =======================================================
                        REFRESH TOKEN TESTS
       ======================================================= */

    @Test
    void refresh_shouldReturnNewTokens_whenTokenValid() {

        String oldRefresh = "valid-refresh";

        user.setRefreshToken(oldRefresh);
        user.setRefreshTokenExpiresAt(Instant.now().plusSeconds(3600));

        when(userRepo.findByRefreshToken(oldRefresh))
                .thenReturn(Optional.of(user));

        when(jwtService.generateAccessToken(anyLong(), anyString()))
                .thenReturn("new-access-token");

        when(userRepo.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AuthResult result = authService.refresh(oldRefresh);

        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.newUser()).isFalse();

        verify(authHelper).ensureNotBlocked(user);
        verify(userRepo).save(user);
    }

    @Test
    void refresh_shouldThrow_whenTokenExpired() {

        String expiredToken = "expired";

        user.setRefreshToken(expiredToken);
        user.setRefreshTokenExpiresAt(Instant.now().minusSeconds(10));

        when(userRepo.findByRefreshToken(expiredToken))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refresh(expiredToken))
                .isInstanceOf(AppException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refresh_shouldThrow_whenTokenNotFound() {

        when(userRepo.findByRefreshToken("invalid"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("invalid"))
                .isInstanceOf(AppException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /* =======================================================
                    TOKEN ISSUING BEHAVIOR
       ======================================================= */

    @Test
    void issueTokens_shouldFail_ifUserNotSaved() {

        User unsaved = new User(); // id = null

        assertThatThrownBy(() ->
                authService.refresh("anything")
        );
    }
}
