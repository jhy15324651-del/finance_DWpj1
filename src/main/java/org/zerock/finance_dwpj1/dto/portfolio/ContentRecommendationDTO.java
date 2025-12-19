package org.zerock.finance_dwpj1.dto.portfolio;

import lombok.*;
import org.zerock.finance_dwpj1.entity.content.ContentReview;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 콘텐츠 추천 응답 DTO
 * 포트폴리오 분석 페이지에서 사용자에게 추천할 콘텐츠 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentRecommendationDTO {

    private Long id;
    private String title;
    private String categoryLabel;       // 배지용 카테고리
    private Double rating;              // 평점
    private List<String> hashtags;      // 해시태그 리스트
    private String thumbnailUrl;        // 썸네일
    private String authorName;          // 작성자
    private String detailUrl;           // 상세 URL

    // 디버그용 (optional)
    private Double recommendationScore; // 추천 점수
    private List<String> matchedTags;   // 매칭된 태그
    private List<String> matchedTickers; // 매칭된 티커
    private String matchedInvestor;      // 매칭된 투자대가

    /**
     * ContentReview 엔티티에서 DTO로 변환
     *
     * @param content ContentReview 엔티티
     * @param ratingAvg 평점 (별도 계산된 값)
     * @return ContentRecommendationDTO
     */
    public static ContentRecommendationDTO fromEntity(ContentReview content, Double ratingAvg) {
        // 해시태그 파싱 ("#태그1 #태그2" → ["태그1", "태그2"])
        List<String> hashtagList = parseHashtags(content.getHashtags());

        return ContentRecommendationDTO.builder()
                .id(content.getId())
                .title(content.getTitle())
                .categoryLabel(determineCategoryLabel(content.getType()))
                .rating(ratingAvg != null ? ratingAvg : 0.0)
                .hashtags(hashtagList)
                .thumbnailUrl(content.getThumbnail())
                .authorName(content.getWriter())
                .detailUrl("/content/post/" + content.getId())
                .build();
    }

    /**
     * 해시태그 문자열을 리스트로 파싱
     *
     * @param hashtags "#태그1 #태그2 #태그3" 형식
     * @return ["태그1", "태그2", "태그3"]
     */
    private static List<String> parseHashtags(String hashtags) {
        if (hashtags == null || hashtags.isEmpty()) {
            return List.of();
        }

        return Arrays.stream(hashtags.split("#"))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 타입에 따라 카테고리 라벨 결정
     *
     * @param type 콘텐츠 타입
     * @return 한글 카테고리 라벨
     */
    private static String determineCategoryLabel(String type) {
        if (type == null) {
            return "콘텐츠";
        }

        return switch (type.toLowerCase()) {
            case "review" -> "투자 리뷰";
            case "strategy" -> "투자 전략";
            case "analysis" -> "시장 분석";
            case "news" -> "뉴스";
            case "tutorial" -> "투자 입문";
            default -> "콘텐츠";
        };
    }

    /**
     * 디버그 정보 설정
     */
    public void setDebugInfo(Double score, List<String> matchedTags,
                             List<String> matchedTickers, String matchedInvestor) {
        this.recommendationScore = score;
        this.matchedTags = matchedTags;
        this.matchedTickers = matchedTickers;
        this.matchedInvestor = matchedInvestor;
    }
}