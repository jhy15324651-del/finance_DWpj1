package org.zerock.finance_dwpj1.dto.sec;

import lombok.Data;

import javax.xml.bind.annotation.*;

/**
 * 서명 블록 (사용하지 않을 수 있음)
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SignatureBlock {

    @XmlElement(name = "name")
    private String name;

    @XmlElement(name = "title")
    private String title;

    @XmlElement(name = "phone")
    private String phone;

    @XmlElement(name = "signature")
    private String signature;

    @XmlElement(name = "city")
    private String city;

    @XmlElement(name = "stateOrCountry")
    private String stateOrCountry;

    @XmlElement(name = "signatureDate")
    private String signatureDate;
}