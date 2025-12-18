package org.zerock.finance_dwpj1.controller.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.dto.stock.StockBoardDTO;
import org.zerock.finance_dwpj1.dto.stock.StockInfoDTO;
import org.zerock.finance_dwpj1.dto.user.UserSessionDTO;
import org.zerock.finance_dwpj1.entity.stock.StockBoard;
import org.zerock.finance_dwpj1.service.stock.StockBoardService;
import org.zerock.finance_dwpj1.service.stock.YahooFinanceStockService;
import org.zerock.finance_dwpj1.service.user.CustomUserDetails;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/stock/board")
@RequiredArgsConstructor
public class StockBoardController {

    private final StockBoardService stockBoardService;
    private final YahooFinanceStockService yahooFinanceStockService;


    //로그인 체크
    @GetMapping("/api/login-check")
    @ResponseBody
    public Map<String, Boolean> loginCheck(@AuthenticationPrincipal CustomUserDetails user) {
        return Map.of("login", user != null);
    }

    //게시판 폼
    @GetMapping("/{ticker}")
    public String list(@PathVariable String ticker, Model model) {
        model.addAttribute("ticker", ticker);
        return "stock/board/list";   // templates/stock/board/list.html
    }

    //글 쓰기 폼
    @GetMapping("/{ticker}/write")
    public String writeForm(
            @PathVariable String ticker,
            @AuthenticationPrincipal CustomUserDetails loginUser,
            Model model) {
        // 로그인 필요
        if (loginUser == null) {
            return "redirect:/user/login";
        }

        StockInfoDTO info = yahooFinanceStockService.getStockInfo(ticker);


        StockBoardDTO dto = StockBoardDTO.builder()
                .ticker(ticker)
                .build();

        model.addAttribute("dto", dto);
        model.addAttribute("ticker", ticker);
        model.addAttribute("stockName", info != null ? info.getName() : ticker);
        model.addAttribute("loginUser", loginUser);

        return "stock/board/write";
    }



    //글 쓰기
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


    //글 읽기 조회수 증가
    @GetMapping("/{ticker}/read/{id}")
    public String read(@PathVariable String ticker,
                       @PathVariable Long id,
                       @AuthenticationPrincipal CustomUserDetails loginUser,
                       Model model
    ) {

        stockBoardService.addView(id);

        StockBoardDTO dto = stockBoardService.get(id);

        model.addAttribute("dto", dto);
        model.addAttribute("ticker", ticker);
        model.addAttribute("loginUser", loginUser);

        return "stock/board/read";
    }



    //글 수정
    @GetMapping("/{ticker}/edit/{id}")
    public String editForm(
            @PathVariable String ticker,
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails loginUser,
            Model model) {

        if (loginUser == null) {
            return "redirect:/user/login";
        }

        StockBoardDTO dto = stockBoardService.get(id);

        //권한 체크
        if (!dto.getWriter().equals(loginUser.getNickname())) {
            return "redirect:/stock/board/" + ticker + "/read/" + id;
        }

        model.addAttribute("dto", dto);
        model.addAttribute("ticker", ticker);

        return "stock/board/edit";
    }




    //글 수정 포스트
    @PostMapping("/{ticker}/edit/{id}")
    public String edit(
            @PathVariable String ticker,
            @PathVariable Long id,
            @ModelAttribute("dto") StockBoardDTO dto,
            @AuthenticationPrincipal CustomUserDetails loginUser) {

        if (loginUser == null) {
            return "redirect:/user/login";
        }

        dto.setId(id);
        dto.setTicker(ticker);
        dto.setWriter(loginUser.getNickname());

        stockBoardService.modify(dto);

        return "redirect:/stock/board/" + ticker + "/read/" + id;
    }




    //글 삭제
    @PostMapping("/{ticker}/delete/{id}")
    @ResponseBody
    public String delete(
            @PathVariable String ticker,
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails loginUser) {

        if (loginUser == null) {
            return "UNAUTHORIZED";
        }

        StockBoardDTO dto = stockBoardService.get(id);

        //권한 체크 (글쓴이 or 관리자)
        boolean isAdmin = loginUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !dto.getWriter().equals(loginUser.getNickname())) {
            return "FORBIDDEN";
        }

        stockBoardService.remove(id);

        return "OK";
    }




    //검색
    @GetMapping("/api/{ticker}")
    @ResponseBody
    public Page<StockBoardDTO> search(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "tc") String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return stockBoardService.getBoardListDTO(ticker, type, keyword, pageable);
    }


    //추천
    @PostMapping("/{ticker}/recommend/{id}")
    @ResponseBody
    public ResponseEntity<String> recommend(
            @PathVariable String ticker,
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails loginUser) {

        if (loginUser == null) {
            return ResponseEntity.status(401).body("UNAUTHORIZED");
        }

        boolean success = stockBoardService.addRecommend(id, loginUser.getId());

        if (!success) {
            return ResponseEntity.ok("DUPLICATE_RECOMMEND"); // 추천 중복
        }

        return ResponseEntity.ok("OK");
    }


    //비추
    @PostMapping("/{ticker}/unrecommend/{id}")
    @ResponseBody
    public ResponseEntity<String> unrecommend(
            @PathVariable String ticker,
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails loginUser) {

        if (loginUser == null) {
            return ResponseEntity.status(401).body("UNAUTHORIZED");
        }

        boolean success = stockBoardService.addUnrecommend(id, loginUser.getId());

        if (!success) {
            return ResponseEntity.ok("DUPLICATE_UNRECOMMEND"); // 비추천 중복
        }

        return ResponseEntity.ok("OK");
    }


    //핫한 글
    @GetMapping("/{ticker}/hot")
    @ResponseBody
    public Map<String, Object> getHotBoard(@PathVariable String ticker) {

        Map<String, Object> hotResult = new HashMap<>();

        hotResult.put("topRecommend", stockBoardService.getTopRecommend(ticker));

        hotResult.put("topComment", stockBoardService.getTopComment(ticker));

        return hotResult;
    }


}