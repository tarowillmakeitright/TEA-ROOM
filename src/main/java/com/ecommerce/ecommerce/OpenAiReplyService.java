package com.ecommerce.ecommerce;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OpenAiReplyService {
    private static final Logger log = LoggerFactory.getLogger(OpenAiReplyService.class);

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generate(String agentName, String stance, String postContent) {
        String token = apiKey == null ? "" : apiKey.trim();
        String selectedModel = model == null || model.isBlank() ? "gpt-4o-mini" : model.trim();
        if (token.isBlank()) {
            return fallback(agentName);
        }
        try {
            String system = "You write the " + agentName + " section for Coffeehouse. Your task is: " + stance + ". " +
                    "Reply in English only. Be factual, clear, and concise. Avoid jokes, roleplay, and generic filler.";

            Map<String, Object> body = Map.of(
                    "model", selectedModel,
                    "messages", List.of(
                            Map.of("role", "system", "content", system),
                            Map.of("role", "user", "content", "User post: " + postContent)
                    ),
                    "temperature", 0.8
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);

            ResponseEntity<Map> resp = restTemplate.postForEntity("https://api.openai.com/v1/chat/completions", req, Map.class);
            if (resp.getBody() == null) return fallback(agentName);
            Object choicesObj = resp.getBody().get("choices");
            if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) return fallback(agentName);
            Object first = choices.get(0);
            if (!(first instanceof Map<?, ?> fm)) return fallback(agentName);
            Object msgObj = fm.get("message");
            if (!(msgObj instanceof Map<?, ?> mm)) return fallback(agentName);
            Object content = mm.get("content");
            return content == null ? fallback(agentName) : content.toString().trim();
        } catch (HttpStatusCodeException e) {
            log.warn("OpenAI request failed with status {}", e.getStatusCode());
            return fallback(agentName);
        } catch (Exception e) {
            log.warn("OpenAI request failed: {}", e.getClass().getSimpleName());
            return fallback(agentName);
        }
    }

    private String fallback(String agentName) {
        if ("Knowledge".equals(agentName)) {
            return "Background details are not available because the AI service could not respond. Check the original news source and trusted article references for context.";
        }
        return "[" + agentName + "] The AI service could not respond, so this section is temporarily unavailable.";
    }
}
