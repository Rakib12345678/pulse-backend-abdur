package com.zendot.pulseLive.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserProfileRequest {

    @NotBlank
    @Size(min = 3, max = 30)
    private String username;

    @NotBlank
    @Size(min = 2, max = 50)
    private String fullName;

    @Size(max = 100)
    private String bio;

    @Size(max = 50)
    private String location;

    private String profileImageUrl;
}
