package org.zerock.finance_dwpj1.dto.sec;

import lombok.Data;

import javax.xml.bind.annotation.*;

/**
 * 보고서 제출 기관 정보
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class FilingManager {

    @XmlElement(name = "name")
    private String name; // BERKSHIRE HATHAWAY INC

    @XmlElement(name = "address")
    private Address address;
}