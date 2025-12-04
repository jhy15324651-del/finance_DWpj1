package org.zerock.finance_dwpj1.repository.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.finance_dwpj1.entity.stock.StockComment;
import java.util.List;


public interface StockCommentRepository extends JpaRepository<StockComment, Long> {

    List<StockComment> findByStock_IdOrderByComment_IdAsc(Long id);

    int countByBoardId(Long boardId);
}
