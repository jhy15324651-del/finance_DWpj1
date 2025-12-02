package org.zerock.finance_dwpj1.repository.insights;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.finance_dwpj1.entity.insights.InsightsTwitter;

import java.util.List;

/**
 * 트위터 인사이트 Repository
 */
public interface InsightsTwitterRepository extends JpaRepository<InsightsTwitter, Long> {

    /**
     * 활성 트윗 전체 조회 (삭제되지 않은 것, 최신순)
     */
    @Query("SELECT t FROM InsightsTwitter t WHERE t.isDeleted = false ORDER BY t.createdAt DESC")
    List<InsightsTwitter> findAllActive();

    /**
     * 출처별 트윗 조회 (삭제되지 않은 것)
     */
    @Query("SELECT t FROM InsightsTwitter t WHERE t.source = :source AND t.isDeleted = false ORDER BY t.createdAt DESC")
    List<InsightsTwitter> findBySource(@Param("source") InsightsTwitter.TwitterSource source);

    /**
     * 핸들로 트윗 조회 (중복 체크용)
     */
    @Query("SELECT t FROM InsightsTwitter t WHERE t.handle = :handle AND t.isDeleted = false")
    List<InsightsTwitter> findByHandle(@Param("handle") String handle);

    /**
     * 활성 트윗 개수 조회
     */
    @Query("SELECT COUNT(t) FROM InsightsTwitter t WHERE t.isDeleted = false")
    long countActive();

    /**
     * 출처별 트윗 개수 조회
     */
    @Query("SELECT COUNT(t) FROM InsightsTwitter t WHERE t.source = :source AND t.isDeleted = false")
    long countBySource(@Param("source") InsightsTwitter.TwitterSource source);
}