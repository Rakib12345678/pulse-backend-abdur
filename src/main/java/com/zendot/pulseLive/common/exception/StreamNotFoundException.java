package com.zendot.pulseLive.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class StreamNotFoundException extends RuntimeException {

    public StreamNotFoundException(String message) {
        super(message);
    }
}

