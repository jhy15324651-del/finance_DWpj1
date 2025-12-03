package org.zerock.finance_dwpj1.controller.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.dto.stock.StockBoardDTO;
import org.zerock.finance_dwpj1.dto.user.UserSessionDTO;
import org.zerock.finance_dwpj1.service.stock.StockBoardService;
import org.zerock.finance_dwpj1.service.user.CustomUserDetails;

@Slf4j
@Controller
@RequestMapping("/stock/board")
@RequiredArgsConstructor
public class StockBoardController {

    private final StockBoardService stockBoardService;

    /**
     * 종목별 게시판 목록
     * 예: /stock/board/005930?page=0
     */

    /**
     * 글쓰기 폼
     * GET /stock/board/005930/write
     */
    @GetMapping("/{ticker}/write")
    public String writeForm(
            @PathVariable String ticker,
            @AuthenticationPrincipal CustomUserDetails loginUser,
            Model model) {
        // 로그인 필요
        if (loginUser == null) {
            return "redirect:/user/login";
        }

        StockBoardDTO dto = StockBoardDTO.builder()
                .ticker(ticker)
                .build();

        model.addAttribute("dto", dto);
        model.addAttribute("ticker", ticker);
        model.addAttribute("loginUser", loginUser);

        return "stock/board/write";
    }


    /**
     * 글쓰기 처리
     * POST /stock/board/005930/write
     */
    @PostMapping("/{ticker}/write")
    public String write(
            @PathVariable String ticker,
            @ModelAttribute("dto") StockBoardDTO dto,
            @AuthenticationPrincipal CustomUserDetails loginUser) {

        //  로그인 확인
        if (loginUser == null) {
            return "redirect:/user/login";
        }

        // 작성자 자동 설정
        dto.setWriter(loginUser.getNickname());
        dto.setTicker(ticker);


        stockBoardService.register(dto);

        return "redirect:/stock/board/" + ticker;
    }



    @GetMapping("/{ticker}/read/{id}")
    public String read(@PathVariable String ticker,
                       @PathVariable Long id,
                       Model model) {

        StockBoardDTO dto = stockBoardService.get(id);

        model.addAttribute("dto", dto);
        model.addAttribute("ticker", ticker);

        return "stock/board/read";
    }



    /**
     * 글 상세 보기
     * GET /stock/board/005930/read/1
//     */
//    @GetMapping("/{ticker}/read/{id}")
//    public String read(@PathVariable String ticker,
//                       @PathVariable Long id,
//                       Model model) {
//
//        StockBoardDTO dto = stockBoardService.get(id);
//
//        model.addAttribute("dto", dto);
//        model.addAttribute("ticker", ticker);
//
//        return "stock/board/read"; // templates/stock/board/read.html
//    }

    /**
     * 글 수정 폼
     */
    @GetMapping("/{ticker}/edit/{id}")
    public String editForm(@PathVariable String ticker,
                           @PathVariable Long id,
                           Model model) {

        StockBoardDTO dto = stockBoardService.get(id);

        model.addAttribute("dto", dto);
        model.addAttribute("ticker", ticker);

        return "stock/board/edit"; // 원하면 분리, 아니면 read랑 같이 써도 됨
    }

    /**
     * 글 수정 처리
     */
    @PostMapping("/{ticker}/edit/{id}")
    public String edit(@PathVariable String ticker,
                       @PathVariable Long id,
                       @ModelAttribute("dto") StockBoardDTO dto) {

        dto.setId(id);
        dto.setTicker(ticker); // 혹시 모를 조작 방지

        stockBoardService.modify(dto);

        return "redirect:/stock/board/" + ticker + "/read/" + id;
    }

    /**
     * 글 삭제
     */
    @PostMapping("/{ticker}/delete/{id}")
    public String delete(@PathVariable String ticker,
                         @PathVariable Long id) {

        stockBoardService.remove(id);
        return "redirect:/stock/board/" + ticker;
    }

    @GetMapping("/api/{ticker}")
    @ResponseBody
    public Page<StockBoardDTO> apiList(@PathVariable String ticker,
                                       @RequestParam(defaultValue = "0") int page) {

        PageRequest pageable = PageRequest.of(page, 10);
        return stockBoardService.getList(ticker, pageable);
    }



}