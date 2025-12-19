package org.zerock.finance_dwpj1.util.stock;


import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class StockFileStorage {

    private final String uploadDir = "C:/upload/stock/";

    public String save(MultipartFile file) {
        try{
            String uuid = UUID.randomUUID().toString();
            String savedName = uuid+"_"+file.getOriginalFilename();

            Path path = Paths.get(uploadDir, savedName);
            Files.copy(file.getInputStream(), path);

            return savedName;
        } catch(Exception e){
            throw new RuntimeException("파일 저장 실패", e);
        }
    }
}
