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
            @AuthenticationPrincipal CustomUserDetails user){
        log.info("댓글 등록 요청: {}", dto);

        // 로그인 필요
        if (user == null) {
            return ResponseEntity.status(401).body("UNAUTHORIZED");
        }

        // 로그인 상태 → 작성자 자동 설정
        dto.setWriter(user.getNickname());


        if (dto.getParentId() != null) {
            dto.setDepth(1);
        } else {
            dto.setDepth(0);
        }

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
    public String remove(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails loginUser) {

        if (loginUser == null) {
            return "UNAUTHORIZED";
        }

        StockCommentDTO dto = stockCommentService.get(id);

        boolean isAdmin = loginUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // 작성자도 아니고 관리자도 아니면 차단
        if (!isAdmin && !dto.getWriter().equals(loginUser.getNickname())) {
            return "관리자 또는 작성자만 삭제할 수 있습니다.";
        }

        stockCommentService.remove(id);
        return "댓글이 삭제됐습니다.";
    }

    @GetMapping("/{boardId}/count")
    public int count(@PathVariable Long boardId) {
        return stockCommentService.getCountByBoard(boardId);
    }


}
