package org.zerock.finance_dwpj1.repository.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.finance_dwpj1.entity.stock.StockBoard;

public interface StockBoardRepository extends JpaRepository<StockBoard, Long> {

    // 특정 종목(ticker) 게시판 목록
    Page<StockBoard> findByTickerOrderByIdDesc(String ticker, Pageable pageable);


}