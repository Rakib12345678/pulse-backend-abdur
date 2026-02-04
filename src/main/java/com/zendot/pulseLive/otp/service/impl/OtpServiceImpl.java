package com.zendot.pulseLive.otp.service.impl;

import com.zendot.pulseLive.auth.dto.AuthResult;
import com.zendot.pulseLive.auth.helper.AuthHelper;
import com.zendot.pulseLive.common.exception.AppException;
import com.zendot.pulseLive.otp.model.EmailOtp;
import com.zendot.pulseLive.otp.repo.EmailOtpRepository;
import com.zendot.pulseLive.otp.service.OtpService;
import com.zendot.pulseLive.security.jwt.JwtService;
import com.zendot.pulseLive.user.model.User;
import com.zendot.pulseLive.user.model.UserStatus;
import com.zendot.pulseLive.user.repo.UserRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class OtpServiceImpl implements OtpService {

    private final EmailOtpRepository otpRepo;
    private final UserRepository userRepo;
    private final JavaMailSender mailSender;
    private final Executor vExecutor;
    private final JwtService jwtService;
    private final AuthHelper authHelper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    @Value("${app.otp.ttl-minutes:10}") private int ttlMinutes;
    @Value("${app.otp.max-attempts:5}") private int maxAttempts;
    @Value("${app.otp.max-resends:3}") private int maxResends;
    @Value("${app.otp.resend-cooldown-seconds:60}") private int resendCooldownSeconds;
    @Value("${app.jwt.refresh-days:30}") private int refreshDays;

    public OtpServiceImpl(
            EmailOtpRepository otpRepo,
            UserRepository userRepo,
            JavaMailSender mailSender,
            Executor virtualThreadExecutor,
            JwtService jwtService,
            AuthHelper authHelper) {

        this.otpRepo = otpRepo;
        this.userRepo = userRepo;
        this.mailSender = mailSender;
        this.vExecutor = virtualThreadExecutor;
        this.jwtService = jwtService;
        this.authHelper = authHelper;
    }

    /**
     * STEP 1: Send OTP (SEND OR RESEND)
     * - Same endpoint for signup & login
     * - Idempotent
     * - Cooldown + max resends enforced
     */
    @Override
    @Transactional
    public void sendOtp(String email) {
        sendOrResendOtp(email, false);
    }

    /**
     * Explicit resend (used when user clicks "Resend OTP")
     */
    @Override
    @Transactional
    public void resendOtp(String email) {
        sendOrResendOtp(email, true);
    }

    private void sendOrResendOtp(String email, boolean explicitResend) {

        EmailOtp latest = otpRepo.findTopByEmailOrderByIdDesc(email).orElse(null);

        if (latest != null && !latest.isVerified()) {

            if (latest.getResends() >= maxResends) {
                throw new AppException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "OTP resend limit reached"
                );
            }

            long secondsSinceLastSend =
                    ChronoUnit.SECONDS.between(latest.getCreatedAt(), Instant.now());

            if (secondsSinceLastSend < resendCooldownSeconds) {
                throw new AppException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait " +
                                (resendCooldownSeconds - secondsSinceLastSend) +
                                " seconds before requesting OTP again"
                );
            }
        }

        // ✅ Always allow new OTP if:
        // - no OTP exists
        // - last OTP was verified
        // - last OTP expired

        String code = String.valueOf(100000 + random.nextInt(900000));

        EmailOtp otp = new EmailOtp();
        otp.setEmail(email);
        otp.setOtpHash(encoder.encode(code));
        otp.setExpiresAt(Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES));
        otp.setAttempts(0);
        otp.setResends(latest == null ? 1 :
                latest.isVerified() ? 1 : latest.getResends() + 1);
        otp.setVerified(false);

        otpRepo.save(otp);

        vExecutor.execute(() -> {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(email);
                msg.setSubject("Your OTP Code");
                msg.setText("Your OTP is: " + code);
                mailSender.send(msg);
            } catch (Exception e) {
                log.error("Failed to send OTP email to {}", email, e);
            }
        });
    }


    /**
     * STEP 2: Verify OTP & Login
     * - User created ONLY here (if not exists)
     */
    @Override
    @Transactional
    public AuthResult verifyOtpAndLogin(String email, String otpPlain) {

        EmailOtp otp = otpRepo.findTopByEmailOrderByIdDesc(email)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "OTP not found"));

        if (Instant.now().isAfter(otp.getExpiresAt())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "OTP expired");
        }

        if (otp.getAttempts() >= maxAttempts) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS, "OTP attempts exceeded");
        }

        otp.setAttempts(otp.getAttempts() + 1);

        if (!encoder.matches(otpPlain, otp.getOtpHash())) {
            otpRepo.save(otp);
            throw new AppException(HttpStatus.BAD_REQUEST, "Invalid OTP");
        }

        otp.setVerified(true);
        otpRepo.save(otp);

        boolean newUser = false;

        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            newUser = true;
            user = new User();
            user.setEmail(email);
            user.setEmailVerified(true);
            user.setEighteenPlus(false);
            user.setStatus(UserStatus.ACTIVE);

            user = userRepo.save(user);
        }

        authHelper.ensureNotBlocked(user);

        String access = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refresh = UUID.randomUUID().toString();

        user.setRefreshToken(refresh);
        user.setRefreshTokenExpiresAt(Instant.now().plus(refreshDays, ChronoUnit.DAYS));
        userRepo.save(user);

        return new AuthResult(access, refresh, user, newUser);
    }
}
