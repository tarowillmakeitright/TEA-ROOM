package com.ecommerce.ecommerce;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class TrendingTopicProvider {

    @Value("${news.api.key:}")
    private String newsApiKey;

    @Value("${news.api.country:us}")
    private String country;

    private final RestTemplate restTemplate = new RestTemplate();

    public record NewsTopic(String title, String url) {}

    public List<NewsTopic> fetchTopTopics(int max) {
        if (newsApiKey == null || newsApiKey.isBlank()) return List.of();
        try {
            String url = "https://newsapi.org/v2/top-headlines?country=" + country + "&pageSize=20&apiKey=" + newsApiKey;
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return List.of();
            Object articlesObj = response.get("articles");
            if (!(articlesObj instanceof List<?> articles)) return List.of();

            LinkedHashSet<String> topics = new LinkedHashSet<>();
            List<NewsTopic> result = new ArrayList<>();
            for (Object a : articles) {
                if (!(a instanceof Map<?, ?> m)) continue;
                Object title = m.get("title");
                if (title == null) continue;
                String t = normalize(title.toString());
                if (t.length() < 12) continue;
                if (!topics.add(t)) continue;
                Object articleUrl = m.get("url");
                result.add(new NewsTopic(t, articleUrl == null ? null : articleUrl.toString()));
                if (result.size() >= max) break;
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
