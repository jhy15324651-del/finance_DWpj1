/**
 * 자동완성 기능 담당 모듈
 */
export class AutocompleteHandler {
    constructor(investorManager, apiService) {
        this.investorManager = investorManager;
        this.apiService = apiService;
        this.searchInput = null;
        this.suggestionsDiv = null;
        this.searchBtn = null;
    }

    /**
     * 초기화 및 이벤트 리스너 등록
     */
    init() {
        this.searchInput = document.getElementById('investorSearch');
        this.suggestionsDiv = document.getElementById('suggestions');
        this.searchBtn = document.getElementById('searchBtn');

        if (!this.searchInput) return;

        // 입력할 때마다 자동완성 표시
        this.searchInput.addEventListener('input', (event) => {
            this.handleInput(event.target.value.trim());
        });

        // 엔터키로 검색
        this.searchInput.addEventListener('keypress', (event) => {
            if (event.key === 'Enter') {
                this.handleEnterKey();
            }
        });

        // 입력창에서 포커스 잃으면 잠시 후 제안 숨김
        this.searchInput.addEventListener('blur', () => {
            setTimeout(() => this.hideSuggestions(), 200);
        });
    }

    /**
     * 입력 핸들러
     */
    handleInput(searchTerm) {
        if (searchTerm.length === 0) {
            this.hideSuggestions();
            return;
        }

        const matches = this.investorManager.filterInvestors(searchTerm);
        this.showSuggestions(matches);
    }

    /**
     * 제안 표시
     */
    showSuggestions(investors) {
        if (investors.length === 0) {
            this.suggestionsDiv.innerHTML = '<div class="no-suggestions">검색 결과가 없습니다. 직접 입력하여 추가하세요.</div>';
            this.suggestionsDiv.classList.add('show');
            return;
        }

        this.suggestionsDiv.innerHTML = '';
        investors.forEach(investor => {
            const item = document.createElement('div');
            item.className = 'suggestion-item';
            item.innerHTML = `
                <div class="suggestion-name">${investor.name}</div>
                <div class="suggestion-style">${investor.style}</div>
                <div class="suggestion-desc">${investor.description}</div>
            `;
            item.dataset.investorId = investor.id;
            this.suggestionsDiv.appendChild(item);
        });

        this.suggestionsDiv.classList.add('show');
    }

    /**
     * 제안 숨김
     */
    hideSuggestions() {
        this.suggestionsDiv.classList.remove('show');
        this.suggestionsDiv.innerHTML = '';
    }

    /**
     * 제안 선택
     */
    selectSuggestion(investorId) {
        const result = this.investorManager.addInvestor(investorId);

        if (!result.success) {
            alert(result.message);
            this.hideSuggestions();
            return null;
        }

        // 입력창 초기화 및 제안 숨김
        this.searchInput.value = '';
        this.hideSuggestions();

        return result;
    }

    /**
     * 엔터키 핸들러
     */
    handleEnterKey() {
        // 자동완성 제안이 있고 첫 번째 항목 선택
        const firstSuggestion = this.suggestionsDiv.querySelector('.suggestion-item');
        if (firstSuggestion && this.suggestionsDiv.classList.contains('show')) {
            const investorId = firstSuggestion.dataset.investorId;
            return { type: 'suggestion', investorId };
        } else {
            // 자동완성 제안이 없으면 API로 검색
            return { type: 'search' };
        }
    }

    /**
     * API를 통한 투자자 검색
     */
    async searchInvestor() {
        const investorName = this.searchInput.value.trim();

        // 자동완성 제안 숨김
        this.hideSuggestions();

        if (!investorName) {
            alert('투자자 이름을 입력해주세요.');
            return null;
        }

        if (!this.investorManager.canAddMore()) {
            alert('최대 4명까지만 비교할 수 있습니다.');
            return null;
        }

        // 버튼 비활성화 및 로딩 표시
        this.searchBtn.disabled = true;
        this.searchBtn.textContent = '검색 중...';

        try {
            const investorData = await this.apiService.searchInvestor(investorName);

            // 커스텀 투자자 추가
            const customInvestor = this.investorManager.addCustomInvestor({
                name: investorData.name || investorName,
                style: investorData.style,
                description: investorData.description
            });

            // 선택된 투자자에 추가
            this.investorManager.addInvestor(customInvestor.id);

            // 입력창 초기화
            this.searchInput.value = '';

            return { success: true, investor: customInvestor };
        } catch (error) {
            console.error('투자자 검색 오류:', error);
            alert('투자자 정보를 가져오는데 실패했습니다. 다시 시도해주세요.');
            return null;
        } finally {
            this.searchBtn.disabled = false;
            this.searchBtn.textContent = '추가';
        }
    }

    /**
     * 입력창 초기화
     */
    clearInput() {
        if (this.searchInput) {
            this.searchInput.value = '';
        }
        this.hideSuggestions();
    }
}