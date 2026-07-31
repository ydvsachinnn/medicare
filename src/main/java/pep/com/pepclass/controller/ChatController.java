package pep.com.pepclass.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pep.com.pepclass.dto.ChatRequest;
import pep.com.pepclass.dto.ChatResponse;
import pep.com.pepclass.service.ChatService;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    // Constructor injection for best practices and SOLID compliance
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ChatResponse> handleChat(@Valid @RequestBody ChatRequest request, org.springframework.security.core.Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "anonymous_patient";
        String answer = chatService.processChat(username, request.getMessage());
        int remaining = chatService.getRemainingQuota(username);
        return ResponseEntity.ok(new ChatResponse(answer, remaining, ChatService.MAX_USER_QUOTA));
    }

    @GetMapping("/chat/quota")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Integer> getRemainingQuota(org.springframework.security.core.Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "anonymous_patient";
        return ResponseEntity.ok(chatService.getRemainingQuota(username));
    }
}
