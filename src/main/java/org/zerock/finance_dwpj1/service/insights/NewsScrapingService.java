package org.zerock.finance_dwpj1.service.insights;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.zerock.finance_dwpj1.dto.insights.NewsDTO;
import org.zerock.finance_dwpj1.service.common.GPTService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class NewsScrapingService {

    private final GPTService gptService;

    public List<NewsDTO> scrapeYahooFinanceNews(String category) {
        List<NewsDTO> newsList = new ArrayList<>();

        try {
            String url = category.equals("hot-topics")
                ? "https://finance.yahoo.com/topic/stock-market-news/"
                : "https://finance.yahoo.com/";

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();

            // 야후 파이낸스 뉴스 선택자 (실제로는 사이트 구조에 따라 조정 필요)
            Elements newsElements = doc.select("div.Ov\\(h\\) > h3 > a, article h3 a");

            int count = 0;
            for (Element newsElement : newsElements) {
                if (count >= 10) break; // 최대 10개까지

                try {
                    String title = newsElement.text();
                    String newsUrl = newsElement.attr("abs:href");

                    if (title.isEmpty() || newsUrl.isEmpty()) continue;

                    // 뉴스 상세 페이지에서 본문 가져오기
                    String content = fetchNewsContent(newsUrl);

                    // GPT API로 번역 및 요약
                    String gptResponse = gptService.translateAndSummarizeNews(
                            "Title: " + title + "\n\nContent: " + content.substring(0, Math.min(500, content.length())));

                    NewsDTO newsDTO = parseGPTResponse(gptResponse, newsUrl, category);
                    if (newsDTO != null) {
                        newsList.add(newsDTO);
                        count++;
                    }
                } catch (Exception e) {
                    log.error("뉴스 처리 오류", e);
                }
            }
        } catch (Exception e) {
            log.error("뉴스 스크래핑 오류", e);
        }

        // 실제 데이터가 없을 경우 샘플 데이터 반환
        if (newsList.isEmpty()) {
            newsList = getSampleNews(category);
        }

        return newsList;
    }

    private String fetchNewsContent(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();

            Element body = doc.selectFirst("div.caas-body, article");
            return body != null ? body.text() : "";
        } catch (Exception e) {
            log.error("뉴스 본문 가져오기 오류", e);
            return "";
        }
    }

    private NewsDTO parseGPTResponse(String gptResponse, String url, String category) {
        try {
            String[] lines = gptResponse.split("\n");
            String title = "";
            List<String> keywords = new ArrayList<>();
            List<String> summary = new ArrayList<>();

            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("제목:")) {
                    title = line.substring(3).trim();
                } else if (line.startsWith("키워드:")) {
                    String keywordStr = line.substring(4).trim();
                    keywords = Arrays.asList(keywordStr.split(",\\s*"));
                } else if (line.startsWith("요약")) {
                    String summaryLine = line.substring(line.indexOf(":") + 1).trim();
                    summary.add(summaryLine);
                }
            }

            return NewsDTO.builder()
                    .title(title.isEmpty() ? "뉴스 제목" : title)
                    .category(category.equals("hot-topics") ? "핫토픽" : "금주의 뉴스")
                    .date(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .source("Yahoo Finance")
                    .url(url)
                    .keywords(keywords.isEmpty() ? Arrays.asList("금융", "시장", "투자") : keywords)
                    .summary(summary.isEmpty() ? Arrays.asList("뉴스 요약 정보", "주요 내용", "시장 전망") : summary)
                    .build();
        } catch (Exception e) {
            log.error("GPT 응답 파싱 오류", e);
            return null;
        }
    }

    private List<NewsDTO> getSampleNews(String category) {
        List<NewsDTO> sampleNews = new ArrayList<>();

        String cat = category.equals("hot-topics") ? "핫토픽" : "금주의 뉴스";

        sampleNews.add(NewsDTO.builder()
                .title("연준, 금리 동결 결정...시장 안정 신호")
                .category(cat)
                .date("2025-01-15")
                .source("Yahoo Finance")
                .url("https://finance.yahoo.com")
                .keywords(Arrays.asList("연준", "금리", "통화정책"))
                .summary(Arrays.asList(
                        "연방준비제도가 기준금리를 현행 수준으로 유지하기로 결정",
                        "인플레이션 둔화 추세를 반영한 신중한 접근",
                        "시장은 긍정적으로 반응하며 주요 지수 상승"))
                .build());

        sampleNews.add(NewsDTO.builder()
                .title("엔비디아, AI 칩 수요 급증으로 실적 기대 이상")
                .category(cat)
                .date("2025-01-14")
                .source("Yahoo Finance")
                .url("https://finance.yahoo.com")
                .keywords(Arrays.asList("엔비디아", "AI", "반도체"))
                .summary(Arrays.asList(
                        "엔비디아가 AI 칩 수요 급증으로 분기 실적 예상치 초과 달성",
                        "데이터센터 부문 매출이 전년 대비 200% 이상 증가",
                        "향후 AI 시장 성장에 따른 추가 성장 전망"))
                .build());

        sampleNews.add(NewsDTO.builder()
                .title("테슬라, 자율주행 기술 업데이트 발표")
                .category(cat)
                .date("2025-01-13")
                .source("Yahoo Finance")
                .url("https://finance.yahoo.com")
                .keywords(Arrays.asList("테슬라", "자율주행", "FSD"))
                .summary(Arrays.asList(
                        "테슬라가 완전자율주행(FSD) 베타 버전 최신 업데이트 공개",
                        "도심 주행 안전성과 편의성이 크게 향상",
                        "2025년 내 완전 자율주행 상용화 목표"))
                .build());

        return sampleNews;
    }
}