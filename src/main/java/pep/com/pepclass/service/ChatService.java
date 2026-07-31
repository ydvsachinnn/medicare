package pep.com.pepclass.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pep.com.pepclass.model.VectorChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    // Security: Rate Limiter Configuration (10 messages per 60 seconds per user)
    private static final int RATE_LIMIT_MAX_REQUESTS = 10;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000L;
    private static final int MAX_MESSAGE_LENGTH = 2000;

    // In-Memory Query Response Cache (max 300 entries to prevent memory leaks)
    private static final int MAX_CACHE_ENTRIES = 300;
    private final Map<String, String> responseCache = new ConcurrentHashMap<>();
    private final Map<String, Queue<Long>> rateLimiterMap = new ConcurrentHashMap<>();

    private final GeminiService geminiService;
    private final ConversationService conversationService;
    private final VectorSearchService vectorSearchService;
    private final PromptService promptService;

    // Constructor injection for full compliance with SOLID principles
    public ChatService(
            GeminiService geminiService,
            ConversationService conversationService,
            VectorSearchService vectorSearchService,
            PromptService promptService) {
        this.geminiService = geminiService;
        this.conversationService = conversationService;
        this.vectorSearchService = vectorSearchService;
        this.promptService = promptService;
    }

    public static final int MAX_USER_QUOTA = 50;
    private final Map<String, Integer> userQueryCounts = new ConcurrentHashMap<>();

    public int getRemainingQuota(String username) {
        return Math.max(0, MAX_USER_QUOTA - userQueryCounts.getOrDefault(username, 0));
    }

    public String processChat(String username, String message) {
        if (message == null || message.trim().isBlank()) {
            return "Please type a message, and I'll do my best to help you.";
        }

        // === SECURITY LAYER 1: Rate Limiting ===
        if (!checkRateLimit(username)) {
            log.warn("[ChatService] Rate limit exceeded for user '{}'", username);
            return "⏳ You are sending messages too quickly. Please wait a moment and try again.";
        }

        // === SECURITY LAYER 2: Input Length Validation ===
        if (message.length() > MAX_MESSAGE_LENGTH) {
            return "Your message is too long. Please keep your question under " + MAX_MESSAGE_LENGTH + " characters.";
        }

        // === SECURITY LAYER 3: Sanitize Input (Strip HTML, scripts, injection attempts) ===
        String sanitizedMessage = sanitizeInput(message.trim());

        // === SECURITY LAYER 4: Prompt Injection Detection ===
        if (isPromptInjection(sanitizedMessage)) {
            log.warn("[ChatService] Prompt injection attempt detected from user '{}': '{}'", username, sanitizedMessage);
            return "I am a medical assistant designed to help with health-related questions. How can I assist you today?";
        }

        String input = sanitizedMessage.toLowerCase();

        // 1. STAGE 1: FAST FIXED GREETINGS & PLEASANTRIES (Zero API Call Cost)
        String fixedGreeting = handleGreetingOrPleasantry(input);
        if (fixedGreeting != null) {
            log.info("[ChatService] Fast fixed response served for greeting query: '{}'", sanitizedMessage);
            conversationService.addMessage(username, "user", sanitizedMessage);
            conversationService.addMessage(username, "model", fixedGreeting);
            return fixedGreeting;
        }

        // 2. STAGE 2: 50-QUERY USER QUOTA CHECK
        int usedQuota = userQueryCounts.getOrDefault(username, 0);
        if (usedQuota >= MAX_USER_QUOTA) {
            log.warn("[ChatService] User '{}' has exhausted daily query quota (50/50)", username);
            return "⚠️ **Daily AI Quota Reached (50/50)**\n\nYou have used all 50 of your free daily AI queries. Please try again tomorrow or contact MediCare Support (+91 800-555-CARE).";
        }
        
        userQueryCounts.put(username, usedQuota + 1);
        int remainingQuota = MAX_USER_QUOTA - (usedQuota + 1);

        // 3. STAGE 3: IMMEDIATE EMERGENCY TRIAGE (Local Rule Overrides)
        if (isEmergencyQuery(input)) {
            return "🚨 **URGENT EMERGENCY WARNING**\n\n"
                    + "Your symptoms may indicate a serious medical emergency requiring immediate attention.\n"
                    + "Please do not wait. Take these steps immediately:\n"
                    + "- 📞 Call our 24/7 Trauma Emergency Line: **+91 (800) 555-CARE** (+91 800-555-2273)\n"
                    + "- 🏥 Go to the nearest emergency department immediately.\n\n"
                    + "*Do not attempt self-medication. Keep physical exertion to a minimum.*"
                    + "\n\n---\n*⚡ Free Tier Quota: **" + remainingQuota + " / 50** queries remaining today.*";
        }

        // 4. STAGE 4: QUERY RESPONSE CACHE CHECK
        String cacheKey = input.replaceAll("[^a-z0-9]", "");
        if (!cacheKey.isBlank() && responseCache.containsKey(cacheKey)) {
            log.info("[Cache] CACHE HIT for user '{}' query: '{}'", username, sanitizedMessage);
            String cachedAnswer = responseCache.get(cacheKey)
                    + "\n\n---\n*⚡ Free Tier Quota: **" + remainingQuota + " / 50** queries remaining today.*";

            // Record turn in session memory
            conversationService.addMessage(username, "user", sanitizedMessage);
            conversationService.addMessage(username, "model", cachedAnswer);

            return cachedAnswer;
        }

        // 5. STAGE 5: DIRECT GEMINI-3.6-FLASH EXECUTION WITH FULL SYSTEM CONTEXT
        log.info("[ChatService] Processing query via gemini-3.6-flash for user '{}': '{}'", username, sanitizedMessage);
        
        // A. Load session context history (memory)
        List<ConversationService.Message> history = conversationService.getHistory(username);

        // B. Synthesize prompt (attaches system instructions & full application context to every chat)
        String prompt = promptService.buildPrompt(sanitizedMessage, null, history);

        // C. Request generation strictly using gemini-3.6-flash
        Optional<String> geminiResponse = geminiService.generateResponse(prompt);

        if (geminiResponse.isPresent()) {
            String rawAnswer = geminiResponse.get();
            String answer = rawAnswer + "\n\n---\n*⚡ Free Tier Quota: **" + remainingQuota + " / 50** queries remaining today.*";
            
            // Store response in cache for instant future reuse
            if (!cacheKey.isBlank() && responseCache.size() < MAX_CACHE_ENTRIES) {
                responseCache.put(cacheKey, rawAnswer);
                log.info("[Cache] CACHE STORED for query: '{}'", sanitizedMessage);
            }

            // Record this successful turn in session memory
            conversationService.addMessage(username, "user", sanitizedMessage);
            conversationService.addMessage(username, "model", answer);
            
            return answer;
        }

        // 6. STAGE 6: UNREACHABLE ERROR (Only if gemini-3.6-flash fails or key missing)
        log.warn("[ChatService] gemini-3.6-flash API is unreachable or rate-limited.");
        return "I am currently unable to reach my AI processor (**gemini-3.6-flash**). Please verify your Google Gemini API key or try again in a few moments.";
    }

    /**
     * Fast fixed responses for conversational greetings, farewells, and pleasantries.
     * Prevents unnecessary external API calls for basic greetings.
     */
    private String handleGreetingOrPleasantry(String input) {
        String clean = input.replaceAll("[^a-z0-9 ]", "").trim();

        // Greetings (matching common variations like hi, hii, hello, helloo, hey, heyy)
        if (clean.startsWith("hi") || clean.startsWith("hello") || clean.startsWith("hey")
                || clean.equals("good morning") || clean.equals("good afternoon")
                || clean.equals("good evening") || clean.equals("greetings")) {
            return "Hello! I am **Neura – AI That Cares**. How can I assist you with your health, symptoms, medicines, or hospital services today?";
        }

        // Thanks
        if (clean.equals("thank you") || clean.equals("thanks")
                || clean.equals("thank you so much") || clean.equals("thanks a lot")
                || clean.equals("thank u") || clean.equals("thx")) {
            return "You're very welcome! I'm happy to help. Stay safe and healthy! If you have any more questions, feel free to ask anytime.";
        }

        // Farewells
        if (clean.equals("bye") || clean.equals("goodbye") || clean.equals("see you")
                || clean.equals("take care") || clean.equals("bye bye")) {
            return "Goodbye! Take good care of your health. Reconnect whenever you need clinical guidance.";
        }

        // Identity / Meta queries
        if (clean.equals("who are you") || clean.equals("what is your name")
                || clean.equals("what can you do") || clean.equals("help")) {
            return "I am **Neura – AI That Cares**, your compassionate healthcare assistant.\n\nI can help you with:\n"
                    + "- 🩺 **Symptoms & Diseases:** Explanation of common health conditions\n"
                    + "- 💊 **Medicines Information:** Uses, side effects, and precautions\n"
                    + "- 🧪 **Lab Tests & Reports:** CBC, HbA1c, LFT, KFT guidelines\n"
                    + "- 🥗 **Diet & Nutrition:** Specialized diet recommendations\n"
                    + "- 🏥 **Hospital Services:** OPD timings, appointments, and departments";
        }

        return null;
    }

    // ========== SECURITY METHODS ==========

    /**
     * Sliding-window rate limiter: allows RATE_LIMIT_MAX_REQUESTS per RATE_LIMIT_WINDOW_MS per user.
     */
    private boolean checkRateLimit(String username) {
        long now = System.currentTimeMillis();
        Queue<Long> timestamps = rateLimiterMap.computeIfAbsent(username, k -> new ConcurrentLinkedQueue<>());

        // Remove timestamps outside the current window
        while (!timestamps.isEmpty() && (now - timestamps.peek()) > RATE_LIMIT_WINDOW_MS) {
            timestamps.poll();
        }

        if (timestamps.size() >= RATE_LIMIT_MAX_REQUESTS) {
            return false;
        }

        timestamps.add(now);
        return true;
    }

    /**
     * Sanitizes user input by stripping HTML tags, script content, and control characters.
     */
    private String sanitizeInput(String input) {
        if (input == null) return "";

        // Remove HTML/XML tags
        String sanitized = input.replaceAll("<[^>]*>", "");

        // Remove common script injection patterns
        sanitized = sanitized.replaceAll("(?i)(javascript|onerror|onload|onclick|eval\\(|alert\\()", "");

        // Remove control characters (except newlines and tabs)
        sanitized = sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");

        return sanitized.trim();
    }

    /**
     * Detects common prompt injection patterns that attempt to override system instructions.
     */
    private boolean isPromptInjection(String input) {
        String lower = input.toLowerCase();
        return lower.contains("ignore all previous instructions")
                || lower.contains("ignore above instructions")
                || lower.contains("forget your instructions")
                || lower.contains("disregard your system prompt")
                || lower.contains("override your rules")
                || lower.contains("you are now")
                || lower.contains("new persona")
                || lower.contains("act as a different")
                || lower.contains("pretend you are not")
                || lower.contains("system prompt:")
                || lower.contains("admin override");
    }

    private boolean isEmergencyQuery(String input) {
        return input.contains("chest pain") || input.contains("heart attack") 
                || input.contains("breathing difficulty") || input.contains("cannot breathe") 
                || input.contains("short of breath") || input.contains("stroke") 
                || input.contains("unconscious") || input.contains("severe bleeding") 
                || input.contains("suicidal") || input.contains("allergic reaction");
    }

}

