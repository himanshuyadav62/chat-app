package com.example.backend.controller;

import com.example.backend.repository.MessageRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.ActiveCallService;
import com.example.backend.service.MessageService;
import com.example.backend.service.WebSocketEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatControllerTest {

    @Mock
    private ActiveCallService activeCallService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private UserRepository userRepository; // Not used in these specific tests but part of constructor

    @Mock
    private MessageRepository messageRepository; // Not used in these specific tests but part of constructor

    @Mock
    private WebSocketEventListener webSocketEventListener; // Not used in these specific tests but part of constructor

    @Mock
    private MessageService messageService; // Not used in these specific tests but part of constructor

    @Mock
    private Principal principal;

    @InjectMocks
    private ChatController chatController;

    private String callerEmail;
    private String calleeEmail;

    @BeforeEach
    void setUp() {
        callerEmail = "caller@example.com";
        calleeEmail = "callee@example.com";
    }

    @Test
    void answerCall_accepted_registersCall() {
        // Arrange
        when(principal.getName()).thenReturn(calleeEmail);
        Map<String, Object> answerInfo = Map.of("caller", callerEmail, "accepted", true);

        // Act
        chatController.answerCall(answerInfo, principal);

        // Assert
        verify(activeCallService).startCall(callerEmail, calleeEmail);
        verify(messagingTemplate).convertAndSendToUser(
                eq(callerEmail),
                eq("/queue/call"),
                eq(Map.of("type", "call_answered", "callee", calleeEmail, "accepted", true))
        );
    }

    @Test
    void answerCall_rejected_doesNotRegisterCall() {
        // Arrange
        when(principal.getName()).thenReturn(calleeEmail);
        Map<String, Object> answerInfo = Map.of("caller", callerEmail, "accepted", false);

        // Act
        chatController.answerCall(answerInfo, principal);

        // Assert
        verify(activeCallService, never()).startCall(anyString(), anyString());
        verify(messagingTemplate).convertAndSendToUser(
                eq(callerEmail),
                eq("/queue/call"),
                eq(Map.of("type", "call_answered", "callee", calleeEmail, "accepted", false))
        );
    }

    @Test
    void handleHangupCall_userInCall_sendsNotificationAndEndsCall() {
        // Arrange
        String user1Email = "user1@example.com";
        String user2Email = "user2@example.com";
        when(principal.getName()).thenReturn(user1Email);
        when(activeCallService.getOtherParticipant(user1Email)).thenReturn(user2Email);
        when(activeCallService.endCall(user1Email)).thenReturn(user2Email); // Mocking endCall to return the other participant
        Map<String, String> hangupInfo = Map.of(); // Payload content doesn't matter

        // Act
        chatController.handleHangupCall(hangupInfo, principal);

        // Assert
        verify(messagingTemplate).convertAndSendToUser(
                eq(user2Email),
                eq("/queue/call"),
                eq(Map.of("type", "call_hungup", "hungupByUser", user1Email))
        );
        verify(activeCallService).endCall(user1Email);
    }

    @Test
    void handleHangupCall_userNotInCall_noNotification() {
        // Arrange
        String user1Email = "user1@example.com";
        when(principal.getName()).thenReturn(user1Email);
        when(activeCallService.getOtherParticipant(user1Email)).thenReturn(null);
        Map<String, String> hangupInfo = Map.of(); // Payload content doesn't matter

        // Act
        chatController.handleHangupCall(hangupInfo, principal);

        // Assert
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), anyMap());
        verify(activeCallService, never()).endCall(anyString());
    }
}
