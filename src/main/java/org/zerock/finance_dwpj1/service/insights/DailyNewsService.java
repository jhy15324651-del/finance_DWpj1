package org.zerock.finance_dwpj1.service.insights;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.finance_dwpj1.dto.insights.InsightsCommentDTO;
import org.zerock.finance_dwpj1.dto.insights.InsightsDailyNewsDTO;
import org.zerock.finance_dwpj1.entity.insights.InsightsComment;
import org.zerock.finance_dwpj1.entity.insights.InsightsNews;
import org.zerock.finance_dwpj1.repository.insights.InsightsCommentRepository;
import org.zerock.finance_dwpj1.repository.insights.InsightsNewsRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 데일리 뉴스 비즈니스 로직 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DailyNewsService {

    private final InsightsNewsRepository newsRepository;
    private final InsightsCommentRepository commentRepository;
    private final org.zerock.finance_dwpj1.service.common.GPTService gptService;

    // Self-injection for REQUIRES_NEW transaction propagation
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private DailyNewsService self;

    /**
     * 데일리 뉴스 조회 (24시간 이내)
     */
    @Transactional(readOnly = true)
    public List<InsightsDailyNewsDTO> getDailyNews() {
        List<InsightsNews> newsList = newsRepository.findDailyNews();
        return newsList.stream()
                .map(news -> {
                    Long commentCount = commentRepository.countByNewsId(news.getId());
                    return InsightsDailyNewsDTO.fromEntity(news, commentCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * 데일리 뉴스 페이징 조회
     */
    @Transactional(readOnly = true)
    public Page<InsightsDailyNewsDTO> getDailyNews(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InsightsNews> newsPage = newsRepository.findDailyNews(pageable);

        return newsPage.map(news -> {
            Long commentCount = commentRepository.countByNewsId(news.getId());
            return InsightsDailyNewsDTO.fromEntity(news, commentCount);
        });
    }

    /**
     * 아카이브 뉴스 조회 (24시간 이상)
     */
    @Transactional(readOnly = true)
    public Page<InsightsDailyNewsDTO> getArchiveNews(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InsightsNews> newsPage = newsRepository.findArchiveNews(pageable);

        return newsPage.map(news -> {
            Long commentCount = commentRepository.countByNewsId(news.getId());
            return InsightsDailyNewsDTO.fromEntity(news, commentCount);
        });
    }

    /**
     * 금주의 뉴스 (조회수 TOP 10)
     */
    @Transactional(readOnly = true)
    public List<InsightsDailyNewsDTO> getWeeklyTopNews() {
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        Pageable top10 = PageRequest.of(0, 10);
        List<InsightsNews> topNews = newsRepository.findTopNewsByViewCount(weekAgo, top10);

        return topNews.stream()
                .map(news -> {
                    Long commentCount = commentRepository.countByNewsId(news.getId());
                    return InsightsDailyNewsDTO.fromEntity(news, commentCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * 뉴스 상세 조회 (조회수 증가)
     */
    @Transactional
    public InsightsDailyNewsDTO getNewsDetail(Long newsId) {
        InsightsNews news = newsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("뉴스를 찾을 수 없습니다: " + newsId));

        // 조회수 증가
        news.incrementViewCount();
        newsRepository.save(news);

        Long commentCount = commentRepository.countByNewsId(newsId);
        return InsightsDailyNewsDTO.fromEntity(news, commentCount);
    }

    /**
     * 뉴스 검색
     */
    @Transactional(readOnly = true)
    public Page<InsightsDailyNewsDTO> searchNews(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InsightsNews> newsPage = newsRepository.searchByTitle(keyword, pageable);

        return newsPage.map(news -> {
            Long commentCount = commentRepository.countByNewsId(news.getId());
            return InsightsDailyNewsDTO.fromEntity(news, commentCount);
        });
    }

    /**
     * 뉴스 수정 (관리자 전용)
     */
    @Transactional
    public InsightsDailyNewsDTO updateNews(Long newsId, InsightsDailyNewsDTO newsDTO) {
        InsightsNews news = newsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("뉴스를 찾을 수 없습니다: " + newsId));

        // 업데이트
        news.setTitle(newsDTO.getTitle());
        news.setContent(newsDTO.getContent());
        news.setSummary(newsDTO.getSummary());

        newsRepository.save(news);
        log.info("뉴스 수정 완료: {}", newsId);

        Long commentCount = commentRepository.countByNewsId(newsId);
        return InsightsDailyNewsDTO.fromEntity(news, commentCount);
    }

    /**
     * 뉴스 삭제 (관리자 전용) - 소프트 삭제
     */
    @Transactional
    public void deleteNews(Long newsId) {
        InsightsNews news = newsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("뉴스를 찾을 수 없습니다: " + newsId));

        news.softDelete();
        newsRepository.save(news);
        log.info("뉴스 삭제 완료: {}", newsId);
    }

    /**
     * 댓글 조회
     */
    @Transactional(readOnly = true)
    public List<InsightsCommentDTO> getComments(Long newsId) {
        List<InsightsComment> comments = commentRepository.findByNewsId(newsId);
        return comments.stream()
                .map(InsightsCommentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 댓글 작성 (일반 댓글 및 답글)
     */
    @Transactional
    public InsightsCommentDTO addComment(InsightsCommentDTO commentDTO) {
        InsightsNews news = newsRepository.findById(commentDTO.getNewsId())
                .orElseThrow(() -> new IllegalArgumentException("뉴스를 찾을 수 없습니다: " + commentDTO.getNewsId()));

        InsightsComment comment;

        // 답글인 경우
        if (commentDTO.getParentCommentId() != null) {
            InsightsComment parentComment = commentRepository.findById(commentDTO.getParentCommentId())
                    .orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다: " + commentDTO.getParentCommentId()));
            comment = commentDTO.toEntity(news, parentComment);
            log.info("답글 작성 완료 - 부모 댓글 ID: {}, 작성자: {}", parentComment.getId(), commentDTO.getUserName());
        } else {
            // 일반 댓글인 경우
            comment = commentDTO.toEntity(news);
            log.info("댓글 작성 완료 - 뉴스 ID: {}, 작성자: {}", news.getId(), commentDTO.getUserName());
        }

        commentRepository.save(comment);
        return InsightsCommentDTO.fromEntity(comment);
    }

    /**
     * 댓글 좋아요
     */
    @Transactional
    public InsightsCommentDTO likeComment(Long commentId) {
        InsightsComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        comment.incrementLike();
        commentRepository.save(comment);
        log.info("댓글 좋아요 - ID: {}, 현재 좋아요: {}", commentId, comment.getLikeCount());

        return InsightsCommentDTO.fromEntity(comment);
    }

    /**
     * 댓글 싫어요
     */
    @Transactional
    public InsightsCommentDTO dislikeComment(Long commentId) {
        InsightsComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        comment.incrementDislike();
        commentRepository.save(comment);
        log.info("댓글 싫어요 - ID: {}, 현재 싫어요: {}", commentId, comment.getDislikeCount());

        return InsightsCommentDTO.fromEntity(comment);
    }

    /**
     * 댓글 삭제 (관리자 전용) - 소프트 삭제
     */
    @Transactional
    public void deleteComment(Long commentId) {
        InsightsComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        comment.softDelete();
        commentRepository.save(comment);
        log.info("댓글 삭제 완료: {}", commentId);
    }

    /**
     * 샘플 뉴스 데이터 생성 (테스트용)
     */
    @Transactional
    public int createSampleNews() {
        String[] sampleTitles = {
            "Tech Stocks Rally as AI Revolution Continues",
            "Federal Reserve Hints at Rate Cuts in 2025",
            "Bitcoin Surges Past $100,000 Mark",
            "Major Oil Companies Report Record Profits",
            "Amazon Announces New AI-Powered Shopping Features",
            "Tesla's New Model Breaks Pre-Order Records",
            "Global Markets React to Economic Data Release",
            "Apple Unveils Revolutionary AR Glasses",
            "Inflation Shows Signs of Cooling Down",
            "Emerging Markets Attract Record Investment"
        };

        String[] sampleContents = {
            "Technology stocks experienced significant gains today as investors continue to bet on the artificial intelligence revolution. Major tech companies reported strong earnings driven by AI-related products and services.",
            "The Federal Reserve signaled potential interest rate cuts in 2025 during today's policy meeting. Chairman Powell emphasized the central bank's commitment to supporting economic growth while maintaining price stability.",
            "Bitcoin reached a historic milestone today, breaking through the $100,000 barrier for the first time. Cryptocurrency enthusiasts attribute the surge to institutional adoption and regulatory clarity.",
            "Leading oil companies posted exceptional quarterly profits, driven by elevated energy prices and strong global demand. Industry analysts predict continued strength in the energy sector.",
            "Amazon revealed its latest innovation in e-commerce with AI-powered shopping assistants that can predict customer needs and provide personalized recommendations in real-time.",
            "Tesla's newest electric vehicle model has shattered pre-order records, with over 500,000 reservations placed in the first 48 hours. The company plans to begin deliveries in Q3 2025.",
            "Stock markets worldwide showed mixed reactions to the latest economic indicators, with investors carefully analyzing data for signs of recession or continued growth.",
            "Apple unveiled its long-awaited augmented reality glasses at a special event today. The device promises to revolutionize how users interact with digital content in their daily lives.",
            "Latest inflation data suggests price pressures are easing, with core CPI showing its smallest monthly increase in two years. Economists see this as positive news for consumers and the economy.",
            "Emerging market economies attracted record levels of foreign investment this quarter, as investors seek higher returns and diversification opportunities in developing nations."
        };

        int savedCount = 0;

        for (int i = 0; i < sampleTitles.length; i++) {
            try {
                // 중복 체크 (URL 기반)
                String sampleUrl = "https://finance.sample.com/news/" + (System.currentTimeMillis() + i);
                if (newsRepository.existsByUrl(sampleUrl)) {
                    continue;
                }

                InsightsDailyNewsDTO newsDTO = InsightsDailyNewsDTO.builder()
                        .title(sampleTitles[i])
                        .content(sampleContents[i])
                        .summary("테스트 뉴스: " + sampleTitles[i].substring(0, Math.min(50, sampleTitles[i].length())) + "...")
                        .url(sampleUrl)
                        .source("Sample Finance News")
                        .build();

                InsightsNews news = newsDTO.toEntity();
                newsRepository.save(news);
                savedCount++;

                log.info("샘플 뉴스 생성: {}", sampleTitles[i]);

            } catch (Exception e) {
                log.error("샘플 뉴스 생성 중 오류: {}", sampleTitles[i], e);
            }
        }

        log.info("총 {}개의 샘플 뉴스가 생성되었습니다.", savedCount);
        return savedCount;
    }

    /**
     * 번역 안 된 뉴스 개수 조회
     */
    @Transactional(readOnly = true)
    public long getUntranslatedNewsCount() {
        return newsRepository.countUntranslatedNews();
    }

    /**
     * 번역 안 된 기존 뉴스 재번역 (기존 GPTService 재사용)
     * 데드락 방지: 각 뉴스를 독립 트랜잭션으로 처리, 배치 사이즈 제한
     * @return 재번역 성공/실패 카운트
     */
    public java.util.Map<String, Integer> retranslateUntranslatedNews() {
        log.info("===== 번역 안 된 뉴스 재번역 시작 =====");

        List<InsightsNews> untranslatedNews = newsRepository.findUntranslatedNews();
        log.info("번역 대상 뉴스: {}개", untranslatedNews.size());

        // 배치 사이즈 제한 (데드락 방지)
        int batchSize = 50;
        int processCount = Math.min(untranslatedNews.size(), batchSize);
        log.info("이번 배치 처리 개수: {}개 (배치 사이즈: {})", processCount, batchSize);

        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < processCount; i++) {
            InsightsNews news = untranslatedNews.get(i);

            try {
                // 각 뉴스를 독립적인 트랜잭션으로 처리 (데드락 방지)
                // self-injection을 통해 프록시 호출 (REQUIRES_NEW 트랜잭션 적용)
                boolean success = self.translateSingleNewsInNewTransaction(news);

                if (success) {
                    successCount++;
                } else {
                    failCount++;
                }

                // 데드락 방지를 위한 짧은 지연 (100ms)
                if (i < processCount - 1) {
                    Thread.sleep(100);
                }

            } catch (Exception e) {
                log.error("뉴스 재번역 중 오류 - ID: {}", news.getId(), e);
                failCount++;
            }
        }

        log.info("===== 재번역 완료 - 성공: {}개, 실패: {}개 =====", successCount, failCount);

        return java.util.Map.of(
            "success", successCount,
            "fail", failCount,
            "total", untranslatedNews.size(),
            "processed", processCount
        );
    }

    /**
     * 개별 뉴스를 독립적인 트랜잭션으로 번역 처리 (데드락 방지)
     * 주의: Spring AOP 프록시를 위해 public으로 선언 (self-injection 호출용)
     * @param news 번역할 뉴스 엔티티
     * @return 번역 성공 여부
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public boolean translateSingleNewsInNewTransaction(InsightsNews news) {
        try {
            String originalContent = news.getOriginalContent();
            if (originalContent == null || originalContent.isEmpty()) {
                log.warn("원문 없음 - 뉴스 ID: {}", news.getId());
                return false;
            }

            // 재시도 로직 (최대 2회)
            String translatedContent = null;

            for (int attempt = 1; attempt <= 2; attempt++) {
                try {
                    log.info("뉴스 ID: {} - 번역 시도 {}/2", news.getId(), attempt);

                    // 기존 GPTService 재사용
                    translatedContent = gptService.translateNewsToKorean(originalContent);

                    // 성공 시 break
                    if (translatedContent != null && !translatedContent.isEmpty()
                        && !translatedContent.equals(originalContent)) {
                        break;
                    }

                } catch (Exception e) {
                    log.warn("번역 실패 (시도 {}/2): {}", attempt, e.getMessage());

                    if (attempt < 2) {
                        Thread.sleep(500); // 500ms 대기 후 재시도
                    }
                }
            }

            // 번역 결과 저장
            if (translatedContent != null && !translatedContent.isEmpty()
                && !translatedContent.equals(originalContent)) {
                news.setContent(translatedContent);
                newsRepository.save(news);
                log.info("번역 성공 - 뉴스 ID: {}", news.getId());
                return true;
            } else {
                log.warn("번역 실패, 원문 유지 - 뉴스 ID: {}", news.getId());
                return false;
            }

        } catch (Exception e) {
            log.error("개별 뉴스 번역 중 오류 - ID: {}", news.getId(), e);
            return false;
        }
    }
}
