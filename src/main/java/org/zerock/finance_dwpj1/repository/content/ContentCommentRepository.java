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
}
