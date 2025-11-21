# Portfolio Comparison JavaScript 모듈

포트폴리오 비교 페이지의 프론트엔드 로직을 담당하는 JavaScript 모듈입니다.

## 📁 파일 구조

```
portfolio-comparison/
├── README.md                  # 이 문서
├── main.js                    # 애플리케이션 진입점
├── investor-manager.js        # 투자자 데이터 관리
├── api-service.js             # API 통신
├── ui-controller.js           # UI 렌더링 및 DOM 조작
└── autocomplete-handler.js    # 검색 자동완성
```

## 🏗️ 아키텍처

### 모듈 의존성 그래프

```
main.js
├── investor-manager.js (데이터 계층)
├── api-service.js (통신 계층)
├── ui-controller.js (뷰 계층)
│   ├── investor-manager.js
│   └── api-service.js
└── autocomplete-handler.js (기능 계층)
    ├── investor-manager.js
    └── api-service.js
```

## 📄 모듈 설명

### 1. main.js
**역할**: 애플리케이션 진입점 및 전체 조율

**주요 기능**:
- 모든 모듈 초기화
- 이벤트 리스너 등록 (이벤트 위임 패턴 사용)
- 모듈 간 통신 조율

**클래스**:
- `PortfolioComparisonApp`: 메인 애플리케이션 클래스

**사용 예시**:
```javascript
// DOMContentLoaded 시 자동 초기화
const app = new PortfolioComparisonApp();
app.init();
```

---

### 2. investor-manager.js
**역할**: 투자자 데이터 및 선택 상태 관리

**주요 기능**:
- 투자자 목록 관리 (기본 투자자 + 커스텀 투자자)
- 선택된 투자자 관리 (최대 4명)
- 투자자 추가/제거
- 투자자 검색 및 필터링

**클래스**:
- `InvestorManager`

**주요 메서드**:
```javascript
setDefaultInvestors()           // 기본 투자자 설정
addInvestor(investorId)         // 투자자 추가
removeInvestor(investorId)      // 투자자 제거
addCustomInvestor(data)         // 커스텀 투자자 추가
getInvestorById(id)             // ID로 투자자 조회
filterInvestors(searchTerm)     // 투자자 검색
canGeneratePortfolio()          // 포트폴리오 생성 가능 여부
```

**데이터 구조**:
```javascript
{
    id: 'buffett',
    name: '워렌 버핏',
    style: '가치투자',
    description: '장기 가치투자의 대가...'
}
```

---

### 3. api-service.js
**역할**: 백엔드 API와의 통신

**주요 기능**:
- 투자자 비교 데이터 조회
- AI 포트폴리오 추천 생성
- 투자자 검색 (GPT API 활용)

**클래스**:
- `APIService`

**주요 메서드**:
```javascript
async fetchComparison(investorIds)     // 비교 데이터 조회
async generatePortfolio(investorIds)   // 포트폴리오 생성
async searchInvestor(investorName)     // 투자자 검색
```

**API 엔드포인트**:
- `POST /api/portfolio/compare` - 투자자 비교
- `POST /api/portfolio/recommend` - 포트폴리오 추천
- `POST /api/portfolio/search` - 투자자 검색

**요청 예시**:
```javascript
const apiService = new APIService();
const data = await apiService.fetchComparison(['buffett', 'lynch']);
```

---

### 4. ui-controller.js
**역할**: UI 렌더링 및 DOM 조작

**주요 기능**:
- 선택된 투자자 카드 렌더링
- 투자 철학 비교 차트 표시
- 포트폴리오 추천 결과 표시
- 모달 열기/닫기
- 버튼 상태 관리

**클래스**:
- `UIController`

**주요 메서드**:
```javascript
updateSelectedInvestors()       // 선택된 투자자 UI 업데이트
openModal()                     // 투자자 선택 모달 열기
closeModal()                    // 모달 닫기
async displayComparison()       // 비교 데이터 표시
updateGenerateButton()          // 생성 버튼 상태 업데이트
async generatePortfolio()       // 포트폴리오 생성 및 표시
displayPortfolio(data)          // 포트폴리오 결과 표시
```

**DOM 요소 ID**:
- `#selected-investors` - 선택된 투자자 컨테이너
- `#philosophy-container` - 비교 차트 컨테이너
- `#portfolio-container` - 포트폴리오 결과 컨테이너
- `#investorModal` - 투자자 선택 모달
- `#generateBtn` - 포트폴리오 생성 버튼

---

### 5. autocomplete-handler.js
**역할**: 검색 입력 자동완성 및 투자자 검색

**주요 기능**:
- 실시간 자동완성 제안
- 키보드 이벤트 처리 (엔터키)
- API를 통한 투자자 검색
- 검색 입력창 관리

**클래스**:
- `AutocompleteHandler`

**주요 메서드**:
```javascript
init()                          // 이벤트 리스너 초기화
handleInput(searchTerm)         // 입력 처리
showSuggestions(investors)      // 제안 목록 표시
hideSuggestions()               // 제안 숨김
selectSuggestion(investorId)    // 제안 선택
async searchInvestor()          // API 검색
handleEnterKey()                // 엔터키 처리
```

**DOM 요소 ID**:
- `#investorSearch` - 검색 입력창
- `#suggestions` - 자동완성 드롭다운
- `#searchBtn` - 검색 버튼

---

## 🔄 데이터 흐름

