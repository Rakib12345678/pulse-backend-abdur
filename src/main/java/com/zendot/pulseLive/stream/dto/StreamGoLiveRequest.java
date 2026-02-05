package com.zendot.pulseLive.stream.dto;

import jakarta.validation.constraints.NotBlank;

public record StreamGoLiveRequest(
        @NotBlank(message="Title is required")
        String title,
        @NotBlank(message="Category is required")
        String category
){}
