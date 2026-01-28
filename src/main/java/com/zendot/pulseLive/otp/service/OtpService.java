package com.zendot.pulseLive.otp.service;

import com.zendot.pulseLive.auth.dto.AuthResult;

public interface OtpService {
    void sendOtp(String email);
    AuthResult verifyOtpAndLogin(String email, String otp);
    void resendOtp(String email);

}
