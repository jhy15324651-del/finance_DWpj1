package org.zerock.finance_dwpj1.dto.insights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zerock.finance_dwpj1.entity.insights.InsightsTwitter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightsTwitterDTO {
    private Long id;
    private String name;
    private String handle;
    private String avatar;
    private boolean verified;
    private String originalText;
    private String translatedText;
    private String date;
    private String url;
    private String source; // DUMMY, MANUAL, API

    /**
     * Entity -> DTO 변환
     */
    public static InsightsTwitterDTO fromEntity(InsightsTwitter entity) {
        return InsightsTwitterDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .handle(entity.getHandle())
                .avatar(entity.getAvatar())
                .verified(entity.getVerified())
                .originalText(entity.getOriginalText())
                .translatedText(entity.getTranslatedText())
                .date(entity.getTweetDate())
                .url(entity.getUrl())
                .source(entity.getSource().name())
                .build();
    }

    /**
     * DTO -> Entity 변환
     */
    public InsightsTwitter toEntity() {
        return InsightsTwitter.builder()
                .name(this.name)
                .handle(this.handle)
                .avatar(this.avatar)
                .verified(this.verified)
                .originalText(this.originalText)
                .translatedText(this.translatedText)
                .tweetDate(this.date)
                .url(this.url)
                .source(this.source != null
                    ? InsightsTwitter.TwitterSource.valueOf(this.source)
                    : InsightsTwitter.TwitterSource.MANUAL)
                .build();
    }
}