package com.ecommerce.ecommerce;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TeaRoomReplyRepository extends MongoRepository<TeaRoomReply, String> {
    List<TeaRoomReply> findByPostIdOrderByCreatedAtAsc(String postId);
}
