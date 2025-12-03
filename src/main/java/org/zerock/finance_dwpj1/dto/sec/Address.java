package org.zerock.finance_dwpj1.dto.sec;

import lombok.Data;

import javax.xml.bind.annotation.*;

/**
 * 주소 정보
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Address {

    @XmlElement(name = "street1")
    private String street1;

    @XmlElement(name = "street2")
    private String street2;

    @XmlElement(name = "city")
    private String city;

    @XmlElement(name = "stateOrCountry")
    private String stateOrCountry;

    @XmlElement(name = "zipCode")
    private String zipCode;
}