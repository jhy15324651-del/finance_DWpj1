package org.zerock.finance_dwpj1.service.stock;


import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zerock.finance_dwpj1.dto.stock.StockCommentDTO;
import org.zerock.finance_dwpj1.entity.stock.StockBoard;
import org.zerock.finance_dwpj1.entity.stock.StockComment;
import org.zerock.finance_dwpj1.repository.stock.StockCommentRepository;
import org.zerock.finance_dwpj1.service.user.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StockCommentServiceImpl implements StockCommentService {

    private final StockCommentRepository stockCommentRepository;
    private final UserService userService;
    private final StockGradeCalculatorService stockGradeCalculatorService;




    private StockComment dtoToEntity(StockCommentDTO dto) {

        return StockComment.builder()
                .id(dto.getId())
                .board(StockBoard.builder()
                        .id(dto.getBoardId())
                        .build())
                .writer(dto.getWriter())
                .content(dto.getContent())
                .regDate(dto.getRegDate())
                .modDate(dto.getModDate())
                .build();
    }

    private StockCommentDTO entityToDto(StockComment comment) {

        Long writerId = userService.getUserIdByNickname(comment.getWriter());

        String grade = userService.getUserGrade(writerId);

        String medal = stockGradeCalculatorService.gradeToEmoji(grade);



        return StockCommentDTO.builder()
                .id(comment.getId())
                .boardId(comment.getBoard().getId())
                .writer(comment.getWriter())
                .content(comment.getContent())
                .regDate(comment.getRegDate())
                .modDate(comment.getModDate())
                .grade(grade)
                .medal(medal)
                .build();
    }

    @Override
    public Long register(StockCommentDTO dto) {
        StockComment comment = dtoToEntity(dto);
        StockComment saved = stockCommentRepository.save(comment);
        return saved.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockCommentDTO> getListByBoard(Long boardId){
        return stockCommentRepository.findByBoard_IdOrderByIdAsc(boardId)
                .stream()
                .map(this::entityToDto)
               .collect(Collectors.toList());
    }

    @Override
    public void modify(StockCommentDTO dto){
        StockComment comment = stockCommentRepository.findById(dto.getId())
                .orElseThrow(()-> new IllegalArgumentException("댓글이 없습니다."));
        comment.setContent(dto.getContent());
        comment.setModDate(LocalDateTime.now());

        stockCommentRepository.save(comment);
    }

    @Override
    public void remove(Long id){
        stockCommentRepository.deleteById(id);
    }



    //댓글 갯수
    @Override
    public int getCountByBoard(Long boardId) {
        return stockCommentRepository.countByBoard_Id(boardId);

    }


}
