# Finance DWpj1 - 코드 분석 학습 일지

> **대상 독자**: Spring Boot, JPA, 웹 개발을 처음 접하는 개발자
> **작성일**: 2025-12-19
> **프로젝트 목표**: 개인 투자자가 전문 투자자의 포트폴리오와 비교하고 AI 추천을 받는 금융 플랫폼

---

## 1️⃣ 프로젝트 전체 한 줄 요약

**"스크린샷 한 장으로 내 포트폴리오를 워렌 버핏, 캐시 우드와 비교하고, AI가 맞춤 투자 전략을 제안해주는 서비스"**

### 핵심 문제
- 일반 투자자들은 "내가 잘 투자하고 있는지" 모른다
- 전문 투자자의 포트폴리오는 공개되지만 이해하기 어렵다
- 증권사 앱의 포트폴리오를 수동으로 입력하기 번거롭다

### 이 프로젝트의 해결책
1. **OCR 기술**: 증권사 앱 스크린샷 → 자동으로 종목 인식
2. **SEC 13F 데이터**: 워렌 버핏 등 투자대가의 실제 보유 종목 수집
3. **코사인 유사도**: 내 포트폴리오와 투자대가의 유사도 계산
4. **AI 추천**: OpenAI/Gemini가 4명의 투자 철학을 혼합한 맞춤 포트폴리오 제안

---

## 2️⃣ 전체 아키텍처 큰 그림

### 📐 계층 구조 (Spring Boot MVC 패턴)

```
사용자 요청 (웹 브라우저)
    ↓
┌─────────────────────────────────────────────────┐
│  Controller 계층 (35개 Controller)               │
│  - 요청 받기, 검증, 응답 생성                      │
│  - 예: PortfolioAnalyzerController               │
└─────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────┐
│  Service 계층 (48개 Service)                     │
│  - 비즈니스 로직 처리                             │
│  - 예: OcrService, PortfolioMatchingService      │
└─────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────┐
│  Repository 계층 (Spring Data JPA)               │
│  - 데이터베이스 CRUD                              │
└─────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────┐
│  Database (MariaDB) - 22개 테이블                │
│  - User, Stock, Content, Insights, Portfolio    │
└─────────────────────────────────────────────────┘
```

### 🔄 실제 요청 흐름 예시: "포트폴리오 분석"

```
1. 사용자가 증권사 스크린샷 업로드
   ↓
2. PortfolioAnalyzerController.analyzePortfolio()
   - MultipartFile 이미지 받기
   - BrokerType(증권사) 선택
   ↓
3. OcrService.extractPortfolioFromImage()
   - Tesseract OCR로 텍스트 추출
   - "삼성전자 30%" → "SSNLF 30%"
   ↓
4. TickerMappingService.applyMappingToText()
   - 한글 종목명 → 영문 티커 변환
   - 500+ 종목 매핑 테이블 사용
   ↓
5. PortfolioMatchingService.findTopMatches()
   - SEC 13F 데이터 조회 (투자대가 보유 종목)
   - 코사인 유사도 계산
   - TOP 3 투자자 반환
   ↓
6. GPTService.generateConsensusPortfolio()
   - AI에게 4명의 투자 철학 혼합 요청
   - JSON 형식으로 10개 종목 추천
   ↓
7. View (Thymeleaf 템플릿)
   - 분석 결과 화면에 표시
   - 차트, 추천 종목 목록
```

---

## 3️⃣ 기능 단위 분석

---

### 🔹 기능 1: 회원가입 / 로그인 (Spring Security)

#### ✔ 이 기능은 왜 필요한가?

사용자마다 다른 포트폴리오를 저장하고, 알림을 받고, 게시글을 작성하려면 "누가 접속했는지" 알아야 한다.
만약 이 기능이 없으면 → 모든 사용자가 동일한 데이터를 보게 되고, 개인화가 불가능하다.

#### ✔ 사용자가 이 기능을 쓰면 어떤 경험을 하게 되는가?

1. 회원가입 시 이메일 중복 체크
2. 비밀번호는 암호화되어 저장 (절대 평문 저장 안 함!)
3. 로그인 후 "내 포트폴리오", "내 게시글"에 접근 가능
4. 관리자 계정은 뉴스 삭제, 콘텐츠 관리 권한 획득

#### ✔ 내부 동작 흐름 (초보자 시점)

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/config/SecurityConfig.java`
- Class: `SecurityConfig`
- Method: `filterChain(HttpSecurity http)`
- Related: `User.java`, `UserService.java`, `CustomUserDetailsService.java`

[CODE-SNIPPET]
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())

        // 개발 모드: 모든 접근 허용
        .authorizeHttpRequests(auth -> auth
            .anyRequest().permitAll()
        )

        .formLogin(form -> form
            .loginPage("/user/login")
            .loginProcessingUrl("/user/login")
            .successHandler(customLoginSuccessHandler)
            .permitAll()
        )

        .logout(logout -> logout
            .logoutUrl("/user/logout")
            .logoutSuccessHandler(customLogoutSuccessHandler)
            .permitAll()
        );

    return http.build();
}
```

**동작 순서**:
1. 사용자가 `/user/signup`에서 회원가입 폼 제출
2. `UserController.signup(UserSignupDTO)` 호출
3. `UserService.signup()`에서 이메일/닉네임 중복 체크
4. 비밀번호를 `BCryptPasswordEncoder`로 암호화
5. `User` 엔티티를 DB에 저장

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/service/user/UserService.java`
- Class: `UserService`
- Method: `signup(UserSignupDTO)`

[CODE-SNIPPET]
```java
public User signup(UserSignupDTO dto) {
    // 중복 검증
    if (userRepository.existsByEmail(dto.getEmail())) {
        throw new IllegalStateException("이미 사용 중인 이메일입니다.");
    }
    if (userRepository.existsByNickname(dto.getNickname())) {
        throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
    }

    // 비밀번호 암호화
    String encodedPassword = passwordEncoder.encode(dto.getPassword());

    // User 엔티티 생성
    User user = User.builder()
        .email(dto.getEmail())
        .nickname(dto.getNickname())
        .password(encodedPassword)  // 암호화된 비밀번호
        .role(Role.USER)            // 기본 권한
        .isActive(true)
        .build();

    return userRepository.save(user);
}
```

#### ✔ 왜 이 기술을 선택했는가?

**Q: 왜 Spring Security인가?**
- 직접 로그인 로직을 만들면 → 보안 취약점 발생 위험
- Spring Security는 → CSRF, 세션 고정 공격 방어를 자동으로 처리

**Q: 왜 BCrypt 암호화인가?**
- 평문 저장: DB 해킹 시 모든 비밀번호 노출
- BCrypt: 같은 비밀번호도 매번 다른 해시값 생성 (Salt 자동 추가)
- 레인보우 테이블 공격 방어

**Q: 왜 Role을 ENUM으로 관리하는가?**
- 잘못된 값 입력 방지 (`"ADMIN"` ⭕, `"admin"` ❌)
- 컴파일 타임에 오류 발견 가능

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/entity/user/Role.java`
- Enum: `Role`

[CODE-SNIPPET]
```java
public enum Role {
    USER,    // 일반 사용자
    ADMIN    // 관리자
}
```

#### ✔ 이 기능에서 초보자가 가장 헷갈리는 지점

