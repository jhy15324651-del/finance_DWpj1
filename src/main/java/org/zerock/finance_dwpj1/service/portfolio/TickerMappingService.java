package org.zerock.finance_dwpj1.service.portfolio;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 한국어 종목명 → 영어 티커 매핑 서비스
 * OCR 후처리 레이어로 사용
 */
@Service
@Slf4j
public class TickerMappingService {

    // 한국어 종목명 → 영어 티커 매핑 테이블 (원본)
    private static final Map<String, String> KOREAN_TO_TICKER = new HashMap<>();

    // 정규화된 한국어 → 영어 티커 매핑 테이블 (공백/숫자/특수문자 제거)
    private static final Map<String, String> NORMALIZED_MAPPING = new HashMap<>();

    static {
        // 주요 미국 주식 (한국어 → 영어)
        KOREAN_TO_TICKER.put("애플", "AAPL");
        KOREAN_TO_TICKER.put("테슬라", "TSLA");
        KOREAN_TO_TICKER.put("마이크로소프트", "MSFT");
        KOREAN_TO_TICKER.put("엔비디아", "NVDA");
        KOREAN_TO_TICKER.put("아마존", "AMZN");
        KOREAN_TO_TICKER.put("알파벳", "GOOGL");
        KOREAN_TO_TICKER.put("구글", "GOOGL");
        KOREAN_TO_TICKER.put("메타", "META");
        KOREAN_TO_TICKER.put("페이스북", "META");
        KOREAN_TO_TICKER.put("넷플릭스", "NFLX");
        KOREAN_TO_TICKER.put("AMD", "AMD");
        KOREAN_TO_TICKER.put("인텔", "INTC");
        KOREAN_TO_TICKER.put("코카콜라", "KO");
        KOREAN_TO_TICKER.put("나이키", "NKE");
        KOREAN_TO_TICKER.put("맥도날드", "MCD");
        KOREAN_TO_TICKER.put("스타벅스", "SBUX");
        KOREAN_TO_TICKER.put("월마트", "WMT");
        KOREAN_TO_TICKER.put("JP모건", "JPM");
        KOREAN_TO_TICKER.put("제이피모건", "JPM");
        KOREAN_TO_TICKER.put("뱅크오브아메리카", "BAC");
        KOREAN_TO_TICKER.put("비자", "V");
        KOREAN_TO_TICKER.put("마스터카드", "MA");
        KOREAN_TO_TICKER.put("디즈니", "DIS");
        KOREAN_TO_TICKER.put("보잉", "BA");
        KOREAN_TO_TICKER.put("존슨앤존슨", "JNJ");
        KOREAN_TO_TICKER.put("화이자", "PFE");
        KOREAN_TO_TICKER.put("모더나", "MRNA");
        KOREAN_TO_TICKER.put("버크셔해서웨이", "BRK.B");
        KOREAN_TO_TICKER.put("팔란티어", "PLTR");
        KOREAN_TO_TICKER.put("우버", "UBER");
        KOREAN_TO_TICKER.put("에어비앤비", "ABNB");
        KOREAN_TO_TICKER.put("스냅", "SNAP");
        KOREAN_TO_TICKER.put("트위터", "TWTR");
        KOREAN_TO_TICKER.put("X", "X");
        KOREAN_TO_TICKER.put("스포티파이", "SPOT");
        KOREAN_TO_TICKER.put("세 노 버스 에너지","CVE");
        KOREAN_TO_TICKER.put("일루미나","ILMN");
        KOREAN_TO_TICKER.put(" 유나이티드 파 셀 서비스","UPS");
        KOREAN_TO_TICKER.put("써클 인터넷 그룹","CRCL");
        KOREAN_TO_TICKER.put("노 보 노 디 스 크","NVO");
        KOREAN_TO_TICKER.put("코카콜라","KO");
        KOREAN_TO_TICKER.put("아이온큐","IONQ");



        // 한국 주식 (기업 이름 → 코드)
        KOREAN_TO_TICKER.put("삼성전자", "005930.KS");
        KOREAN_TO_TICKER.put("SK하이닉스", "000660.KS");
        KOREAN_TO_TICKER.put("네이버", "035420.KS");
        KOREAN_TO_TICKER.put("카카오", "035720.KS");
        KOREAN_TO_TICKER.put("LG전자", "066570.KS");
        KOREAN_TO_TICKER.put("현대차", "005380.KS");
        KOREAN_TO_TICKER.put("기아", "000270.KS");
        KOREAN_TO_TICKER.put("POSCO", "005490.KS");
        KOREAN_TO_TICKER.put("포스코", "005490.KS");
        KOREAN_TO_TICKER.put("셀트리온", "068270.KS");
        KOREAN_TO_TICKER.put("삼성바이오로직스", "207940.KS");
        KOREAN_TO_TICKER.put("삼성물산", "028260.KS");
        KOREAN_TO_TICKER.put("삼성SDI", "006400.KS");
        KOREAN_TO_TICKER.put("LG화학", "051910.KS");
        KOREAN_TO_TICKER.put("LG에너지솔루션", "373220.KS");
        KOREAN_TO_TICKER.put("KB금융", "105560.KS");
        KOREAN_TO_TICKER.put("신한지주", "055550.KS");
        KOREAN_TO_TICKER.put("하나금융지주", "086790.KS");
        KOREAN_TO_TICKER.put("우리금융지주", "316140.KS");
        KOREAN_TO_TICKER.put("NH투자증권", "005940.KS");
        KOREAN_TO_TICKER.put("삼성생명", "032830.KS");
        KOREAN_TO_TICKER.put("SK이노베이션", "096770.KS");
        KOREAN_TO_TICKER.put("SK텔레콤", "017670.KS");
        KOREAN_TO_TICKER.put("KT", "030200.KS");
        KOREAN_TO_TICKER.put("LG유플러스", "032640.KS");

        // 중국 주식
        KOREAN_TO_TICKER.put("알리바바", "BABA");
        KOREAN_TO_TICKER.put("텐센트", "TCEHY");
        KOREAN_TO_TICKER.put("바이두", "BIDU");
        KOREAN_TO_TICKER.put("JD닷컴", "JD");
        KOREAN_TO_TICKER.put("징동", "JD");
        KOREAN_TO_TICKER.put("니오", "NIO");

        // 정규화된 매핑 테이블 생성
        for (Map.Entry<String, String> entry : KOREAN_TO_TICKER.entrySet()) {
            String normalized = normalize(entry.getKey());
            NORMALIZED_MAPPING.put(normalized, entry.getValue());
        }

        log.info("티커 매핑 데이터 로드 완료: 원본 {}개, 정규화 {}개",
                KOREAN_TO_TICKER.size(), NORMALIZED_MAPPING.size());
    }

