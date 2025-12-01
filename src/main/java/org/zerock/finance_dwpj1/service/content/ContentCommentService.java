package org.zerock.finance_dwpj1.service.content;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zerock.finance_dwpj1.dto.content.ContentCommentWriteDTO;
import org.zerock.finance_dwpj1.entity.content.ContentComment;
import org.zerock.finance_dwpj1.repository.content.ContentCommentRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentCommentService {

    private final ContentCommentRepository repository;

    /**
     * 댓글 저장
     */
    public void write(Long userId, String nickname, ContentCommentWriteDTO dto) {

        ContentComment comment = ContentComment.builder()
                .postId(dto.getPostId())
                .userId(userId)
                .writer(nickname)
                .content(dto.getContent())
                .parentId(null)
                .build();

        repository.save(comment);
    }

    /**
     * 특정 게시글 댓글 목록 조회
     */
    public List<ContentComment> getComments(Long postId) {
        return repository.findByPostIdOrderByCreatedDateAsc(postId);
    }
}
