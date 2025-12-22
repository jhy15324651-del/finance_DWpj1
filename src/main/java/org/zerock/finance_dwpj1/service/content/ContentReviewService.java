package org.zerock.finance_dwpj1.service.content;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jmx.export.notification.NotificationPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.zerock.finance_dwpj1.entity.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.finance_dwpj1.entity.content.ContentComment;
import org.zerock.finance_dwpj1.entity.content.ContentReview;
import org.zerock.finance_dwpj1.entity.notification.Notification;
import org.zerock.finance_dwpj1.entity.notification.NotificationType;
import org.zerock.finance_dwpj1.entity.user.Role;
import org.zerock.finance_dwpj1.repository.content.ContentCommentRepository;
import org.zerock.finance_dwpj1.repository.content.ContentReviewRepository;
import org.zerock.finance_dwpj1.repository.notification.NotificationRepository;
import org.zerock.finance_dwpj1.repository.user.UserRepository;
import org.zerock.finance_dwpj1.service.notification.NotificationPolicyService;
import org.zerock.finance_dwpj1.service.user.CustomUserDetails;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
@Getter
@Setter
public class ContentReviewService {

    private final ContentReviewRepository repo;
    private final ContentCommentRepository commentRepo;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationPolicyService notificationPolicyService;


    // writer 기준으로 게시글 조회
    public List<ContentReview> getReviewsByWriter(String writer) {
        return repo
                .findByWriterAndIsDeletedFalseOrderByCreatedDateDesc(writer);
    }


    // ---------------------------------------------------------
    // 🔥 preview 생성 유틸 (HTML 제거 + 길이 제한)
    // ---------------------------------------------------------
    private String makePreview(String content) {
        if (content == null) return "";

        // 1) HTML 태그 제거
        String plain = content.replaceAll("<[^>]*>", "");

        // 2) 길이 제한 (원하면 100~150 사이로 조절 가능)
        if (plain.length() > 120) {
            return plain.substring(0, 120) + "...";
        }
        return plain;
    }


    // ---------------------------------------------------------
    // 🔥 최신/인기 게시글 (preview 자동 세팅)
    // ---------------------------------------------------------
    public List<ContentReview> getLatestContents() {
        List<ContentReview> list =
                repo.findTop8ByIsDeletedFalseOrderByCreatedDateDesc();

        // ⭐ preview 세팅
        list.forEach(post -> {
            post.setPreview(makePreview(post.getContent()));
            post.setRatingAvg(getAverageRating(post.getId()));
        });

        return list;
    }

    public List<ContentReview> getPopularContents() {
        List<ContentReview> list =
                repo.findTop5ByIsDeletedFalseOrderByViewCountDesc();

        // ⭐ preview 세팅
        list.forEach(post -> {
            post.setPreview(makePreview(post.getContent()));
            post.setRatingAvg(getAverageRating(post.getId()));
        });
        return list;
    }

    // ---------------------------------------------------------
    // 🔥 추천 콘텐츠 전용 메서드 (ratingAvg + 가중치 점수 계산해서 상위 N개 뽑는 메서드)
    // ---------------------------------------------------------
    public List<ContentReview> getRecommendedContents(int limit) {

        // 1️⃣ DB에서 후보군 30개 가져오기
        List<ContentReview> candidates =
                new ArrayList<>(
                        repo.findRecommendationCandidates(PageRequest.of(0, 30))
                                .getContent()
                );


        // 2️⃣ preview + ratingAvg 세팅
        candidates.forEach(post -> {
            post.setPreview(makePreview(post.getContent()));
            post.setRatingAvg(getAverageRating(post.getId()));
        });

        // 3️⃣ 최종 점수 기준 정렬
        candidates.sort(
                (a, b) -> Double.compare(
                        calculateRecommendationScore(b),
                        calculateRecommendationScore(a)
                )
        );

        // 4️⃣ 상위 limit개 반환
        return candidates.stream()
                .limit(limit)
                .toList();
    }


