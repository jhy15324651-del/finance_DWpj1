package org.zerock.finance_dwpj1.service.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.finance_dwpj1.entity.content.Newsletter;
import org.zerock.finance_dwpj1.repository.content.NewsletterRepository;

import java.util.List;

/**
 * Newsletter Service
 * 뉴스레터 비즈니스 로직 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsletterService {

    private final NewsletterRepository newsletterRepository;

    /**
     * 전체 뉴스레터 목록 조회 (최신순)
     */
    public List<Newsletter> getAllNewsletters() {
        log.debug("전체 뉴스레터 목록 조회");
        return newsletterRepository.findByIsDeletedFalseOrderByCreatedDateDesc();
    }

    /**
     * 뉴스레터 저장
     */
    @Transactional
    public Newsletter saveNewsletter(Newsletter newsletter) {
        log.debug("뉴스레터 저장: title={}", newsletter.getTitle());
        return newsletterRepository.save(newsletter);
    }

    /**
     * 뉴스레터 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteNewsletter(Long id) {
        log.debug("뉴스레터 삭제: id={}", id);

        Newsletter newsletter = newsletterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("뉴스레터를 찾을 수 없습니다: " + id));

        newsletter.softDelete();
        newsletterRepository.save(newsletter);
    }

    /**
     * 전체 뉴스레터 수 조회
     */
    public long getTotalCount() {
        log.debug("전체 뉴스레터 수 조회");
        return newsletterRepository.countByIsDeletedFalse();
    }
}
