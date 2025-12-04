package org.zerock.finance_dwpj1.controller.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.finance_dwpj1.service.portfolio.OcrService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OCR 테스트용 컨트롤러
 * Tesseract OCR에서 추출되는 모든 데이터를 확인할 수 있습니다
 */
@Controller
@RequestMapping("/test/ocr")
@RequiredArgsConstructor
@Slf4j
public class OcrTestController {

    private final OcrService ocrService;

    @Value("${tesseract.datapath}")
    private String tesseractDataPath;

    @Value("${tesseract.language:eng}")
    private String tesseractLanguage;

    /**
     * OCR 테스트 페이지
     */
    @GetMapping("")
    public String testPage(Model model) {
        model.addAttribute("ocrAvailable", ocrService.isAvailable());
        model.addAttribute("tesseractPath", tesseractDataPath);
        model.addAttribute("tesseractLanguage", tesseractLanguage);
        return "test/ocr-test";
    }

    /**
     * OCR 테스트 실행 (모든 데이터 반환)
     */
    @PostMapping("/extract")
    @ResponseBody
    public Map<String, Object> extractOcrData(@RequestParam("image") MultipartFile imageFile) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("=".repeat(80));
            log.info("OCR 테스트 시작");
            log.info("파일명: {}", imageFile.getOriginalFilename());
            log.info("파일 크기: {} bytes", imageFile.getSize());
            log.info("=".repeat(80));

            // 이미지 변환
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));
            if (image == null) {
                throw new IllegalArgumentException("이미지를 읽을 수 없습니다.");
            }

            // Tesseract 인스턴스 생성 (OcrService와 동일한 설정)
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tesseractDataPath);
            tesseract.setLanguage(tesseractLanguage);

            // OCR 실행
            long startTime = System.currentTimeMillis();
            String rawText = tesseract.doOCR(image);
            long endTime = System.currentTimeMillis();

            log.info("\n" + "=".repeat(80));
            log.info("OCR 원본 텍스트 (전체):");
            log.info("-".repeat(80));
            log.info("{}", rawText);
            log.info("=".repeat(80));

            // 라인별로 분리
            String[] lines = rawText.split("\n");
            List<Map<String, Object>> lineDetails = new ArrayList<>();

            log.info("\n라인별 분석:");
            log.info("-".repeat(80));

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                Map<String, Object> lineInfo = new HashMap<>();
                lineInfo.put("lineNumber", i + 1);
                lineInfo.put("rawText", line);
                lineInfo.put("trimmed", line.trim());
                lineInfo.put("length", line.length());
                lineInfo.put("isEmpty", line.trim().isEmpty());

                // 티커 패턴 찾기
                Pattern tickerPattern = Pattern.compile("\\b([A-Z]{2,5})\\b");
                Matcher tickerMatcher = tickerPattern.matcher(line);
                List<String> foundTickers = new ArrayList<>();
                while (tickerMatcher.find()) {
                    foundTickers.add(tickerMatcher.group(1));
                }
                lineInfo.put("foundTickers", foundTickers);

                // 숫자 패턴 찾기
                Pattern numberPattern = Pattern.compile("\\d+(?:\\.\\d+)?");
                Matcher numberMatcher = numberPattern.matcher(line);
                List<String> foundNumbers = new ArrayList<>();
                while (numberMatcher.find()) {
                    foundNumbers.add(numberMatcher.group());
                }
                lineInfo.put("foundNumbers", foundNumbers);

                // 퍼센트 패턴 찾기
                Pattern percentPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%");
                Matcher percentMatcher = percentPattern.matcher(line);
                List<String> foundPercents = new ArrayList<>();
                while (percentMatcher.find()) {
                    foundPercents.add(percentMatcher.group(1) + "%");
                }
                lineInfo.put("foundPercents", foundPercents);

                lineDetails.add(lineInfo);

                log.info("라인 {}: \"{}\"", i + 1, line);
                if (!foundTickers.isEmpty()) {
                    log.info("  → 티커 발견: {}", foundTickers);
                }
                if (!foundPercents.isEmpty()) {
                    log.info("  → 퍼센트 발견: {}", foundPercents);
                }
                if (!foundNumbers.isEmpty()) {
                    log.info("  → 숫자 발견: {}", foundNumbers);
                }
            }

            log.info("=".repeat(80));

            // OcrService의 파싱 결과도 함께 반환
            List<OcrService.PortfolioStock> parsedStocks = ocrService.extractPortfolioFromImage(imageFile);

            log.info("\n파싱 결과:");
            log.info("-".repeat(80));
            for (OcrService.PortfolioStock stock : parsedStocks) {
                log.info("종목: {} - {}%", stock.getTicker(), stock.getWeight());
            }
            log.info("=".repeat(80));
            log.info("OCR 테스트 완료 (처리 시간: {}ms)", endTime - startTime);
            log.info("=".repeat(80));

            // 응답 구성
            result.put("success", true);
            result.put("rawText", rawText);
            result.put("totalLines", lines.length);
            result.put("lineDetails", lineDetails);
            result.put("parsedStocks", parsedStocks);
            result.put("parsedCount", parsedStocks.size());
            result.put("processingTime", endTime - startTime);
            result.put("imageInfo", Map.of(
                    "filename", imageFile.getOriginalFilename(),
                    "size", imageFile.getSize(),
                    "width", image.getWidth(),
                    "height", image.getHeight(),
                    "type", imageFile.getContentType()
            ));

        } catch (TesseractException e) {
            log.error("OCR 처리 중 오류", e);
            result.put("success", false);
            result.put("error", "OCR 처리 중 오류: " + e.getMessage());
        } catch (IOException e) {
            log.error("이미지 파일 읽기 오류", e);
            result.put("success", false);
            result.put("error", "이미지 파일 읽기 오류: " + e.getMessage());
        } catch (Exception e) {
            log.error("예상치 못한 오류", e);
            result.put("success", false);
            result.put("error", "오류: " + e.getMessage());
        }

        return result;
    }
}