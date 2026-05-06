package com.ecommerce.ecommerce;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TeaRoomService {
    private final TeaRoomPostRepository postRepository;
    private final TeaRoomReplyRepository replyRepository;

    public TeaRoomService(TeaRoomPostRepository postRepository, TeaRoomReplyRepository replyRepository) {
        this.postRepository = postRepository;
        this.replyRepository = replyRepository;
    }

    public List<TeaRoomPost> allPosts() {
        return postRepository.findAll().stream().sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).toList();
    }

    public TeaRoomPost createPostWithoutDefaultReplies(String authorName, String content) {
        return createPostWithoutDefaultReplies(authorName, content, null);
    }

    public TeaRoomPost createPostWithoutDefaultReplies(String authorName, String content, String sourceUrl) {
        TeaRoomPost p = new TeaRoomPost();
        p.setId(UUID.randomUUID().toString());
        p.setAuthorName((authorName == null || authorName.isBlank()) ? "Anonymous" : authorName.trim());
        p.setContent(content.trim());
        p.setSourceUrl((sourceUrl == null || sourceUrl.isBlank()) ? null : sourceUrl.trim());
        return postRepository.save(p);
    }

    public TeaRoomPost findPost(String id) {
        return postRepository.findById(id).orElse(null);
    }

    public List<TeaRoomReply> repliesFor(String postId) {
        return replyRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    public void addReplies(List<TeaRoomReply> newReplies) {
        replyRepository.saveAll(newReplies);
    }
}
