package org.zerock.finance_dwpj1.repository.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.finance_dwpj1.entity.stock.StockGechu;

import java.util.Optional;

public interface StockGechuRepository extends JpaRepository<StockGechu,Long> {

    Optional<StockGechu> findByBoardIdAndUserId(Long boardId, Long userId);
}
