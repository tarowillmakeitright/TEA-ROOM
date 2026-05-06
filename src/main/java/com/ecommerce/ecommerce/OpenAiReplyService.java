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
        if (apiKey == null || apiKey.isBlank()) {
            return fallback(agentName);
        }
        try {
            String system;
            if (stance != null && stance.contains("Jokes only")) {
                system = "You are " + agentName + " in TEA ROOM. Your role is: " + stance + ". " +
                        "Reply in English with only jokes. Do not analyze, advise, summarize, or explain. " +
                        "Use 1-2 short sentences. Keep it witty and harmless.";
            } else {
                system = "You are " + agentName + " in TEA ROOM. Your perspective is: " + stance + ". " +
                        "Reply in English in 2-3 concise sentences. Be intellectually sharp, concrete, and a little funny. " +
                        "Make one clear argument, name the trade-off, and avoid generic encouragement.";
            }

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", system),
                            Map.of("role", "user", "content", "User post: " + postContent)
                    ),
                    "temperature", 0.8
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
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
        return "[" + agentName + "] That is the pressure point. Challenge one assumption, test it with a number, and do not let vibes drive the bus.";
    }
}
