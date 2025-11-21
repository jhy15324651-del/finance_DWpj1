package org.zerock.finance_dwpj1.service.insights;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.finance_dwpj1.dto.insights.DailyNewsDTO;
import org.zerock.finance_dwpj1.entity.insights.News;
import org.zerock.finance_dwpj1.repository.insights.NewsRepository;
import org.zerock.finance_dwpj1.service.common.GPTService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 뉴스 스케줄러 서비스
 * 1시간마다 뉴스를 크롤링하고, 24시간 경과한 뉴스를 아카이브 처리합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NewsSchedulerService {

    private final YahooFinanceCrawlerService crawlerService;
    private final NewsRepository newsRepository;
    private final GPTService gptService;

    /**
     * 하루에 한 번 뉴스 크롤링
     * cron: 매일 오전 9시에 실행
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void crawlNewsDaily() {
        log.info("===== 일일 뉴스 크롤링 시작 (매일 오전 9시) =====");

        try {
            // Yahoo Finance에서 최신 뉴스 크롤링
            List<DailyNewsDTO> crawledNews = crawlerService.crawlLatestNews();
            log.info("크롤링한 뉴스 개수: {}", crawledNews.size());

            int savedCount = 0;
            int duplicateCount = 0;

            for (DailyNewsDTO newsDTO : crawledNews) {
                try {
                    // 중복 체크 (URL 기준)
                    if (newsRepository.existsByUrl(newsDTO.getUrl())) {
                        log.debug("중복 뉴스 스킵: {}", newsDTO.getUrl());
                        duplicateCount++;
                        continue;
                    }

                    // GPT 요약 생성 (실패해도 계속 진행)
                    try {
                        String summary = generateSummary(newsDTO.getContent());
                        newsDTO.setSummary(summary);
                    } catch (Exception gptError) {
                        log.warn("GPT 요약 생성 실패, 기본 요약 사용: {}", gptError.getMessage());
                        newsDTO.setSummary("GPT API 키를 설정하면 자동 요약이 생성됩니다.");
                    }

                    // Entity로 변환 후 저장
                    News newsEntity = newsDTO.toEntity();
                    newsRepository.save(newsEntity);

                    savedCount++;
                    log.info("뉴스 저장 완료: {}", newsDTO.getTitle());

                } catch (Exception e) {
                    log.error("뉴스 저장 중 오류: {}", newsDTO.getTitle(), e);
                }
            }

            log.info("===== 크롤링 완료 - 저장: {}개, 중복: {}개 =====", savedCount, duplicateCount);

        } catch (Exception e) {
            log.error("뉴스 크롤링 중 오류 발생", e);
        }
    }

    /**
     * 하루에 한 번 24시간 경과한 뉴스를 아카이브 처리
     * cron: 매일 오전 9시 10분에 실행
     */
    @Scheduled(cron = "0 10 9 * * *")
    @Transactional
    public void archiveOldNews() {
        log.info("===== 24시간 경과 뉴스 아카이브 처리 시작 =====");

        try {
            LocalDateTime threshold = LocalDateTime.now().minusHours(24);
            List<News> newsToArchive = newsRepository.findNewsToArchive(threshold);

            log.info("아카이브 대상 뉴스: {}개", newsToArchive.size());

            for (News news : newsToArchive) {
                news.archiveNews();
                log.debug("뉴스 아카이브 처리: {}", news.getTitle());
            }

            newsRepository.saveAll(newsToArchive);
            log.info("===== 아카이브 처리 완료: {}개 =====", newsToArchive.size());

        } catch (Exception e) {
            log.error("아카이브 처리 중 오류 발생", e);
        }
    }

    /**
     * GPT를 사용하여 뉴스 요약 생성
     *
     * @param content 뉴스 본문
     * @return GPT 요약
     */
    private String generateSummary(String content) {
        try {
            if (content == null || content.isEmpty()) {
                return "요약을 생성할 수 없습니다.";
            }

            // 내용이 너무 길면 앞부분만 사용 (GPT 토큰 제한)
            String truncatedContent = content.length() > 2000 ?
                    content.substring(0, 2000) + "..." : content;

            String prompt = String.format(
                    "다음 금융 뉴스를 3-4문장으로 요약해주세요. 핵심 내용과 시장 영향을 포함해주세요.\n\n%s",
                    truncatedContent
            );

            return gptService.generateResponse(prompt);

        } catch (Exception e) {
            log.error("GPT 요약 생성 중 오류", e);
            return "요약 생성 실패";
        }
    }

    /**
     * 수동으로 크롤링 실행 (테스트용)
     */
    public void manualCrawl() {
        log.info("수동 크롤링 시작");
        crawlNewsDaily();
    }

    /**
     * 수동으로 아카이브 처리 실행 (테스트용)
     */
    public void manualArchive() {
        log.info("수동 아카이브 처리 시작");
        archiveOldNews();
    }

    /**
     * 크롤러만 테스트 (GPT 없이, DB 저장 없이)
     * @return 크롤링한 뉴스 목록
     */
    public List<DailyNewsDTO> testCrawlerOnly() {
        log.info("===== 크롤러 테스트 시작 (GPT/DB 비활성화) =====");

        try {
            List<DailyNewsDTO> crawledNews = crawlerService.crawlLatestNews();
            log.info("크롤러 테스트 완료 - 발견한 뉴스: {}개", crawledNews.size());

            // 발견한 뉴스의 제목만 로그 출력
            for (int i = 0; i < Math.min(crawledNews.size(), 5); i++) {
                log.info("뉴스 {}: {}", i + 1, crawledNews.get(i).getTitle());
            }

            return crawledNews;

        } catch (Exception e) {
            log.error("크롤러 테스트 중 오류 발생", e);
            throw e;
        }
    }
}
