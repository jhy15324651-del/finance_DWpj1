package org.zerock.finance_dwpj1.service.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
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

    @Override
    public StockOCRResultDTO processImage(MultipartFile file) throws Exception {

        // 1) 저장 없이 바로 OCR
        String text = doOCR(file);

        // 2) 화면 타입 자동 판별
        String type = detectType(text);

        // 3) A/B 파싱
        if (type.equals("A")) return parseA(text);
        else return parseB(text);
    }

    // OCR 실행
    private String doOCR(MultipartFile file) throws Exception {

        Tesseract tess = new Tesseract();
        tess.setDatapath(System.getProperty("user.dir") + "/tessdata");
        tess.setLanguage("kor+eng");

        // MultipartFile → BufferedImage 변환
        BufferedImage image = ImageIO.read(file.getInputStream());

        return tess.doOCR(image);
    }

    // 파일 A인지 B인지 판별
    private String detectType(String text) {
        if (text.contains("판매") || text.contains("판 매")) return "A";
        return "B";
    }

    // ======= A 파일 (판매수익 화면) 추출 =======
    private StockOCRResultDTO parseA(String text) {
        StockOCRResultDTO r = new StockOCRResultDTO();
        r.setType("A");

        Pattern amountPattern =
                Pattern.compile("판\\s*매\\s*수\\s*익[^\\d\\-+]*([+\\-]?\\d[\\d,]*)");

        Pattern percentPattern =
                Pattern.compile("판\\s*매\\s*수\\s*익.*\\(([+\\-]?\\d+\\.?\\d*)%\\)");

        Matcher m1 = amountPattern.matcher(text);
        Matcher m2 = percentPattern.matcher(text);

        if (m1.find()) r.setAmount(clean(m1.group(1)));
        if (m2.find()) r.setPercent(m2.group(1));

        return r;
    }

    // ======= B 파일 (내 종목보기 화면) 추출 =======
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
            if (count == 2) { // 두 번째 원 단위 금액 = 수익
                r.setAmount(clean(m1.group(1)));
                break;
            }
        }

        if (m2.find()) r.setPercent(m2.group(1));

        return r;
    }

    // 숫자 정리
    private String clean(String s) {
        return s.replace(",", "").replace(" ", "");
    }
}