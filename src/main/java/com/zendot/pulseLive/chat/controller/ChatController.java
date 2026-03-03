package com.zendot.pulseLive.chat.controller;

import com.zendot.pulseLive.chat.dto.ChatTokenResponse;
import com.zendot.pulseLive.chat.dto.DirectChatRequest;
import com.zendot.pulseLive.chat.dto.DirectChatResponse;
import com.zendot.pulseLive.chat.service.StreamChatService;
import com.zendot.pulseLive.common.exception.AppException;
import com.zendot.pulseLive.user.dto.UserProfileResponse;
import com.zendot.pulseLive.user.mapper.UserDtoMapper;
import com.zendot.pulseLive.user.repo.UserRepository;
import com.zendot.pulseLive.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final StreamChatService streamChatService;
    private final UserService userService;
    private final UserRepository userRepository;

    @Value("${io.getstream.apiKey}")
    private String streamApiKey;

    @Value("${io.getstream.chat.apiKey:}")
    private String chatApiKey;

    @Value("${stream.chat.directChannelType:messaging}")
    private String directChannelType;

    public ChatController(
            StreamChatService streamChatService,
            UserService userService,
            UserRepository userRepository
    ) {
        this.streamChatService = streamChatService;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @PostMapping("/direct")
    public DirectChatResponse startDirectChat(@Valid @RequestBody DirectChatRequest request) {
        UserProfileResponse currentUser = userService.getCurrentUserProfile();
        Long targetUserId = request.targetUserId();

        if (currentUser.id().equals(targetUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot start a chat with yourself");
        }

        var targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        userService.ensureNotBlocked(currentUser.id(), targetUserId);

        UserProfileResponse targetProfile = UserDtoMapper.toProfile(targetUser);

        streamChatService.upsertUser(currentUser);
        streamChatService.upsertUser(targetProfile);

        String channelId = streamChatService.ensureDirectChannel(currentUser, targetProfile);
        String chatToken = streamChatService.createChatUserToken(String.valueOf(currentUser.id()));

        String effectiveChatApiKey = (chatApiKey != null && !chatApiKey.isBlank())
                ? chatApiKey
                : streamApiKey;

        return new DirectChatResponse(
                effectiveChatApiKey,
                directChannelType,
                channelId,
                chatToken,
                targetUserId
        );
    }

    @GetMapping("/token")
    public ResponseEntity<ChatTokenResponse> generateChatToken() {

        // Fetch logged-in user from security context
        UserProfileResponse currentUser = userService.getCurrentUserProfile();

        // Convert userId to String (Stream requires String)
        String streamUserId = String.valueOf(currentUser.id());

        // Generate token
        String token = streamChatService.createChatUserToken(streamUserId);

        return ResponseEntity.ok(new ChatTokenResponse(token));
    }
}
