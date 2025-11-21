package org.zerock.finance_dwpj1.repository.insights;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.finance_dwpj1.entity.insights.News;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 뉴스 Repository
 */
public interface NewsRepository extends JpaRepository<News, Long> {

    /**
     * URL로 뉴스 조회 (중복 체크용)
     */
    Optional<News> findByUrl(String url);

    /**
     * URL 존재 여부 확인
     */
    boolean existsByUrl(String url);

    /**
     * 데일리 뉴스 조회 (24시간 이내, 삭제되지 않은 것)
     */
    @Query("SELECT n FROM News n WHERE n.status = 'DAILY' AND n.isDeleted = false ORDER BY n.createdAt DESC")
    List<News> findDailyNews();

    /**
     * 데일리 뉴스 페이징 조회
     */
    @Query("SELECT n FROM News n WHERE n.status = 'DAILY' AND n.isDeleted = false ORDER BY n.createdAt DESC")
    Page<News> findDailyNews(Pageable pageable);

    /**
     * 아카이브 뉴스 조회 (24시간 이상 경과, 삭제되지 않은 것)
     */
    @Query("SELECT n FROM News n WHERE n.status = 'ARCHIVE' AND n.isDeleted = false ORDER BY n.createdAt DESC")
    Page<News> findArchiveNews(Pageable pageable);

    /**
     * 금주의 뉴스 (조회수 TOP N)
     */
    @Query("SELECT n FROM News n WHERE n.isDeleted = false AND n.createdAt >= :weekAgo ORDER BY n.viewCount DESC")
    List<News> findTopNewsByViewCount(@Param("weekAgo") LocalDateTime weekAgo, Pageable pageable);

    /**
     * 24시간 이상 경과한 DAILY 상태의 뉴스 조회 (아카이브 처리용)
     */
    @Query("SELECT n FROM News n WHERE n.status = 'DAILY' AND n.createdAt < :twentyFourHoursAgo")
    List<News> findNewsToArchive(@Param("twentyFourHoursAgo") LocalDateTime twentyFourHoursAgo);

    /**
     * 제목으로 검색
     */
    @Query("SELECT n FROM News n WHERE n.isDeleted = false AND n.title LIKE %:keyword% ORDER BY n.createdAt DESC")
    Page<News> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 상태별 뉴스 조회
     */
    @Query("SELECT n FROM News n WHERE n.status = :status AND n.isDeleted = false ORDER BY n.createdAt DESC")
    Page<News> findByStatus(@Param("status") News.NewsStatus status, Pageable pageable);
}
