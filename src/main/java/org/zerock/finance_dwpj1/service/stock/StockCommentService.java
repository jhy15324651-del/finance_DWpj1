package org.zerock.finance_dwpj1.service.stock;

import org.zerock.finance_dwpj1.dto.stock.StockCommentDTO;

import java.util.List;

public interface StockCommentService {

    List<StockCommentDTO> getListByBoard(Long boardId);

   Long register(StockCommentDTO dto);

   void modify(StockCommentDTO dto);

   void remove(Long id);

   int getCountByBoard(Long boardId);

}
