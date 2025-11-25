package org.zerock.finance_dwpj1.service.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.finance_dwpj1.entity.content.ContentReview;
import org.zerock.finance_dwpj1.repository.content.ContentReviewRepository;

import java.util.List;

/**
 * ContentReview Service
 * 콘텐츠 리뷰 비즈니스 로직 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentReviewService {

    private final ContentReviewRepository contentReviewRepository;

    /**
     * 최신 콘텐츠 8개 조회
     */
    public List<ContentReview> getLatestContents() {
        log.debug("최신 콘텐츠 8개 조회");
        return contentReviewRepository.findTop8ByIsDeletedFalseOrderByCreatedDateDesc();
    }

    /**
     * 인기 콘텐츠 5개 조회
     */
    public List<ContentReview> getPopularContents() {
        log.debug("인기 콘텐츠 5개 조회");
        return contentReviewRepository.findTop5ByIsDeletedFalseOrderByViewCountDesc();
    }

    /**
     * 콘텐츠 상세 조회 및 조회수 증가
     */
    @Transactional
    public ContentReview getContentDetail(Long id) {
        log.debug("콘텐츠 상세 조회: id={}", id);

        ContentReview content = contentReviewRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("콘텐츠를 찾을 수 없습니다: " + id));

        // 조회수 증가
        content.incrementViewCount();
        contentReviewRepository.save(content);

        return content;
    }

    /**
     * 카테고리별 콘텐츠 조회
     */
    public List<ContentReview> getContentsByCategory(String category) {
        log.debug("카테고리별 콘텐츠 조회: category={}", category);
        return contentReviewRepository.findByCategoryAndIsDeletedFalseOrderByCreatedDateDesc(category);
    }

    /**
     * 카테고리별 콘텐츠 수 조회
     */
    public int getCountByCategory(String category) {
        log.debug("카테고리별 콘텐츠 수 조회: category={}", category);
        return contentReviewRepository.countByCategoryAndIsDeletedFalse(category);
    }

    /**
     * 전체 콘텐츠 수 조회
     */
    public long getTotalCount() {
        log.debug("전체 콘텐츠 수 조회");
        return contentReviewRepository.countByIsDeletedFalse();
    }

    /**
     * 타입별 콘텐츠 조회
     */
    public List<ContentReview> getContentsByType(String type) {
        log.debug("타입별 콘텐츠 조회: type={}", type);
        return contentReviewRepository.findByTypeAndIsDeletedFalseOrderByCreatedDateDesc(type);
    }

    /**
     * 콘텐츠 저장
     */
    @Transactional
    public ContentReview saveContent(ContentReview content) {
        log.debug("콘텐츠 저장: title={}", content.getTitle());
        return contentReviewRepository.save(content);
    }

    /**
     * 콘텐츠 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteContent(Long id) {
        log.debug("콘텐츠 삭제: id={}", id);

        ContentReview content = contentReviewRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("콘텐츠를 찾을 수 없습니다: " + id));

        content.softDelete();
        contentReviewRepository.save(content);
    }
}
