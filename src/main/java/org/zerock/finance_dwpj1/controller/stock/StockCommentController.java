package org.zerock.finance_dwpj1.controller.stock;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.dto.stock.StockCommentDTO;
import org.zerock.finance_dwpj1.service.stock.StockCommentService;
import org.zerock.finance_dwpj1.service.user.CustomUserDetails;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/comments")
public class StockCommentController {

    private final StockCommentService stockCommentService;

    //댓글 등록
    @PostMapping
    public ResponseEntity<?> register(
            @RequestBody StockCommentDTO dto,
            @AuthenticationPrincipal CustomUserDetails user
    ){
        log.info("댓글 등록 요청: {}", dto);

        // 🔒 로그인 필요
        if (user == null) {
            return ResponseEntity.status(401).body("UNAUTHORIZED");
        }

        // 🔥 로그인 상태 → 작성자 자동 설정
        dto.setWriter(user.getNickname());

        Long commentId = stockCommentService.register(dto);

        return ResponseEntity.ok(commentId);
    }

    //글 댓글 목록 조회
    @GetMapping("/{boardId}")
    public List<StockCommentDTO> getList(@PathVariable Long boardId) {
        log.info("댓글 목록 조회: boardId={}", boardId);
        return stockCommentService.getListByBoard(boardId);
    }

    @PutMapping("/{id}")
    public String modify(@PathVariable Long id, @RequestBody StockCommentDTO dto){
        log.info("댓글 수정", id, dto);
        dto.setId(id);
        stockCommentService.modify(dto);
        return "수정 완료";
    }

    @DeleteMapping("/{id}")
    public String remove(@PathVariable Long id){
        log.info("댓글 삭제", id);
        stockCommentService.remove(id);
        return "삭제 완료";
    }

    @GetMapping("/{boardId}/count")
    public int count(@PathVariable Long boardId) {
        return stockCommentService.getCountByBoard(boardId);
    }


}
