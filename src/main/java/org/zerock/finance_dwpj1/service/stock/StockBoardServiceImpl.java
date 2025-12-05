package org.zerock.finance_dwpj1.service.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class StockBoardServiceImpl implements StockBoardService {

    private final StockBoardRepository stockBoardRepository;
    private final StockGechuRepository stockGechuRepository;
    private final StockBichuRepository stockBichuRepository;


    @Override
    @Transactional(readOnly = true)
    public Page<StockBoardDTO> getList(String ticker, Pageable pageable) {

        Page<StockBoard> result = stockBoardRepository
                .findByTickerOrderByIdDesc(ticker, pageable);

        // 엔티티 → DTO 변환
        return result.map(this::entityToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public StockBoardDTO get(Long id) {
        StockBoard board = stockBoardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 글입니다."));

        return entityToDto(board);
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

        return entityToDto(board);
    }


    //추천
    @Override
    public void addRecommend(Long id, Long userId){
        StockBoard board = stockBoardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));

        boolean exist = stockGechuRepository
                .findByBoardIdAndUserId(id, userId)
                .isPresent();

        if(exist){
            return;
        }

        board.setRecommend(board.getRecommend() + 1);

        stockGechuRepository.save(
                StockGechu.builder()
                        .board(board)
                        .userId(userId)
                        .build()
        );
    }

    //비추천
    @Override
    public void addUnrecommend(Long id, Long userId){
        StockBoard board = stockBoardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));

        boolean exist = stockBichuRepository
                .findByBoardIdAndUserId(id, userId)
                .isPresent();

        if(exist){
            return;
        }

        board.setUnrecommend(board.getUnrecommend() + 1);

        stockBichuRepository.save(
                StockBichu.builder()
                        .board(board)
                        .userId(userId)
                        .build()
        );
    }




//    @Override
//    public void addRecommend(Long boardId, Long memberId){
//
//        if (voteService.hasVoted(boardId, memberId)){
//            throw new IllegalStateException("이미 추천한 글입니다.");
//        }
//
//        StockBoard board = stockBoardRepository.findById(boardId);
//
//    }


    // ======== 변환 메서드 ========

    private StockBoardDTO entityToDto(StockBoard board) {
        return StockBoardDTO.builder()
                .id(board.getId())
                .ticker(board.getTicker())
                .title(board.getTitle())
                .content(board.getContent())
                .writer(board.getWriter())
                .regDate(board.getRegDate())
                .modDate(board.getModDate())
                .view(board.getView())
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