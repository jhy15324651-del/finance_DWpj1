package org.zerock.finance_dwpj1.repository.insights;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.finance_dwpj1.entity.insights.InsightsComment;

import java.util.List;

/**
 * 댓글 Repository
 */
public interface InsightsCommentRepository extends JpaRepository<InsightsComment, Long> {

    /**
     * 특정 뉴스의 댓글 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT c FROM InsightsComment c WHERE c.news.id = :newsId AND c.isDeleted = false ORDER BY c.createdAt DESC")
    List<InsightsComment> findByNewsId(@Param("newsId") Long newsId);

    /**
     * 특정 뉴스의 댓글 페이징 조회
     */
    @Query("SELECT c FROM InsightsComment c WHERE c.news.id = :newsId AND c.isDeleted = false ORDER BY c.createdAt DESC")
    Page<InsightsComment> findByNewsId(@Param("newsId") Long newsId, Pageable pageable);

    /**
     * 특정 뉴스의 댓글 개수 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT COUNT(c) FROM InsightsComment c WHERE c.news.id = :newsId AND c.isDeleted = false")
    Long countByNewsId(@Param("newsId") Long newsId);
}
