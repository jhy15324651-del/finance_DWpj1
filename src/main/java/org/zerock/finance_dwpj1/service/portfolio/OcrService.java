package org.zerock.finance_dwpj1.service.portfolio;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.opencv.core.Core;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.finance_dwpj1.service.portfolio.ocr.BrokerType;
import org.zerock.finance_dwpj1.service.portfolio.ocr.OcrParser;
import org.zerock.finance_dwpj1.service.portfolio.ocr.OcrPreprocessor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    // ThreadLocal: 각 스레드마다 별도의 Tesseract 인스턴스 생성
    // 다중 이미지 병렬 처리 시 Thread-Safe 보장
    private final ThreadLocal<Tesseract> tesseractThreadLocal;
    private final boolean isAvailable;
    private final Map<BrokerType, OcrPreprocessor> preprocessors;
    private final Map<BrokerType, OcrParser> parsers;
    private final TickerMappingService tickerMappingService;
    // Tesseract 설정 정보를 저장 (ThreadLocal 생성 시 사용)
    private final String datapath;
    private final String language;
    private final int ocrEngineMode;
    private final int pageSegMode;

    @PostConstruct
    public void loadOpenCV() {
        nu.pattern.OpenCV.loadLocally(); // OpenCV 네이티브 라이브러리 로드
        System.out.println("OpenCV Loaded: " + Core.VERSION);
    }
    // OpenCV ON

    public OcrService(
            @Value("${tesseract.datapath}") String datapath,
            @Value("${tesseract.language:eng}") String language,
            @Value("${tesseract.ocr-engine-mode:3}") int ocrEngineMode,
            @Value("${tesseract.page-seg-mode:6}") int pageSegMode,
            List<OcrPreprocessor> preprocessorList,
            List<OcrParser> parserList, TickerMappingService tickerMappingService
    ) {
        // 설정 정보 저장
        this.datapath = datapath;
        this.language = language;
        this.ocrEngineMode = ocrEngineMode;
        this.pageSegMode = pageSegMode;
        this.tickerMappingService = tickerMappingService;

        // ThreadLocal 초기화: 각 스레드가 처음 접근할 때 새 Tesseract 인스턴스 생성
        this.tesseractThreadLocal = ThreadLocal.withInitial(() -> {
            Tesseract tess = new Tesseract();
            tess.setDatapath(this.datapath);
            tess.setLanguage(this.language);
            tess.setOcrEngineMode(this.ocrEngineMode);
            tess.setPageSegMode(this.pageSegMode);
            log.debug("스레드 {}에 새 Tesseract 인스턴스 생성", Thread.currentThread().getName());
            return tess;
        });

        // Preprocessor와 Parser를 BrokerType별로 매핑
        this.preprocessors = new HashMap<>();
        for (OcrPreprocessor preprocessor : preprocessorList) {
            preprocessors.put(preprocessor.getSupportedBroker(), preprocessor);
        }

        this.parsers = new HashMap<>();
        for (OcrParser parser : parserList) {
            parsers.put(parser.getSupportedBroker(), parser);
        }

        log.info("등록된 Preprocessor: {}", preprocessors.keySet());
        log.info("등록된 Parser: {}", parsers.keySet());

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

            // 초기 검증을 위한 임시 Tesseract 인스턴스 생성 (설정만 확인)
            Tesseract tempTesseract = new Tesseract();
            tempTesseract.setDatapath(datapath);
            tempTesseract.setLanguage(language);
            tempTesseract.setOcrEngineMode(ocrEngineMode);
            tempTesseract.setPageSegMode(pageSegMode);

            tempAvailable = true;
            log.info("✅ Tesseract OCR 초기화 완료 (ThreadLocal 모드)");
            log.info("   - 데이터 경로: {}", datapath);
            log.info("   - 언어: {}", language);
            log.info("   - OCR 엔진 모드: {}", ocrEngineMode);
            log.info("   - 페이지 세그멘테이션 모드: {}", pageSegMode);
            log.info("   - 다중 스레드 병렬 처리 지원: Thread-Safe");

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
     * 이미지에서 포트폴리오 데이터 추출 (기본 파서 사용)
     * @param imageFile 업로드된 이미지 파일
     * @return 추출된 포트폴리오 종목 리스트
     * @throws IllegalStateException OCR을 사용할 수 없는 경우
     * @deprecated 증권사를 지정하는 extractPortfolioFromImage(MultipartFile, BrokerType) 사용 권장
     */
    public List<PortfolioStock> extractPortfolioFromImage(MultipartFile imageFile) {
        return extractPortfolioFromImage(imageFile, BrokerType.DEFAULT);
    }

    /**
     * 이미지에서 포트폴리오 데이터 추출 (증권사별 처리)
     * @param imageFile 업로드된 이미지 파일
     * @param brokerType 증권사 타입
     * @return 추출된 포트폴리오 종목 리스트
     * @throws IllegalStateException OCR을 사용할 수 없는 경우
     */
    public List<PortfolioStock> extractPortfolioFromImage(MultipartFile imageFile, BrokerType brokerType) {
        List<PortfolioStock> stocks = new ArrayList<>();

        // OCR 사용 가능 여부 확인
        if (!isAvailable) {
            throw new IllegalStateException(
                    "Tesseract OCR을 사용할 수 없습니다. " +
                    "Tesseract 설치 및 설정을 확인하세요."
            );
        }

        log.info("OCR 처리 시작 - 증권사: {}", brokerType);

        try {
            // MultipartFile을 BufferedImage로 변환
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));

            if (image == null) {
                log.error("이미지 로드 실패: 지원하지 않는 이미지 형식일 수 있습니다.");
                throw new IllegalArgumentException("이미지를 읽을 수 없습니다.");
            }

            // 1. 증권사별 이미지 전처리
            BufferedImage preprocessedImage = image;
            OcrPreprocessor preprocessor = preprocessors.get(brokerType);

            if (preprocessor != null) {
                log.info("이미지 전처리 시작 - {}", brokerType);
                preprocessedImage = preprocessor.preprocess(image);
            } else {
                log.warn("{}에 대한 전처리기를 찾을 수 없습니다. 원본 이미지 사용", brokerType);
            }

            // 2. OCR 실행 (ThreadLocal에서 현재 스레드의 Tesseract 인스턴스 가져오기)
            log.info("OCR 처리 시작: {} ({} bytes) [스레드: {}]",
                    imageFile.getOriginalFilename(), imageFile.getSize(), Thread.currentThread().getName());
            String extractedText = tesseractThreadLocal.get().doOCR(preprocessedImage);
            log.info("OCR 추출 완료:\n{}", extractedText);

            // 2.5. 🔥 한국어 종목명 → 영어 티커로 치환 (Parser 호출 전)
            String mappedText = tickerMappingService.applyMappingToText(extractedText);
            log.info("티커 매핑 완료:\n{}", mappedText);

            // 3. 증권사별 텍스트 파싱
            OcrParser parser = parsers.get(brokerType);

            if (parser != null) {
                log.info("증권사별 파싱 시작 - {}", brokerType);
                stocks = parser.parse(mappedText); // 🔥 치환된 텍스트 사용
            } else {
                log.warn("{}에 대한 파서를 찾을 수 없습니다. 기본 파싱 사용", brokerType);
                stocks = parsePortfolioText(mappedText); // 🔥 치환된 텍스트 사용
            }

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

        // 기존: weight = 비중(%) 용도로 쓰던 값 (기존 코드 호환용)
        private Double weight; // 비중 (%)

        // 🔥 새로 추가: 보유 주식 수 & 평가금액 (원)
        private Double shares;   // 보유 주식 수 (정수/소수 둘 다 가능)
        private Long amount;     // 평가금액 (원 단위)

        // === 기존 코드와 호환용 생성자 (건들지 말기) ===
        public PortfolioStock(String ticker, Double weight) {
            this.ticker = ticker;
            this.weight = weight;
        }

        // === 🔥 TOSS 전용: 주식 수 + 평가금액용 생성자 ===
        public PortfolioStock(String ticker, Double shares, Long amount) {
            this.ticker = ticker;
            this.shares = shares;
            this.amount = amount;
        }

        public String getTicker() {
            return ticker;
        }

        public Double getWeight() {
            return weight;
        }

        public Double getShares() {
            return shares;
        }

        public Long getAmount() {
            return amount;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(ticker);
            if (shares != null) {
                sb.append(" / ").append(shares).append("주");
            }
            if (amount != null) {
                sb.append(" / ").append(amount).append("원");
            }
            if (weight != null) {
                sb.append(" / ").append(weight).append("%");
            }
            return sb.toString();
        }
    }
}