**헷갈림 1**: "SecurityContext에서 email이 나오는데, 왜 username이라고 부르나요?"
- Spring Security는 내부적으로 `username`이라는 용어 사용
- 우리 프로젝트에서는 email을 username으로 사용 (로그인 아이디)

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/service/user/CustomUserDetailsService.java`
- Class: `CustomUserDetailsService`
- Method: `loadUserByUsername(String email)`

[CODE-SNIPPET]
```java
@Override
public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

    // Spring Security의 UserDetails로 변환
    return org.springframework.security.core.userdetails.User.builder()
        .username(user.getEmail())  // email을 username으로 사용!
        .password(user.getPassword())
        .roles(user.getRole().name())
        .build();
}
```

**헷갈림 2**: "개발 모드에서 `.anyRequest().permitAll()`인데 로그인이 왜 필요한가요?"
- 현재는 개발 편의를 위해 인증 없이 모든 페이지 접근 가능
- 프로덕션 배포 시 주석을 해제하면 → 관리자 페이지는 ROLE_ADMIN만 접근 가능

```java
// 프로덕션 배포 시 활성화할 코드 (주석 참고)
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/admin/**").hasRole("ADMIN")
    .requestMatchers("/user/mypage/**").authenticated()
    .anyRequest().permitAll()
)
```

---

### 🔹 기능 2: OCR 포트폴리오 인식 (Tesseract + 전처리)

#### ✔ 이 기능은 왜 필요한가?

투자자가 토스, 미래에셋 등 증권사 앱에서 보유 종목을 **일일이 타이핑**하는 것은 비현실적이다.
스크린샷 한 장만 올리면 자동으로 종목과 비중을 인식해야 사용자 경험이 좋아진다.

**현실 문제**:
- 증권사마다 화면 레이아웃이 다르다 (토스 ≠ 미래에셋)
- 한글 종목명을 영문 티커로 변환해야 미국 투자자와 비교 가능

#### ✔ 사용자가 이 기능을 쓰면 어떤 경험을 하게 되는가?

1. 증권사 앱에서 포트폴리오 화면 스크린샷
2. 웹사이트에 이미지 업로드 + 증권사 선택 (TOSS, DEFAULT 등)
3. 2~3초 후 → "삼성전자 30%, 테슬라 25%" 등이 자동 인식됨
4. 인식 결과 확인 후 → "투자대가와 비교하기" 버튼 클릭

#### ✔ 내부 동작 흐름 (초보자 시점)

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/service/portfolio/OcrService.java`
- Class: `OcrService`
- Method: `extractPortfolioFromImage(MultipartFile, BrokerType)`
- Related: `TesseractConfig`, `TickerMappingService`, `OcrPreprocessor`

**전체 흐름**:
```
1. 이미지 업로드 (MultipartFile)
   ↓
2. 증권사별 이미지 전처리 (OcrPreprocessor)
   - 토스: 배경 제거, 대비 증가
   - 기본: 그레이스케일 변환
   ↓
3. Tesseract OCR 실행 (ThreadLocal 방식)
   - "삼성전자 30%\nTesla 25%" 추출
   ↓
4. 한글 → 영문 티커 매핑 (TickerMappingService)
   - "삼성전자" → "SSNLF" (미국 ADR 티커)
   - "테슬라" → "TSLA"
   ↓
5. 증권사별 파싱 (OcrParser)
   - 정규식으로 "종목명 비중%" 패턴 인식
   ↓
6. PortfolioStock 리스트 반환
```

[CODE-SNIPPET]
```java
public List<PortfolioStock> extractPortfolioFromImage(MultipartFile imageFile, BrokerType brokerType) {
    List<PortfolioStock> stocks = new ArrayList<>();

    // OCR 사용 가능 여부 확인
    if (!isAvailable) {
        throw new IllegalStateException("Tesseract OCR을 사용할 수 없습니다.");
    }

    try {
        // 1. 이미지 로드
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));

        // 2. 증권사별 전처리
        BufferedImage preprocessedImage = image;
        OcrPreprocessor preprocessor = preprocessors.get(brokerType);
        if (preprocessor != null) {
            preprocessedImage = preprocessor.preprocess(image);
        }

        // 3. OCR 실행 (ThreadLocal에서 현재 스레드의 Tesseract 인스턴스 가져오기)
        String extractedText = tesseractThreadLocal.get().doOCR(preprocessedImage);
        log.info("OCR 추출 완료:\n{}", extractedText);

        // 4. 한국어 종목명 → 영어 티커로 치환
        String mappedText = tickerMappingService.applyMappingToText(extractedText);

        // 5. 증권사별 텍스트 파싱
        OcrParser parser = parsers.get(brokerType);
        if (parser != null) {
            stocks = parser.parse(mappedText);
        } else {
            stocks = parsePortfolioText(mappedText); // 기본 파싱
        }

        log.info("✅ 총 {}개 종목 추출 완료", stocks.size());

    } catch (TesseractException | IOException e) {
        throw new RuntimeException("OCR 처리 중 오류: " + e.getMessage(), e);
    }

    return stocks;
}
```

#### ✔ 왜 이 기술을 선택했는가?

**Q: 왜 Tesseract인가? (Google Vision API가 더 정확하지 않나요?)**
- Tesseract: 무료, 로컬 실행 (외부 API 호출 없음)
- Google Vision: 월 $1.50/1000건, 네트워크 필요
- 이 프로젝트는 비용 절감 우선 → Tesseract 선택

**Q: 왜 ThreadLocal을 사용하는가?**
- Tesseract는 Thread-Safe하지 않음 (멀티스레드 환경에서 오류 발생)
- ThreadLocal: 각 스레드마다 별도의 Tesseract 인스턴스 생성
- 병렬 이미지 처리 시 안전성 보장

[CODE-SNIPPET]
```java
// ThreadLocal 초기화: 각 스레드가 처음 접근할 때 새 Tesseract 인스턴스 생성
this.tesseractThreadLocal = ThreadLocal.withInitial(() -> {
    Tesseract tess = new Tesseract();
    tess.setDatapath(this.datapath);  // "C:\\Program Files\\Tesseract-OCR\\tessdata"
    tess.setLanguage(this.language);   // "eng"
    tess.setOcrEngineMode(this.ocrEngineMode);  // 3 = LSTM + Legacy
    tess.setPageSegMode(this.pageSegMode);      // 6 = 단일 블록 텍스트
    return tess;
});
```

**Q: 왜 이미지 전처리가 필요한가?**
- 원본 이미지: 배경색, 그림자, 노이즈 → OCR 정확도 하락
- 전처리 후: 흑백 변환, 대비 증가, 노이즈 제거 → 인식률 향상

**Q: 왜 증권사별로 전처리 방식을 다르게 하나요?**
- 토스: 파란 배경 제거 필요
- 미래에셋: 흰 배경이라 기본 전처리만 필요
- 전략 패턴으로 증권사별 전처리기를 플러그인처럼 교체

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/service/portfolio/ocr/TossPreprocessor.java`
- Class: `TossPreprocessor`
- Method: `preprocess(BufferedImage)`

#### ✔ 이 기능에서 초보자가 가장 헷갈리는 지점

**헷갈림 1**: "한글 종목명을 왜 영문 티커로 바꾸나요?"
- SEC 13F 데이터는 미국 상장 종목만 포함
- "삼성전자"는 한국 상장이지만 미국에는 `SSNLF`(ADR)로 거래됨
- 비교를 위해 통일된 티커 사용 필요

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/service/portfolio/TickerMappingService.java`
- Class: `TickerMappingService`
- Method: `applyMappingToText(String)`

[CODE-SNIPPET]
```java
public String applyMappingToText(String ocrText) {
    String result = ocrText;

    // 500+ 종목 매핑 테이블에서 치환
    for (Map.Entry<String, String> entry : koreanToTickerMap.entrySet()) {
        String koreanName = entry.getKey();    // "삼성전자"
        String ticker = entry.getValue();      // "SSNLF"

        // 정규식: 단어 경계로 정확히 매칭
        result = result.replaceAll("\\b" + Pattern.quote(koreanName) + "\\b", ticker);
    }

    return result;
}
```

**헷갈림 2**: "OCR 인식률이 낮으면 어떻게 하나요?"
- 사용자가 직접 수정할 수 있는 편집 UI 제공
- 또는 증권사별 전처리 파라미터 튜닝 (대비, 임계값 조정)

---

### 🔹 기능 3: 포트폴리오 매칭 (코사인 유사도)

#### ✔ 이 기능은 왜 필요한가?

사용자가 "내 투자 스타일이 누구와 비슷한가?"를 알아야 학습할 대상을 찾을 수 있다.
만약 이 기능이 없으면 → 워렌 버핏, 캐시 우드의 포트폴리오를 보여줘도 "나랑 상관없네" 하고 닫을 것이다.

**핵심 아이디어**:
- 내 포트폴리오 = [AAPL 30%, TSLA 20%, MSFT 15%, ...]
- 워렌 버핏 = [AAPL 40%, BRK.B 25%, KO 10%, ...]
- **겹치는 종목**과 **비중 비율**을 수치화 → "버핏과 75% 유사!"

#### ✔ 사용자가 이 기능을 쓰면 어떤 경험을 하게 되는가?

1. OCR로 포트폴리오 인식 완료
2. "투자대가와 비교하기" 버튼 클릭
3. 로딩 2~3초 후 → TOP 3 결과 표시
   - 1위: 워렌 버핏 (83% 유사)
   - 2위: 캐시 우드 (67% 유사)
   - 3위: 레이 달리오 (52% 유사)
4. 각 투자자의 투자 철학, 장단점, 개선 제안 표시

#### ✔ 내부 동작 흐름 (초보자 시점)

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/service/portfolio/PortfolioMatchingService.java`
- Class: `PortfolioMatchingService`
- Method: `findTopMatches(Map<String, Double>)`
- Related: `Investor13FHolding`, `SimilarityCalculator`, `PortfolioExposureNormalizer`

**전체 흐름**:
```
1. 사용자 포트폴리오 입력
   { "AAPL": 30.0, "TSLA": 25.0, "MSFT": 20.0 }
   ↓
2. 레버리지 ETF 환산 (PortfolioExposureNormalizer)
   { "TSLL": 10.0 } → { "TSLA": 20.0 }  // 2배 레버리지
   ↓
3. 활성화된 투자대가 목록 조회 (InvestorProfileRepository)
   [buffett, wood, dalio, ...]
   ↓
4. 각 투자대가별 반복:
   a. SEC 13F 데이터 조회 (최신 분기)
   b. 투자대가 포트폴리오도 환산
   c. 코사인 유사도 계산 (SimilarityCalculator)
   d. 종목 겹침 분석
   e. 최종 매칭 점수 = 유사도*0.4 + 겹침*0.4 + 상위종목겹침*0.2
   ↓
5. 점수 내림차순 정렬 → TOP 3 반환
```

[CODE-SNIPPET]
```java
public List<MatchResult> findTopMatches(Map<String, Double> userPortfolio) {
    log.info("=== 포트폴리오 매칭 시작 ===");

    // 1. 사용자 포트폴리오를 노출 기준으로 환산
    Map<String, Double> userExposure = exposureNormalizer.normalize(userPortfolio);

    List<MatchResult> results = new ArrayList<>();

    // 2. 활성화된 모든 투자대가 조회
    List<InvestorProfile> profiles = profileRepository.findByActiveTrue();

    for (InvestorProfile profile : profiles) {
        // 3. 투자대가의 최신 13F 보유 종목 조회
        List<Investor13FHolding> holdings = holdingRepository
            .findLatestHoldingsByInvestor(profile.getInvestorId());

        // 4. 투자대가 포트폴리오를 Map으로 변환
        Map<String, Double> investorPortfolioRaw = holdings.stream()
            .collect(Collectors.toMap(
                Investor13FHolding::getTicker,
                Investor13FHolding::getPortfolioWeight,
                Double::sum
            ));

        // 5. 투자대가 포트폴리오도 환산
        Map<String, Double> investorExposure = exposureNormalizer.normalize(investorPortfolioRaw);

        // 6. 코사인 유사도 계산
        double similarity = similarityCalculator.calculate(userExposure, investorExposure);

        // 7. 종목 겹침 분석
        List<String> matchedStocks = findMatchedStocks(userExposure, investorExposure);
        double overlapPercentage = calculateOverlapPercentage(userExposure, investorExposure);

        // 8. 최종 매칭 점수
        double matchScore = calculateMatchScore(
            similarity,
            overlapPercentage,
            userExposure,
            investorExposure
        );

        // 9. 결과 저장
        MatchResult result = MatchResult.builder()
            .investorName(profile.getName())
            .similarity(Math.round(matchScore))
            .matchedStocks(matchedStocks)
            .overlapPercentage(Math.round(overlapPercentage))
            .philosophy(profile.getPhilosophy())
            .suggestions(generateSuggestions(userExposure, investorExposure, profile))
            .build();

        results.add(result);
    }

    // 10. 유사도 내림차순 정렬 → TOP 3
    results.sort((a, b) -> Long.compare(b.getSimilarity(), a.getSimilarity()));
    return results.stream().limit(3).collect(Collectors.toList());
}
```

#### ✔ 왜 이 기술을 선택했는가?

**Q: 왜 코사인 유사도인가? (단순히 겹치는 종목 개수만 세면 안 되나요?)**
- 종목 개수만 비교: AAPL 1%, TSLA 1% vs AAPL 40%, TSLA 30% → 동일하게 취급 (잘못됨)
- 코사인 유사도: **비중(가중치)**을 고려하여 포트폴리오의 "방향성" 측정
- 수학적 정의: `cos(θ) = (A·B) / (||A|| × ||B||)`

[CODE-SNIPPET]
```java
// 코사인 유사도 계산
private double calculateWeightedSimilarity(Map<String, Double> portfolio1,
                                           Map<String, Double> portfolio2) {
    Set<String> allTickers = new HashSet<>();
    allTickers.addAll(portfolio1.keySet());
    allTickers.addAll(portfolio2.keySet());

    double dotProduct = 0.0;
    double magnitude1 = 0.0;
    double magnitude2 = 0.0;

    for (String ticker : allTickers) {
        double weight1 = portfolio1.getOrDefault(ticker, 0.0);
        double weight2 = portfolio2.getOrDefault(ticker, 0.0);

        dotProduct += weight1 * weight2;      // 내적
        magnitude1 += weight1 * weight1;      // 벡터 크기^2
        magnitude2 += weight2 * weight2;
    }

    magnitude1 = Math.sqrt(magnitude1);
    magnitude2 = Math.sqrt(magnitude2);

    if (magnitude1 == 0 || magnitude2 == 0) {
        return 0.0;
    }

    // 코사인 유사도 (0~1) → 퍼센트 (0~100)
    return (dotProduct / (magnitude1 * magnitude2)) * 100.0;
}
```

**Q: 왜 최종 점수에서 유사도, 겹침, 상위종목을 혼합하나요?**
- 코사인 유사도만: 비중은 비슷하지만 종목이 안 겹칠 수 있음
- 종목 겹침만: 비중이 전혀 다를 수 있음
- **사람의 감각**에 맞게 3가지 지표를 혼합 (40% + 40% + 20%)

```java
// 최종 매칭 점수 계산
private double calculateMatchScore(double similarity,
                                   double overlapPercentage,
                                   Map<String, Double> userPortfolio,
                                   Map<String, Double> investorPortfolio) {
    // 사용자의 상위 5개 종목 기준
    double topOverlapScore = calculateTopOverlapScore(userPortfolio, investorPortfolio, 5);

    // 가중치 조합
    return similarity * 0.4          // 코사인 유사도
         + overlapPercentage * 0.4   // 종목 겹침 비율
         + topOverlapScore * 0.2;    // 상위 종목 겹침
}
```

**Q: 왜 레버리지 ETF를 기초자산으로 환산하나요?**
- 사용자가 `TSLL` (테슬라 2배 레버리지) 보유
- 투자대가는 `TSLA` (테슬라 현물) 보유
- 환산 없이 비교하면 → 겹치지 않는 종목으로 취급 (잘못됨)
- 환산 후: `TSLL 10%` → `TSLA 20%` 노출로 계산 → 정확한 비교

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/service/portfolio/PortfolioExposureNormalizer.java`
- Class: `PortfolioExposureNormalizer`
- Method: `normalize(Map<String, Double>)`

#### ✔ 이 기능에서 초보자가 가장 헷갈리는 지점

**헷갈림 1**: "SEC 13F 데이터는 어디서 오나요?"
- SEC (미국 증권거래위원회)는 자산 $100M 이상 펀드매니저에게 분기별 보유 종목 공개 의무
- 워렌 버핏의 Berkshire Hathaway는 CIK `0001067983`로 조회
- `SEC13FService`가 자동으로 수집 (최신 분기 데이터만)

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/service/portfolio/SEC13FService.java`
- Class: `SEC13FService`
- Method: `fetch13FDataForInvestor(String investorId)`

**헷갈림 2**: "TOP 3가 나와도 어떤 투자자를 따라야 할지 모르겠어요"
- 각 투자자의 `philosophy`, `strengths`, `weaknesses` 필드 제공
- AI 추천 기능으로 4명의 철학을 혼합한 맞춤 포트폴리오 제안

---

### 🔹 기능 4: SEC 13F 데이터 수집 (자동화 크롤러)

#### ✔ 이 기능은 왜 필요한가?

투자대가의 포트폴리오는 **분기마다 변한다**.
수동으로 업데이트하면 → 누락, 실수 발생
자동 수집으로 → 최신 데이터 유지

**SEC 13F란?**
- 미국 SEC(증권거래위원회)가 제공하는 대규모 투자자의 분기별 보유 종목 보고서
- $100M 이상 운용사는 45일 내 제출 의무
- 예: 워렌 버핏이 2024Q4에 AAPL 40%, KO 10% 보유

#### ✔ 사용자가 이 기능을 쓰면 어떤 경험을 하게 되는가?

**사용자 시점**:
- 직접 사용 안 함 (백그라운드 자동 실행)
- 단, 관리자 페이지에서 "수동 재수집" 버튼 클릭 가능

**관리자 시점**:
1. 관리자 페이지 → "SEC 13F 수집 시작" 버튼
2. 8명의 투자대가 순차 수집 (약 2~3분 소요)
3. 진행 상황: `[3/8] 워렌 버핏 수집 중...`
4. 완료 후 → "✅ 성공: 7명, ❌ 실패: 1명"
5. 실패 시 자동 재시도 (최대 3회)

#### ✔ 내부 동작 흐름 (초보자 시점)

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/service/portfolio/SEC13FService.java`
- Class: `SEC13FService`
- Method: `fetch13FDataForInvestor(String investorId)`
- Related: `CusipToTickerService`, `SEC13FTransactionalService`, `Investor13FHolding`

**전체 흐름**:
```
1. 관리자가 "수집 시작" 버튼 클릭
   ↓
2. SEC13FController.startCollection() (비동기 실행)
   ↓
3. 활성화된 투자대가 목록 조회
   [buffett, wood, dalio, ...]
   ↓
4. 각 투자대가별 반복:
   a. SEC API 호출 (RateLimiter로 초당 9회 제한)
      GET https://data.sec.gov/submissions/CIK0001067983.json
   ↓
   b. 최신 13F-HR 파일 URL 추출
      https://www.sec.gov/.../primary_doc.xml
   ↓
   c. XML 파일 다운로드 및 JAXB 파싱
      <infoTable>
        <cusip>037833100</cusip>
        <nameOfIssuer>Apple Inc.</nameOfIssuer>
        <value>150000000</value> (천 단위)
      </infoTable>
   ↓
   d. CUSIP → Ticker 변환 (CusipToTickerService)
      "037833100" → "AAPL"
   ↓
   e. 포트폴리오 비중 계산
      value = 150,000,000 (천 단위) = $150B
      전체 포트폴리오 = $1T
      비중 = 15%
   ↓
   f. Investor13FHolding 엔티티 생성 및 저장
   ↓
5. Checkpoint 업데이트 (SUCCESS/FAILED)
```

[CODE-SNIPPET]
```java
public int fetch13FDataForInvestor(String investorId) {
    log.info("=== {} 투자대가 13F 데이터 수집 시작 ===", investorId);

    InvestorProfile profile = profileRepository.findById(investorId)
        .orElseThrow(() -> new IllegalArgumentException("투자대가를 찾을 수 없습니다: " + investorId));

    // 1. RateLimiter 획득 (초당 9회 제한)
    rateLimiter.acquire();

    // 2. SEC API에서 최신 13F 파일 URL 가져오기
    String latest13FUrl = getLatest13FFileUrl(profile.getCik());
    if (latest13FUrl == null) {
        log.warn("{}의 13F 파일을 찾을 수 없습니다", profile.getName());
        return 0;
    }

    // 3. RateLimiter 획득 (XML 파일 다운로드)
    rateLimiter.acquire();

    // 4. 13F XML 파일 파싱
    List<Investor13FHolding> holdings = parse13FFile(latest13FUrl, investorId);

    if (!holdings.isEmpty()) {
        String quarter = holdings.get(0).getFilingQuarter();

        // 5. 트랜잭션 서비스로 저장
        int count = transactionalService.saveHoldings(investorId, quarter, holdings);

        log.info("{}의 13F 데이터 {}건 저장 완료 (분기: {})",
                profile.getName(), holdings.size(), quarter);
        return count;
    }

    return 0;
}
```

#### ✔ 왜 이 기술을 선택했는가?

**Q: 왜 RateLimiter를 사용하나요?**
- SEC API는 초당 10회 제한 (공식 규정)
- 초과 시 → 429 Too Many Requests 응답 + IP 차단
- Guava RateLimiter로 초당 9회로 안전하게 제한

[CODE-SNIPPET]
```java
// Guava RateLimiter 생성 (초당 9회)
private final RateLimiter rateLimiter = RateLimiter.create(9.0);

// API 호출 전 반드시 acquire() 호출
rateLimiter.acquire();  // 필요시 자동으로 대기
String response = httpClient.newCall(request).execute();
```

**Q: 왜 재시도 로직이 있나요?**
- 네트워크 일시적 오류: 타임아웃, 503 Service Unavailable
- Exponential Backoff: 1차 2초, 2차 4초, 3차 8초 대기 후 재시도
- 재시도 불가능한 오류 (404 Not Found)는 즉시 실패 처리

[CODE-SNIPPET]
```java
private int fetch13FDataWithRetry(InvestorProfile profile) throws Exception {
    int retryCount = 0;
    Exception lastException = null;

    while (retryCount < MAX_RETRIES) {
        try {
            return fetch13FDataForInvestor(profile.getInvestorId());
        } catch (Exception e) {
            retryCount++;
            lastException = e;

            if (isRetryable(e)) {
                long delay = BASE_DELAY_MS * (long) Math.pow(2, retryCount - 1);
                log.warn("⚠️ 재시도 {}/{} - {}초 후 재시도", retryCount, MAX_RETRIES, delay / 1000);
                Thread.sleep(delay);
            } else {
                throw e;  // 재시도 불가능한 오류
            }
        }
    }

    throw lastException;  // 최대 재시도 횟수 초과
}
```

**Q: 왜 CUSIP를 Ticker로 변환하나요?**
- SEC 13F 데이터는 CUSIP 코드 사용 (예: `037833100`)
- 일반 사용자는 Ticker로 알고 있음 (예: `AAPL`)
- OpenFIGI API로 CUSIP → Ticker 변환 (일일 5,000회 무료)

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/service/portfolio/CusipToTickerService.java`
- Class: `CusipToTickerService`
- Method: `convertCusipToTicker(String cusip)`

**Q: 왜 Checkpoint를 저장하나요?**
- 수집 중 서버 재시작 → 어디까지 완료했는지 모름
- Checkpoint 저장 → 재시작 시 이미 성공한 투자자는 건너뜀 (Resume 기능)

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/entity/portfolio/SecCollectorCheckpoint.java`
- Entity: `SecCollectorCheckpoint`
- Status: `PENDING | IN_PROGRESS | SUCCESS | FAILED | SKIPPED`

#### ✔ 이 기능에서 초보자가 가장 헷갈리는 지점

**헷갈림 1**: "왜 트랜잭션을 별도 서비스로 분리했나요?"
- SEC13FService: RateLimiter, 네트워크 호출 (트랜잭션 없음)
- SEC13FTransactionalService: DB 저장만 담당 (`@Transactional`)
- 이유: 네트워크 호출 중 DB 트랜잭션이 열려있으면 → 커넥션 낭비, 타임아웃 위험

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/service/portfolio/SEC13FTransactionalService.java`
- Class: `SEC13FTransactionalService`
- Method: `saveHoldings(String investorId, String quarter, List<Investor13FHolding>)`

**헷갈림 2**: "분기(quarter)는 어떻게 계산하나요?"
- 2024-03-15 → `2024Q1`
- 2024-06-15 → `2024Q2`
- 분기 계산 공식: `quarter = (month - 1) / 3 + 1`

[CODE-SNIPPET]
```java
private String extractQuarter(LocalDate date) {
    int year = date.getYear();
    int quarter = (date.getMonthValue() - 1) / 3 + 1;
    return year + "Q" + quarter;
}
```

---

### 🔹 기능 5: AI 포트폴리오 추천 (OpenAI ↔ Gemini 스위칭)

#### ✔ 이 기능은 왜 필요한가?

사용자가 TOP 3 투자자를 보고 → "그래서 나는 뭘 사야 하는데?"
이 질문에 답하려면 **구체적인 종목 추천**이 필요하다.

**핵심 아이디어**:
- 4명의 투자자 철학을 AI에게 설명
- AI가 4명이 회의실에서 합의한 듯한 포트폴리오 생성
- 단순 평균(25%씩) 아니라 **철학적 합의점** 도출

#### ✔ 사용자가 이 기능을 쓰면 어떤 경험을 하게 되는가?

1. 매칭 결과에서 "AI 추천 받기" 버튼 클릭
2. 로딩 5~10초 (AI 응답 대기)
3. 결과 화면:
   - **통합 투자 철학**: "4명의 투자자는 기술주 중심 + 안정성 중시..."
   - **추천 종목 10개**: AAPL 15%, MSFT 12%, ...
   - **각 종목 선정 이유**: "애플은 버핏의 가치투자 + 우드의 혁신 철학 반영"
   - **리스크 프로필**: "중위험 중수익, 기술주 변동성 주의"

#### ✔ 내부 동작 흐름 (초보자 시점)

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/service/common/GPTService.java`
- Class: `GPTService`
- Method: `generateConsensusPortfolio(List<String> investorIds)`
- Related: `AiClient`, `GeminiClient`, `OpenAiClient`

**전체 흐름**:
```
1. 사용자가 4명의 투자자 선택 (또는 TOP 3 자동)
   [buffett, wood, dalio, thiel]
   ↓
2. GPTService.generateConsensusPortfolio() 호출
   ↓
3. 투자자 ID → 이름 변환
   "buffett" → "워렌 버핏 (Warren Buffett)"
   ↓
4. AI 프롬프트 생성
   시스템: "당신은 투자 전문가입니다. JSON만 출력하세요."
   사용자: "워렌 버핏, 캐시 우드, 레이 달리오, 피터 틸이 회의실에서
            토론 후 합의한 10개 종목을 선정하세요.
            각 종목은 4명 모두가 동의해야 합니다."
   ↓
5. AiClient 선택 (환경변수 기준)
   - ai.provider=gemini → GeminiClient
   - ai.provider=openai → OpenAiClient
   ↓
6. AI API 호출 (Gemini 2.0 또는 GPT-4)
   - temperature: 0.3 (일관성 우선)
   - maxTokens: 3000 (긴 응답 허용)
   - timeout: 120초
   ↓
7. JSON 응답 파싱
   {
     "investmentCommitteePhilosophy": "...",
     "stocks": [
       {
         "ticker": "AAPL",
         "weightPercent": 15.0,
         "consensusReason": "버핏의 브랜드 가치 + 우드의 혁신..."
       },
       ...
     ]
   }
   ↓
8. Controller → View로 전달 → 사용자에게 표시
```

[CODE-SNIPPET]
```java
public String generateConsensusPortfolio(List<String> investorIds) {
    try {
        // 1. 투자자 이름 리스트 생성
        StringBuilder investorNames = new StringBuilder();
        for (String id : investorIds) {
            investorNames.append(getInvestorName(id)).append(", ");
        }
        String investorsStr = investorNames.substring(0, investorNames.length() - 2);

        // 2. 모델 선택 (Gemini 우선, OpenAI 대체)
        String model = null;
        int maxTokens = 3000;

        if (aiClient.getProviderName().equals("openai")) {
            model = "gpt-4";
            maxTokens = 2500;
        } else if (aiClient.getProviderName().equals("gemini")) {
            model = "gemini-2.0-flash-exp";
        }

        // 3. 프롬프트 생성
        String userPrompt = String.format(
            "다음 4명의 투자자 (%s)가 한 회의실에 모여 토론했습니다.\n\n" +
            "**핵심 가정: 이들이 토론 끝에 '모두가 동의한 합의 종목 10개'를 선정합니다.**\n\n" +
            "- 단순히 각자 포트폴리오를 25%%씩 섞는 것이 아닙니다.\n" +
            "- 4명이 모두 '이 종목은 투자할 가치가 있다'고 동의해야 선정됩니다.\n\n" +
            "**JSON 스키마 (이 형식만 출력):**\n" +
            "{\n" +
            "  \"investmentCommitteePhilosophy\": \"3~4문장\",\n" +
            "  \"stocks\": [\n" +
            "    {\n" +
            "      \"ticker\": \"AAPL\",\n" +
            "      \"weightPercent\": 15.0,\n" +
            "      \"consensusReason\": \"4명 모두 동의한 이유\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n\n" +
            "**필수 조건:**\n" +
            "1. stocks 배열은 정확히 10개\n" +
            "2. weightPercent 합계 = 100\n",
            investorsStr
        );

        // 4. AI 요청 객체 생성
        AiRequest request = AiRequest.builder()
            .system("당신은 투자 전문가입니다. JSON만 출력하세요.")
            .user(userPrompt)
            .temperature(0.3)  // 낮은 temperature로 일관성 확보
            .maxTokens(maxTokens)
            .timeoutSeconds(120)
            .model(model)
            .build();

        // 5. AI API 호출
        AiResult result = aiClient.generate(request);
        String response = result.getText().trim();

        // 6. 마크다운 코드 블록 제거
        if (response.startsWith("```json")) {
            response = response.substring(7);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }

        return response.trim();

    } catch (AiClientException e) {
        log.error("[{}] API 호출 오류: {}", e.getProvider(), e.getMessage());
        throw new RuntimeException("포트폴리오 생성 실패: " + e.getMessage(), e);
    }
}
```

#### ✔ 왜 이 기술을 선택했는가?

**Q: 왜 OpenAI와 Gemini를 둘 다 지원하나요?**
- OpenAI GPT-4: 높은 품질, 비용 비쌈 ($0.03/1K tokens)
- Gemini 2.0: 무료 할당량 많음, 속도 빠름
- 환경변수로 스위칭 → 비용 절감 가능

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/ai/client/AiClient.java`
- Interface: `AiClient`
- Implementations: `OpenAiClient`, `GeminiClient`

[CODE-SNIPPET]
```java
// application.properties
ai.provider=gemini  # 또는 openai

// AiClient 자동 선택 (Strategy Pattern)
@Service
@RequiredArgsConstructor
public class GPTService {
    private final AiClient aiClient;  // Spring이 자동으로 주입 (환경변수 기준)

    public String generateResponse(String prompt) {
        AiRequest request = AiRequest.builder()
            .user(prompt)
            .build();

        AiResult result = aiClient.generate(request);  // 실제 구현체는 Gemini 또는 OpenAI
        return result.getText();
    }
}
```

**Q: 왜 JSON 형식으로 응답받나요?**
- AI가 일반 텍스트로 응답 → 파싱 어려움 (정규식 복잡)
- JSON 응답 → Jackson으로 바로 DTO 변환 가능
- **단, AI는 ```json ... ``` 마크다운을 붙이기도 함 → 제거 로직 필요**

**Q: 왜 temperature를 0.3으로 낮게 설정하나요?**
- temperature 높음 (0.8~1.0): 창의적, 랜덤성 높음
- temperature 낮음 (0.1~0.3): 일관적, 결정론적
- 포트폴리오 추천은 일관성이 중요 → 0.3 선택

**Q: 왜 "합의형 포트폴리오"라고 명시하나요?**
- 단순 프롬프트: "4명의 포트폴리오를 섞어주세요" → AI가 25%씩 평균
- 합의형 프롬프트: "4명이 회의실에서 토론 후 합의" → AI가 철학적 교집합 도출

#### ✔ 이 기능에서 초보자가 가장 헷갈리는 지점

**헷갈림 1**: "AI가 JSON을 안 지키고 텍스트를 반환하면 어떻게 하나요?"
- 프롬프트에 "**JSON만 출력하세요**" 강조
- 응답 검증 로직 추가 (JSON 파싱 실패 시 재시도)

[CODE-SNIPPET]
```java
// JSON 검증 예시
try {
    ObjectMapper mapper = new ObjectMapper();
    PortfolioRecommendation dto = mapper.readValue(response, PortfolioRecommendation.class);

    // 비중 합계 검증
    double sum = dto.getStocks().stream()
        .mapToDouble(Stock::getWeightPercent)
        .sum();

    if (Math.abs(sum - 100.0) > 1.0) {
        throw new IllegalStateException("비중 합계가 100%가 아닙니다: " + sum);
    }

} catch (JsonProcessingException e) {
    log.error("JSON 파싱 실패: {}", response);
    throw new RuntimeException("AI 응답이 올바른 JSON 형식이 아닙니다.");
}
```

**헷갈림 2**: "maxTokens가 부족하면 응답이 잘리나요?"
- 네, 중간에 잘림 (예: 10개 종목 중 7개만 반환)
- Gemini 2.0: 8192 토큰까지 출력 가능 (넉넉함)
- GPT-4: 4096 토큰 제한 (긴 응답 시 주의)

---

### 🔹 기능 6: 뉴스 크롤링 및 번역 (스케줄러 + AI)

#### ✔ 이 기능은 왜 필요한가?

투자 판단에는 **최신 뉴스**가 필수다.
Yahoo Finance, Naver Finance 등에서 수동으로 복사 → 비효율적
자동 크롤링 + AI 번역 → 실시간 뉴스 피드 제공

#### ✔ 사용자가 이 기능을 쓰면 어떤 경험을 하게 되는가?

1. 메인 페이지 → "오늘의 뉴스" 섹션
2. 최신 24시간 이내 뉴스 20개 표시
3. 영문 뉴스는 한글 번역 자동 제공
4. 클릭 시 → 원문 링크 + 전문 보기

**백그라운드 동작**:
- 매일 오전 9시 자동 크롤링 (Spring @Scheduled)
- 중복 URL 자동 필터링
- 24시간 경과 뉴스는 `ARCHIVE` 상태로 변경

#### ✔ 내부 동작 흐름 (초보자 시점)

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/service/insights/NewsSchedulerService.java`
- Class: `NewsSchedulerService`
- Method: `scheduleDailyCrawling()`
- Related: `YahooFinanceCrawlerService`, `RssNewsCrawlerService`, `GPTService`

**전체 흐름**:
```
1. Spring @Scheduled 트리거 (매일 09:00)
   ↓
2. YahooFinanceCrawlerService.crawlLatestNews()
   ↓
3. Jsoup으로 HTML 파싱
   https://finance.yahoo.com/topic/stock-market-news
   ↓
4. 뉴스 제목, URL, 본문 추출
   <div class="Ov(h)">
     <h3>Market opens higher...</h3>
     <a href="/news/...">Read more</a>
   </div>
   ↓
5. 중복 URL 체크 (URL은 UNIQUE 제약)
   ↓
6. GPTService.translateNewsToKorean(englishContent)
   AI: "Market opens higher" → "시장이 상승세로 개장"
   ↓
7. InsightsNews 엔티티 생성
   - originalContent: 영어 원문
   - content: 한글 번역
   - status: DAILY
   ↓
8. DB 저장 (InsightsNewsRepository)
   ↓
9. 24시간 경과 뉴스 자동 ARCHIVE 처리
   UPDATE news SET status = 'ARCHIVE'
   WHERE created_at < NOW() - INTERVAL 24 HOUR
```

[CODE-SNIPPET]
```java
@Service
@Slf4j
public class NewsSchedulerService {

    private final YahooFinanceCrawlerService yahooFinanceCrawler;
    private final RssNewsCrawlerService rssNewsCrawler;
    private final DailyNewsService dailyNewsService;

    /**
     * 매일 오전 9시에 뉴스 크롤링 실행
     */
    @Scheduled(cron = "0 0 9 * * *")  // 초 분 시 일 월 요일
    public void scheduleDailyCrawling() {
        log.info("========== 일일 뉴스 크롤링 시작 ==========");

        try {
            // 1. Yahoo Finance 크롤링
            int yahooCount = yahooFinanceCrawler.crawlLatestNews();
            log.info("Yahoo Finance: {}개 뉴스 수집", yahooCount);

            // 2. RSS 크롤링 (MarketWatch, Reuters 등)
            int rssCount = rssNewsCrawler.crawlFromRss();
            log.info("RSS Feeds: {}개 뉴스 수집", rssCount);

            // 3. 24시간 경과 뉴스 자동 아카이브
            int archivedCount = dailyNewsService.archiveOldNews();
            log.info("{}개 뉴스 아카이브 처리", archivedCount);

            log.info("========== 일일 뉴스 크롤링 완료 ==========");

        } catch (Exception e) {
            log.error("뉴스 크롤링 중 오류 발생", e);
        }
    }
}
```

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/service/insights/YahooFinanceCrawlerService.java`
- Class: `YahooFinanceCrawlerService`
- Method: `crawlLatestNews()`

