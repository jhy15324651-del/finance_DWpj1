package org.zerock.finance_dwpj1.repository.content;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zerock.finance_dwpj1.entity.content.ContentReview;

import java.util.List;
import java.util.Optional;

/**
 * ContentReview Repository
 * 콘텐츠 리뷰 데이터 접근 인터페이스
 */
@Repository
public interface ContentReviewRepository extends JpaRepository<ContentReview, Long> {

    /**
     * 최신 콘텐츠 N개 조회
     */
    List<ContentReview> findTop8ByIsDeletedFalseOrderByCreatedDateDesc();

    /**
     * 인기 콘텐츠 N개 조회 (조회수 높은 순)
     */
    List<ContentReview> findTop5ByIsDeletedFalseOrderByViewCountDesc();

    /**
     * 특정 타입별 콘텐츠 조회 (뉴스레터, 정보 등)
     */
    List<ContentReview> findByTypeAndIsDeletedFalseOrderByCreatedDateDesc(String type);

    /**
     * 카테고리별 콘텐츠 조회 (최신순)
     */
    List<ContentReview> findByCategoryAndIsDeletedFalseOrderByCreatedDateDesc(String category);

    /**
     * 카테고리별 콘텐츠 수 조회
     */
    int countByCategoryAndIsDeletedFalse(String category);

    /**
     * 전체 콘텐츠 수 조회 (삭제되지 않은 것만)
     */
    long countByIsDeletedFalse();

    /**
     * 카테고리별 콘텐츠 목록 조회
     */
    List<ContentReview> findByCategoryAndIsDeletedFalse(String category);

    /**
     * ID로 콘텐츠 조회 (삭제되지 않은 것만)
     */
    Optional<ContentReview> findByIdAndIsDeletedFalse(Long id);

    /**
     * 전체 페이징 조회 (삭제되지 않은 것만)
     */
    Page<ContentReview> findByIsDeletedFalse(Pageable pageable);

    /**
     * 카테고리별 페이징 조회 (삭제되지 않은 것만)
     */
    Page<ContentReview> findByCategoryAndIsDeletedFalse(String category, Pageable pageable);
}