### 1. 페이지 로드 시
```
DOMContentLoaded
    ↓
PortfolioComparisonApp.init()
    ↓
InvestorManager.setDefaultInvestors()
    ↓
UIController.updateSelectedInvestors()
    ↓
UIController.displayComparison()
```

### 2. 투자자 추가 시
```
사용자 클릭
    ↓
InvestorManager.addInvestor(id)
    ↓
UIController.updateSelectedInvestors()
    ↓
UIController.displayComparison()
    ↓
UIController.updateGenerateButton()
```

### 3. 포트폴리오 생성 시
```
생성 버튼 클릭
    ↓
UIController.generatePortfolio()
    ↓
APIService.generatePortfolio(ids)
    ↓
UIController.displayPortfolio(data)
```

### 4. 투자자 검색 시
```
검색어 입력
    ↓
AutocompleteHandler.handleInput()
    ↓
InvestorManager.filterInvestors()
    ↓
AutocompleteHandler.showSuggestions()
    ↓
사용자 선택 또는 엔터
    ↓
APIService.searchInvestor() (제안 없을 경우)
```

---

## 🎯 주요 디자인 패턴

### 1. 모듈 패턴
- ES6 모듈 시스템 사용
- 각 모듈이 명확한 단일 책임

### 2. 의존성 주입
```javascript
// UIController와 AutocompleteHandler는
// InvestorManager와 APIService를 주입받음
const uiController = new UIController(investorManager, apiService);
const autocompleteHandler = new AutocompleteHandler(investorManager, apiService);
```

### 3. 이벤트 위임
```javascript
// 부모 요소에서 이벤트를 한 번만 등록
document.getElementById('selected-investors').addEventListener('click', (event) => {
    if (event.target.classList.contains('remove-btn')) {
        // 동적으로 생성된 버튼도 처리
    }
});
```

### 4. 비동기 처리
```javascript
// async/await 패턴으로 가독성 높은 비동기 코드
async displayComparison() {
    try {
        const data = await this.apiService.fetchComparison(ids);
        // 데이터 표시
    } catch (error) {
        // 에러 처리
    }
}
```

---

## 🛠️ 사용 방법

### HTML에서 모듈 로드
```html
<script type="module" src="/js/portfolio-comparison/main.js"></script>
```

### 새로운 기능 추가하기

#### 1. 새로운 API 엔드포인트 추가
```javascript
// api-service.js에 메서드 추가
async fetchInvestorDetails(investorId) {
    const response = await fetch(`/api/portfolio/${investorId}`);
    return await response.json();
}
```

#### 2. UI 컴포넌트 추가
```javascript
// ui-controller.js에 메서드 추가
displayInvestorDetails(details) {
    const container = document.getElementById('details-container');
    container.innerHTML = `<div>${details.name}</div>`;
}
```

#### 3. 이벤트 핸들러 등록
```javascript
// main.js의 attachEventListeners()에 추가
document.getElementById('details-btn').addEventListener('click', () => {
    this.showInvestorDetails();
});
```

---

## 🐛 디버깅 팁

### 1. 콘솔 로그 확인
각 모듈은 에러 발생 시 `console.error()`로 로그를 남깁니다.

### 2. 네트워크 탭 확인
API 호출 실패 시 브라우저 개발자 도구의 Network 탭에서 요청/응답 확인

### 3. 상태 확인
```javascript
// 콘솔에서 현재 선택된 투자자 확인
console.log(app.investorManager.selectedInvestors);
```

---

## 📝 개발 규칙

### 1. 코드 스타일
- ES6+ 문법 사용
- `const`/`let` 사용 (`var` 사용 금지)
- 화살표 함수 적극 활용
- async/await 패턴 사용

### 2. 에러 처리
```javascript
try {
    const data = await apiCall();
} catch (error) {
    console.error('설명:', error);
    // 사용자 친화적 에러 메시지 표시
}
```

### 3. JSDoc 주석
```javascript
/**
 * 투자자를 추가합니다
 * @param {string} investorId - 투자자 ID
 * @returns {Object} - {success: boolean, message?: string}
 */
addInvestor(investorId) {
    // ...
}
```

---

## 🔒 보안 고려사항

1. **XSS 방지**: innerHTML 사용 시 사용자 입력 검증
2. **API 에러 처리**: 민감한 정보 노출 방지
3. **입력 검증**: 클라이언트 측에서도 기본 검증 수행

---

## 🚀 성능 최적화

### 1. 이벤트 위임 사용
- 동적 요소에 개별 리스너 등록 대신 부모에 위임

### 2. 비동기 처리
- API 호출 시 로딩 표시로 UX 개선
- 불필요한 API 호출 방지

### 3. 캐싱
- 브라우저가 JS 파일을 캐싱하여 재방문 시 빠른 로딩

---

## 📚 참고 자료

- [ES6 Modules](https://developer.mozilla.org/ko/docs/Web/JavaScript/Guide/Modules)
- [Fetch API](https://developer.mozilla.org/ko/docs/Web/API/Fetch_API)
- [Event Delegation](https://javascript.info/event-delegation)
- [Async/Await](https://developer.mozilla.org/ko/docs/Web/JavaScript/Reference/Statements/async_function)

---

## 📞 문의

프로젝트 관련 문의나 버그 리포트는 이슈 트래커를 통해 제출해주세요.

**Last Updated**: 2025-11-20