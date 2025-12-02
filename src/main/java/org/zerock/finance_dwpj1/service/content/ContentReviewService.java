package org.zerock.finance_dwpj1.service.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.finance_dwpj1.entity.content.ContentReview;
import org.zerock.finance_dwpj1.repository.content.ContentReviewRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ContentReviewService {

    private final ContentReviewRepository repo;


    // ---------------------------------------------------------
    // 🔥 최신/인기 게시글
    // ---------------------------------------------------------

    public List<ContentReview> getLatestContents() {
        return repo.findTop8ByIsDeletedFalseOrderByCreatedDateDesc();
    }

    public List<ContentReview> getPopularContents() {
        return repo.findTop5ByIsDeletedFalseOrderByViewCountDesc();
    }


    // ---------------------------------------------------------
    // 🔥 상세조회 + 조회수 증가
    // ---------------------------------------------------------
    @Transactional
    public ContentReview getContentDetail(Long id) {
        ContentReview content = repo.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
        content.incrementViewCount();
        return repo.save(content);
    }


    // ---------------------------------------------------------
    // 🔥 해시태그 / 제목 / 내용 검색
    // ---------------------------------------------------------
    public List<ContentReview> getContentsByHashtag(String hashtag) {
        return repo.findByHashtagsContainingAndIsDeletedFalseOrderByCreatedDateDesc(hashtag);
    }

    public Page<ContentReview> getPagedContentsByHashtag(String hashtag, Pageable pageable) {
        return repo.findByHashtagsContainingAndIsDeletedFalse(hashtag, pageable);
    }

    public int getCountByHashtag(String hashtag) {
        return repo.countByHashtagsContainingAndIsDeletedFalse(hashtag);
    }

    public Page<ContentReview> searchByTitle(String keyword, Pageable pageable) {
        return repo.findByTitleContainingAndIsDeletedFalse(keyword, pageable);
    }

    public Page<ContentReview> searchByContent(String keyword, Pageable pageable) {
        return repo.findByContentContainingAndIsDeletedFalse(keyword, pageable);
    }

    public Page<ContentReview> searchTitleInTag(String tag, String keyword, Pageable pageable) {
        return repo.findByHashtagsContainingAndTitleContainingAndIsDeletedFalse(tag, keyword, pageable);
    }

    public Page<ContentReview> searchContentInTag(String tag, String keyword, Pageable pageable) {
        return repo.findByHashtagsContainingAndContentContainingAndIsDeletedFalse(tag, keyword, pageable);
    }


    // ---------------------------------------------------------
    // 🔥 다중 해시태그 검색 (AND)
    // ---------------------------------------------------------
    public Page<ContentReview> searchByMultipleTags(Set<String> tags, Pageable pageable) {

        Specification<ContentReview> spec =
                (root, query, cb) -> cb.isFalse(root.get("isDeleted"));

        if (tags == null || tags.isEmpty()) {
            return repo.findAll(spec, pageable);
        }

        for (String tag : tags) {
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("hashtags"), "%" + tag + "%")
            );
        }

        return repo.findAll(spec, pageable);
    }


    // ---------------------------------------------------------
    // 🔥 전체 글 수
    // ---------------------------------------------------------
    public long getTotalCount() {
        return repo.countByIsDeletedFalse();
    }


    // ---------------------------------------------------------
    // 🔥 저장
    // ---------------------------------------------------------
    @Transactional
    public ContentReview saveContent(ContentReview content) {
        return repo.save(content);
    }


    // ---------------------------------------------------------
    // 🔥 수정 기능 (핵심 추가)
    // ---------------------------------------------------------
    @Transactional
    public void updateContent(Long id, String title, String content,
                              String hashtags, org.springframework.web.multipart.MultipartFile image)
            throws IOException {

        ContentReview post = repo.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 값 변경
        post.setTitle(title);
        post.setContent(content);
        post.setHashtags(hashtags);

        // 이미지 업로드 처리
        if (image != null && !image.isEmpty()) {

            String uploadDir = "src/main/resources/static/upload/";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);

            Files.write(filePath, image.getBytes());
            post.setImgUrl("/upload/" + fileName);

            log.info("이미지 변경 완료: {}", post.getImgUrl());
        }

        repo.save(post);
    }


    // ---------------------------------------------------------
    // 🔥 삭제 (Soft Delete)
    // ---------------------------------------------------------
    @Transactional
    public void deleteContent(Long id) {
        ContentReview content = repo.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
        content.softDelete();
        repo.save(content);
    }
}