[CODE-SNIPPET]
```java
public int crawlLatestNews() {
    String url = "https://finance.yahoo.com/topic/stock-market-news";
    int count = 0;

    try {
        // Jsoup으로 HTML 다운로드
        Document doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0")
            .timeout(10000)
            .get();

        // 뉴스 아이템 선택
        Elements newsItems = doc.select("div[class*='Ov(h)']");

        for (Element item : newsItems) {
            try {
                String title = item.select("h3").text();
                String link = item.select("a").attr("href");

                // 상대 URL → 절대 URL 변환
                if (link.startsWith("/")) {
                    link = "https://finance.yahoo.com" + link;
                }

                // 중복 체크 (URL은 UNIQUE 제약)
                if (newsRepository.existsByUrl(link)) {
                    log.debug("중복 뉴스 건너뜀: {}", title);
                    continue;
                }

                // 본문 추출 (링크 클릭하여 상세 페이지 크롤링)
                String content = extractArticleContent(link);

                // AI 번역
                String translatedContent = gptService.translateNewsToKorean(content);

                // 엔티티 생성 및 저장
                InsightsNews news = InsightsNews.builder()
                    .title(title)
                    .url(link)
                    .originalContent(content)        // 영어 원문
                    .content(translatedContent)      // 한글 번역
                    .source("Yahoo Finance")
                    .publishedAt(LocalDateTime.now())
                    .status(NewsStatus.DAILY)
                    .build();

                newsRepository.save(news);
                count++;

            } catch (Exception e) {
                log.error("뉴스 파싱 중 오류: {}", item.text(), e);
            }
        }

    } catch (IOException e) {
        log.error("Yahoo Finance 크롤링 실패", e);
    }

    return count;
}
```

