package com.zendot.pulseLive.auth.helper;

import com.zendot.pulseLive.common.exception.AppException;
import com.zendot.pulseLive.user.model.User;
import com.zendot.pulseLive.user.model.UserStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AuthHelper {

    public void ensureNotBlocked(User user) {
        if (user.getStatus() == UserStatus.DELETED) {
            throw new AppException(HttpStatus.FORBIDDEN, "Account deleted");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AppException(HttpStatus.FORBIDDEN, "User is suspended");
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(HttpStatus.FORBIDDEN, "User is locked");
        }
    }
}
