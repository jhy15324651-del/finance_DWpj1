package org.zerock.finance_dwpj1.service.portfolio.ocr;

import java.awt.image.BufferedImage;

/**
 * OCR 이미지 전처리 인터페이스
 * 각 증권사의 UI 특성에 맞게 이미지를 전처리하여 OCR 정확도를 향상시킵니다.
 *
 * 전처리 작업 예시:
 * - 그레이스케일 변환
 * - 이진화 (Binarization)
 * - 노이즈 제거
 * - 대비 조정
 * - 특정 영역 크롭 (헤더, 푸터, 로고 제거)
 * - 회전 보정
 * - 해상도 조정
 */
public interface OcrPreprocessor {

    /**
     * 이미지 전처리 수행
     *
     * @param originalImage 원본 이미지
     * @return 전처리된 이미지 (OCR에 최적화된 상태)
     */
    BufferedImage preprocess(BufferedImage originalImage);

    /**
     * 전처리기가 지원하는 증권사 타입
     *
     * @return 지원하는 증권사 타입
     */
    BrokerType getSupportedBroker();
}