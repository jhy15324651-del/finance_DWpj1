package org.zerock.finance_dwpj1.service.stock;

import org.zerock.finance_dwpj1.dto.stock.StockCandleDTO;
import org.zerock.finance_dwpj1.dto.stock.StockDetailDTO;
import org.zerock.finance_dwpj1.dto.stock.StockInfoDTO;

import java.util.List;
import java.util.Map;

/**
 * 주식 데이터 서비스 인터페이스
 * Mock 서비스와 실제 API 서비스가 이 인터페이스를 구현합니다.
 */
public interface StockService {

    /**
     * 종목 정보 조회
     *
     * @param ticker 티커 (예: 005930, AAPL)
     * @return 종목 정보
     */
    StockInfoDTO getStockInfo(String ticker);

    /**
     * 캔들 데이터 조회
     *
     * @param ticker 티커
     * @param timeframe 시간프레임 (D=일봉, W=주봉, M=월봉)
     * @param count 데이터 개수
     * @return 캔들 데이터 리스트
     */
    List<StockCandleDTO> getCandleData(String ticker, String timeframe, int count);

    /**
     * 종목 검색
     *
     * @param query 검색어
     * @return 검색 결과 리스트
     */
    List<StockInfoDTO> searchStocks(String query);

    StockDetailDTO fetchCompanyDetail(String ticker);



}