#### ✔ 왜 이 기술을 선택했는가?

**Q: 왜 Jsoup인가?**
- Selenium: 무겁고 느림 (브라우저 실행 필요)
- Jsoup: 가볍고 빠름 (HTML 파싱만)
- Yahoo Finance는 JavaScript 렌더링 불필요 → Jsoup으로 충분

**Q: 왜 중복 URL 체크를 DB 레벨에서 하나요?**
- DB: `UNIQUE INDEX(url)` 제약 → 중복 INSERT 시 자동 에러
- Application: `newsRepository.existsByUrl()` → 한 번 더 체크
- 이유: DB 제약이 최종 방어선, Application 체크로 불필요한 AI 번역 방지

**Q: 왜 번역을 AI에게 맡기나요? (Google Translate API는?)**
- Google Translate: 직역, 금융 용어 오역 많음
- AI: 문맥 이해, 자연스러운 번역
- 예: "bearish sentiment" → 구글 "곰같은 감정" / AI "약세 심리"

**Q: 왜 24시간 경과 뉴스를 삭제하지 않고 ARCHIVE로 변경하나요?**
- 삭제: 데이터 유실, 통계 분석 불가
- ARCHIVE: 메인 페이지에서만 숨김, DB에는 유지
- 나중에 "과거 뉴스 검색" 기능 추가 가능

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/service/insights/DailyNewsService.java`
- Class: `DailyNewsService`
- Method: `archiveOldNews()`

[CODE-SNIPPET]
```java
@Transactional
public int archiveOldNews() {
    LocalDateTime threshold = LocalDateTime.now().minusHours(24);

    // 24시간 경과 + DAILY 상태인 뉴스 조회
    List<InsightsNews> oldNews = newsRepository
        .findByStatusAndCreatedAtBefore(NewsStatus.DAILY, threshold);

    // ARCHIVE 상태로 변경
    for (InsightsNews news : oldNews) {
        news.setStatus(NewsStatus.ARCHIVE);
    }

    log.info("{}개 뉴스를 ARCHIVE로 변경", oldNews.size());
    return oldNews.size();
}
```

#### ✔ 이 기능에서 초보자가 가장 헷갈리는 지점

**헷갈림 1**: "Cron 표현식이 뭔가요?"
- `@Scheduled(cron = "0 0 9 * * *")`
- 형식: `초(0-59) 분(0-59) 시(0-23) 일(1-31) 월(1-12) 요일(0-7)`
- `0 0 9 * * *` = 매일 오전 9시 0분 0초

**헷갈림 2**: "Jsoup 선택자(selector)가 CSS 선택자와 같나요?"
- 네, Jsoup은 CSS 선택자 문법 사용
- `doc.select("div.Ov(h)")` → class명에 `Ov(h)` 포함
- `doc.select("div[class*='Ov(h)']")` → 더 정확한 표현

---

### 🔹 기능 7: Soft Delete + 감사 로그 (규제 대응)

#### ✔ 이 기능은 왜 필요한가?

관리자가 부적절한 게시글이나 뉴스를 삭제할 때 → **완전 삭제하면 안 됨**
이유:
1. 법적 분쟁 시 증거 필요
2. 규제 기관 요구 시 삭제 이력 제출
3. 사용자 이의제기 시 복구 가능

**Soft Delete란?**
- 물리적 삭제(DELETE) 대신 `is_deleted = true` 플래그만 변경
- 화면에서는 보이지 않지만 DB에는 유지

#### ✔ 사용자가 이 기능을 쓰면 어떤 경험을 하게 되는가?

**관리자 시점**:
1. 관리자 페이지 → "콘텐츠 관리" 탭
2. 부적절한 게시글 발견 → "삭제" 버튼 클릭
3. 삭제 사유 입력 (필수): "욕설 포함"
4. 확인 → 게시글이 목록에서 사라짐

**일반 사용자 시점**:
- 삭제된 게시글은 보이지 않음
- 직접 URL 접근 시 → "삭제된 게시글입니다" 메시지

**감사 로그**:
- 누가 (관리자 이메일)
- 언제 (삭제 시각)
- 무엇을 (게시글 ID, 제목)
- 왜 (삭제 사유)
- 어디서 (IP 주소, User-Agent)

#### ✔ 내부 동작 흐름 (초보자 시점)

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/service/admin/AdminContentDeletionService.java`
- Class: `AdminContentDeletionService`
- Method: `softDeleteContent(Long contentId, String reason, String actorEmail)`
- Related: `ContentReview`, `AuditDeleteLog`

