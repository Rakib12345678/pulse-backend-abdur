package com.zendot.pulseLive.otp.repo;

import com.zendot.pulseLive.otp.model.EmailOtp;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {
    Optional<EmailOtp> findTopByEmailOrderByIdDesc(String email);
}
