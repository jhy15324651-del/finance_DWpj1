package org.zerock.finance_dwpj1.service.content;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zerock.finance_dwpj1.dto.content.ContentCommentNodeDTO;
import org.zerock.finance_dwpj1.entity.content.ContentComment;
import org.zerock.finance_dwpj1.repository.content.ContentCommentRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentCommentNodeService {

    private final ContentCommentRepository commentRepo;

    /**
     * ⭐ 특정 게시글의 댓글 트리를 생성하는 핵심 메소드
     */
    public List<ContentCommentNodeDTO> getCommentTree(Long postId) {

        // 1) 해당 게시글의 모든 댓글을 오래된 순으로 조회
        List<ContentComment> comments =
                commentRepo.findByPostIdOrderByCreatedDateAsc(postId);

        // 2) 모든 댓글을 DTO로 변환
        Map<Long, ContentCommentNodeDTO> nodeMap = new HashMap<>();
        for (ContentComment c : comments) {
            nodeMap.put(c.getId(), new ContentCommentNodeDTO(c));
        }

        // 3) 루트 댓글 리스트
        List<ContentCommentNodeDTO> roots = new ArrayList<>();

        // 4) 계층 구조 구성
        for (ContentComment c : comments) {
            ContentCommentNodeDTO node = nodeMap.get(c.getId());

            if (c.getParentId() == null) {
                // 부모가 없으면 → 루트 댓글
                roots.add(node);
            } else {
                // 부모가 있으면 → 부모의 children에 추가
                ContentCommentNodeDTO parentNode = nodeMap.get(c.getParentId());

                // 부모가 DB에서 삭제되었거나 없는 경우 방어 코드
                if (parentNode != null) {
                    parentNode.getChildren().add(node);
                }
            }
        }

        return roots;
    }

}
