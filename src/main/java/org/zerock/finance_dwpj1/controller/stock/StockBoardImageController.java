package org.zerock.finance_dwpj1.controller.stock;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class StockBoardImageController {

    private static final String UPLOAD_DIR = "C:/upload/stock/";

    @GetMapping("/upload/{filename}")
    public ResponseEntity<Resource> image(@PathVariable String filename) {

        FileSystemResource resource =
                new FileSystemResource(UPLOAD_DIR + filename);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG) // png여도 브라우저는 잘 그림
                .body(resource);
    }
}