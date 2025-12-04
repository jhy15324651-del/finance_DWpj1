package org.zerock.finance_dwpj1.repository.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.finance_dwpj1.entity.stock.StockBichu;

import java.util.Optional;

public interface StockBichuRepository extends JpaRepository<StockBichu,Long> {


    Optional<StockBichu> findByBoardIdAndUserId(Long BoardId, Long UserId);
}
