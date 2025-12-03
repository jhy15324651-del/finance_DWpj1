package org.zerock.finance_dwpj1.dto.sec;

import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * SEC 13F XML 루트 엘리먼트
 */
@Data
@XmlRootElement(name = "edgarSubmission")
@XmlAccessorType(XmlAccessType.FIELD)
public class EdgarSubmission {

    @XmlElement(name = "formData")
    private List<FormData> formDataList;

    public InformationTable getInformationTable() {
        if (formDataList != null) {
            for (FormData formData : formDataList) {
                if (formData.getInformationTable() != null) {
                    return formData.getInformationTable();
                }
            }
        }
        return null;
    }

    public CoverPage getCoverPage() {
        if (formDataList != null) {
            for (FormData formData : formDataList) {
                if (formData.getCoverPage() != null) {
                    return formData.getCoverPage();
                }
            }
        }
        return null;
    }
}