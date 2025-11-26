package org.zerock.finance_dwpj1.service.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.finance_dwpj1.dto.stock.StockBoardDTO;
import org.zerock.finance_dwpj1.entity.stock.StockBoard;
import org.zerock.finance_dwpj1.repository.stock.StockBoardRepository;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class StockBoardServiceImpl implements StockBoardService {

    private final StockBoardRepository stockBoardRepository;

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
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 글입니다. id=" + id));

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
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 글입니다. id=" + dto.getId()));

        board.setTitle(dto.getTitle());
        board.setContent(dto.getContent());
        // writer, ticker은 보통 수정 안 함

        // 더티 체킹으로 자동 update
    }

    @Override
    public void remove(Long id) {
        stockBoardRepository.deleteById(id);
    }

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