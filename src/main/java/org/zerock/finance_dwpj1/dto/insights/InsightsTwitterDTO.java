package org.zerock.finance_dwpj1.dto.insights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightsTwitterDTO {
    private String name;
    private String handle;
    private String avatar;
    private boolean verified;
    private String originalText;
    private String translatedText;
    private String date;
    private String url;
}