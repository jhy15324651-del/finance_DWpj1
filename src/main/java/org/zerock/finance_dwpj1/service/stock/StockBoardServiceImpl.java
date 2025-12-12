package org.zerock.finance_dwpj1.service.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.finance_dwpj1.dto.stock.StockBoardDTO;
import org.zerock.finance_dwpj1.entity.stock.StockBichu;
import org.zerock.finance_dwpj1.entity.stock.StockBoard;
import org.zerock.finance_dwpj1.entity.stock.StockGechu;
import org.zerock.finance_dwpj1.repository.stock.StockBichuRepository;
import org.zerock.finance_dwpj1.repository.stock.StockBoardRepository;
import org.zerock.finance_dwpj1.repository.stock.StockGechuRepository;
import org.zerock.finance_dwpj1.service.stock.StockCommentService;
import org.zerock.finance_dwpj1.service.user.UserService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StockBoardServiceImpl implements StockBoardService {

    private final StockBoardRepository stockBoardRepository;
    private final StockGechuRepository stockGechuRepository;
    private final StockBichuRepository stockBichuRepository;
    private final StockCommentService stockCommentService;
    private final UserService userService;
    private final StockGradeCalculatorService stockGradeCalculatorService;


    @Override
    @Transactional(readOnly = true)
    public Page<StockBoardDTO> getList(String ticker, Pageable pageable) {

        Page<StockBoard> result = stockBoardRepository
                .findByTickerOrderByIdDesc(ticker, pageable);

        // 엔티티 → DTO 변환
        return result.map(this::entityToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public StockBoardDTO get(Long id) {
        StockBoard board = stockBoardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 글입니다."));

        return entityToDTO(board);
    }

    @Override
    public Long register(StockBoardDTO dto) {
        StockBoard board = dtoToEntity(dto);
        stockBoardRepository.save(board);
        return board.getId();
    }

    @Override
    public void modify(StockBoardDTO dto) {
        StockBoard board = stockBoardRepository.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 글입니다."));

        board.setTitle(dto.getTitle());
        board.setContent(dto.getContent());
        // 더티 체킹으로 자동 update
    }

    @Override
    public void remove(Long id) {
        stockBoardRepository.deleteById(id);
    }



    //조회수 증가
    @Override
    public StockBoardDTO addView(Long id) {
        StockBoard board = stockBoardRepository.findById(id).orElseThrow();
        board.setView(board.getView() + 1);

        return entityToDTO(board);
    }


    //추천
    @Override
    public boolean addRecommend(Long id, Long userId){
        StockBoard board = stockBoardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));

        boolean exist = stockGechuRepository
                .findByBoardIdAndUserId(id, userId)
                .isPresent();

        if(exist){
            return false;  // 중복 체크
        }

        board.setRecommend(board.getRecommend() + 1);

        stockGechuRepository.save(
                StockGechu.builder()
                        .board(board)
                        .userId(userId)
                        .build()
        );

        return true;  // 추천 성공
    }

    //비추천
    @Override
    public boolean addUnrecommend(Long id, Long userId){
        StockBoard board = stockBoardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));

        boolean exist = stockBichuRepository
                .findByBoardIdAndUserId(id, userId)
                .isPresent();

        if(exist){
            return false;  // 중복 체크
        }

        board.setUnrecommend(board.getUnrecommend() + 1);

        stockBichuRepository.save(
                StockBichu.builder()
                        .board(board)
                        .userId(userId)
                        .build()
        );

        return true; //비추천 성공
    }


    @Override
    public int commentCount(Long boardId) {

        return stockCommentService.getCountByBoard(boardId);
    }

    //개추순위권찾기
    @Override
    public List<StockBoardDTO> getTopRecommend(String ticker) {

        LocalDateTime since = LocalDateTime.now().minusDays(3);

        Pageable limit3 = PageRequest.of(0, 3);

        return stockBoardRepository.findTopRecommend(ticker, since, limit3)
                .stream()
                .map(this::entityToDTO)
                .toList();
    }


    //댓글순위권찾기
    @Override
    public List<StockBoardDTO> getTopComment(String ticker) {

        LocalDateTime since = LocalDateTime.now().minusDays(3);

        Pageable limit3 = PageRequest.of(0, 3);

        return stockBoardRepository.findTopComment(ticker, since, limit3)
                .stream()
                .map(this::entityToDTO)
                .toList();
    }





    private StockBoardDTO entityToDTO(StockBoard board) {

        Long writerId = userService.getUserIdByNickname(board.getWriter());

        String grade = userService.getUserGrade(writerId);

        String medal = stockGradeCalculatorService.gradeToEmoji(grade);

        return StockBoardDTO.builder()
                .id(board.getId())
                .ticker(board.getTicker())
                .title(board.getTitle())
                .content(board.getContent())
                .writer(board.getWriter())
                .grade(grade)
                .medal(medal)
                .regDate(board.getRegDate())
                .modDate(board.getModDate())
                .view(board.getView())
                .commentCount(commentCount(board.getId()))
                .recommend(board.getRecommend())
                .unrecommend(board.getUnrecommend())
                .build();
    }

    private StockBoard dtoToEntity(StockBoardDTO dto) {
        return StockBoard.builder()
                .id(dto.getId())
                .ticker(dto.getTicker())
                .title(dto.getTitle())
                .content(dto.getContent())
                .writer(dto.getWriter())
                .build();
    }
}