**전체 흐름**:
```
1. 관리자가 "삭제" 버튼 클릭
   ↓
2. AdminContentReviewController.deleteContent() 호출
   ↓
3. SecurityContext에서 관리자 정보 추출
   - 이메일: admin@example.com
   - 역할: ADMIN
   - IP: 192.168.1.100
   ↓
4. AdminContentDeletionService.softDeleteContent() 호출
   ↓
5. ContentReview 엔티티 조회
   ↓
6. Soft Delete 메서드 호출
   contentReview.softDelete(actorEmail, reason);
   - isDeleted = true
   - deletedAt = 현재 시각
   - deletedBy = "admin@example.com"
   - deleteReason = "욕설 포함"
   ↓
7. 감사 로그 생성 (AuditDeleteLog)
   - action = SOFT_DELETE
   - targetType = CONTENT_REVIEW
   - targetId = 123
   - actorEmail = "admin@example.com"
   - ipAddress = "192.168.1.100"
   ↓
8. 트랜잭션 커밋 (두 테이블 동시 업데이트)
```

[CODE-SNIPPET]
```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminContentDeletionService {

    private final ContentReviewRepository contentRepository;
    private final AuditDeleteLogRepository auditLogRepository;

    /**
     * 콘텐츠 소프트 삭제 (감사 로그 포함)
     */
    public void softDeleteContent(
            Long contentId,
            String reason,
            String actorEmail,
            String actorRole,
            String ipAddress,
            String userAgent) {

        // 1. 콘텐츠 조회
        ContentReview content = contentRepository.findById(contentId)
            .orElseThrow(() -> new IllegalArgumentException("콘텐츠를 찾을 수 없습니다: " + contentId));

        // 2. 이미 삭제된 경우
        if (content.getIsDeleted()) {
            throw new IllegalStateException("이미 삭제된 콘텐츠입니다");
        }

        // 3. Soft Delete 실행
        content.softDelete(actorEmail, reason);

        log.info("콘텐츠 소프트 삭제: ID={}, 제목={}, 삭제자={}, 사유={}",
                contentId, content.getTitle(), actorEmail, reason);

        // 4. 감사 로그 생성
        AuditDeleteLog auditLog = AuditDeleteLog.builder()
            .action(DeleteAction.SOFT_DELETE)
            .targetType(TargetType.CONTENT_REVIEW)
            .targetId(contentId)
            .targetTitle(content.getTitle())
            .actorEmail(actorEmail)
            .actorRole(actorRole)
            .reason(reason)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();

        auditLogRepository.save(auditLog);

        log.info("감사 로그 저장 완료: ID={}", auditLog.getId());
    }
}
```

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/entity/content/ContentReview.java`
- Entity: `ContentReview`
- Method: `softDelete(String deletedBy, String deleteReason)`

[CODE-SNIPPET]
```java
@Entity
public class ContentReview {

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;  // 삭제한 관리자 이메일

