package com.zendot.pulseLive.auth.service;

import com.zendot.pulseLive.auth.dto.AuthResult;

public interface AuthService {
    AuthResult loginGoogle(String idToken, Boolean eighteenPlus);
    AuthResult refresh(String refreshToken);
}
