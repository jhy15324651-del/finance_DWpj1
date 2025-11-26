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

import java.util.List;
import java.util.Set;

/**
 * ContentReviewService
 *
 * - 비즈니스 로직 담당
 * - 조회수 증가 + 다중 해시태그 검색 기능 완전 지원
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ContentReviewService {

    private final ContentReviewRepository repo;


    // ---------------------------------------------------------
    // 🔥 홈 화면용 최신 8개, 인기 5개
    // ---------------------------------------------------------

    /** 최신 콘텐츠 8개 */
    public List<ContentReview> getLatestContents() {
        return repo.findTop8ByIsDeletedFalseOrderByCreatedDateDesc();
    }

    /** 인기 콘텐츠 5개 */
    public List<ContentReview> getPopularContents() {
        return repo.findTop5ByIsDeletedFalseOrderByViewCountDesc();
    }


    // ---------------------------------------------------------
    // 🔥 상세페이지 + 조회수 증가
    // ---------------------------------------------------------

    @Transactional
    public ContentReview getContentDetail(Long id) {
        ContentReview content = repo.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        content.incrementViewCount();   // 조회수 증가
        return repo.save(content);
    }


    // ---------------------------------------------------------
    // 🔥 단일 해시태그 기반 조회 (기존 기능)
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


    // ---------------------------------------------------------
    // 🔥 제목 / 내용 검색
    // ---------------------------------------------------------

    public Page<ContentReview> searchByTitle(String keyword, Pageable pageable) {
        return repo.findByTitleContainingAndIsDeletedFalse(keyword, pageable);
    }

    public Page<ContentReview> searchByContent(String keyword, Pageable pageable) {
        return repo.findByContentContainingAndIsDeletedFalse(keyword, pageable);
    }


    // ---------------------------------------------------------
    // 🔥 특정 해시태그 내 검색 (기존 기능)
    // ---------------------------------------------------------

    public Page<ContentReview> searchTitleInTag(String tag, String keyword, Pageable pageable) {
        return repo.findByHashtagsContainingAndTitleContainingAndIsDeletedFalse(tag, keyword, pageable);
    }

    public Page<ContentReview> searchContentInTag(String tag, String keyword, Pageable pageable) {
        return repo.findByHashtagsContainingAndContentContainingAndIsDeletedFalse(tag, keyword, pageable);
    }


    // ---------------------------------------------------------
    // 🔥 다중 해시태그 AND 검색 (새로운 핵심 기능)
    // ---------------------------------------------------------

    /**
     * 입력된 모든 태그가 포함된 게시글만 조회
     * - 순서 무관
     * - "#테슬라 #엔비디아" → 각각 LIKE 검색
     */
    public Page<ContentReview> searchByMultipleTags(Set<String> tags, Pageable pageable) {

        // 기본 조건: isDeleted = false
        Specification<ContentReview> spec =
                (root, query, cb) -> cb.isFalse(root.get("isDeleted"));

        // 태그가 하나도 없으면 전체 검색과 동일
        if (tags == null || tags.isEmpty()) {
            return repo.findAll(spec, pageable);
        }

        // 선택한 태그 개수만큼 계속 AND 조건 추가 (hashtags LIKE %tag%)
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
    // 🔥 저장/삭제
    // ---------------------------------------------------------

    @Transactional
    public ContentReview saveContent(ContentReview content) {
        return repo.save(content);
    }

    @Transactional
    public void deleteContent(Long id) {
        ContentReview content = repo.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
        content.softDelete();
        repo.save(content);
    }
}
