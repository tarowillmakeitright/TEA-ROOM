package com.ecommerce.ecommerce;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/tea-room")
public class TeaRoomController {
    private final TeaRoomService teaRoomService;

    public TeaRoomController(TeaRoomService teaRoomService) {
        this.teaRoomService = teaRoomService;
    }

    @GetMapping
    public String feed(Model model) {
        model.addAttribute("posts", teaRoomService.allPosts());
        return "tea-room-feed";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model) {
        TeaRoomPost post = teaRoomService.findPost(id);
        if (post == null) return "redirect:/tea-room";
        model.addAttribute("post", post);
        model.addAttribute("replies", teaRoomService.repliesFor(id));
        return "tea-room-detail";
    }

    @GetMapping("/{id}/replies")
    public String repliesFragment(@PathVariable String id, Model model) {
        model.addAttribute("replies", teaRoomService.repliesFor(id));
        return "fragments/tea-room-replies :: replyList";
    }
}
