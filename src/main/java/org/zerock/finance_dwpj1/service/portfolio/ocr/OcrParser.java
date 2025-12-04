package org.zerock.finance_dwpj1.service.portfolio.ocr;

import org.zerock.finance_dwpj1.service.portfolio.OcrService.PortfolioStock;

import java.util.List;

/**
 * OCR 텍스트 파싱 인터페이스
 * 증권사별로 다른 텍스트 포맷을 파싱하여 포트폴리오 종목 리스트로 변환합니다.
 *
 * 파싱 작업 예시:
 * - 줄 단위 분리 및 정제
 * - 종목 티커 추출
 * - 비중(%) 추출
 * - 수량, 평가금액 등 추가 정보 추출
 * - 불필요한 텍스트 필터링 (헤더, 푸터, 합계 등)
 * - 증권사별 특수 포맷 처리
 */
public interface OcrParser {

    /**
     * OCR 추출 텍스트를 파싱하여 포트폴리오 종목 리스트로 변환
     *
     * @param ocrText OCR로 추출한 원본 텍스트
     * @return 파싱된 포트폴리오 종목 리스트
     */
    List<PortfolioStock> parse(String ocrText);

    /**
     * 파서가 지원하는 증권사 타입
     *
     * @return 지원하는 증권사 타입
     */
    BrokerType getSupportedBroker();
}