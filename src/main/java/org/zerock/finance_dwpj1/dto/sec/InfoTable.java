package org.zerock.finance_dwpj1.dto.sec;

import lombok.Data;

import javax.xml.bind.annotation.*;

/**
 * SEC 13F 개별 보유 종목 정보
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class InfoTable {

    @XmlElement(name = "nameOfIssuer")
    private String nameOfIssuer; // 회사명 (예: APPLE INC)

    @XmlElement(name = "titleOfClass")
    private String titleOfClass; // 증권 종류 (COM = 보통주)

    @XmlElement(name = "cusip")
    private String cusip; // CUSIP 코드 (037833100 = AAPL)

    @XmlElement(name = "value")
    private Long value; // 시장 가치 (USD, 천 단위)

    @XmlElement(name = "sshPrnamt")
    private Long sshPrnamt; // 보유 주식 수

    @XmlElement(name = "sshPrnamtType")
    private String sshPrnamtType; // SH = 주식

    @XmlElement(name = "investmentDiscretion")
    private String investmentDiscretion; // SOLE, SHARED, DEFINED

    @XmlElement(name = "otherManager")
    private String otherManager;

    @XmlElement(name = "votingAuthority")
    private VotingAuthority votingAuthority;
}
