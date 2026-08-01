package com.medicare.chatbot.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationService {

    private static final int MAX_HISTORY_TURNS = 10; // Holds the last 10 messages (5 turns)

    // Thread-safe map storing username to list of conversation messages
    private final Map<String, List<Message>> chatMemories = new ConcurrentHashMap<>();

    public void addMessage(String username, String role, String text) {
        if (username == null || username.isBlank() || role == null || text == null) {
            return;
        }

        List<Message> history = chatMemories.computeIfAbsent(username, k -> new ArrayList<>());
        
        synchronized (history) {
            history.add(new Message(role, text));
            
            // Prune history if it exceeds our memory depth limit
            while (history.size() > MAX_HISTORY_TURNS) {
                history.remove(0);
            }
        }
    }

    public List<Message> getHistory(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }

        List<Message> history = chatMemories.get(username);
        if (history == null) {
            return List.of();
        }

        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    public void clearHistory(String username) {
        if (username != null && !username.isBlank()) {
            chatMemories.remove(username);
        }
    }

    public static class Message {
        private final String role; // "user" or "model"
        private final String text;

        public Message(String role, String text) {
            this.role = role;
            this.text = text;
        }

        public String getRole() {
            return role;
        }

        public String getText() {
            return text;
        }
    }
}
