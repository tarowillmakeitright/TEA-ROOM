package com.ecommerce.ecommerce;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
public class TrendingTopicProvider {

    @Value("${news.api.key:}")
    private String newsApiKey;

    @Value("${news.api.country:us}")
    private String country;

    @Value("${news.api.categories:science,technology,business}")
    private String categories;

    private final RestTemplate restTemplate = new RestTemplate();

    public record NewsTopic(String title, String url) {}

    public List<NewsTopic> fetchTopTopics(int max) {
        return fetchTopTopics(max, null);
    }

    public List<NewsTopic> fetchTopTopics(int max, List<String> overrideCategories) {
        String apiKey = newsApiKey == null ? "" : newsApiKey.trim();
        if (apiKey.isBlank()) return List.of();
        try {
            List<String> cats = (overrideCategories != null && !overrideCategories.isEmpty())
                    ? overrideCategories
                    : Arrays.stream(categories.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();

            LinkedHashSet<String> topics = new LinkedHashSet<>();
            List<NewsTopic> result = new ArrayList<>();

            for (String cat : cats) {
                String url = UriComponentsBuilder
                        .fromHttpUrl("https://newsapi.org/v2/top-headlines")
                        .queryParam("country", country)
                        .queryParam("category", cat)
                        .queryParam("pageSize", 10)
                        .queryParam("apiKey", apiKey)
                        .toUriString();
                Map<?, ?> response = restTemplate.getForObject(url, Map.class);
                if (response == null) continue;
                Object articlesObj = response.get("articles");
                if (!(articlesObj instanceof List<?> articles)) continue;

                for (Object a : articles) {
                    if (!(a instanceof Map<?, ?> m)) continue;
                    Object title = m.get("title");
                    if (title == null) continue;
                    String t = normalize(title.toString());
                    if (t.length() < 12) continue;
                    if (!topics.add(t)) continue;
                    Object articleUrl = m.get("url");
                    result.add(new NewsTopic(t, articleUrl == null ? null : articleUrl.toString()));
                    if (result.size() >= max) return result;
                }
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    private String normalize(String s) {
        String t = s.replaceAll("\\s*[-|｜].*$", "").trim();
        t = t.replaceAll("\\s+", " ");
        return t;
    }
}
