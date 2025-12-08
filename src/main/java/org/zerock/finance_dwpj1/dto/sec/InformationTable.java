package org.zerock.finance_dwpj1.dto.sec;

import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * SEC 13F 보유 종목 테이블
 * 실제 포트폴리오 데이터가 들어있는 핵심 부분
 */
@Data
@XmlRootElement(name = "informationTable", namespace = "http://www.sec.gov/edgar/document/thirteenf/informationtable")
@XmlAccessorType(XmlAccessType.FIELD)
public class InformationTable {

    @XmlElement(name = "infoTable", namespace = "http://www.sec.gov/edgar/document/thirteenf/informationtable")
    private List<InfoTable> infoTables;
}