    // ---------------------------------------------------------
    // 🔥 상세조회 + 조회수 증가
    // ---------------------------------------------------------
    @Transactional
    public ContentReview getContentDetail(Long id) {
        ContentReview content = repo.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 1. 누적 조회수 증가
        content.setViewCount(content.getViewCount() + 1);

        // 2. 현재 월 (yyyy-MM)
        String currentMonth = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // 3. 월 변경 감지
        if (content.getViewMonth() == null || !currentMonth.equals(content.getViewMonth())) {
            content.setViewMonth(currentMonth);
            content.setViewCountMonth(1);
        } else {
            content.setViewCountMonth(content.getViewCountMonth() + 1);
        }

        return content; // @Transactional → save() 생략 가능
    }


    // 이 달의 콘텐츠 관련 코드
    // home 전용 래퍼 메서드로만 사용
    public List<ContentReview> getMonthlyPopularContents() {
        return getMonthlyTopContents("review", 5);
    }


    // 메인 페이지 이 달의 자료 관련
    public List<ContentReview> getMonthlyTopContents(String type, int limit) {

        String currentMonth = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM"));

        List<ContentReview> list =
                repo.findMonthlyTopByType(
                        currentMonth,
                        type,
                        PageRequest.of(0, limit)   // ⭐ 핵심 수정
                );

        list.forEach(post -> {
            post.setPreview(makePreview(post.getContent()));
            post.setRatingAvg(getAverageRating(post.getId()));
        });

        return list;
    }




    // ---------------------------------------------------------
    // 🔥 해시태그 / 제목 / 내용 검색
    // ---------------------------------------------------------

    public Page<ContentReview> searchByTitle(String keyword, Pageable pageable) {
        return repo.findByTitleContainingAndIsDeletedFalse(keyword, pageable);
    }

    public Page<ContentReview> searchByContent(String keyword, Pageable pageable) {
        return repo.findByContentContainingAndIsDeletedFalse(keyword, pageable);
    }

    public Page<ContentReview> searchByWriter(String writer, Pageable pageable) {
        return repo.findByWriterContainingAndIsDeletedFalse(writer, pageable);
    }

    //더미코드---------------------------------------------------------------------------------------

    public List<ContentReview> getContentsByHashtag(String hashtag) {
        return repo.findByHashtagsContainingAndIsDeletedFalseOrderByCreatedDateDesc(hashtag);
    }

    public Page<ContentReview> getPagedContentsByHashtag(String hashtag, Pageable pageable) {
        return repo.findByHashtagsContainingAndIsDeletedFalse(hashtag, pageable);
    }

    public int getCountByHashtag(String hashtag) {
        return repo.countByHashtagsContainingAndIsDeletedFalse(hashtag);
    }

    public Page<ContentReview> searchTitleInTag(String tag, String keyword, Pageable pageable) {
        return repo.findByHashtagsContainingAndTitleContainingAndIsDeletedFalse(tag, keyword, pageable);
    }

    public Page<ContentReview> searchContentInTag(String tag, String keyword, Pageable pageable) {
        return repo.findByHashtagsContainingAndContentContainingAndIsDeletedFalse(tag, keyword, pageable);
    }


