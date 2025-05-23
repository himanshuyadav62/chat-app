package com.example.backend.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ActiveCallService {

    private Map<String, String> activeCalls = new ConcurrentHashMap<>();

    public void startCall(String user1Email, String user2Email) {
        activeCalls.put(user1Email, user2Email);
        activeCalls.put(user2Email, user1Email);
    }

    public String endCall(String userEmail) {
        String otherParticipantEmail = activeCalls.get(userEmail);
        if (otherParticipantEmail != null) {
            activeCalls.remove(userEmail);
            activeCalls.remove(otherParticipantEmail);
            return otherParticipantEmail;
        }
        return null;
    }

    public String getOtherParticipant(String userEmail) {
        return activeCalls.get(userEmail);
    }

    public boolean isInCall(String userEmail) {
        return activeCalls.containsKey(userEmail);
    }
}
