package org.zerock.finance_dwpj1.service.portfolio;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tesseract OCR 서비스
 * 이미지에서 텍스트를 추출하여 포트폴리오 데이터를 파싱합니다
 *
 * 사전 준비:
 * 1. build.gradle에 의존성 추가:
 *    implementation 'net.sourceforge.tess4j:tess4j:5.9.0'
 *
 * 2. Tesseract 설치 (Windows):
 *    - https://github.com/UB-Mannheim/tesseract/wiki 에서 다운로드
 *    - 기본 경로: C:\Program Files\Tesseract-OCR
 *
 * 3. application.properties에 경로 설정:
 *    tesseract.datapath=C:\\Program Files\\Tesseract-OCR\\tessdata
 *    tesseract.language=eng
 */
@Service
@Slf4j
public class OcrService {

    private final Tesseract tesseract;
    private final boolean isAvailable;

    public OcrService(
            @Value("${tesseract.datapath}") String datapath,
            @Value("${tesseract.language:eng}") String language,
            @Value("${tesseract.ocr-engine-mode:3}") int ocrEngineMode,
            @Value("${tesseract.page-seg-mode:6}") int pageSegMode
    ) {
        this.tesseract = new Tesseract();
        boolean tempAvailable = false;

        // Tesseract 설정
        try {
            // tessdata 경로 검증
            File tessdataDir = new File(datapath);
            if (!tessdataDir.exists() || !tessdataDir.isDirectory()) {
                throw new IllegalStateException(
                        "Tesseract tessdata 디렉토리를 찾을 수 없습니다: " + datapath +
                        "\nTesseract가 설치되어 있는지 확인하세요."
                );
            }

            // 언어 데이터 파일 검증
            String langFile = language.split("\\+")[0] + ".traineddata";
            File langDataFile = new File(tessdataDir, langFile);
            if (!langDataFile.exists()) {
                throw new IllegalStateException(
                        "언어 데이터 파일을 찾을 수 없습니다: " + langDataFile.getAbsolutePath() +
                        "\n'" + language + "' 언어 데이터를 다운로드하세요."
                );
            }

            // Tesseract 설정 적용
            tesseract.setDatapath(datapath);
            tesseract.setLanguage(language);
            tesseract.setOcrEngineMode(ocrEngineMode);
            tesseract.setPageSegMode(pageSegMode);

            tempAvailable = true;
            log.info("✅ Tesseract OCR 초기화 완료");
            log.info("   - 데이터 경로: {}", datapath);
            log.info("   - 언어: {}", language);
            log.info("   - OCR 엔진 모드: {}", ocrEngineMode);
            log.info("   - 페이지 세그멘테이션 모드: {}", pageSegMode);

        } catch (Exception e) {
            log.error("❌ Tesseract OCR 초기화 실패: {}", e.getMessage());
            log.error("   OCR 기능을 사용할 수 없습니다.");
            log.error("   해결 방법:");
            log.error("   1. Tesseract 설치: https://github.com/UB-Mannheim/tesseract/wiki");
            log.error("   2. application.properties에서 tesseract.datapath 경로 확인");
            log.error("   3. 언어 데이터 파일 확인 (예: eng.traineddata)");
        }

        this.isAvailable = tempAvailable;
    }

    /**
     * OCR 기능 사용 가능 여부 확인
     */
    public boolean isAvailable() {
        return isAvailable;
    }

    /**
     * 이미지에서 포트폴리오 데이터 추출
     * @param imageFile 업로드된 이미지 파일
     * @return 추출된 포트폴리오 종목 리스트
     * @throws IllegalStateException OCR을 사용할 수 없는 경우
     */
    public List<PortfolioStock> extractPortfolioFromImage(MultipartFile imageFile) {
        List<PortfolioStock> stocks = new ArrayList<>();

        // OCR 사용 가능 여부 확인
        if (!isAvailable) {
            throw new IllegalStateException(
                    "Tesseract OCR을 사용할 수 없습니다. " +
                    "Tesseract 설치 및 설정을 확인하세요."
            );
        }

        try {
            // MultipartFile을 BufferedImage로 변환
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));

            if (image == null) {
                log.error("이미지 로드 실패: 지원하지 않는 이미지 형식일 수 있습니다.");
                throw new IllegalArgumentException("이미지를 읽을 수 없습니다.");
            }

            // OCR 실행
            log.info("OCR 처리 시작: {} ({} bytes)", imageFile.getOriginalFilename(), imageFile.getSize());
            String extractedText = tesseract.doOCR(image);
            log.info("OCR 추출 완료:\n{}", extractedText);

            // 추출된 텍스트에서 종목 정보 파싱
            stocks = parsePortfolioText(extractedText);

            log.info("✅ 총 {}개 종목 추출 완료", stocks.size());

        } catch (TesseractException e) {
            log.error("❌ OCR 처리 중 오류 발생", e);
            throw new RuntimeException("OCR 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("❌ 이미지 파일 읽기 오류", e);
            throw new RuntimeException("이미지 파일을 읽을 수 없습니다: " + e.getMessage(), e);
        }

        return stocks;
    }

    /**
     * OCR로 추출한 텍스트에서 종목 정보 파싱
     *
     * 예상 형식:
     * AAPL  150  30%
     * TSLA  80   25%
     * MSFT  200  20%
     */
    private List<PortfolioStock> parsePortfolioText(String text) {
        List<PortfolioStock> stocks = new ArrayList<>();

        // 줄 단위로 분리
        String[] lines = text.split("\\n");

        // 티커 패턴: 2-5자 대문자 (AAPL, TSLA, GOOGL 등)
        Pattern tickerPattern = Pattern.compile("\\b([A-Z]{2,5})\\b");

        // 비중 패턴: 숫자 + % (30%, 25.5% 등)
        Pattern weightPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            log.debug("파싱 중: {}", line);

            // 티커 추출
            Matcher tickerMatcher = tickerPattern.matcher(line);
            String ticker = null;
            if (tickerMatcher.find()) {
                ticker = tickerMatcher.group(1);
            }

            // 비중 추출
            Matcher weightMatcher = weightPattern.matcher(line);
            Double weight = null;
            if (weightMatcher.find()) {
                weight = Double.parseDouble(weightMatcher.group(1));
            }

            // 티커와 비중이 모두 있으면 추가
            if (ticker != null && weight != null) {
                PortfolioStock stock = new PortfolioStock(ticker, weight);
                stocks.add(stock);
                log.info("종목 인식: {} - {}%", ticker, weight);
            }
        }

        return stocks;
    }

    /**
     * 포트폴리오 종목 DTO
     */
    public static class PortfolioStock {
        private String ticker;
        private Double weight; // 비중 (%)

        public PortfolioStock(String ticker, Double weight) {
            this.ticker = ticker;
            this.weight = weight;
        }

        public String getTicker() {
            return ticker;
        }

        public Double getWeight() {
            return weight;
        }

        @Override
        public String toString() {
            return ticker + ": " + weight + "%";
        }
    }
}
