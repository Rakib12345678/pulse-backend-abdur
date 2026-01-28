package com.zendot.pulseLive.auth.service;

import com.zendot.pulseLive.auth.dto.AuthResult;

public interface AuthService {
    AuthResult loginGoogle(String idToken, Boolean is18Plus);
    AuthResult refresh(String refreshToken);
}
