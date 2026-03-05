# Common Controllers

공통 페이지 라우팅을 담당하는 컨트롤러 패키지입니다.

## 📁 파일 목록

### PageController.java
애플리케이션의 주요 페이지 라우팅을 처리하는 컨트롤러입니다.

#### 주요 엔드포인트
- `GET /` - 메인 홈페이지
- `GET /news` - 뉴스 인사이트 페이지
- `GET /stock-detail` - 주식 상세 정보 페이지
- `GET /portfolio-comparison` - 포트폴리오 & 투자자 비교 페이지

#### 특징
- Thymeleaf 템플릿을 사용하여 뷰 렌더링
- 모든 GET 요청에 대한 페이지 라우팅 담당
- RESTFUL API가 아닌 전통적인 MVC 패턴의 컨트롤러

## 📌 사용 예시

```java
@Controller
@RequestMapping("/")
public class PageController {

    @GetMapping
    public String home() {
        return "index";
    }

    @GetMapping("/news")
    public String news() {
        return "news-insights";
    }
}
```

## 🔗 연관 패키지
- `src/main/resources/templates/` - 뷰 템플릿 파일들
- 다른 API 컨트롤러들 (content, stock, portfolio, insights)