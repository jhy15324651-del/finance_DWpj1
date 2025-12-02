package org.zerock.finance_dwpj1.service.content;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.finance_dwpj1.dto.content.ContentCommentWriteDTO;
import org.zerock.finance_dwpj1.entity.content.ContentComment;
import org.zerock.finance_dwpj1.entity.content.ContentReview;
import org.zerock.finance_dwpj1.repository.content.ContentCommentRepository;
import org.zerock.finance_dwpj1.repository.content.ContentReviewRepository;
import org.zerock.finance_dwpj1.service.user.CustomUserDetails;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentCommentService {

    // 댓글 Repo
    private final ContentCommentRepository commentRepo;

    // 게시글 Repo (게시글 작성자 권한 확인용)
    private final ContentReviewRepository contentRepo;


    /**
     * 🔥 댓글 저장
     * - 로그인한 유저의 ID, 닉네임을 그대로 저장
     */
    public void write(Long userId, String nickname, ContentCommentWriteDTO dto) {

        ContentComment comment = ContentComment.builder()
                .postId(dto.getPostId())
                .userId(userId)
                .writer(nickname)
                .content(dto.getContent())
                .parentId(null)   // 대댓글은 추후 구현
                .build();

        commentRepo.save(comment);
    }


    /**
     * 🔥 특정 게시글의 댓글 전체 조회
     */
    public List<ContentComment> getComments(Long postId) {
        return commentRepo.findByPostIdOrderByCreatedDateAsc(postId);
    }


    /**
     * 🔥 댓글 삭제
     * - 댓글 작성자 O → 삭제 가능
     * - 게시글 작성자 O → 삭제 가능
     * - 그 외 사용자 → 삭제 불가
     */
    @Transactional
    public String deleteComment(Long id, CustomUserDetails user) {

        // 1) 댓글 조회
        ContentComment comment = commentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("댓글 없음"));

        // 2) 댓글이 달린 게시글 조회
        ContentReview post = contentRepo.findById(comment.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));

        // 3) 권한 체크
        boolean isCommentWriter = comment.getUserId().equals(user.getId());
        boolean isPostWriter = post.getUserId().equals(user.getId());

        if (!isCommentWriter && !isPostWriter) {
            return "NO_PERMISSION";
        }

        // 4) 삭제
        commentRepo.delete(comment);
        return "SUCCESS";
    }


    /**
     * 🔥 댓글 수정
     * - 댓글 작성자만 수정 가능
     */
    @Transactional
    public String editComment(Long id, String newContent, CustomUserDetails user) {

        // 1) 댓글 조회
        ContentComment comment = commentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("댓글 없음"));

        // 2) 권한 체크
        boolean isCommentWriter = comment.getUserId().equals(user.getId());
        if (!isCommentWriter) return "NO_PERMISSION";

        // 3) 수정
        comment.setContent(newContent);
        return "SUCCESS";
    }
}
