package org.zerock.finance_dwpj1.dto.sec;

import lombok.Data;

import javax.xml.bind.annotation.*;

/**
 * SEC 13F FormData 엘리먼트
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class FormData {

    @XmlElement(name = "coverPage")
    private CoverPage coverPage;

    @XmlElement(name = "informationTable")
    private InformationTable informationTable;

    @XmlElement(name = "signatureBlock")
    private SignatureBlock signatureBlock;
}