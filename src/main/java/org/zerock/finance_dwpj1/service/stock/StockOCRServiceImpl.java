package org.zerock.finance_dwpj1.service.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.finance_dwpj1.dto.stock.StockOCRResultDTO;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockOCRServiceImpl implements StockOCRService {

    private final String uploadDir = System.getProperty("user.dir") + "/uploads";

    // application.properties에서 Tesseract 설정 주입 (Stock OCR 전용)
    @Value("${tesseract.datapath}")
    private String tesseractDatapath;

    @Value("${tesseract.language}")
    private String tesseractLanguage;

    @Override
    public StockOCRResultDTO processImage(MultipartFile file) throws Exception {

        // 1) 저장 없이 바로 OCR
        String text = doOCR(file);


        //1.5) 음수 정렬
        text = text.replace("−", "-")
                .replace("–", "-")
                .replace("—", "-");

        // 2) 화면 타입 자동 판별
        String type = detectType(text);

        // 3) A/B 파싱
        if (type.equals("A")) return parseA(text);
        else return parseB(text);
    }

    // OCR 실행
    private String doOCR(MultipartFile file) throws Exception {

        log.info("========================================");
        log.info("[Stock OCR] OCR 실행 시작");
        log.info("========================================");

        // 1️⃣ application.properties에서 주입받은 경로 사용
        log.info("[Stock OCR] tesseractDatapath from properties: {}", tesseractDatapath);
        log.info("[Stock OCR] tesseractLanguage from properties: {}", tesseractLanguage);

        // 2️⃣ tessdata 폴더 경로를 그대로 사용 (Tesseract는 datapath 바로 아래에서 traineddata를 찾음)
        // 예: "C:\Program Files\Tesseract-OCR\tessdata" → 이것을 그대로 setDatapath에 사용
        String tessdataPath = tesseractDatapath;

        // 경로 정규화 (슬래시 통일)
        tessdataPath = tessdataPath.replace("\\", "/");
        log.info("[Stock OCR] 사용할 tessdata 경로 (정규화 후): {}", tessdataPath);

        // 3️⃣ TESSDATA_PREFIX 환경변수 강제 설정 (Tesseract가 참조할 수 있음)
        System.setProperty("TESSDATA_PREFIX", tessdataPath);
        log.info("[Stock OCR] TESSDATA_PREFIX 설정 완료: {}", System.getProperty("TESSDATA_PREFIX"));

        // 4️⃣ traineddata 파일 존재 체크 (크래시 방지 - 최우선 가드)
        // 중요: tessdata 폴더 자체가 datapath이므로, 바로 그 아래에서 traineddata를 찾음
        File tessdataDir = new File(tessdataPath);
        File engTrainedData = new File(tessdataDir, "eng.traineddata");
        File korTrainedData = new File(tessdataDir, "kor.traineddata");

        log.info("[Stock OCR] tessdata 폴더: {} (exists: {})", tessdataDir.getAbsolutePath(), tessdataDir.exists());
        log.info("[Stock OCR] eng.traineddata: {} (exists: {})", engTrainedData.getAbsolutePath(), engTrainedData.exists());
        log.info("[Stock OCR] kor.traineddata: {} (exists: {})", korTrainedData.getAbsolutePath(), korTrainedData.exists());

        // 🔥 언어 파일이 하나라도 없으면 절대 doOCR 호출하지 않음 (Invalid memory access 방지)
        if (!engTrainedData.exists() || !korTrainedData.exists()) {
            String errorMsg = String.format(
                "[Stock OCR] 필수 traineddata 파일을 찾을 수 없습니다. OCR을 실행할 수 없습니다.\n" +
                "  - tessdata 폴더: %s (exists: %s)\n" +
                "  - eng.traineddata: %s (exists: %s)\n" +
                "  - kor.traineddata: %s (exists: %s)\n" +
                "  - application.properties에서 tesseract.datapath=%s 를 확인하세요.\n" +
                "  - 해당 경로에 eng.traineddata와 kor.traineddata가 모두 있어야 합니다.",
                tessdataDir.getAbsolutePath(), tessdataDir.exists(),
                engTrainedData.getAbsolutePath(), engTrainedData.exists(),
                korTrainedData.getAbsolutePath(), korTrainedData.exists(),
                tesseractDatapath
            );
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 5️⃣ MultipartFile → BufferedImage 변환
        BufferedImage image = ImageIO.read(file.getInputStream());

        // 6️⃣ 이미지 null 체크 (크래시 방지 두 번째 가드)
        if (image == null) {
            String errorMsg = "[Stock OCR] 이미지를 읽을 수 없습니다. 파일 형식을 확인하세요: " + file.getOriginalFilename();
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        log.info("[Stock OCR] 이미지 로드 성공 - 파일명: {}, 크기: {}x{}",
                 file.getOriginalFilename(), image.getWidth(), image.getHeight());

        // 7️⃣ Tesseract 설정 (tessdata 폴더 자체를 datapath로 설정)
        Tesseract tess = new Tesseract();
        tess.setDatapath(tessdataPath);  // ✅ tessdata 폴더 경로를 직접 넣음
        tess.setLanguage(tesseractLanguage);

        log.info("[Stock OCR] Tesseract 설정 완료:");
        log.info("  - datapath: {}", tessdataPath);
        log.info("  - language: {}", tesseractLanguage);
        log.info("  - TESSDATA_PREFIX: {}", System.getProperty("TESSDATA_PREFIX"));

        // 8️⃣ OCR 실행 (모든 가드를 통과한 경우에만 실행됨)
        try {
            log.info("[Stock OCR] doOCR() 호출 시작...");
            String result = tess.doOCR(image);
            log.info("[Stock OCR] doOCR() 완료 - 추출된 텍스트 길이: {}", result != null ? result.length() : 0);

            if (result != null && result.length() > 0) {
                log.info("[Stock OCR] 추출된 텍스트 미리보기 (최대 100자): {}",
                         result.substring(0, Math.min(100, result.length())));
            }

            log.info("========================================");
            log.info("[Stock OCR] OCR 실행 성공");
            log.info("========================================");

            return result;

        } catch (TesseractException e) {
            String errorMsg = String.format(
                "[Stock OCR] Tesseract OCR 실행 중 오류 발생:\n" +
                "  - 오류 메시지: %s\n" +
                "  - datapath: %s\n" +
                "  - language: %s\n" +
                "  - 원인: traineddata 파일이 손상되었거나 언어 설정이 잘못되었을 수 있습니다.",
                e.getMessage(), tessdataPath, tesseractLanguage
            );
            log.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format(
                "[Stock OCR] OCR 처리 중 예상치 못한 오류:\n" +
                "  - 오류 유형: %s\n" +
                "  - 오류 메시지: %s",
                e.getClass().getName(), e.getMessage()
            );
            log.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    // 파일 A인지 B인지 판별
    private String detectType(String text) {
        if (text.contains("판매") || text.contains("판 매")) return "A";
        return "B";
    }

    // 판매수익 추출
    private StockOCRResultDTO parseA(String text) {
        StockOCRResultDTO r = new StockOCRResultDTO();
        r.setType("A");

        Pattern amountPattern =
                Pattern.compile("판\\s*매\\s*수\\s*익[^\\d\\-+]*([+\\-]?\\d[\\d,]*)");

        Pattern percentPattern =
                Pattern.compile("판\\s*매\\s*수\\s*익.*\\(([+\\-]?\\d+\\.?\\d*)%\\)");

        Matcher m1 = amountPattern.matcher(text);
        Matcher m2 = percentPattern.matcher(text);

        if (m1.find()) r.setAmount(Long.parseLong(clean(m1.group(1))));
        if (m2.find()) r.setPercent(Double.parseDouble(m2.group(1)));

        return r;
    }

    // 주식 등락 추출
    private StockOCRResultDTO parseB(String text) {
        StockOCRResultDTO r = new StockOCRResultDTO();
        r.setType("B");

        Pattern amountPattern =
                Pattern.compile("([+\\-]?\\d[\\d,]*)\\s*원");

        Pattern percentPattern =
                Pattern.compile("\\(([+\\-]?\\d+\\.?\\d*)\\s*%\\)");

        Matcher m1 = amountPattern.matcher(text);
        Matcher m2 = percentPattern.matcher(text);

        int count = 0;
        while (m1.find()) {
            count++;
            if (count == 2) { // 두 번째 원 단위 금액 사용
                r.setAmount(Long.parseLong(clean(m1.group(1))));
                break;
            }
        }

        if (m2.find()) r.setPercent(Double.parseDouble(m2.group(1)));


        return r;
    }

    // 숫자 정리
    private String clean(String s) {
        return s.replace(",", "").replace(" ", "");
    }
}