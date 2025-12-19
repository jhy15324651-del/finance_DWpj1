package org.zerock.finance_dwpj1.repository.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.finance_dwpj1.entity.stock.StockBoard;
import org.zerock.finance_dwpj1.entity.stock.StockBoardImage;

import java.util.List;

public interface StockBoardImageRepository extends JpaRepository<StockBoardImage, Long> {

    List<StockBoardImage> findByBoard(StockBoard board);
}
