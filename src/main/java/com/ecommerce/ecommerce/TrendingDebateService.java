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
    private final ReferenceLookupService referenceLookupService;

    public TrendingDebateService(TeaRoomService teaRoomService,
                                 OpenAiReplyService openAiReplyService,
                                 TrendingTopicProvider topicProvider,
                                 ReferenceLookupService referenceLookupService) {
        this.teaRoomService = teaRoomService;
        this.openAiReplyService = openAiReplyService;
        this.topicProvider = topicProvider;
        this.referenceLookupService = referenceLookupService;
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
                    "Coffeehouse Auto Editor",
                    topic.title() + "\n" + TITLE_TIME_FORMAT.format(Instant.now()),
                    topic.url(),
                    topic.imageUrl()
            );
            createdPostIds.add(saved.getId());
            generateDebateRepliesAsync(saved.getId(), topic.title(), topic.url());
        }
        return createdPostIds;
    }

    @Async
    public void generateDebateRepliesAsync(String postId, String topic, String sourceUrl) {
        List<TeaRoomReply> replies = new ArrayList<>();
        List<ReferenceLookupService.Reference> references = referenceLookupService.findReferences(topic, sourceUrl);
        String referenceText = referenceLookupService.formatReferences(references);
        String context = "News title: " + topic + "\n" + referenceText;

        replies.add(make(postId, "Summary", "Brief overview", openAiReplyService.generate(
                "Summary",
                "Summarize the news in plain English. Use 2-3 concise sentences. Do not add jokes or personal opinion.",
                context)));
        replies.add(make(postId, "Knowledge", "Background and references", openAiReplyService.generate(
                "Knowledge",
                "Explain the key background knowledge in a readable way. Use the provided reliable article links as context, but do not invent citations or URLs. Do not write a References section.",
                context) + "\n\n" + referenceText));
        replies.add(make(postId, "Explanation", "Why it matters", openAiReplyService.generate(
                "Explanation",
                "Explain why this matters, who is affected, and the main trade-off. Use clear cause-and-effect reasoning in 3-4 concise sentences.",
                context)));
        replies.add(make(postId, "Conclusion", "Bottom line", openAiReplyService.generate(
                "Conclusion",
                "Give a balanced conclusion. State the most important takeaway and one open question to watch next. Use 2-3 concise sentences.",
                context)));
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
