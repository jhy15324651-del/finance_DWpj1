package org.zerock.finance_dwpj1.repository.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zerock.finance_dwpj1.entity.content.ContentComment;

import java.util.List;

@Repository
public interface ContentCommentRepository extends JpaRepository<ContentComment, Long> {

    /**
     * 특정 게시글의 댓글 목록 조회 (오래된 순)
     */
    List<ContentComment> findByPostIdOrderByCreatedDateAsc(Long postId);

    int countByPostIdAndRatingIsNotNull(Long postId);

    /**
     * 내가 쓴 댓글(마이페이지)
     */
    List<ContentComment> findByWriter(String writer);

    /**
     * 원댓글(부모 없는 댓글) 조회
     */
    List<ContentComment> findByPostIdAndParentCommentIdIsNullOrderByCreatedDateAsc(Long postId);

    /**
     * 특정 댓글의 대댓글(답글) 조회
     */
    List<ContentComment> findByParentCommentIdOrderByCreatedDateAsc(Long parentCommentId);


}
