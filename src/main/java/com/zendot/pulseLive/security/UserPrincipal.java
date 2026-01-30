package com.zendot.pulseLive.security;

import java.io.Serializable;

public record UserPrincipal(Long userId, String email) implements Serializable {}
