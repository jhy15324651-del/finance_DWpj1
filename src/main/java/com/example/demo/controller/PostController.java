package com.example.demo.controller;

import com.example.demo.model.Post;
import com.example.demo.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequiredArgsConstructor
public class PostController {

    private final PostRepository postRepository;

    @GetMapping("/write")
    public String writeForm() {
        return "write";
    }

    @PostMapping("/post/write")
    public String writePost(
            @RequestParam String title,
            @RequestParam String summary,
            @RequestParam String content,
            @RequestParam String category,
            @RequestParam(required = false) MultipartFile image
    ) throws IOException {

        Post post = new Post();
        post.setTitle(title);
        post.setSummary(summary);
        post.setContent(content);
        post.setCategory(category);
        post.setViewCount(0);

        // 이미지 저장
        if (image != null && !image.isEmpty()) {

            String savePath = "src/main/resources/static/upload/";

            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();

            Path path = Paths.get(savePath + fileName);
            Files.write(path, image.getBytes());

            post.setImgUrl("/upload/" + fileName);
        }

        postRepository.save(post);

        return "redirect:/category";
    }
}

