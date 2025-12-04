package org.zerock.finance_dwpj1.dto.content;

import lombok.Data;
import org.zerock.finance_dwpj1.entity.content.ContentComment;

import java.util.ArrayList;
import java.util.List;

@Data
public class ContentCommentNodeDTO {

    private ContentComment comment;
    private List<ContentCommentNodeDTO> children = new ArrayList<>();

    public ContentCommentNodeDTO(ContentComment comment) {
        this.comment = comment;
    }
}