    @Column(name = "delete_reason", columnDefinition = "TEXT")
    private String deleteReason;

    /**
     * 관리자용 소프트 삭제
     */
    public void softDelete(String deletedBy, String deleteReason) {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
        this.deleteReason = deleteReason;
    }

    /**
     * 복구 (관리자가 실수로 삭제한 경우)
     */
    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
        this.deleteReason = null;
    }
}
```

#### ✔ 왜 이 기술을 선택했는가?

**Q: 왜 Soft Delete인가? 하드 삭제(DELETE)는?**
- 하드 삭제: 복구 불가, 법적 리스크
- Soft Delete: 복구 가능, 감사 로그와 조합 시 완벽한 추적성
- GDPR 등 개인정보 규제는 30일 후 하드 삭제로 대응

**Q: 왜 감사 로그를 별도 테이블로 분리했나요?**
- ContentReview 테이블: 비즈니스 데이터
- AuditDeleteLog 테이블: 감사 데이터 (절대 삭제 금지)
- 분리 이유: 규제 요구 시 감사 로그만 따로 제출 가능

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/entity/audit/AuditDeleteLog.java`
- Entity: `AuditDeleteLog`
- Enums: `DeleteAction (SOFT_DELETE, HARD_DELETE)`, `TargetType (NEWS, CONTENT_REVIEW)`

**Q: 왜 IP 주소와 User-Agent를 저장하나요?**
- 계정 해킹 의심 시 IP 추적
- 자동화 도구(봇) 감지
- 법적 분쟁 시 증거 자료

[CODE-SNIPPET]
```java
@Entity
public class AuditDeleteLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private DeleteAction action;  // SOFT_DELETE, HARD_DELETE

    @Enumerated(EnumType.STRING)
    private TargetType targetType;  // NEWS, CONTENT_REVIEW

    private Long targetId;
    private String targetTitle;

    private String actorEmail;    // 삭제한 사람
    private String actorRole;     // ADMIN

    private String reason;        // 삭제 사유
    private String ipAddress;     // 192.168.1.100
    private String userAgent;     // Mozilla/5.0...

    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

#### ✔ 이 기능에서 초보자가 가장 헷갈리는 지점

**헷갈림 1**: "Soft Delete된 게시글은 어떻게 숨기나요?"
- Repository 쿼리에 조건 추가: `WHERE is_deleted = false`
- 또는 `@Where(clause = "is_deleted = false")` 어노테이션 (JPA)

[CODE-SNIPPET]
```java
public interface ContentReviewRepository extends JpaRepository<ContentReview, Long> {

    // Soft Delete된 게시글 제외
    @Query("SELECT c FROM ContentReview c WHERE c.isDeleted = false ORDER BY c.createdDate DESC")
    Page<ContentReview> findActiveContents(Pageable pageable);

    // 삭제된 게시글만 조회 (관리자용)
    @Query("SELECT c FROM ContentReview c WHERE c.isDeleted = true")
    List<ContentReview> findDeletedContents();
}
```

**헷갈림 2**: "30일 후 하드 삭제는 어떻게 구현하나요?"
- Spring @Scheduled로 매일 자동 실행
- `deletedAt < NOW() - INTERVAL 30 DAY` 조건으로 조회
- 실제 DELETE 쿼리 실행

[CODE-SNIPPET]
```java
@Scheduled(cron = "0 0 2 * * *")  // 매일 새벽 2시
@Transactional
public void hardDeleteOldRecords() {
    LocalDateTime threshold = LocalDateTime.now().minusDays(30);

    // 30일 경과 + 소프트 삭제된 콘텐츠 조회
    List<ContentReview> oldContents = contentRepository
        .findByIsDeletedTrueAndDeletedAtBefore(threshold);

    for (ContentReview content : oldContents) {
        // 감사 로그 기록
        AuditDeleteLog hardDeleteLog = AuditDeleteLog.builder()
            .action(DeleteAction.HARD_DELETE)
            .targetType(TargetType.CONTENT_REVIEW)
            .targetId(content.getId())
            .reason("30일 경과 자동 삭제")
            .build();

        auditLogRepository.save(hardDeleteLog);

        // 하드 삭제 (실제 DELETE)
        contentRepository.delete(content);
        log.info("하드 삭제 완료: ID={}", content.getId());
    }
}
```

---

## 4️⃣ 보안 / MyCloset 설계 의도

*(Note: "MyCloset"은 오타로 추정됨. 이 프로젝트에는 MyCloset 관련 코드가 없음. 대신 보안 설계에 초점)*

---

### 🔒 보안 설계 (Spring Security + BCrypt + CSRF)

#### ✔ 왜 보안이 중요한가?

금융 플랫폼은 **사용자의 포트폴리오 정보**를 다룬다.
보안 취약점 → 계정 탈취, 정보 유출, 신뢰도 하락

**이 프로젝트의 보안 위협**:
1. SQL Injection: 악의적 SQL 쿼리 주입
2. XSS (Cross-Site Scripting): 악성 스크립트 실행
3. CSRF (Cross-Site Request Forgery): 위조 요청
4. 비밀번호 평문 저장: DB 해킹 시 즉시 노출
5. 무단 권한 상승: 일반 사용자 → 관리자 권한 탈취

#### ✔ 보안 대책 1: Spring Security + RBAC

**RBAC (Role-Based Access Control)**: 역할 기반 접근 제어

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/config/SecurityConfig.java`
- Class: `SecurityConfig`
- Method: `filterChain(HttpSecurity)`

[CODE-SNIPPET]
```java
// 프로덕션 배포 시 활성화할 설정 (현재는 주석 처리)
.authorizeHttpRequests(auth -> auth
    // 1. 누구나 접근 가능
    .requestMatchers("/", "/user/signup", "/user/login").permitAll()
    .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()

    // 2. 관리자만 접근 가능
    .requestMatchers("/admin/**").hasRole("ADMIN")

    // 3. 로그인한 사용자만 접근 가능
    .requestMatchers("/user/mypage/**").authenticated()

    // 4. 나머지는 모두 허용
    .anyRequest().permitAll()
)
```

**동작 원리**:
1. 사용자가 `/admin/content` 접근 시도
2. Spring Security가 `hasRole("ADMIN")` 확인
3. 현재 사용자 Role이 `USER`면 → 403 Forbidden
4. `ADMIN`이면 → 접근 허용

#### ✔ 보안 대책 2: BCrypt 비밀번호 암호화

**왜 평문 저장은 위험한가?**
- DB 해킹 시 모든 비밀번호 즉시 노출
- 내부 직원이 비밀번호 열람 가능

**BCrypt의 특징**:
- Salt 자동 생성 (같은 비밀번호도 매번 다른 해시)
- 레인보우 테이블 공격 방어
- 의도적으로 느림 (브루트포스 공격 어렵게)

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/service/user/UserService.java`
- Method: `signup(UserSignupDTO)`

[CODE-SNIPPET]
```java
// 회원가입 시
String plainPassword = "myPassword123";
String encodedPassword = passwordEncoder.encode(plainPassword);
// "$2a$10$N9qo8..." (매번 다른 값)

user.setPassword(encodedPassword);  // 암호화된 값 저장

// 로그인 시
boolean matches = passwordEncoder.matches(inputPassword, user.getPassword());
if (matches) {
    // 로그인 성공
}
```

#### ✔ 보안 대책 3: CSRF 방어

**CSRF란?**
- 사용자가 의도하지 않은 요청을 악성 사이트가 대신 전송
- 예: 로그인 상태에서 악성 링크 클릭 → 계정 삭제 요청 전송

**Spring Security CSRF 토큰**:
```html
<!-- Thymeleaf 템플릿 -->
<form action="/user/delete" method="post">
    <input type="hidden" name="_csrf" value="${_csrf.token}">
    <button type="submit">계정 삭제</button>
