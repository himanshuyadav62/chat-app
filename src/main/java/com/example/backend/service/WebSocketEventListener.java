package com.example.backend.service;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.example.backend.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WebSocketEventListener {

    private final UserRepository userRepository;
    private final ActiveCallService activeCallService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(UserRepository userRepository, ActiveCallService activeCallService, SimpMessagingTemplate messagingTemplate) {
        this.userRepository = userRepository;
        this.activeCallService = activeCallService;
        this.messagingTemplate = messagingTemplate;
    }

    private final Map<String, String> onlineUsers = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String email = headers.getUser().getName(); // Assuming email is used as the principal
        String sessionId = headers.getSessionId();
        onlineUsers.put(sessionId, email);
        log.info("User connected: {} with session id: {}", email, sessionId);
    }

    @EventListener
    @Transactional
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String email = headers.getUser().getName();
        String sessionId = headers.getSessionId();
        onlineUsers.remove(sessionId);
        this.userRepository.updateLastSeenAt(email, LocalDateTime.now());
        log.info("User disconnected: {} with session id: {}", email, sessionId);

        String otherParticipantEmail = activeCallService.getOtherParticipant(email);
        if (otherParticipantEmail != null) {
            log.info("Ending call between {} and {}", email, otherParticipantEmail);
            messagingTemplate.convertAndSendToUser(
                otherParticipantEmail,
                "/queue/call",
                Map.of("type", "call_disconnected", "disconnectedUser", email)
            );
            log.info("Sent call_disconnected message to {}", otherParticipantEmail);
            activeCallService.endCall(email); // Clean up the call state
        }
    }

    public boolean isUserOnline(String email) {
        return onlineUsers.containsValue(email);
    }
}