package org.zerock.finance_dwpj1.service.insights;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.zerock.finance_dwpj1.dto.insights.DailyNewsDTO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Yahoo Finance 뉴스 크롤러 서비스
 * Yahoo Finance에서 최신 금융 뉴스를 크롤링합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YahooFinanceCrawlerService {

    private static final String YAHOO_FINANCE_URL = "https://finance.yahoo.com/topic/stock-market-news/";
    private static final int TIMEOUT = 10000; // 10초

    /**
     * Yahoo Finance에서 최신 뉴스 크롤링
     *
     * @return 크롤링한 뉴스 목록 (DailyNewsDTO)
     */
    public List<DailyNewsDTO> crawlLatestNews() {
        List<DailyNewsDTO> newsList = new ArrayList<>();

        try {
            log.info("Yahoo Finance 뉴스 크롤링 시작: {}", YAHOO_FINANCE_URL);

            Document doc = Jsoup.connect(YAHOO_FINANCE_URL)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(TIMEOUT)
                    .get();

            // 뉴스 목록 선택자 (여러 선택자 시도)
            Elements newsItems = doc.select("div.js-stream-content a");

            // 선택자가 작동하지 않으면 대체 선택자 사용
            if (newsItems.isEmpty()) {
                log.warn("첫 번째 선택자로 뉴스를 찾지 못함. 대체 선택자 시도...");
                newsItems = doc.select("a[data-test='article-link']");
            }

            if (newsItems.isEmpty()) {
                log.warn("두 번째 선택자로도 뉴스를 찾지 못함. 세 번째 선택자 시도...");
                newsItems = doc.select("h3 a");
            }

            log.info("크롤링한 뉴스 항목 개수: {} (선택자 결과)", newsItems.size());

            for (Element newsItem : newsItems) {
                try {
                    String url = newsItem.attr("abs:href");

                    // 유효한 뉴스 URL인지 확인
                    if (url.isEmpty() || !url.contains("yahoo.com/news")) {
                        continue;
                    }

                    // 제목 추출
                    String title = newsItem.text();
                    if (title.isEmpty()) {
                        continue;
                    }

                    // 뉴스 상세 페이지 크롤링
                    DailyNewsDTO newsDTO = crawlNewsDetail(url, title);
                    if (newsDTO != null) {
                        newsList.add(newsDTO);
                    }

                    // 너무 많은 요청 방지 (최대 20개)
                    if (newsList.size() >= 20) {
                        break;
                    }

                } catch (Exception e) {
                    log.error("개별 뉴스 크롤링 중 오류: {}", e.getMessage());
                }
            }

            log.info("크롤링 완료. 총 {}개 뉴스 수집", newsList.size());

        } catch (Exception e) {
            log.error("Yahoo Finance 크롤링 중 오류 발생", e);
        }

        return newsList;
    }

    /**
     * 뉴스 상세 페이지 크롤링
     *
     * @param url 뉴스 URL
     * @param title 뉴스 제목
     * @return DailyNewsDTO
     */
    private DailyNewsDTO crawlNewsDetail(String url, String title) {
        try {
            log.debug("뉴스 상세 크롤링: {}", url);

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(TIMEOUT)
                    .get();

            // 본문 내용 추출 (Yahoo Finance 구조에 맞게 조정)
            StringBuilder contentBuilder = new StringBuilder();
            Elements paragraphs = doc.select("div.caas-body p");

            for (Element p : paragraphs) {
                contentBuilder.append(p.text()).append("\n\n");
            }

            String content = contentBuilder.toString().trim();

            // 발행 시간 추출 (메타 태그에서)
            String publishedTime = doc.select("meta[property=article:published_time]")
                    .attr("content");

            LocalDateTime publishedAt = null;
            if (!publishedTime.isEmpty()) {
                try {
                    publishedAt = LocalDateTime.parse(
                            publishedTime,
                            DateTimeFormatter.ISO_DATE_TIME
                    );
                } catch (Exception e) {
                    log.warn("발행 시간 파싱 실패: {}", publishedTime);
                }
            }

            // DTO 생성 (GPT 요약은 나중에 추가)
            return DailyNewsDTO.builder()
                    .title(title)
                    .content(content.isEmpty() ? "내용을 가져올 수 없습니다." : content)
                    .url(url)
                    .source("Yahoo Finance")
                    .publishedAt(publishedAt != null ?
                            publishedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : null)
                    .build();

        } catch (Exception e) {
            log.error("뉴스 상세 페이지 크롤링 실패: {}", url, e);
            return null;
        }
    }

    /**
     * 특정 키워드로 뉴스 검색 및 크롤링
     *
     * @param keyword 검색 키워드
     * @return 크롤링한 뉴스 목록
     */
    public List<DailyNewsDTO> crawlNewsByKeyword(String keyword) {
        List<DailyNewsDTO> newsList = new ArrayList<>();

        try {
            String searchUrl = "https://finance.yahoo.com/search?p=" + keyword;
            log.info("키워드 검색 크롤링 시작: {}", searchUrl);

            Document doc = Jsoup.connect(searchUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(TIMEOUT)
                    .get();

            // 검색 결과에서 뉴스 링크 추출
            Elements newsLinks = doc.select("a[href*=/news/]");

            for (Element link : newsLinks) {
                String url = link.attr("abs:href");
                String title = link.text();

                if (url.isEmpty() || title.isEmpty()) {
                    continue;
                }

                DailyNewsDTO newsDTO = crawlNewsDetail(url, title);
                if (newsDTO != null) {
                    newsList.add(newsDTO);
                }

                if (newsList.size() >= 10) {
                    break;
                }
            }

        } catch (Exception e) {
            log.error("키워드 검색 크롤링 중 오류: {}", keyword, e);
        }

        return newsList;
    }
}
