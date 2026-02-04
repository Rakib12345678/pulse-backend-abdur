package com.zendot.pulseLive.auth.service.impl;

import com.zendot.pulseLive.auth.dto.AuthResult;
import com.zendot.pulseLive.auth.helper.AuthHelper;
import com.zendot.pulseLive.auth.service.AuthService;
import com.zendot.pulseLive.common.exception.AppException;
import com.zendot.pulseLive.security.jwt.JwtService;
import com.zendot.pulseLive.user.model.User;
import com.zendot.pulseLive.user.model.UserStatus;
import com.zendot.pulseLive.user.mapper.UserDtoMapper;
import com.zendot.pulseLive.user.repo.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepo;
    private final JwtService jwtService;
    private final AuthHelper authHelper;
    private final Executor vExecutor;

    private final RestClient restClient = RestClient.create();

    @Value("${app.jwt.refresh-days:30}")
    private int refreshDays;

    public AuthServiceImpl(
            UserRepository userRepo,
            JwtService jwtService,
            AuthHelper authHelper,
            Executor virtualThreadExecutor) {

        this.userRepo = userRepo;
        this.jwtService = jwtService;
        this.authHelper = authHelper;
        this.vExecutor = virtualThreadExecutor;
    }

    /**
     * SINGLE API
     * Google signup + login
     */
    @Override
    public AuthResult loginGoogle(String idToken, Boolean eighteenPlus) {

        GooglePayload payload = verifyGoogleToken(idToken);

        String email = payload.email();
        String googleId = payload.sub();

        if (email == null || email.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Google account email missing");
        }

        boolean newUser = false;

        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            newUser = true;
            user = new User();
            user.setEmail(email);
            user.setGoogleId(googleId);
            user.setEmailVerified(true);
            user.setEighteenPlus(Boolean.TRUE.equals(eighteenPlus));
            user.setStatus(UserStatus.ACTIVE);

            user = userRepo.save(user);
        } else {
            user.setGoogleId(googleId);
            if (!user.isEighteenPlus() && Boolean.TRUE.equals(eighteenPlus)) {
                user.setEighteenPlus(true);
            }
        }

        authHelper.ensureNotBlocked(user);

        return issueTokens(user, newUser);
    }

    @Override
    public AuthResult refresh(String refreshToken) {

        User user = userRepo.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (user.getRefreshTokenExpiresAt() == null ||
                Instant.now().isAfter(user.getRefreshTokenExpiresAt())) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        authHelper.ensureNotBlocked(user);

        return issueTokens(user, false);
    }

    /* ======================= helpers ======================= */

    private AuthResult issueTokens(User user, boolean newUser) {

        if (user.getId() == null) {
            throw new IllegalStateException("Cannot issue token for unsaved user");
        }

        String access = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refresh = UUID.randomUUID().toString();

        user.setRefreshToken(refresh);
        user.setRefreshTokenExpiresAt(
                Instant.now().plus(refreshDays, ChronoUnit.DAYS)
        );

        userRepo.save(user);
        return new AuthResult(access, refresh, UserDtoMapper.toAuthUser(user), newUser);
    }

    private GooglePayload verifyGoogleToken(String idToken) {

        try {
            return CompletableFuture.supplyAsync(() -> {
                Map<?, ?> resp = restClient.get()
                        .uri("https://oauth2.googleapis.com/tokeninfo?id_token={t}", idToken)
                        .retrieve()
                        .body(Map.class);

                if (resp == null) {
                    throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
                }

                return new GooglePayload(
                        (String) resp.get("sub"),
                        (String) resp.get("email")
                );
            }, vExecutor).join();

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
        }
    }

    private record GooglePayload(String sub, String email) {}
}
