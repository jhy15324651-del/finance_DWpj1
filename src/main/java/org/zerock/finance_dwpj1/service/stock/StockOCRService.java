package org.zerock.finance_dwpj1.service.stock;

import org.springframework.web.multipart.MultipartFile;
import org.zerock.finance_dwpj1.dto.stock.StockOCRResultDTO;

public interface StockOCRService {

    StockOCRResultDTO processImage(MultipartFile file) throws Exception;
}
