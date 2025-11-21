package org.zerock.finance_dwpj1.repository.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zerock.finance_dwpj1.entity.stock.StockQuoteCache;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface StockQuoteCacheRepository extends JpaRepository<StockQuoteCache, String> {

    /**
     * 캐시가 유효한지 확인 (updatedAt이 특정 시간 이후인 경우)
     */
    Optional<StockQuoteCache> findByTickerAndUpdatedAtAfter(String ticker, LocalDateTime after);
}