    // ---------------------------------------------------------
    // 🔥 다중 해시태그 검색 (AND)
    // ---------------------------------------------------------
    public Page<ContentReview> searchByMultipleTags(Set<String> tags, Pageable pageable) {

        Specification<ContentReview> spec =
                (root, query, cb) -> cb.isFalse(root.get("isDeleted"));

        if (tags == null || tags.isEmpty()) {
            return repo.findAll(spec, pageable);
        }

        for (String tag : tags) {
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("hashtags"), "%" + tag + "%")
            );
        }

        return repo.findAll(spec, pageable);
    }


    // ---------------------------------------------------------
    // 🔥 전체 글 수
    // ---------------------------------------------------------
    public long getTotalCount() {
        return repo.countByIsDeletedFalse();
    }


    // ---------------------------------------------------------
    // 🔥 저장
    // ---------------------------------------------------------
    @Transactional
    public ContentReview saveContent(ContentReview post, CustomUserDetails loginUser) {

        // 🔐 공지 태그 권한 체크
        String hashtags = post.getHashtags();
        if (hashtags != null) {
            List<String> tagList = Arrays.stream(hashtags.split("\\s+"))
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .toList();

            if (tagList.contains("#공지")) {
                boolean isAdmin = loginUser.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                if (!isAdmin) {
                    throw new IllegalArgumentException("공지 태그는 관리자만 사용할 수 있습니다.");
                }
            }
        }

        // =====================================================
        // 🔥🔥🔥 작성자 정보 세팅 (핵심 수정 부분)
        // =====================================================

        // 작성자 ID
        post.setUserId(loginUser.getId());

        // 작성자 표시 이름
        String writerName = loginUser.getNickname();

        // 관리자 계정 nickname 방어 처리
        if (writerName == null || writerName.isBlank()) {
            boolean isAdmin = loginUser.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            writerName = isAdmin ? "관리자" : "익명";
        }

        post.setWriter(writerName);

        // =====================================================
        // 🔥 기타 자동 처리
        // =====================================================

        // 썸네일 자동 추출
        String thumbnail = extractFirstImage(post.getContent());
        post.setThumbnail(thumbnail);

        // 미리보기 자동 생성
        post.setPreview(makePreview(post.getContent()));

        // 기본값 보장 (혹시 null 방어)
        if (post.getViewCount() == null) {
            post.setViewCount(0);
        }

        if (post.getType() == null) {
            post.setType("review");
        }

        post.setIsDeleted(false);

        // =====================================================
        // 🔥 저장 + 알림
        // =====================================================

        ContentReview savedPost = repo.save(post);
        createTagNotifications(savedPost);

        return savedPost;
    }


    private void createTagNotifications(ContentReview post) {

        String hashtags = post.getHashtags();
        if (hashtags == null || hashtags.isBlank()) return;

        List<String> tags = parseTags(hashtags);
        if (tags.isEmpty()) return;

        String tag1 = tags.size() > 0 ? tags.get(0) : "";
        String tag2 = tags.size() > 1 ? tags.get(1) : "";
        String tag3 = tags.size() > 2 ? tags.get(2) : "";
        String tag4 = tags.size() > 3 ? tags.get(3) : "";
        String tag5 = tags.size() > 4 ? tags.get(4) : "";

        List<Long> targetUserIds =
                userRepository.findUserIdsByInterestedTags(tag1, tag2, tag3, tag4, tag5);

        for (Long userId : targetUserIds) {

            // 1️⃣ 자기 글 제외
            if (userId.equals(post.getUserId())) continue;

            // 2️⃣ 🔔 알림 정책 검사 (⭐ 여기만 변경)
            if (!notificationPolicyService.canSendNotification(
                    userId,
                    NotificationType.TAG
            )) {
                continue;
            }

            Notification notification = Notification.builder()
                    .receiverId(userId)
                    .type(NotificationType.TAG)
                    .message("관심 태그 글이 새로 올라왔습니다.")
                    .targetUrl("/content/post/" + post.getId())
                    .build();

            notificationRepository.save(notification);
        }
    }


    // ---------------------------------------------------------
    // 🔥 수정 기능
    // ---------------------------------------------------------
    @Transactional
    public void updateContent(Long id, String title, String content,
                              String hashtags, MultipartFile image,
                              CustomUserDetails loginUser) throws IOException {

        ContentReview post = repo.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 🔐 공지 태그 수정 권한 체크 (작성과 동일 방식)
        if (hashtags != null) {
            List<String> tagList = Arrays.stream(hashtags.split("\\s+"))
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .toList();

            if (tagList.contains("#공지")) {
                boolean isAdmin = loginUser.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                if (!isAdmin) {
                    throw new IllegalArgumentException("공지 태그는 관리자만 사용할 수 있습니다.");
                }
            }
        }

        post.setTitle(title);
        post.setContent(content);
        post.setHashtags(hashtags);

        String thumbnail = extractFirstImage(content);
        post.setThumbnail(thumbnail);

        if (image != null && !image.isEmpty()) {
            String uploadDir = "src/main/resources/static/upload/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, image.getBytes());

            post.setImgUrl("/upload/" + fileName);
        }

        repo.save(post);
    }




    // ---------------------------------------------------------
    // 🔥 삭제 (Soft Delete)
    // ---------------------------------------------------------
    @Transactional
    public void deleteContent(Long id) {
        ContentReview content = repo.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
        content.softDelete();
        repo.save(content);
    }

    // ---------------------------------------------------------
    // 🔥 삭제된 컨텐츠 복구 및 리포스팅
    // ---------------------------------------------------------
    @Transactional
    public void restoreContent(Long id) {
        // 🔥 삭제된 글도 찾아야 하므로 "findByIdAndIsDeletedFalse"가 아니라 "findById" 사용!
        ContentReview content = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        content.restore();  // isDeleted = false, deletedAt = null
        repo.save(content);
    }

    @Transactional(readOnly = true)
    public ContentReview getContentById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
    }


    // ---------------------------------------------------------
