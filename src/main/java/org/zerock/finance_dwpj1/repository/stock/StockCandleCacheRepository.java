package org.zerock.finance_dwpj1.repository.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zerock.finance_dwpj1.entity.stock.StockCandleCache;
import org.zerock.finance_dwpj1.entity.stock.StockCandleCacheId;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockCandleCacheRepository extends JpaRepository<StockCandleCache, StockCandleCacheId> {

    /**
     * 특정 종목의 캔들 데이터 조회 (최신순, 개수 제한)
     */
    List<StockCandleCache> findByTickerAndTimeframeOrderByDateDesc(String ticker, String timeframe);

    /**
     * 캐시가 유효한 캔들 데이터 조회
     */
    @Query("SELECT c FROM StockCandleCache c WHERE c.ticker = :ticker AND c.timeframe = :timeframe AND c.updatedAt > :after ORDER BY c.date ASC")
    List<StockCandleCache> findValidCache(@Param("ticker") String ticker,
                                          @Param("timeframe") String timeframe,
                                          @Param("after") LocalDateTime after);

    /**
     * 특정 종목/타임프레임 캐시 삭제
     */
    void deleteByTickerAndTimeframe(String ticker, String timeframe);
}
