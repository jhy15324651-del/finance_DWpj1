package com.example.demo;

import com.example.demo.model.Post;
import com.example.demo.repository.PostRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}


    @Bean
    CommandLineRunner init(PostRepository postRepository) {
        return args -> {

            for (int i = 1; i <= 15; i++) {
                Post p = new Post();
                p.setTitle("테스트 글 " + i);
                p.setSummary("요약 내용입니다. (" + i + ")");
                p.setContent("본문 내용입니다. (" + i + ")");
                p.setThumbnailUrl(null);
                p.setViewCount((int)(Math.random() * 100));
                p.setCreatedDate(LocalDateTime.now().minusDays(i));
                p.setCategory("기타");
                p.setType("normal");

                postRepository.save(p);
            }
        };
    }

}
