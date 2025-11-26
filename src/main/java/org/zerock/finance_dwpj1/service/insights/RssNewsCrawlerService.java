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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * RSS 피드 뉴스 크롤러 서비스
 * 여러 금융 뉴스 소스의 RSS 피드를 통합하여 뉴스를 수집합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RssNewsCrawlerService {

    private static final int TIMEOUT = 10000; // 10초

    // RSS 피드 URL 목록
    private static final List<RssFeedSource> RSS_FEEDS = List.of(
            new RssFeedSource("Yahoo Finance", "https://finance.yahoo.com/news/rssindex"),
            new RssFeedSource("MarketWatch", "https://feeds.marketwatch.com/marketwatch/topstories/"),
            new RssFeedSource("Reuters Business", "https://www.reuters.com/rssfeed/businessNews"),
            new RssFeedSource("Investing.com", "https://www.investing.com/rss/news.rss"),
            new RssFeedSource("CNBC", "https://www.cnbc.com/id/100003114/device/rss/rss.html")
    );

    /**
     * 모든 RSS 피드에서 뉴스 수집
     *
     * @return 크롤링한 뉴스 목록
     */
    public List<DailyNewsDTO> crawlAllRssFeeds() {
        List<DailyNewsDTO> allNews = new ArrayList<>();

        for (RssFeedSource feed : RSS_FEEDS) {
            try {
                log.info("RSS 피드 크롤링 시작: {} ({})", feed.name, feed.url);
                List<DailyNewsDTO> news = crawlRssFeed(feed);
                allNews.addAll(news);
                log.info("{} RSS 피드에서 {}개 뉴스 수집", feed.name, news.size());
            } catch (Exception e) {
                log.error("{} RSS 피드 크롤링 실패: {}", feed.name, e.getMessage());
            }
        }

        log.info("전체 RSS 피드 크롤링 완료: 총 {}개 뉴스 수집", allNews.size());
        return allNews;
    }

    /**
     * 특정 RSS 피드에서 뉴스 수집
     *
     * @param feedSource RSS 피드 소스
     * @return 크롤링한 뉴스 목록
     */
    private List<DailyNewsDTO> crawlRssFeed(RssFeedSource feedSource) {
        List<DailyNewsDTO> newsList = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(feedSource.url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(TIMEOUT)
                    .parser(org.jsoup.parser.Parser.xmlParser()) // XML 파서 사용
                    .get();

            // RSS item 태그 선택
            Elements items = doc.select("item");

            for (Element item : items) {
                try {
                    String title = item.select("title").text();
                    String link = item.select("link").text();
                    String description = item.select("description").text();
                    String pubDate = item.select("pubDate").text();

                    // 빈 값 체크
                    if (title.isEmpty() || link.isEmpty()) {
                        continue;
                    }

                    // 발행 시간 파싱
                    LocalDateTime publishedAt = parsePublishDate(pubDate);

                    // DTO 생성
                    DailyNewsDTO newsDTO = DailyNewsDTO.builder()
                            .title(cleanHtmlTags(title))
                            .content(cleanHtmlTags(description))
                            .url(link)
                            .source(feedSource.name)
                            .publishedAt(publishedAt != null ?
                                    publishedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : null)
                            .build();

                    newsList.add(newsDTO);

                    // 각 소스당 최대 15개
                    if (newsList.size() >= 15) {
                        break;
                    }

                } catch (Exception e) {
                    log.warn("RSS 아이템 파싱 중 오류: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("RSS 피드 크롤링 중 오류: {}", feedSource.url, e);
        }

        return newsList;
    }

    /**
     * 발행 날짜 파싱
     */
    private LocalDateTime parsePublishDate(String pubDate) {
        if (pubDate == null || pubDate.isEmpty()) {
            return null;
        }

        try {
            // RFC 822 형식 (RSS 표준)
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME);
            return zonedDateTime.toLocalDateTime();
        } catch (Exception e1) {
            try {
                // ISO 8601 형식
                return LocalDateTime.parse(pubDate, DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e2) {
                log.warn("발행 날짜 파싱 실패: {}", pubDate);
                return null;
            }
        }
    }

    /**
     * HTML 태그 제거
     */
    private String cleanHtmlTags(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        return Jsoup.parse(html).text();
    }

    /**
     * RSS 피드 소스 정보
     */
    private record RssFeedSource(String name, String url) {
    }
}