package org.zerock.finance_dwpj1.service.portfolio.ocr;

/**
 * 증권사 타입 Enum
 * OCR 처리 시 증권사별 전처리 및 파싱 전략을 선택하기 위해 사용
 */
public enum BrokerType {
    /**
     * 토스 증권
     */
    TOSS("토스증권", "Toss Securities"),

    /**
     * 기본 (증권사 미지정 또는 자동 감지 실패)
     */
    DEFAULT("기본", "Default");

    private final String koreanName;
    private final String englishName;

    BrokerType(String koreanName, String englishName) {
        this.koreanName = koreanName;
        this.englishName = englishName;
    }

    public String getKoreanName() {
        return koreanName;
    }

    public String getEnglishName() {
        return englishName;
    }

    /**
     * 문자열로부터 BrokerType 찾기 (대소문자 무시)
     * @param value 증권사 이름 또는 코드
     * @return BrokerType, 찾지 못하면 DEFAULT 반환
     */
    public static BrokerType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT;
        }

        String normalized = value.trim().toUpperCase();

        try {
            return BrokerType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Enum 값으로 찾지 못한 경우, 한글명으로 검색
            for (BrokerType type : values()) {
                if (type.koreanName.equals(value) || type.englishName.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return DEFAULT;
        }
    }

    @Override
    public String toString() {
        return koreanName + " (" + name() + ")";
    }
}