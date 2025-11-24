package com.example.demo.repository;

import com.example.demo.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 최신 글 8개
    List<Post> findTop8ByOrderByCreatedDateDesc();

    // 많이 본 글 5개
    List<Post> findTop5ByOrderByViewCountDesc();

    // 특정 타입(뉴스레터 등)
    List<Post> findByTypeOrderByCreatedDateDesc(String type);

    List<Post> findByCategoryOrderByCreatedDateDesc(String category);

    int countByCategory(String category);

    List<Post> findByCategory(String category);

    Optional<Post> findById(Long id);

    Page<Post> findAll(Pageable pageable);

    Page<Post> findByCategory(String category, Pageable pageable);

}


