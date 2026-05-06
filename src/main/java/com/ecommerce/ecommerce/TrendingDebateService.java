package com.ecommerce.ecommerce;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TrendingDebateService {
    private static final DateTimeFormatter TITLE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z").withZone(ZoneId.of("Asia/Tokyo"));

    private final TeaRoomService teaRoomService;
    private final OpenAiReplyService openAiReplyService;
    private final TrendingTopicProvider topicProvider;

    public TrendingDebateService(TeaRoomService teaRoomService,
                                 OpenAiReplyService openAiReplyService,
                                 TrendingTopicProvider topicProvider) {
        this.teaRoomService = teaRoomService;
        this.openAiReplyService = openAiReplyService;
        this.topicProvider = topicProvider;
    }

    public List<String> runOnceNow() {
        List<TrendingTopicProvider.NewsTopic> topics = topicProvider.fetchTopTopics(5);
        if (topics.isEmpty()) {
            topics = List.of(
                    new TrendingTopicProvider.NewsTopic("Inflation policy and household support trade-offs", null),
                    new TrendingTopicProvider.NewsTopic("How far social media regulation should go during elections", null),
                    new TrendingTopicProvider.NewsTopic("Balancing renewable energy growth with grid stability", null)
            );
        }

        List<String> createdPostIds = new ArrayList<>();
        for (TrendingTopicProvider.NewsTopic topic : topics) {
            TeaRoomPost saved = teaRoomService.createPostWithoutDefaultReplies(
                    "TEA ROOM Auto Moderator",
                    topic.title() + "\n" + TITLE_TIME_FORMAT.format(Instant.now()),
                    topic.url()
            );
            createdPostIds.add(saved.getId());
            generateDebateRepliesAsync(saved.getId(), topic.title());
        }
        return createdPostIds;
    }

    @Async
    public void generateDebateRepliesAsync(String postId, String topic) {
        List<TeaRoomReply> replies = new ArrayList<>();
        replies.add(make(postId, "Professor Logic", "Premise breakdown", openAiReplyService.generate("Professor Logic", "Break down assumptions, causality, and logical leaps. Smart, concise, slightly sharp.", "News debate: " + topic)));
        replies.add(make(postId, "Data Samurai", "Numbers and counter-evidence", openAiReplyService.generate("Data Samurai", "Cut through the story with numbers, comparisons, and falsifiable claims. Dislike unsupported opinions.", "News debate: " + topic)));
        replies.add(make(postId, "Contrarian Clown", "Entertaining contrarian", openAiReplyService.generate("Contrarian Clown", "Take a contrarian angle and expose blind spots in a funny way, while keeping the logic intact.", "News debate: " + topic)));
        replies.add(make(postId, "Host Matcha", "Debate synthesis", openAiReplyService.generate("Host Matcha", "Organize the debate for viewers and identify the most important question left open.", "Summarize this news debate: " + topic)));
        replies.add(make(postId, "Joke Shogun", "Jokes only", openAiReplyService.generate("Joke Shogun", "Jokes only. Make a short joke related to the news topic. No analysis, no advice, no conclusion. Keep it witty, harmless, and non-abusive.", "Joke material: " + topic)));
        teaRoomService.addReplies(replies);
    }

    private TeaRoomReply make(String postId, String agent, String stance, String content) {
        TeaRoomReply r = new TeaRoomReply();
        r.setId(UUID.randomUUID().toString());
        r.setPostId(postId);
        r.setAgentName(agent);
        r.setStance(stance);
        r.setContent(content);
        return r;
    }
}
