package org.zerock.finance_dwpj1.controller.content;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.service.content.ContentCommentService;
import org.zerock.finance_dwpj1.service.user.CustomUserDetails;

@RestController
@RequiredArgsConstructor
@RequestMapping("/content/comment")
public class ContentCommentController {

    private final ContentCommentService commentService;

    /** 댓글 삭제 */
    @DeleteMapping("/{id}")
    public String deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        if (user == null) return "NOT_LOGIN";
        return commentService.deleteComment(id, user);
    }

    /** 댓글 수정 */
    @PutMapping("/{id}")
    public String editComment(
            @PathVariable Long id,
            @RequestBody CommentEditRequest dto,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        if (user == null) return "NOT_LOGIN";
        return commentService.editComment(id, dto.getContent(), user);
    }
}

/** 댓글 수정 요청 DTO */
@Getter
@Setter
class CommentEditRequest {
    private String content;
}
