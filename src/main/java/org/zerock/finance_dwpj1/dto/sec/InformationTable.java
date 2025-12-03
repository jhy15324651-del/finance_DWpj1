package org.zerock.finance_dwpj1.dto.sec;

import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * SEC 13F 보유 종목 테이블
 * 실제 포트폴리오 데이터가 들어있는 핵심 부분
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class InformationTable {

    @XmlElement(name = "infoTable")
    private List<InfoTable> infoTables;
}