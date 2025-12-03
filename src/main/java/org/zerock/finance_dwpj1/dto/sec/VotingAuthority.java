package org.zerock.finance_dwpj1.dto.sec;

import lombok.Data;

import javax.xml.bind.annotation.*;

/**
 * 의결권 정보 (사용하지 않을 수 있음)
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class VotingAuthority {

    @XmlElement(name = "Sole")
    private Long sole;

    @XmlElement(name = "Shared")
    private Long shared;

    @XmlElement(name = "None")
    private Long none;
}
