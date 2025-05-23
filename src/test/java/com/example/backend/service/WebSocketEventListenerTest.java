package com.example.backend.service;

import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WebSocketEventListenerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActiveCallService activeCallService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private SimpMessageHeaderAccessor headerAccessor;

    @Mock
    private SessionDisconnectEvent sessionDisconnectEvent;

    @Mock
    private Message<byte[]> message;

    @Mock
    private Principal principal;

    @InjectMocks
    private WebSocketEventListener webSocketEventListener;

    @BeforeEach
    void setUp() {
        // General setup for headerAccessor and event if needed across tests
        // Specific mocks will be in each test method
    }

    @Test
    void handleWebSocketDisconnectListener_userInCall_sendsNotificationAndEndsCall() {
        // Arrange
        String user1Email = "user1@example.com";
        String user2Email = "user2@example.com";

        when(sessionDisconnectEvent.getMessage()).thenReturn(message);
        // Use a spy for SimpMessageHeaderAccessor to allow direct when().thenReturn() on wrap() if it were static
        // For now, let's assume we can mock the instance and its methods
        // If SimpMessageHeaderAccessor.wrap is static, this needs a different approach (e.g. PowerMockito or refactoring)
        // However, SimpMessageHeaderAccessor is typically instantiated, so mocking it is fine.
        // The problem description implies headerAccessor is a mock, so we configure it.

        // We need to ensure that SimpMessageHeaderAccessor.wrap(message) returns our mocked headerAccessor
        // This typically means the class under test must use a mechanism to get headerAccessor,
        // or wrap must be called on a SimpMessageHeaderAccessor instance we can control/mock.
        // The current code is: SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        // This is a static call. For simplicity, I'll assume this test can be written as if `wrap` is not static,
        // or that the structure allows for this kind of mocking. If not, this test will fail and need adjustment.
        // A common pattern is to have a factory or pass accessor, but let's proceed with direct mocking for now.

        // Due to SimpMessageHeaderAccessor.wrap() being a static method,
        // we cannot directly mock it with Mockito alone to return our 'headerAccessor' mock.
        // We will mock the methods on the 'headerAccessor' that would be returned by 'wrap'.
        // This means we need to ensure that the test environment somehow uses our 'headerAccessor' instance.
        // For this test, we'll assume that when SimpMessageHeaderAccessor.wrap is called,
        // the subsequent calls to getUser() and getSessionId() can be mocked
        // This is a limitation of Mockito with static methods.
        // Let's assume the SUT is refactored to allow injection of SimpMessageHeaderAccessor or a factory.
        // For now, we'll directly mock the instance methods on the injected mock, assuming it's used.

        // If we cannot change the SUT, we would use PowerMock or a similar tool.
        // Given the constraints, we'll proceed by preparing the mocks that would be used if `wrap` returned our mock.
        // This test will only pass if the SUT is structured to use an injected SimpMessageHeaderAccessor,
        // or if SimpMessageHeaderAccessor.wrap() is handled by a testing utility not specified.

        // Let's assume 'SimpMessageHeaderAccessor.wrap(event.getMessage())' is called inside the method
        // and it somehow resolves to our 'headerAccessor' mock. This is a strong assumption.
        // The correct way is to refactor WebSocketEventListener to accept a SimpMessageHeaderAccessor factory or instance.

        // For the purpose of this exercise, we'll proceed by mocking the interactions
        // as if `headerAccessor` is the object returned by `SimpMessageHeaderAccessor.wrap`.
        // This is a simplification.

        // If using `SimpMessageHeaderAccessor.wrap(event.getMessage())` directly, we need to mock static method.
        // We'll mock the `principal` and `headerAccessor.getUser().getName()`.
        when(principal.getName()).thenReturn(user1Email);
        // We need to make SimpMessageHeaderAccessor.wrap(message) return our headerAccessor
        // This is not possible with Mockito for static methods.
        // So, we will mock the instance methods that are called on the result of wrap().
        // This implies that headerAccessor is somehow the instance used.

        // Let's assume `webSocketEventListener` is constructed in such a way that it uses our mocked `headerAccessor`
        // when `SimpMessageHeaderAccessor.wrap` is called. This is not standard.
        // A more realistic test would involve a spy on `SimpMessageHeaderAccessor` or a refactor of the SUT.

        // Given the current SUT: `SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());`
        // We cannot mock `headers` directly using only Mockito unless we use PowerMockito or refactor the SUT.
        // Let's write the test verifying the *logic* assuming the headers are correctly obtained.

        // We will create a real SimpMessageHeaderAccessor and set the user.
        SimpMessageHeaderAccessor realHeaderAccessor = SimpMessageHeaderAccessor.create();
        realHeaderAccessor.setUser(principal);
        realHeaderAccessor.setSessionId("test-session-id");
        Message<byte[]> realMessage = org.springframework.messaging.support.MessageBuilder.createMessage(new byte[0], realHeaderAccessor.getMessageHeaders());
        when(sessionDisconnectEvent.getMessage()).thenReturn(realMessage); // event.getMessage() will now have our principal

        when(activeCallService.getOtherParticipant(user1Email)).thenReturn(user2Email);
        when(activeCallService.endCall(user1Email)).thenReturn(user2Email); // Assuming endCall returns the other participant

        // Act
        webSocketEventListener.handleWebSocketDisconnectListener(sessionDisconnectEvent);

        // Assert
        verify(userRepository).updateLastSeenAt(eq(user1Email), any(LocalDateTime.class));
        verify(messagingTemplate).convertAndSendToUser(
                eq(user2Email),
                eq("/queue/call"),
                eq(Map.of("type", "call_disconnected", "disconnectedUser", user1Email))
        );
        verify(activeCallService).endCall(user1Email);
    }

    @Test
    void handleWebSocketDisconnectListener_userNotInCall_noNotification() {
        // Arrange
        String user1Email = "user1@example.com";

        when(principal.getName()).thenReturn(user1Email);
        SimpMessageHeaderAccessor realHeaderAccessor = SimpMessageHeaderAccessor.create();
        realHeaderAccessor.setUser(principal);
        realHeaderAccessor.setSessionId("test-session-id");
        Message<byte[]> realMessage = org.springframework.messaging.support.MessageBuilder.createMessage(new byte[0], realHeaderAccessor.getMessageHeaders());
        when(sessionDisconnectEvent.getMessage()).thenReturn(realMessage);


        when(activeCallService.getOtherParticipant(user1Email)).thenReturn(null);

        // Act
        webSocketEventListener.handleWebSocketDisconnectListener(sessionDisconnectEvent);

        // Assert
        verify(userRepository).updateLastSeenAt(eq(user1Email), any(LocalDateTime.class));
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), anyMap());
        verify(activeCallService, never()).endCall(anyString());
    }
}
