package com.example.demo.controller;

import com.example.demo.model.Post;
import com.example.demo.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final PostRepository postRepository;

    // 홈 화면
    @GetMapping("/")
    public String home(Model model) {

        List<Post> latestPosts = postRepository.findTop8ByOrderByCreatedDateDesc();
        List<Post> popularPosts = postRepository.findTop5ByOrderByViewCountDesc();

        model.addAttribute("latestPosts", latestPosts);
        model.addAttribute("popularPosts", popularPosts);

        return "home";
    }

    // 상세 보기
    @GetMapping("/post/{id}")
    public String postDetail(@PathVariable Long id, Model model) {
        Post post = postRepository.findById(id).orElseThrow();
        model.addAttribute("post", post);
        return "post-detail";
    }
}