// ⭐ 평점 시스템 구축
// ---------------------------------------------------------
    public double getAverageRating(Long postId) {

        // ⭐ 올바른 엔티티 = ContentComment
        List<ContentComment> comments =
                commentRepo.findByPostIdOrderByCreatedDateAsc(postId);

        double sum = 0;
        int cnt = 0;

        for (ContentComment c : comments) {
            if (c.getRating() != null) {
                sum += c.getRating();
                cnt++;
            }
        }

        if (cnt == 0) return 0.0;

        // ⭐ 0.5 단위 반올림
        return Math.round((sum / cnt) * 2) / 2.0;
    }

    public int getRatingCount(Long postId) {
        return commentRepo.countByPostIdAndRatingIsNotNull(postId);
    }


    //썸네일용 첫 이미지 자동 추출 기능

    private String extractFirstImage(String content) {
        if (content == null) return null;

        Pattern pattern = Pattern.compile("<img[^>]+src=[\"']?([^\"'>]+)[\"']?");
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            return matcher.group(1);  // 첫 번째 이미지 URL
        }
        return null; // 없다면 null
    }

    // 점수 계산 메서드 (가중치)
    private double calculateRecommendationScore(ContentReview c) {

        double score = 0;

        // ⭐ 평점 (가장 중요)
        if (c.getRatingAvg() != null) {
            score += c.getRatingAvg() * 10;
        }

        // 🔥 이번 달 조회수
        score += c.getViewCountMonth() * 2;

        // 👀 누적 조회수
        score += c.getViewCount() * 0.1;

        // 🕒 최신성 보너스
        long days =
                java.time.temporal.ChronoUnit.DAYS.between(
                        c.getCreatedDate(),
                        java.time.LocalDateTime.now()
                );

        if (days <= 7) score += 20;
        else if (days <= 30) score += 10;

        return score;
    }

    // ---------------------------------------------------------
    // 🔥 해시태그 파싱 유틸
    // ---------------------------------------------------------
    private List<String> parseTags(String hashtags) {

        List<String> tags = new ArrayList<>();

        if (hashtags == null) return tags;

        Pattern pattern = Pattern.compile("#\\S+");
        Matcher matcher = pattern.matcher(hashtags);

        while (matcher.find()) {
            tags.add(matcher.group());
        }

        return tags;
    }



}
