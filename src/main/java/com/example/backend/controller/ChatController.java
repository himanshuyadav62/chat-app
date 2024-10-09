package com.example.backend.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.WebSocketSession;

import com.example.backend.entity.Message;
import com.example.backend.entity.User;
import com.example.backend.repository.MessageRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.MessageService;
import com.example.backend.service.WebSocketEventListener;

@RestController
@RequestMapping("/api")
public class ChatController {

    private MessageRepository messageRepository;
    private SimpMessagingTemplate messagingTemplate;
    private UserRepository userRepository;
    private WebSocketEventListener webSocketEventListener;
    private MessageService messageService;

    public ChatController(MessageRepository messageRepository, SimpMessagingTemplate messagingTemplate,
            UserRepository userRepository, WebSocketEventListener webSocketEventListener,
            MessageService messageService) {
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
        this.webSocketEventListener = webSocketEventListener;
        this.messageService = messageService;
    }

    @MessageMapping("/chat")
    public void send(Message message) {
        messageService.addToQueue(message);
        if (message.getReceiver() == null || message.getReceiver().isEmpty()) {
            // Send to all subscribers
            messagingTemplate.convertAndSend("/topic/messages", message);
        } else {
            // Send to specific user
            messagingTemplate.convertAndSendToUser(message.getReceiver(), "/queue/messages", message);
        }
    }

    @MessageMapping("/webrtc.signal")
    public void handleWebRTCSignal(@Payload Map<String, Object> signal, Principal principal) {
        String to = (String) signal.get("to");
        signal.put("from", principal.getName());
        messagingTemplate.convertAndSendToUser(to, "/queue/webrtc", signal);
    }

    @MessageMapping("/call")
    public void initiateCall(@Payload Map<String, String> callInfo, Principal principal) {
        String caller = principal.getName();
        String callee = callInfo.get("callee");
        String callType = callInfo.get("type"); // "audio" or "video"

        // Notify the callee about the incoming call
        messagingTemplate.convertAndSendToUser(callee, "/queue/call",
                Map.of("type", "incoming_call", "caller", caller, "callType", callType));

        System.out.println("Call initiated: " + caller + " is calling " + callee + " via " + callType);
    }

    @MessageMapping("/call/answer")
    public void answerCall(@Payload Map<String, Object> answerInfo, Principal principal) {
        String caller = (String) answerInfo.get("caller");
        String callee = principal.getName();
        boolean accepted = (boolean) answerInfo.get("accepted");

        // Notify the caller about the call answer
        messagingTemplate.convertAndSendToUser(caller, "/queue/call",
                Map.of("type", "call_answered", "callee", callee, "accepted", accepted));

        System.out.println(
                "Call answered: " + callee + " " + (accepted ? "accepted" : "rejected") + " call from " + caller);
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, String>>> getUsers(Authentication authentication) {
        return ResponseEntity.ok(this.userRepository.findAllEmailsAndFullNamesExcept(authentication.getName()));
    }

    @GetMapping("/messages")
    public ResponseEntity<List<Message>> getMessages(@RequestParam String user, Authentication authentication) {
        String currentUserEmail = authentication.getName();
        List<Message> messages = messageRepository.findBySenderAndReceiverOrderByTimeStampDesc(currentUserEmail, user);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/admin/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/user/isOnline")
    public boolean checkIfUserisOnline(@RequestParam String email) {
        return this.webSocketEventListener.isUserOnline(email);
    }

    @GetMapping("/user/lastSeenAt")
    public ResponseEntity<String> getUserLastSeenTimEntity(@RequestParam String email) {
        if (this.webSocketEventListener.isUserOnline(email)) {
            return ResponseEntity.ok("online");
        }
        String lastSeen = this.userRepository.getLastSeenByEmail(email).toString();
        if (lastSeen == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        return ResponseEntity.ok(lastSeen);
    }

}
