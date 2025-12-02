package org.zerock.finance_dwpj1.controller.content;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@Controller
public class ContentUploadImageController {

    // application.properties에 작성할 업로드 폴더 경로
    @Value("${file.upload-path}")
    private String uploadPath;

    @PostMapping(value = "/content/uploadImage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public String uploadImage(@RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("업로드된 파일이 없습니다.");
        }

        // 날짜별 폴더 생성
        String dateFolder = LocalDate.now().toString(); // 예: 2025-12-03
        File uploadDir = new File(uploadPath + "/" + dateFolder);

        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // 확장자
        String originalName = file.getOriginalFilename();
        String ext = "";

        if (originalName != null && originalName.lastIndexOf(".") != -1) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        // 파일명 랜덤 생성
        String savedFileName = UUID.randomUUID().toString() + ext;

        // 실제 저장 위치
        File saveFile = new File(uploadDir, savedFileName);
        file.transferTo(saveFile);

        // 브라우저에서 접근할 수 있는 URL 반환
        String imageUrl = "/uploads/" + dateFolder + "/" + savedFileName;

        return imageUrl;
    }
}
