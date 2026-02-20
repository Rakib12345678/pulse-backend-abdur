package com.zendot.pulseLive.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zendot.pulseLive.user.dto.*;
import com.zendot.pulseLive.user.model.UserStatus;
import com.zendot.pulseLive.user.service.UserService;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    // ================= GET /me =================

    @Test
    void me_shouldReturnCurrentUserProfile() throws Exception {


        Mockito.when(userService.getCurrentUserProfile()).thenReturn(mockProfile());

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk());
    }

    // ================= PUT /me =================

    @Test
    void updateProfile_shouldReturnUpdatedProfile() throws Exception {

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setUsername("testuser");



        Mockito.when(userService.updateCurrentUserProfile(Mockito.any()))
                .thenReturn(mockProfile());

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ================= PUT /me/18plus-consent =================

    @Test
    void set18PlusConsent_shouldReturnProfile() throws Exception {

        EighteenPlusConsentRequest request =
                new EighteenPlusConsentRequest(true);

        Mockito.when(userService.declareEighteenPlusConsent(Mockito.any()))
                .thenReturn(mockProfile());

        mockMvc.perform(put("/api/users/me/18plus-consent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ================= GET username available =================

    @Test
    void usernameAvailable_shouldReturnBoolean() throws Exception {

        Mockito.when(userService.isUsernameAvailable("john"))
                .thenReturn(true);

        mockMvc.perform(get("/api/users/username-available")
                        .param("username", "john"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    // ================= POST follow/unfollow =================

    @Test
    void followUnfollow_shouldReturnOk() throws Exception {

        FollowRequest request = new FollowRequest(2L, true);

        mockMvc.perform(post("/api/users/follow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Mockito.verify(userService).followUnfollow(Mockito.any());
    }

    // ================= GET profile by username =================

    @Test
    void getByUsername_shouldReturnProfile() throws Exception {

        Mockito.when(userService.getUserProfileByUsername("john"))
                .thenReturn(mockPublicProfile());

        mockMvc.perform(get("/api/users/john"))
                .andExpect(status().isOk());
    }

    // ================= POST block/unblock =================

    @Test
    void blockUnblock_shouldReturnOk() throws Exception {

        BlockUserRequest request = new BlockUserRequest(2L, true);

        mockMvc.perform(post("/api/users/block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Mockito.verify(userService).blockUnblock(Mockito.any());
    }

    // ================= GET blocked users =================

    @Test
    void blockedUsers_shouldReturnList() throws Exception {

        Mockito.when(userService.getBlockedUsers())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/users/blocked"))
                .andExpect(status().isOk());
    }

    private UserPublicProfileResponse mockPublicProfile() {
        return new UserPublicProfileResponse(
                1L,
                "john",
                "John Doe",
                "bio",
                "India",
                null,
                UserStatus.ACTIVE,
                true,
                10L,
                5L,
                3L,
                Instant.now(),
                false
        );
    }
    private UserProfileResponse mockProfile() {
        return new UserProfileResponse(
                1L,
                "test@mail.com",
                "john",
                "John Doe",
                "bio",
                "India",
                null,
                UserStatus.ACTIVE,
                true,
                true,
                10L,
                5L,
                3L,
                Instant.now()
        );
    }



}