    /**
     * 문자열 정규화 (공백, 숫자, 특수문자 제거)
     * @param text 원본 텍스트
     * @return 정규화된 텍스트 (한글과 영어만 남김)
     */
    private static String normalize(String text) {
        if (text == null) return "";

        // 1. 공백 제거
        // 2. 숫자 제거
        // 3. 특수문자 제거 (괄호, 쉼표, 마침표 등)
        // 4. 한글, 영어 대문자만 남김
        return text.replaceAll("\\s+", "")           // 공백 제거
                   .replaceAll("\\d+", "")           // 숫자 제거
                   .replaceAll("[^가-힣A-Za-z]", "") // 한글+영어만 남김
                   .toUpperCase();                   // 대문자 변환
    }

    /**
     * 한국어 종목명 → 영어 티커로 변환 (정규화 기반 매칭)
     * @param koreanName 한국어 종목명
     * @return 영어 티커 (매핑 없으면 원본 그대로 반환)
     */
    public String mapToTicker(String koreanName) {
        if (koreanName == null || koreanName.trim().isEmpty()) {
            return koreanName;
        }

        String trimmed = koreanName.trim();

        // 1) 정확히 일치하는 매핑이 있으면 반환 (원본 우선)
        if (KOREAN_TO_TICKER.containsKey(trimmed)) {
            String ticker = KOREAN_TO_TICKER.get(trimmed);
            log.info("✓ 티커 매핑(정확일치): '{}' → '{}'", trimmed, ticker);
            return ticker;
        }

        // 2) 🔥 정규화된 문자열로 매칭 (공백/숫자/특수문자 제거)
        String normalized = normalize(trimmed);
        if (NORMALIZED_MAPPING.containsKey(normalized)) {
            String ticker = NORMALIZED_MAPPING.get(normalized);
            log.info("✓ 티커 매핑(정규화): '{}' (정규화: '{}') → '{}'", trimmed, normalized, ticker);
            return ticker;
        }

        // 3) 영어 티커인지 확인 (이미 AAPL, TSLA 같은 형태)
        if (isEnglishTicker(trimmed)) {
            log.debug("영어 티커로 판단: '{}' (원본 유지)", trimmed);
            return trimmed;
        }

        // 4) 대소문자 구분 없이 검색
        for (Map.Entry<String, String> entry : KOREAN_TO_TICKER.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(trimmed)) {
                String ticker = entry.getValue();
                log.info("✓ 티커 매핑(대소문자무시): '{}' → '{}'", trimmed, ticker);
                return ticker;
            }
        }

        // 5) 부분 매칭 (포함 관계) - 최후 수단
        for (Map.Entry<String, String> entry : KOREAN_TO_TICKER.entrySet()) {
            if (trimmed.contains(entry.getKey()) || entry.getKey().contains(trimmed)) {
                String ticker = entry.getValue();
                log.info("✓ 티커 매핑(부분일치): '{}' → '{}'", trimmed, ticker);
                return ticker;
            }
        }