</form>
```

**동작 원리**:
1. 서버가 페이지 로드 시 랜덤 토큰 생성
2. Form에 숨김 필드로 포함
3. POST 요청 시 토큰 검증
4. 토큰 없으면 → 403 Forbidden

**현재 프로젝트 설정**:
```java
.csrf(csrf -> csrf.disable())  // REST API용으로 비활성화
```
→ **주의**: 프로덕션에서는 활성화 권장! API는 JWT 토큰으로 대체

#### ✔ 보안 대책 4: SQL Injection 방어

**JPA의 자동 방어**:
```java
// 안전 (Prepared Statement 자동 사용)
@Query("SELECT u FROM User u WHERE u.email = :email")
Optional<User> findByEmail(@Param("email") String email);

// 위험 (Native Query + 문자열 연결)
String sql = "SELECT * FROM users WHERE email = '" + email + "'";
// → email = "'; DROP TABLE users; --" 입력 시 테이블 삭제!
```

**이 프로젝트의 방어**:
- 모든 쿼리는 Spring Data JPA 메서드 또는 `@Query` 사용
- 직접 SQL 문자열 연결 금지

#### ✔ 보안 대책 5: XSS 방어

**XSS란?**
- 악성 스크립트를 게시글에 삽입
- 예: `<script>alert(document.cookie)</script>` → 쿠키 탈취

**Thymeleaf의 자동 방어**:
```html
<!-- 안전: 자동 이스케이프 -->
<p th:text="${content}"></p>
<!-- 입력: <script>alert('XSS')</script> -->
<!-- 출력: &lt;script&gt;alert('XSS')&lt;/script&gt; (텍스트로 출력) -->

<!-- 위험: 이스케이프 해제 (신뢰할 수 있는 HTML만 사용!) -->
<div th:utext="${content}"></div>
```

**이 프로젝트의 방어**:
- 사용자 입력은 모두 `th:text` 사용
- HTML 허용이 필요한 경우 (게시글 본문) → Sanitization 라이브러리 추가 권장

---

## 5️⃣ AI 통합 설계 (OpenAI ↔ Gemini 스위칭)

#### ✔ 왜 AI 프로바이더를 교체 가능하게 만들었는가?

**문제 상황**:
- OpenAI API 장애 시 → 서비스 중단
- 비용 절감 필요 시 → 코드 전체 수정

**해결책**: Strategy Pattern + 환경변수 스위칭

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/ai/client/AiClient.java`
- Interface: `AiClient`
- Implementations: `OpenAiClient`, `GeminiClient`

#### ✔ 설계 구조

```
┌─────────────────────────────────────────┐
│  GPTService (비즈니스 로직)              │
│  - translateNews()                       │
│  - generatePortfolio()                   │
└─────────────────────────────────────────┘
              ↓ 의존
┌─────────────────────────────────────────┐
│  AiClient (인터페이스)                   │
│  + generate(AiRequest) : AiResult       │
└─────────────────────────────────────────┘
       ↑                        ↑
       │                        │
┌──────────────┐       ┌──────────────┐
│ OpenAiClient │       │ GeminiClient │
│ (GPT-4)      │       │ (Gemini 2.0) │
└──────────────┘       └──────────────┘
```

[CODE-SNIPPET]
```java
// 1. 인터페이스 정의
public interface AiClient {
    AiResult generate(AiRequest request);
    String getProviderName();
}

// 2. OpenAI 구현체
@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
public class OpenAiClient implements AiClient {

    @Override
    public AiResult generate(AiRequest request) {
        // OpenAI API 호출
        ChatCompletionRequest chatRequest = ChatCompletionRequest.builder()
            .model(request.getModel() != null ? request.getModel() : "gpt-4")
            .messages(...)
            .temperature(request.getTemperature())
            .maxTokens(request.getMaxTokens())
            .build();

        ChatCompletionResult result = openAiService.createChatCompletion(chatRequest);
        return AiResult.builder()
            .text(result.getChoices().get(0).getMessage().getContent())
            .provider("openai")
            .build();
    }
}

// 3. Gemini 구현체
@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiClient implements AiClient {

    @Override
    public AiResult generate(AiRequest request) {
        // Gemini API 호출 (WebClient 사용)
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(Map.of("parts", List.of(Map.of("text", request.getUser())))),
            "generationConfig", Map.of(
                "temperature", request.getTemperature(),
                "maxOutputTokens", request.getMaxTokens()
            )
        );

        String response = webClient.post()
            .uri("/v1beta/models/{model}:generateContent?key={apiKey}",
                 request.getModel() != null ? request.getModel() : "gemini-2.0-flash-exp",
                 geminiApiKey)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .block();

        // JSON 파싱하여 텍스트 추출
        JsonNode root = objectMapper.readTree(response);
        String text = root.at("/candidates/0/content/parts/0/text").asText();

        return AiResult.builder()
            .text(text)
            .provider("gemini")
            .build();
    }
}
```

#### ✔ 환경변수 기반 전환

```properties
# application.properties
ai.provider=gemini  # openai 또는 gemini

# OpenAI 설정
openai.api.key=sk-...

# Gemini 설정
gemini.api.key=AIza...
```

**동작 원리**:
1. Spring Boot 시작 시 `ai.provider` 읽기
2. `@ConditionalOnProperty`로 해당 구현체만 Bean 등록
3. `GPTService`는 `AiClient` 인터페이스만 의존
4. 실행 시점에 실제 구현체 주입 (OpenAi 또는 Gemini)

#### ✔ 이 설계의 장점

1. **코드 변경 없이 프로바이더 전환**: 환경변수만 수정
2. **비용 최적화**: Gemini 무료 할당량 사용 → OpenAI 호출 감소
3. **장애 대응**: OpenAI 다운 시 → Gemini로 즉시 전환
4. **테스트 용이**: Mock AiClient 구현으로 단위 테스트

---

## 6️⃣ OCR 전처리 전략 (증권사별 커스터마이징)

#### ✔ 왜 증권사별 전처리가 필요한가?

증권사마다 화면 디자인이 다르다:
- 토스: 파란 배경, 작은 글씨, 그림자 효과
- 미래에셋: 흰 배경, 큰 글씨, 단순 레이아웃
- 키움증권: 빨간/파란 숫자 (수익률), 복잡한 레이아웃

→ **하나의 전처리 방식으로는 모든 증권사 대응 불가**

#### ✔ 전략 패턴 (Strategy Pattern) 적용

[CODE-REF]
- Path: `src/main/java/org/zerock/finance_dwpj1/service/portfolio/ocr/OcrPreprocessor.java`
- Interface: `OcrPreprocessor`
- Implementations: `TossPreprocessor`, `DefaultPreprocessor`

```
┌────────────────────────────────────────┐
│  OcrService                            │
│  - extractPortfolioFromImage()         │
└────────────────────────────────────────┘
              ↓ 증권사 타입 전달
┌────────────────────────────────────────┐
│  OcrPreprocessor (인터페이스)          │
│  + preprocess(BufferedImage) : Image   │
│  + getSupportedBroker() : BrokerType   │
└────────────────────────────────────────┘
       ↑                        ↑
       │                        │
┌──────────────┐       ┌─────────────────┐
│ TossPreprocessor  │   │ DefaultPreprocessor │
│ (파란 배경 제거)   │   │ (기본 전처리)       │
└──────────────┘       └─────────────────┘
```

[CODE-SNIPPET]
```java
// 1. 인터페이스 정의
public interface OcrPreprocessor {
    BufferedImage preprocess(BufferedImage image);
    BrokerType getSupportedBroker();
}

// 2. 토스 전용 전처리
@Component
public class TossPreprocessor implements OcrPreprocessor {

    @Override
    public BufferedImage preprocess(BufferedImage image) {
        // 1. 파란 배경 제거 (HSV 색공간 변환)
        Mat mat = bufferedImageToMat(image);
        Mat hsvMat = new Mat();
        Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_BGR2HSV);

        // 파란색 범위 마스킹 (Toss 파란 배경)
        Scalar lowerBlue = new Scalar(100, 50, 50);
        Scalar upperBlue = new Scalar(130, 255, 255);
        Mat mask = new Mat();
        Core.inRange(hsvMat, lowerBlue, upperBlue, mask);

        // 배경을 흰색으로 변환
        Mat whiteBackground = new Mat(mat.size(), mat.type(), new Scalar(255, 255, 255));
        whiteBackground.copyTo(mat, mask);

        // 2. 그레이스케일 변환
        Mat grayMat = new Mat();
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY);

        // 3. 적응형 임계처리 (글씨 선명화)
        Mat binaryMat = new Mat();
        Imgproc.adaptiveThreshold(grayMat, binaryMat, 255,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            15, 10);

        return matToBufferedImage(binaryMat);
    }

    @Override
    public BrokerType getSupportedBroker() {
        return BrokerType.TOSS;
    }
}

// 3. 기본 전처리
@Component
public class DefaultPreprocessor implements OcrPreprocessor {

    @Override
    public BufferedImage preprocess(BufferedImage image) {
        // 간단한 그레이스케일 + 대비 증가
        Mat mat = bufferedImageToMat(image);
        Mat grayMat = new Mat();
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY);

        // 대비 증가 (alpha=1.5, beta=0)
        Mat enhancedMat = new Mat();
        grayMat.convertTo(enhancedMat, -1, 1.5, 0);

        return matToBufferedImage(enhancedMat);
    }

    @Override
    public BrokerType getSupportedBroker() {
        return BrokerType.DEFAULT;
    }
}
```

#### ✔ OcrService에서 전처리기 선택

[CODE-SNIPPET]
```java
public List<PortfolioStock> extractPortfolioFromImage(MultipartFile imageFile, BrokerType brokerType) {
    // ...

    // 증권사별 전처리기 조회 (Map에 미리 등록됨)
    OcrPreprocessor preprocessor = preprocessors.get(brokerType);

    BufferedImage preprocessedImage = image;
    if (preprocessor != null) {
        log.info("이미지 전처리 시작 - {}", brokerType);
        preprocessedImage = preprocessor.preprocess(image);
    } else {
        log.warn("{}에 대한 전처리기를 찾을 수 없습니다. 원본 이미지 사용", brokerType);
    }

    // OCR 실행
    String extractedText = tesseractThreadLocal.get().doOCR(preprocessedImage);
    // ...
}
```

