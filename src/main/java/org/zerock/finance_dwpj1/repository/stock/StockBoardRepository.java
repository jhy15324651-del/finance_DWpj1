package org.zerock.finance_dwpj1.repository.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.finance_dwpj1.entity.stock.StockBoard;

import java.time.LocalDateTime;
import java.util.List;

public interface StockBoardRepository extends JpaRepository<StockBoard, Long> {

    // 특정 종목(ticker) 게시판 목록
    Page<StockBoard> findByTickerOrderByIdDesc(String ticker, Pageable pageable);


    // 🔥 로그인한 사용자가 작성한 모든 종목토론 게시글
    List<StockBoard> findByWriter(String writer);


    //개추 많은 글 찾기
    @Query("""
    SELECT b
    FROM StockBoard b
    WHERE b.ticker = :ticker
      AND b.regDate >= :since
    ORDER BY b.recommend DESC
    """)
    List<StockBoard> findTopRecommend(
            @Param("ticker") String ticker,
            @Param("since") LocalDateTime since,
            Pageable pageable
    );


    //댓글 막 달리는 글 찾기
    @Query("""
        SELECT b
        FROM StockBoard b
        LEFT JOIN StockComment c ON c.board.id = b.id
        WHERE b.ticker = :ticker
          AND b.regDate >= :since
        GROUP BY b.id
        ORDER BY COUNT(c.id) DESC
        """)
    List<StockBoard> findTopComment(
            @Param("ticker") String ticker,
            @Param("since") LocalDateTime since,
            Pageable pageable
    );


    //검색
    @Query("""
    SELECT b
    FROM StockBoard b
    WHERE b.ticker = :ticker
    AND (
            (:type = 't'  AND b.title   LIKE %:keyword%)
        OR (:type = 'c'  AND b.content LIKE %:keyword%)
        OR (:type = 'w'  AND b.writer  LIKE %:keyword%)
        OR (:type = 'tc' AND (b.title LIKE %:keyword% OR b.content LIKE %:keyword%))
    )
    ORDER BY b.id DESC
    """)
    Page<StockBoard> search(
            String ticker,
            String type,
            String keyword,
            Pageable pageable
    );

}