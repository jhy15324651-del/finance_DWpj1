package org.zerock.finance_dwpj1.service.portfolio.ocr.preprocessor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.zerock.finance_dwpj1.service.portfolio.ocr.BrokerType;
import org.zerock.finance_dwpj1.service.portfolio.ocr.OcrPreprocessor;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 토스 증권 이미지 전처리 구현체

 */
@Component
@Slf4j
public class TossPreprocessor implements OcrPreprocessor {

    @Override
    public BufferedImage preprocess(BufferedImage originalImage) {
        log.info("토스 증권 이미지 전처리 시작 ({}x{})", originalImage.getWidth(), originalImage.getHeight());

        BufferedImage processedImage = originalImage;

        try {
            // 1. 그레이스케일 변환 (OCR 정확도 향상)
            processedImage = convertToGrayscale(processedImage);
            log.debug("그레이스케일 변환 완료");

            // 2. 대비 향상 (텍스트 명확도 향상)
            processedImage = enhanceContrast(processedImage, 1.3f);
            log.debug("대비 향상 완료");

            // 3. 밝기 조정 (너무 어둡거나 밝은 이미지 보정)
            processedImage = adjustBrightness(processedImage, 1.1f);
            log.debug("밝기 조정 완료");

            // 4. 샤프닝 (텍스트 선명도 향상)
            processedImage = sharpen(processedImage);
            log.debug("샤프닝 완료");

            log.info("토스 증권 이미지 전처리 완료");

        } catch (Exception e) {
            log.error("이미지 전처리 중 오류 발생, 원본 이미지 반환", e);
            return originalImage;
        }

        return processedImage;
    }

    @Override
    public BrokerType getSupportedBroker() {
        return BrokerType.TOSS;
    }

    /**
     * 그레이스케일 변환
     */
    private BufferedImage convertToGrayscale(BufferedImage original) {
        BufferedImage grayscale = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );

        Graphics2D g2d = grayscale.createGraphics();
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();

        return grayscale;
    }

    /**
     * 대비 향상
     * @param factor 대비 배율 (1.0 = 원본, 1.0 초과 = 대비 증가)
     */
    private BufferedImage enhanceContrast(BufferedImage original, float factor) {
        BufferedImage enhanced = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                original.getType()
        );

        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                int rgb = original.getRGB(x, y);
                Color color = new Color(rgb);

                // 대비 조정 (128을 중심으로 확대/축소)
                int r = adjustChannel(color.getRed(), factor);
                int g = adjustChannel(color.getGreen(), factor);
                int b = adjustChannel(color.getBlue(), factor);

                Color newColor = new Color(r, g, b);
                enhanced.setRGB(x, y, newColor.getRGB());
            }
        }

        return enhanced;
    }

    /**
     * 채널 값 조정 (0-255 범위 유지)
     */
    private int adjustChannel(int value, float factor) {
        int adjusted = (int) ((value - 128) * factor + 128);
        return Math.max(0, Math.min(255, adjusted));
    }

    /**
     * 밝기 조정
     * @param factor 밝기 배율 (1.0 = 원본, 1.0 초과 = 밝게, 1.0 미만 = 어둡게)
     */
    private BufferedImage adjustBrightness(BufferedImage original, float factor) {
        BufferedImage adjusted = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                original.getType()
        );

        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                int rgb = original.getRGB(x, y);
                Color color = new Color(rgb);

                int r = (int) Math.min(255, color.getRed() * factor);
                int g = (int) Math.min(255, color.getGreen() * factor);
                int b = (int) Math.min(255, color.getBlue() * factor);

                Color newColor = new Color(r, g, b);
                adjusted.setRGB(x, y, newColor.getRGB());
            }
        }

        return adjusted;
    }

    /**
     * 샤프닝 필터 적용 (텍스트 선명도 향상)
     */
    private BufferedImage sharpen(BufferedImage original) {
        // 3x3 샤프닝 커널
        float[] sharpenKernel = {
                0f, -1f, 0f,
                -1f, 5f, -1f,
                0f, -1f, 0f
        };

        BufferedImage sharpened = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                original.getType()
        );

        // 커널 적용
        for (int y = 1; y < original.getHeight() - 1; y++) {
            for (int x = 1; x < original.getWidth() - 1; x++) {
                float r = 0, g = 0, b = 0;

                // 3x3 영역에 커널 적용
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        Color color = new Color(original.getRGB(x + kx, y + ky));
                        float weight = sharpenKernel[(ky + 1) * 3 + (kx + 1)];

                        r += color.getRed() * weight;
                        g += color.getGreen() * weight;
                        b += color.getBlue() * weight;
                    }
                }

                // 결과 값 범위 제한 (0-255)
                int finalR = Math.max(0, Math.min(255, (int) r));
                int finalG = Math.max(0, Math.min(255, (int) g));
                int finalB = Math.max(0, Math.min(255, (int) b));

                sharpened.setRGB(x, y, new Color(finalR, finalG, finalB).getRGB());
            }
        }

        // 테두리는 원본 유지
        for (int x = 0; x < original.getWidth(); x++) {
            sharpened.setRGB(x, 0, original.getRGB(x, 0));
            sharpened.setRGB(x, original.getHeight() - 1, original.getRGB(x, original.getHeight() - 1));
        }
        for (int y = 0; y < original.getHeight(); y++) {
            sharpened.setRGB(0, y, original.getRGB(0, y));
            sharpened.setRGB(original.getWidth() - 1, y, original.getRGB(original.getWidth() - 1, y));
        }

        return sharpened;
    }
}