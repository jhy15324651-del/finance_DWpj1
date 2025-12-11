package org.zerock.finance_dwpj1.controller.stock;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.finance_dwpj1.dto.stock.StockOCRResultDTO;
import org.zerock.finance_dwpj1.service.stock.StockOCRService;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/stock/ocr")
public class StockOCRController {

    private final StockOCRService stockOCRService;

    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file){


        try{

            StockOCRResultDTO result = stockOCRService.processImage(file);
            return ResponseEntity.ok(result);

        } catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().body("OCR 처리 실패 : "+e.getMessage());
        }
    }


    @GetMapping("/test")
    public String test(){
        return "stock/ocr_test";
    }
}
