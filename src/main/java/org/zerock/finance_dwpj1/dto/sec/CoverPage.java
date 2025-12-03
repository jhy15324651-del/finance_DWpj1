package org.zerock.finance_dwpj1.dto.sec;

import lombok.Data;

import javax.xml.bind.annotation.*;

/**
 * SEC 13F CoverPage (보고서 메타데이터)
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class CoverPage {

    @XmlElement(name = "reportCalendarOrQuarter")
    private String reportCalendarOrQuarter; // 예: 12-31-2023

    @XmlElement(name = "filingManager")
    private FilingManager filingManager;

    @XmlElement(name = "reportType")
    private String reportType; // 13F-HR 또는 13F-HR/A
}