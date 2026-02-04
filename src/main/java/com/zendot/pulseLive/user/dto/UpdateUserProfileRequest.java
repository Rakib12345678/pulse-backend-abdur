package com.zendot.pulseLive.user.dto;

import com.zendot.pulseLive.media.model.RelativePath;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(
            description = "Profile image media key or full CDN URL",
            example = "media/image/profile-image/123/avatar.jpg"
    )
    private RelativePath profileImage;
}
