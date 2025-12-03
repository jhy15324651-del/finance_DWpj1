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

    private final ContentCommentRepository commentRepo;
    private final ContentReviewRepository contentRepo;

    /**
     * 🔥 댓글 저장 (+ 평점 rating 저장 추가!)
     */
    public void write(Long userId, String nickname, ContentCommentWriteDTO dto) {

        ContentComment comment = ContentComment.builder()
                .postId(dto.getPostId())
                .userId(userId)
                .writer(nickname)
                .content(dto.getContent())
                .rating(dto.getRating())   // ⭐ 신규 추가
                .parentId(null)            // 대댓글은 추후 구현
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
     * - 댓글 작성자 OR 게시글 작성자 → 삭제 가능
     */
    @Transactional
    public String deleteComment(Long id, CustomUserDetails user) {

        ContentComment comment = commentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("댓글 없음"));

        ContentReview post = contentRepo.findById(comment.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));

        boolean isCommentWriter = comment.getUserId().equals(user.getId());
        boolean isPostWriter = post.getUserId().equals(user.getId());

        if (!isCommentWriter && !isPostWriter) {
            return "NO_PERMISSION";
        }

        commentRepo.delete(comment);
        return "SUCCESS";
    }

    /**
     * 🔥 댓글 수정
     * - 댓글 작성자만 수정 가능
     */
    @Transactional
    public String editComment(Long id, String newContent, Double newRating, CustomUserDetails user) {

        ContentComment comment = commentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("댓글 없음"));

        boolean isCommentWriter = comment.getUserId().equals(user.getId());
        if (!isCommentWriter) return "NO_PERMISSION";

        // 🔥 내용 수정
        comment.setContent(newContent);

        // 🔥 평점 수정 (반드시 추가!)
        comment.setRating(newRating);

        return "SUCCESS";
    }

    /**
     평균 평점 + 참여자 수 계산 추가
     */
    public double getAverageRating(Long postId) {

        List<ContentComment> list = commentRepo.findByPostIdOrderByCreatedDateAsc(postId);

        double sum = 0;
        int count = 0;

        for (ContentComment c : list) {
            if (c.getRating() != null) {
                sum += c.getRating();
                count++;
            }
        }

        if (count == 0) return 0.0;

        // ⭐ 반올림(0.5 단위)
        return Math.round((sum / count) * 2) / 2.0;
    }

    public int getRatingCount(Long postId) {
        return (int) commentRepo.countByPostIdAndRatingIsNotNull(postId);
    }



}
