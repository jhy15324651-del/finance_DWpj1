package org.zerock.finance_dwpj1.dto.insights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightsNewsDTO {
    private String title;
    private String category;
    private String date;
    private String source;
    private String url;
    private List<String> keywords;
    private List<String> summary;
    private String originalText;
}