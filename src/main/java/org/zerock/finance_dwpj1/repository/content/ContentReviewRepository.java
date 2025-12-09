package org.zerock.finance_dwpj1.repository.content;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.zerock.finance_dwpj1.entity.content.ContentReview;

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

    List<ContentReview> findTop8ByIsDeletedFalseOrderByCreatedDateDesc();

    List<ContentReview> findTop5ByIsDeletedFalseOrderByViewCountDesc();


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
    // 🔥 7) 제목 / 내용 검색
    // ---------------------------------------------------------

    Page<ContentReview> findByTitleContainingAndIsDeletedFalse(String keyword, Pageable pageable);

    Page<ContentReview> findByContentContainingAndIsDeletedFalse(String keyword, Pageable pageable);


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


}
