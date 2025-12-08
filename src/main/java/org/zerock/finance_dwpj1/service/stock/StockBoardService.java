package org.zerock.finance_dwpj1.service.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.zerock.finance_dwpj1.dto.stock.StockBoardDTO;

public interface StockBoardService {

    // 종목별 게시글 목록
    Page<StockBoardDTO> getList(String ticker, Pageable pageable);

    // 글 읽기
    StockBoardDTO get(Long id);

    // 글 작성
    Long register(StockBoardDTO dto);

    // 글 수정
    void modify(StockBoardDTO dto);

    // 글 삭제
    void remove(Long id);


    //추천
    boolean addRecommend(Long id, Long userId);

    //비추천
   boolean addUnrecommend(Long id, Long userId);

    //조회수
    StockBoardDTO addView(Long id);

    //댓글 숫자
    int commentCount(Long boardId);
}