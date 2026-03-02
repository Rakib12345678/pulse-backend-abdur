package com.zendot.pulseLive.stream.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zendot.pulseLive.security.SecurityConfig;
import com.zendot.pulseLive.security.jwt.JwtAuthFilter;
import com.zendot.pulseLive.security.jwt.JwtService;
import com.zendot.pulseLive.stream.dto.LiveCallResponse;
import com.zendot.pulseLive.stream.dto.LiveFeedResponse;
import com.zendot.pulseLive.stream.dto.StreamGoLiveRequest;
import com.zendot.pulseLive.stream.dto.StreamStartResponse;
import com.zendot.pulseLive.stream.dto.UserLiveStatus;
import com.zendot.pulseLive.stream.dto.AddCohostRequest;
import com.zendot.pulseLive.stream.service.StreamService;
import com.zendot.pulseLive.media.util.RelativePathUtils;
import com.zendot.pulseLive.user.model.User;
import com.zendot.pulseLive.user.model.UserStatus;
import com.zendot.pulseLive.user.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StreamController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtAuthFilter.class})
class StreamControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StreamService streamService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RelativePathUtils relativePathUtils;

    @Test
    void feed_isPublic() throws Exception {
        when(streamService.getLiveFeed(null, 10))
                .thenReturn(new LiveFeedResponse<>(List.of(), null, false));

        mockMvc.perform(get("/api/stream/feed"))
                .andExpect(status().isOk());
    }

    @Test
    void goLive_requiresAuth() throws Exception {
        StreamGoLiveRequest request = new StreamGoLiveRequest("Title", "Category");

        mockMvc.perform(post("/api/stream/go-live")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void goLive_returnsResponseWhenAuthenticated() throws Exception {
        StreamGoLiveRequest request = new StreamGoLiveRequest("Title", "Category");
        StreamStartResponse response = new StreamStartResponse(
                "call-1",
                "default",
                "video-token",
                "chat-key",
                "livestream",
                "call-1",
                "chat-token"
        );

        when(jwtService.parseUserId("token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser()));
        when(streamService.startStream(any())).thenReturn(response);

        mockMvc.perform(post("/api/stream/go-live")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.callId").value("call-1"))
                .andExpect(jsonPath("$.token").value("video-token"));
    }

    @Test
    void goLive_blocksDeletedUsers() throws Exception {
        StreamGoLiveRequest request = new StreamGoLiveRequest("Title", "Category");

        when(jwtService.parseUserId("token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithStatus(UserStatus.DELETED)));

        mockMvc.perform(post("/api/stream/go-live")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("ACCOUNT_DELETED"));

        verifyNoInteractions(streamService);
    }

    @Test
    void goLive_blocksDeactivatedUsers() throws Exception {
        StreamGoLiveRequest request = new StreamGoLiveRequest("Title", "Category");

        when(jwtService.parseUserId("token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithStatus(UserStatus.DEACTIVATED)));

        mockMvc.perform(post("/api/stream/go-live")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("ACCOUNT_DEACTIVATED"));

        verifyNoInteractions(streamService);
    }

    @Test
    void join_requiresAuth() throws Exception {
        mockMvc.perform(post("/api/stream/join")
                        .param("callId", "call-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void join_returnsResponseWhenAuthenticated() throws Exception {
        StreamStartResponse response = new StreamStartResponse(
                "call-1",
                "default",
                "video-token",
                "chat-key",
                "livestream",
                "call-1",
                "chat-token"
        );

        when(jwtService.parseUserId("token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser()));
        when(streamService.joinStream(eq("call-1"))).thenReturn(response);

        mockMvc.perform(post("/api/stream/join")
                        .param("callId", "call-1")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.callId").value("call-1"))
                .andExpect(jsonPath("$.token").value("video-token"));
    }

    @Test
    void end_requiresAuth() throws Exception {
        mockMvc.perform(post("/api/stream/end")
                        .param("callId", "call-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void end_returnsOkWhenAuthenticated() throws Exception {
        when(jwtService.parseUserId("token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser()));

        mockMvc.perform(post("/api/stream/end")
                        .param("callId", "call-1")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());

        verify(streamService).endStream("call-1");
    }

    @Test
    void liveStatus_requiresAuth() throws Exception {
        mockMvc.perform(get("/api/stream/alice/live-status"))
                .andExpect(status().isForbidden());
    }

    @Test
    void liveStatus_returnsDataWhenAuthenticated() throws Exception {
        when(jwtService.parseUserId("token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser()));
        when(streamService.getUserLiveStatusByUsername(eq("alice")))
                .thenReturn(new UserLiveStatus(true, "call-1", "alice"));

        mockMvc.perform(get("/api/stream/alice/live-status")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLive").value(true))
                .andExpect(jsonPath("$.callId").value("call-1"));
    }

    @Test
    void syncParticipants_requiresAuth() throws Exception {
        mockMvc.perform(post("/api/stream/sync-participants")
                        .param("callId", "call-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void syncParticipants_returnsDataWhenAuthenticated() throws Exception {
        LiveCallResponse response = new LiveCallResponse(
                "alice",
                "call-1",
                "default",
                "Title",
                "Category",
                "img",
                "1",
                3,
                10L,
                new Date(),
                true,
                new Date(),
                List.of()
        );

        when(jwtService.parseUserId("token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser()));
        when(streamService.syncParticipantCount("call-1")).thenReturn(response);

        mockMvc.perform(post("/api/stream/sync-participants")
                        .param("callId", "call-1")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.callId").value("call-1"))
                .andExpect(jsonPath("$.participantCount").value(3));
    }

    @Test
    void addCohost_returnsDataWhenAuthenticated() throws Exception {
        LiveCallResponse response = new LiveCallResponse(
                "alice",
                "call-1",
                "default",
                "Title",
                "Category",
                "img",
                "1",
                3,
                10L,
                new Date(),
                true,
                new Date(),
                List.of()
        );
        AddCohostRequest request = new AddCohostRequest("call-1", "bob", true);

        when(jwtService.parseUserId("token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser()));
        when(streamService.updateCohost(any())).thenReturn(response);

        mockMvc.perform(post("/api/stream/cohost/add")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.callId").value("call-1"));
    }

    @Test
    void leaveCohost_returnsDataWhenAuthenticated() throws Exception {
        LiveCallResponse response = new LiveCallResponse(
                "alice",
                "call-1",
                "default",
                "Title",
                "Category",
                "img",
                "1",
                2,
                10L,
                new Date(),
                true,
                new Date(),
                List.of()
        );
        AddCohostRequest request = new AddCohostRequest("call-1", "bob", false);

        when(jwtService.parseUserId("token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser()));
        when(streamService.updateCohost(any())).thenReturn(response);

        mockMvc.perform(post("/api/stream/cohost/leave")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.callId").value("call-1"));
    }

    private User activeUser() {
        return userWithStatus(UserStatus.ACTIVE);
    }

    private User userWithStatus(UserStatus status) {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setStatus(status);
        return user;
    }
}