#### ✔ 새로운 증권사 추가 방법

1. 새 전처리기 클래스 생성
```java
@Component
public class KiwoomPreprocessor implements OcrPreprocessor {

    @Override
    public BufferedImage preprocess(BufferedImage image) {
        // 키움증권 전용 전처리 로직
        // ...
    }

    @Override
    public BrokerType getSupportedBroker() {
        return BrokerType.KIWOOM;
    }
}
```

2. BrokerType Enum에 추가
```java
public enum BrokerType {
    DEFAULT,
    TOSS,
    KIWOOM  // 추가
}
```

3. 끝! (OcrService는 자동으로 새 전처리기 인식)

---

## 7️⃣ 데이터베이스 설계 철학

*(ERD.md에 이미 상세 설명되어 있으므로 핵심만 요약)*

### 🎯 설계 원칙 4가지

#### 1. 확장성 (Scalability)
- **도메인별 패키지 분리**: User, Stock, Content, Insights, Portfolio
- **인덱스 전략**: `idx_hashtags`, `idx_status_created_at`
- **캐시 테이블**: `STOCK_QUOTE_CACHE`, `STOCK_CANDLE_CACHE`

#### 2. 추적 가능성 (Traceability)
- **Soft Delete**: `is_deleted`, `deleted_at`, `deleted_by`
- **Audit Log**: `AUDIT_DELETE_LOG` (삭제 행위 추적)
- **타임스탬프**: `created_at`, `updated_at`

#### 3. 데이터 무결성 (Data Integrity)
- **Foreign Key**: `board_id` → `STOCK_BOARD.id`
- **Unique 제약**: `email`, `url` (중복 방지)
- **Enum 타입**: `Role`, `NewsStatus`

#### 4. 성능 최적화 (Performance)
- **LAZY 로딩**: `@ManyToOne(fetch = FetchType.LAZY)`
- **복합키**: `STOCK_CANDLE_CACHE (ticker + date + timeframe)`
- **캐시 TTL**: 15분마다 갱신

### 🔍 헷갈리는 지점: "왜 캐시 테이블을 만들었나요?"

**문제**:
- Yahoo Finance API: 분당 2,000회 제한
- 사용자 100명이 동시에 AAPL 시세 조회 → 100회 API 호출

**해결**:
```java
// 1. 캐시 확인
StockQuoteCache cache = cacheRepository.findByTicker("AAPL");
if (cache != null && cache.getUpdatedAt().isAfter(LocalDateTime.now().minusMinutes(15))) {
    // 15분 이내 데이터 → 캐시 반환
    return cache.toDto();
}

// 2. 캐시 없음 → API 호출 후 저장
Stock stock = yahooFinanceApi.getQuote("AAPL");
cache = StockQuoteCache.fromStock(stock);
cacheRepository.save(cache);
```

**결과**: API 호출 100회 → 1회로 감소!

---

## 8️⃣ 초보자가 가장 헷갈리는 개념 정리

### 🤔 헷갈림 1: "Entity vs DTO, 왜 분리하나요?"

**Entity**: DB 테이블과 1:1 매핑
```java
@Entity
public class User {
    private Long id;
    private String email;
    private String password;  // 암호화된 비밀번호
}
```

**DTO (Data Transfer Object)**: View와 Controller 간 데이터 전달
```java
public class UserSignupDTO {
    private String email;
    private String nickname;
    private String password;  // 평문 비밀번호 (암호화 전)
    // getter/setter
}
```

**왜 분리?**
1. Entity를 View에 노출 → 비밀번호 해시값 노출 위험
2. Entity 변경 → View도 같이 깨짐 (강한 결합)
3. DTO는 화면에 필요한 필드만 포함 (최소 권한 원칙)

---

### 🤔 헷갈림 2: "@Transactional은 언제 사용하나요?"

**필요한 경우**:
1. **여러 테이블 동시 수정**: Soft Delete + Audit Log 저장
2. **데이터 일관성 필수**: 포트폴리오 저장 중 오류 → 전체 롤백
3. **LAZY 로딩**: 트랜잭션 밖에서 연관 엔티티 접근 → `LazyInitializationException`

```java
@Transactional
public void softDeleteContent(Long contentId, String reason) {
    // 1. 콘텐츠 삭제
    ContentReview content = contentRepository.findById(contentId).orElseThrow();
    content.softDelete();

    // 2. 감사 로그 저장
    AuditDeleteLog log = AuditDeleteLog.builder()
        .targetId(contentId)
        .reason(reason)
        .build();
    auditLogRepository.save(log);

    // → 둘 중 하나라도 실패하면 전체 롤백!
}
```

**불필요한 경우**:
- 단순 조회 (SELECT only)
- 단일 테이블 저장 (Spring Data JPA가 자동 트랜잭션 처리)

---

### 🤔 헷갈림 3: "코사인 유사도 공식이 이해 안 돼요"

**일반인 설명**:
- 두 벡터의 "방향"이 얼마나 비슷한지 측정
- 크기(절댓값)는 무시, 방향만 비교

**예시**:
```
포트폴리오 A = [AAPL: 50%, TSLA: 30%, MSFT: 20%]
포트폴리오 B = [AAPL: 25%, TSLA: 15%, MSFT: 10%]

→ B는 A의 "절반" 크기이지만 방향(비율)은 동일
→ 코사인 유사도 = 1.0 (100% 유사)
```

**공식**:
```
cos(θ) = (A·B) / (||A|| × ||B||)

A·B = 50*25 + 30*15 + 20*10 = 2150
||A|| = √(50² + 30² + 20²) = √3800 = 61.64
||B|| = √(25² + 15² + 10²) = √950 = 30.82

cos(θ) = 2150 / (61.64 × 30.82) = 1.0
```

---

### 🤔 헷갈림 4: "LAZY vs EAGER 로딩, 뭐가 다른가요?"

**EAGER (즉시 로딩)**:
```java
@ManyToOne(fetch = FetchType.EAGER)
private StockBoard board;

// User 조회 시 Board도 자동 조회 (JOIN)
StockComment comment = commentRepository.findById(1L);
String title = comment.getBoard().getTitle();  // DB 쿼리 없음 (이미 로드됨)
```

**LAZY (지연 로딩)**:
```java
@ManyToOne(fetch = FetchType.LAZY)
private StockBoard board;

// Comment 조회 시 Board는 조회 안 함 (프록시 객체만)
StockComment comment = commentRepository.findById(1L);
String title = comment.getBoard().getTitle();  // 이 시점에 DB 쿼리 발생!
```

**언제 LAZY?**
- 연관 엔티티를 항상 사용하지 않을 때
- N+1 문제 방지 (Fetch Join으로 해결)

**언제 EAGER?**
- 연관 엔티티를 항상 사용할 때
- 단, 남용 금지! (불필요한 JOIN 발생)

---

## 9️⃣ 성능 최적화 포인트

### ⚡ 최적화 1: ThreadLocal Tesseract

**문제**: Tesseract는 Thread-Safe하지 않음 → 동시 요청 시 오류

**해결**:
```java
private final ThreadLocal<Tesseract> tesseractThreadLocal = ThreadLocal.withInitial(() -> {
    Tesseract tess = new Tesseract();
    // ... 설정
    return tess;
});

// 사용 시
String text = tesseractThreadLocal.get().doOCR(image);
```

**결과**: 10명 동시 업로드 → 각자 독립적인 Tesseract 인스턴스 사용

---

### ⚡ 최적화 2: RateLimiter (SEC API)

**문제**: SEC API 초당 10회 제한 → 초과 시 IP 차단

**해결**:
```java
private final RateLimiter rateLimiter = RateLimiter.create(9.0);  // 초당 9회

// API 호출 전
rateLimiter.acquire();  // 필요시 자동 대기
String response = httpClient.newCall(request).execute();
```

**결과**: API 제한 위반 없이 안정적 수집

---

### ⚡ 최적화 3: 캐시 전략 (Stock Quote)

**문제**: 100명이 AAPL 시세 조회 → Yahoo Finance API 100회 호출

**해결**:
```java
// 1. 캐시 확인 (15분 TTL)
StockQuoteCache cache = cacheRepository.findByTicker("AAPL");
if (cache != null && cache.isFresh()) {
    return cache.toDto();
}

// 2. API 호출 후 캐시 저장
Stock stock = yahooFinanceApi.getQuote("AAPL");
cacheRepository.save(StockQuoteCache.from(stock));
```

**결과**: API 호출 100회 → 1회 (99% 감소)

---

## 🔟 실제 배포 시 체크리스트

### ✅ 1. Security 설정 활성화
```java
// SecurityConfig.java 주석 해제
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/admin/**").hasRole("ADMIN")
    .requestMatchers("/user/mypage/**").authenticated()
)
```

### ✅ 2. CSRF 활성화
```java
.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
```

### ✅ 3. 환경변수 분리 (절대 평문 저장 금지!)
```bash
# .env 파일 (Git 커밋 금지!)
OPENAI_API_KEY=sk-...
GEMINI_API_KEY=AIza...
DB_PASSWORD=...
```

### ✅ 4. 로그 레벨 조정
```properties
# application.properties
logging.level.root=WARN
logging.level.org.zerock.finance_dwpj1=INFO
```

### ✅ 5. HTTPS 적용 (Let's Encrypt)
```properties
server.ssl.enabled=true
server.ssl.certificate=...
server.ssl.key-store-password=...
```

### ✅ 6. 하드 삭제 스케줄러 활성화
```java
@Scheduled(cron = "0 0 2 * * *")  // 매일 새벽 2시
public void hardDeleteOldRecords() {
    // 30일 경과 Soft Delete 데이터 물리 삭제
}
```

---

## 📚 참고 자료

- Spring Security 공식 문서: https://docs.spring.io/spring-security/reference/
- Tesseract OCR Wiki: https://github.com/tesseract-ocr/tesseract/wiki
- SEC EDGAR API: https://www.sec.gov/edgar/sec-api-documentation
- OpenAI API: https://platform.openai.com/docs
- Gemini API: https://ai.google.dev/docs

---

**작성자**: Claude Sonnet 4.5
**최종 수정**: 2025-12-19
**문의**: finance_DWpj1 프로젝트 팀