        // 6) 매핑 없으면 원본 그대로 반환
        log.warn("⚠ 티커 매핑 실패: '{}' (정규화: '{}') - 원본 유지", trimmed, normalized);
        return trimmed;
    }

    /**
     * 티커가 이미 영어 티커인지 확인
     * @param ticker 티커
     * @return 영어 티커 여부 (대문자 알파벳만으로 구성)
     */
    public boolean isEnglishTicker(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            return false;
        }

        String trimmed = ticker.trim();

        // 영어 티커 패턴: 2~6자 대문자 알파벳 (예: AAPL, GOOGL)
        // 또는 한국/중국 주식 코드 (예: 005930.KS, BABA)
        return trimmed.matches("^[A-Z]{2,6}$") || trimmed.matches("^[0-9]{6}\\.[A-Z]{2}$");
    }

    /**
     * 매핑 데이터 추가 (런타임 확장 가능)
     * @param koreanName 한국어 종목명
     * @param ticker 영어 티커
     */
    public void addMapping(String koreanName, String ticker) {
        KOREAN_TO_TICKER.put(koreanName, ticker);
        log.info("새 티커 매핑 추가: '{}' → '{}'", koreanName, ticker);
    }

    /**
     * 전체 매핑 데이터 조회
     * @return 매핑 Map (읽기 전용)
     */
    public Map<String, String> getAllMappings() {
        return new HashMap<>(KOREAN_TO_TICKER);
    }

    /**
     * OCR 텍스트 전체에서 한국어 종목명을 영어 티커로 치환
     * @param ocrText OCR로 추출된 원본 텍스트
     * @return 한국어 종목명이 영어 티커로 치환된 텍스트
     */
    public String applyMappingToText(String ocrText) {
        if (ocrText == null || ocrText.trim().isEmpty()) {
            return ocrText;
        }

        String result = ocrText;
        int replacementCount = 0;

        // 1. 원본 매핑 테이블로 정확 매칭 치환 (긴 종목명부터 처리)
        List<Map.Entry<String, String>> sortedEntries = new ArrayList<>(KOREAN_TO_TICKER.entrySet());
        sortedEntries.sort((e1, e2) -> Integer.compare(e2.getKey().length(), e1.getKey().length()));

        for (Map.Entry<String, String> entry : sortedEntries) {
            String koreanName = entry.getKey();
            String ticker = entry.getValue();

            if (result.contains(koreanName)) {
                result = result.replace(koreanName, ticker);
                replacementCount++;
                log.info("✓ OCR 치환: '{}' → '{}'", koreanName, ticker);
            }
        }

        // 2. 정규화 기반 매칭 (공백/숫자/특수문자 포함 케이스 처리)
        // 각 라인별로 처리하여 정규화된 패턴 매칭
        String[] lines = result.split("\\r?\\n");
        StringBuilder finalResult = new StringBuilder();

        for (String line : lines) {
            String processedLine = line;

            // 라인에서 한국어가 포함되어 있으면 정규화 매칭 시도
            if (containsKorean(line)) {
                for (Map.Entry<String, String> entry : sortedEntries) {
                    String koreanName = entry.getKey();
                    String ticker = entry.getValue();
                    String normalizedKorean = normalize(koreanName);

                    // 라인을 정규화한 후 매칭되는지 확인
                    String normalizedLine = normalize(line);
                    if (normalizedLine.contains(normalizedKorean)) {
                        // 원본 라인에서 한국어 부분을 찾아서 치환
                        // (정규화 전 원본 텍스트에서 한국어만 추출)
                        String koreanPart = extractKoreanPart(line, normalizedKorean);
                        if (koreanPart != null && !koreanPart.equals(ticker)) {
                            processedLine = processedLine.replace(koreanPart, ticker);
                            replacementCount++;
                            log.info("✓ OCR 정규화 치환: '{}' → '{}'", koreanPart, ticker);
                        }
                    }
                }
            }

            finalResult.append(processedLine).append("\n");
        }

        log.info("🔥 OCR 텍스트 티커 치환 완료: {}개 치환", replacementCount);
        return finalResult.toString().trim();
    }

    /**
     * 문자열에 한국어가 포함되어 있는지 확인
     */
    private boolean containsKorean(String text) {
        if (text == null) return false;
        return text.matches(".*[가-힣]+.*");
    }

    /**
     * 라인에서 정규화된 패턴에 해당하는 한국어 부분을 추출
     * @param line 원본 라인
     * @param normalizedPattern 정규화된 패턴
     * @return 매칭되는 한국어 부분 (없으면 null)
     */
    private String extractKoreanPart(String line, String normalizedPattern) {
        // 라인에서 연속된 한국어 + 공백 + 숫자 + 특수문자 부분을 추출
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[가-힣\\s\\d().,]+");
        java.util.regex.Matcher matcher = pattern.matcher(line);

        while (matcher.find()) {
            String candidate = matcher.group().trim();
            String normalizedCandidate = normalize(candidate);

            if (normalizedCandidate.contains(normalizedPattern) ||
                normalizedPattern.contains(normalizedCandidate)) {
                return candidate;
            }
        }

        return null;
    }
}