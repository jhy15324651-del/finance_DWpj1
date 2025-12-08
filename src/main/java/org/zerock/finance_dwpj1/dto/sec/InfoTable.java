package org.zerock.finance_dwpj1.dto.sec;

import lombok.Data;

import javax.xml.bind.annotation.*;

/**
 * SEC 13F 개별 보유 종목 정보
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class InfoTable {

    @XmlElement(name = "nameOfIssuer", namespace = "http://www.sec.gov/edgar/document/thirteenf/informationtable")
    private String nameOfIssuer; // 회사명 (예: APPLE INC)

    @XmlElement(name = "titleOfClass", namespace = "http://www.sec.gov/edgar/document/thirteenf/informationtable")
    private String titleOfClass; // 증권 종류 (COM = 보통주)

    @XmlElement(name = "cusip", namespace = "http://www.sec.gov/edgar/document/thirteenf/informationtable")
    private String cusip; // CUSIP 코드 (037833100 = AAPL)

    @XmlElement(name = "value", namespace = "http://www.sec.gov/edgar/document/thirteenf/informationtable")
    private Long value; // 시장 가치 (USD, 천 단위)

    @XmlElement(name = "sshPrnamt", namespace = "http://www.sec.gov/edgar/document/thirteenf/informationtable")
    private Long sshPrnamt; // 보유 주식 수

    @XmlElement(name = "sshPrnamtType", namespace = "http://www.sec.gov/edgar/document/thirteenf/informationtable")
    private String sshPrnamtType; // SH = 주식

    @XmlElement(name = "investmentDiscretion", namespace = "http://www.sec.gov/edgar/document/thirteenf/informationtable")
    private String investmentDiscretion; // SOLE, SHARED, DEFINED

    @XmlElement(name = "otherManager", namespace = "http://www.sec.gov/edgar/document/thirteenf/informationtable")
    private String otherManager;

    @XmlElement(name = "votingAuthority", namespace = "http://www.sec.gov/edgar/document/thirteenf/informationtable")
    private VotingAuthority votingAuthority;
}
