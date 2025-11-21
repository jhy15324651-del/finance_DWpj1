package org.zerock.finance_dwpj1.controller.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.dto.stock.StockBoardDTO;
import org.zerock.finance_dwpj1.service.stock.StockBoardService;

@Controller
@RequestMapping("/stock/board")
@RequiredArgsConstructor
public class StockBoardController {

    private final StockBoardService stockBoardService;

    /**
     * 종목별 게시판 목록
     * 예: /stock/board/005930?page=0
     */
    @GetMapping("/{symbol}")
    public String list(@PathVariable String symbol,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {

        PageRequest pageable = PageRequest.of(page, 10);

        Page<StockBoardDTO> result = stockBoardService.getList(symbol, pageable);

        model.addAttribute("symbol", symbol);
        model.addAttribute("result", result);

        return "stock/board/list"; // templates/stock/board/list.html
    }

    /**
     * 글쓰기 폼
     * GET /stock/board/005930/write
     */
    @GetMapping("/{symbol}/write")
    public String writeForm(@PathVariable String symbol, Model model) {

        StockBoardDTO dto = StockBoardDTO.builder()
                .symbol(symbol)
                .build();

        model.addAttribute("dto", dto);
        model.addAttribute("symbol", symbol);

        return "stock/board/write"; // templates/stock/board/write.html
    }

    /**
     * 글쓰기 처리
     * POST /stock/board/005930/write
     */
    @PostMapping("/{symbol}/write")
    public String write(@PathVariable String symbol,
                        @ModelAttribute("dto") StockBoardDTO dto) {

        dto.setSymbol(symbol); // path variable 기준으로 강제 세팅

        stockBoardService.register(dto);

        return "redirect:/stock/board/" + symbol;
    }

    /**
     * 글 상세 보기
     * GET /stock/board/005930/read/1
     */
    @GetMapping("/{symbol}/read/{id}")
    public String read(@PathVariable String symbol,
                       @PathVariable Long id,
                       Model model) {

        StockBoardDTO dto = stockBoardService.get(id);

        model.addAttribute("dto", dto);
        model.addAttribute("symbol", symbol);

        return "stock/board/read"; // templates/stock/board/read.html
    }

    /**
     * 글 수정 폼
     */
    @GetMapping("/{symbol}/edit/{id}")
    public String editForm(@PathVariable String symbol,
                           @PathVariable Long id,
                           Model model) {

        StockBoardDTO dto = stockBoardService.get(id);

        model.addAttribute("dto", dto);
        model.addAttribute("symbol", symbol);

        return "stock/board/edit"; // 원하면 분리, 아니면 read랑 같이 써도 됨
    }

    /**
     * 글 수정 처리
     */
    @PostMapping("/{symbol}/edit/{id}")
    public String edit(@PathVariable String symbol,
                       @PathVariable Long id,
                       @ModelAttribute("dto") StockBoardDTO dto) {

        dto.setId(id);
        dto.setSymbol(symbol); // 혹시 모를 조작 방지

        stockBoardService.modify(dto);

        return "redirect:/stock/board/" + symbol + "/read/" + id;
    }

    /**
     * 글 삭제
     */
    @PostMapping("/{symbol}/delete/{id}")
    public String delete(@PathVariable String symbol,
                         @PathVariable Long id) {

        stockBoardService.remove(id);
        return "redirect:/stock/board/" + symbol;
    }
}