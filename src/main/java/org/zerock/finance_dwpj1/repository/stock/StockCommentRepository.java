package org.zerock.finance_dwpj1.repository.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.finance_dwpj1.entity.stock.StockComment;
import java.util.List;


public interface StockCommentRepository extends JpaRepository<StockComment, Long> {


    // 특정 게시글(board_id)의 모든 댓글, 대댓글 조회
    @Query("""
        select c from StockComment c
        where c.board.id = :boardId
        order by
            c.parent.id asc nulls first,
            c.depth asc,
            c.id asc
    """)
    List<StockComment> findByBoard_IdOrderByIdAsc(@Param("boardId") Long boardId);


    //특정 게시글(board_id)의 댓글 수
    int countByBoard_Id(Long boardId);

    //작성자(writer)로 댓글을 조회하는 메서드 추가
    List<StockComment> findByWriter(String writer);

}
