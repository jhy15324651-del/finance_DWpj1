package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
@Entity
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 2000)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String thumbnailUrl;

    private String imgUrl;  // ★ 이미지 경로 추가

    private int viewCount;

    private LocalDateTime createdDate = LocalDateTime.now();

    private String category;

    private String type;   // normal / newsletter / info 등
}
