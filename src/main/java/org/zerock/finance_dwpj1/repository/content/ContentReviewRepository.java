package org.zerock.finance_dwpj1.repository.content;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zerock.finance_dwpj1.entity.content.ContentReview;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ContentReview Repository
 * - DB에 직접 접근하는 계층
 * - Spring Data JPA의 메서드 이름 기반 쿼리 자동 생성 활용
 * - ⚡ Specification 사용 가능하도록 JpaSpecificationExecutor 추가됨
 */
@Repository
public interface ContentReviewRepository
        extends JpaRepository<ContentReview, Long>,
        JpaSpecificationExecutor<ContentReview> {   // ← 여기 추가됨!!

    // ---------------------------------------------------------
    // 🔥 1) 홈 화면용 최신/인기 목록
    // ---------------------------------------------------------

    // 최신콘텐츠
    List<ContentReview> findTop8ByIsDeletedFalseOrderByCreatedDateDesc();

    // 누적 인기 콘텐츠(유지)
    List<ContentReview> findTop5ByIsDeletedFalseOrderByViewCountDesc();


    // 이 달의 콘텐츠(신규)
    List<ContentReview>
    findTop5ByIsDeletedFalseAndViewMonthOrderByViewCountMonthDesc(String viewMonth);



    // ---------------------------------------------------------
    // 🔥 2) 타입 기반 조회
    // ---------------------------------------------------------

    List<ContentReview> findByTypeAndIsDeletedFalseOrderByCreatedDateDesc(String type);


    // ---------------------------------------------------------
    // 🔥 3) 상세 조회 (삭제 제외)
    // ---------------------------------------------------------

    Optional<ContentReview> findByIdAndIsDeletedFalse(Long id);


    // ---------------------------------------------------------
    // 🔥 4) 전체 게시글 수
    // ---------------------------------------------------------

    long countByIsDeletedFalse();


    // ---------------------------------------------------------
    // 🔥 5) 전체 글 페이징
    // ---------------------------------------------------------

    Page<ContentReview> findByIsDeletedFalse(Pageable pageable);


    // ---------------------------------------------------------
    // 🔥 6) 해시태그 검색(단일)
    // ---------------------------------------------------------

    List<ContentReview> findByHashtagsContainingAndIsDeletedFalseOrderByCreatedDateDesc(String hashtag);

    Page<ContentReview> findByHashtagsContainingAndIsDeletedFalse(String hashtag, Pageable pageable);

    int countByHashtagsContainingAndIsDeletedFalse(String hashtag);


    // ---------------------------------------------------------
    // 🔥 7) 제목 / 내용 / 작성자 검색기능
    // ---------------------------------------------------------

    //제목
    Page<ContentReview> findByTitleContainingAndIsDeletedFalse(String keyword, Pageable pageable);

    //내용
    Page<ContentReview> findByContentContainingAndIsDeletedFalse(String keyword, Pageable pageable);

    //작성자
    Page<ContentReview> findByWriterContainingAndIsDeletedFalse(String writer, Pageable pageable);


    // ---------------------------------------------------------
    // 🔥 8) 특정 해시태그 내 검색 (단일 태그 + 제목/내용)
    // ---------------------------------------------------------

    Page<ContentReview> findByHashtagsContainingAndTitleContainingAndIsDeletedFalse(
            String tag, String keyword, Pageable pageable);

    Page<ContentReview> findByHashtagsContainingAndContentContainingAndIsDeletedFalse(
            String tag, String keyword, Pageable pageable);


    // ---------------------------------------------------------
    // 🔥 9) 내가 작성한 글 목록 불러오기
    // ---------------------------------------------------------
    List<ContentReview> findByWriter(String writer);
    List<ContentReview> findByWriterAndIsDeletedFalse(String writer);

    // ---------------------------------------------------------
    // 🔥 10) 7일 지난 soft-delete 글 찾는 쿼리
    // ---------------------------------------------------------
    List<ContentReview> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime time);

    @Query("""
    SELECT c
    FROM ContentReview c
    WHERE c.isDeleted = false
      AND c.viewMonth = :viewMonth
      AND c.type = :type
    ORDER BY c.viewCountMonth DESC
""")
    List<ContentReview> findMonthlyTopByType(
            @Param("viewMonth") String viewMonth,
            @Param("type") String type,
            Pageable pageable
    );

    // ---------------------------------------------------------
    // 🔥 11) 게시물 가중치 옵션
    // ---------------------------------------------------------
    @Query("""
    SELECT c
    FROM ContentReview c
    WHERE c.isDeleted = false
      AND c.type = 'review'
    ORDER BY
      (
        (c.viewCountMonth * 2)
        + (c.viewCount * 0.1)
        + CASE
            WHEN c.createdDate >= CURRENT_TIMESTAMP - 7 DAY THEN 20
            WHEN c.createdDate >= CURRENT_TIMESTAMP - 30 DAY THEN 10
            ELSE 0
          END
      ) DESC
    """)
    Page<ContentReview> findRecommendationCandidates(Pageable pageable);

    }




