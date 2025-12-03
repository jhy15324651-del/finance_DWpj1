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
   // void addRecommend(Long id);

    //비추천
   // void addUnrecommend(Long id);

    //조회수
    //void addview(Long id);
}