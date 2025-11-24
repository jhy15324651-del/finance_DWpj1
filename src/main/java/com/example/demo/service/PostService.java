package com.example.demo.service;

import com.example.demo.model.Post;
import com.example.demo.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    public List<Post> getLatestPosts() {
        return postRepository.findTop8ByOrderByCreatedDateDesc();
    }

    public List<Post> getPopularPosts() {
        return postRepository.findTop5ByOrderByViewCountDesc();
    }

    public Post getPostDetail(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다 : " + id));

        // 조회수 증가
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);

        return post;
    }

    public List<Post> getPostsByCategory(String category) {
        return postRepository.findByCategoryOrderByCreatedDateDesc(category);
    }

    public int getCountByCategory(String category) {
        return postRepository.countByCategory(category);
    }


}
