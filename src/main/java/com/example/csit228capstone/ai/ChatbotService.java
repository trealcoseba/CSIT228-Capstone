package com.example.csit228capstone.ai;

import com.example.csit228capstone.util.SupabaseConnectionManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Integrates with the Anthropic Claude API for resident chatbot queries.
 * Emergency keywords auto-draft an incident for admin review.
 */
public class ChatbotService {

    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final List<String> EMERGENCY_KEYWORDS = Arrays.asList(
            "rescue", "fire", "flood", "missing", "earthquake", "typhoon",
            "sunog", "baha", "lindol", "tulong", "emergency", "bagyo"
    );

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private String getApiKey() {
        return System.getenv("ANTHROPIC_API_KEY");
    }

    private String getSystemPrompt() {
        return """
            You are LIGTAS-AI, the official AI assistant of a Philippine Barangay.
            You help residents with:
            - Document requests (Barangay Clearance, Certificate of Indigency, Certificate of Residency, Blotter Report)
            - General inquiries about barangay services, office hours (Mon-Fri, 8AM-5PM), and requirements
            - Emergency guidance (direct them to call the Barangay Hotline or go to the nearest Evacuation Center)
            
            Respond in Filipino or English depending on the resident's language.
            Be helpful, respectful, and concise. If it's an emergency, immediately advise them to
            contact the Barangay Emergency Hotline and inform them help is on the way.
            
            Available evacuation centers: Barangay Hall, Covered Court, Elementary School Gymnasium.
            """;
    }

    public String chat(UUID sessionId, String userMessage) {
        // Check for emergency keywords
        boolean isEmergency = EMERGENCY_KEYWORDS.stream()
                .anyMatch(kw -> userMessage.toLowerCase().contains(kw));

        if (isEmergency) {
            createIncidentDraft(sessionId, userMessage);
        }

        // Save user message
        saveMessage(sessionId, "user", userMessage);

        // Call Claude API
        String response = callClaude(userMessage);

        // Save assistant response
        saveMessage(sessionId, "assistant", response);

        return response;
    }

    private String callClaude(String userMessage) {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            return "AI service is currently unavailable. Please visit the Barangay Hall for assistance.";
        }

        String requestBody = String.format("""
            {
                "model": "claude-sonnet-4-20250514",
                "max_tokens": 1024,
                "system": "%s",
                "messages": [{"role": "user", "content": "%s"}]
            }
            """,
                getSystemPrompt().replace("\"", "\\\"").replace("\n", "\\n"),
                userMessage.replace("\"", "\\\"")
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CLAUDE_API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            // Basic JSON extraction (in production, use a JSON library)
            String body = resp.body();
            int textStart = body.indexOf("\"text\":\"") + 8;
            int textEnd = body.indexOf("\"", textStart);
            return body.substring(textStart, textEnd).replace("\\n", "\n");
        } catch (IOException | InterruptedException e) {
            return "Sorry, I encountered an error. Please try again or visit the Barangay Hall.";
        }
    }

    private void createIncidentDraft(UUID sessionId, String message) {
        String sql = "INSERT INTO incidents (type, title, description, severity, status, location_purok) " +
                     "VALUES ('other'::incident_type, 'AI-Detected Emergency', ?, 'major'::incident_severity, 'reported'::incident_status, 'Unknown')";
        try (Connection c = SupabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "[Auto-generated from chatbot session " + sessionId + "] " + message);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to create incident draft: " + e.getMessage());
        }
    }

    private void saveMessage(UUID sessionId, String role, String content) {
        String sql = "INSERT INTO chatbot_messages (session_id, role, content) VALUES (?,?,?)";
        try (Connection c = SupabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, sessionId);
            ps.setString(2, role);
            ps.setString(3, content);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to save chatbot message: " + e.getMessage());
        }
    }
}
