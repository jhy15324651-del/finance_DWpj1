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

        // ⭐ rating 검증 추가
        if (dto.getRating() == null || dto.getRating() == 0) {
            throw new IllegalArgumentException("평점이 필요합니다.");
        }

        ContentComment comment = ContentComment.builder()
                .postId(dto.getPostId())
                .userId(userId)
                .writer(nickname)
                .content(dto.getContent())
                .rating(dto.getRating())   // ⭐ 신규 추가
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

        if (user == null) return "NOT_LOGIN";

        ContentComment comment = commentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("댓글 없음"));

        ContentReview post = contentRepo.findById(comment.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));

        boolean isCommentWriter = comment.getUserId().equals(user.getId());
        boolean isPostWriter = post.getUserId().equals(user.getId());

        //권한 체크
        if (!isCommentWriter && !isPostWriter) {
            return "NO_PERMISSION";
        }
        // 삭제(원댓글/대댓글 동일 로직)
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

        // ⭐ 답글이면 수정 금지
        if (comment.getParentCommentId() != null) {
            return "REPLY_CANNOT_EDIT";
        }

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

    public List<ContentComment> getCommentsWithReplies(Long postId) {

        // 1) 원댓글 조회
        List<ContentComment> parentComments =
                commentRepo.findByPostIdAndParentCommentIdIsNullOrderByCreatedDateAsc(postId);

        // 2) 각 원댓글에 대해 대댓글 조회 & 셋팅
        for (ContentComment parent : parentComments) {
            List<ContentComment> replies =
                    commentRepo.findByParentCommentIdOrderByCreatedDateAsc(parent.getId());

            parent.setReplies(replies); // ⭐ 대댓글 붙이기
        }

        return parentComments;
    }

    @Transactional
    public void writeReply(Long userId, String nickname, ContentCommentWriteDTO dto) {

        if (dto.getParentCommentId() == null) {
            throw new IllegalArgumentException("부모 댓글 ID가 필요합니다.");
        }

        ContentComment reply = ContentComment.builder()
                .postId(dto.getPostId())                 // 어느 게시글인지
                .userId(userId)                          // 작성자 ID
                .writer(nickname)                        // 작성자 닉네임
                .content(dto.getContent())               // 대댓글 내용
                .rating(null)                            // ⭐ 대댓글은 rating 없음
                .parentCommentId(dto.getParentCommentId())  // 부모 댓글 ID 설정
                .build();

        commentRepo.save(reply);
    }



}
