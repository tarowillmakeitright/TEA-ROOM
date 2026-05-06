package com.ecommerce.ecommerce;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ReferenceLookupService {
    private static final Set<String> STOP_WORDS = Set.of(
            "about", "after", "again", "against", "being", "between", "could", "during", "first",
            "from", "have", "into", "more", "over", "says", "than", "that", "their", "there",
            "this", "through", "under", "what", "when", "where", "which", "while", "with",
            "would", "will", "news", "latest", "report", "reports", "update", "updates",
            "massive", "major", "minor", "large", "small", "huge", "watch", "video", "photo",
            "photos", "shows", "show", "reveals", "revealed"
    );

    @Value("${news.api.key:}")
    private String newsApiKey;

    @Value("${news.reference.domains:reuters.com,apnews.com,bbc.com,npr.org,pbs.org,scientificamerican.com,nature.com,science.org,who.int,nih.gov,nasa.gov,smithsonianmag.com,nationalgeographic.com,livescience.com,space.com,theguardian.com}")
    private String referenceDomains;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<Reference> findReferences(String topic, String sourceUrl) {
        LinkedHashSet<Reference> references = new LinkedHashSet<>();
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            references.add(new Reference("Original news source", sourceUrl.trim()));
        }

        references.addAll(searchReliableArticles(extractKeywords(topic), sourceUrl));

        return new ArrayList<>(references);
    }

    public String formatReferences(List<Reference> references) {
        if (references == null || references.isEmpty()) {
            return "References\n- No external reference links were found.";
        }

        StringBuilder sb = new StringBuilder("References");
        for (Reference reference : references) {
            sb.append("\n- ")
                    .append(reference.title())
                    .append(": ")
                    .append(reference.url());
        }
        return sb.toString();
    }

    private List<String> extractKeywords(String topic) {
        if (topic == null || topic.isBlank()) {
            return List.of();
        }

        String cleaned = topic.replaceAll("[^A-Za-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
        List<String> terms = Arrays.stream(cleaned.split("\\s+"))
                .map(String::trim)
                .filter(word -> word.length() >= 4)
                .filter(word -> !STOP_WORDS.contains(word.toLowerCase(Locale.ROOT)))
                .limit(6)
                .toList();

        return terms;
    }

    private List<Reference> searchReliableArticles(List<String> keywords, String sourceUrl) {
        String apiKey = newsApiKey == null ? "" : newsApiKey.trim();
        if (apiKey.isBlank() || keywords == null || keywords.isEmpty()) {
            return List.of();
        }

        try {
            String query = keywords.stream().limit(5).reduce((a, b) -> a + " OR " + b).orElse("");
            if (query.isBlank()) {
                return List.of();
            }

            String url = UriComponentsBuilder
                    .fromHttpUrl("https://newsapi.org/v2/everything")
                    .queryParam("q", query)
                    .queryParam("searchIn", "title,description")
                    .queryParam("domains", referenceDomains)
                    .queryParam("language", "en")
                    .queryParam("sortBy", "relevancy")
                    .queryParam("pageSize", 12)
                    .queryParam("apiKey", newsApiKey.trim())
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "Coffeehouse/1.0 related reference lookup");
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            if (response.getBody() == null) {
                return List.of();
            }

            Object articlesObj = response.getBody().get("articles");
            if (!(articlesObj instanceof List<?> articles)) {
                return List.of();
            }

            LinkedHashSet<Reference> references = new LinkedHashSet<>();
            for (Object article : articles) {
                if (!(article instanceof Map<?, ?> item)) {
                    continue;
                }
                Object titleObj = item.get("title");
                Object urlObj = item.get("url");
                if (titleObj == null || urlObj == null) {
                    continue;
                }
                String title = titleObj.toString().trim();
                String articleUrl = urlObj.toString().trim();
                if (title.isBlank() || articleUrl.isBlank() || articleUrl.equals(sourceUrl)) {
                    continue;
                }
                if (!isRelevant(item, title, keywords)) {
                    continue;
                }
                references.add(new Reference("Related source - " + readableTitle(item, title), articleUrl));
                if (references.size() >= 4) {
                    break;
                }
            }
            return new ArrayList<>(references);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String readableTitle(Map<?, ?> item, String title) {
        Object sourceObj = item.get("source");
        if (sourceObj instanceof Map<?, ?> source) {
            Object name = source.get("name");
            if (name != null && !name.toString().isBlank()) {
                return name + ": " + title;
            }
        }
        return title;
    }

    private boolean isRelevant(Map<?, ?> item, String title, List<String> keywords) {
        Object descriptionObj = item.get("description");
        String haystack = (title + " " + (descriptionObj == null ? "" : descriptionObj)).toLowerCase(Locale.ROOT);
        long matches = keywords.stream()
                .map(word -> word.toLowerCase(Locale.ROOT))
                .filter(haystack::contains)
                .count();
        return matches >= Math.min(2, keywords.size());
    }

    public record Reference(String title, String url) {}
}
