/**
 * 포트폴리오 비교 페이지 메인 진입점
 */
import { InvestorManager } from './investor-manager.js';
import { APIService } from './api-service.js';
import { UIController } from './ui-controller.js';
import { AutocompleteHandler } from './autocomplete-handler.js';

class PortfolioComparisonApp {
    constructor() {
        // 모듈 초기화
        this.investorManager = new InvestorManager();
        this.apiService = new APIService();
        this.uiController = new UIController(this.investorManager, this.apiService);
        this.autocompleteHandler = new AutocompleteHandler(this.investorManager, this.apiService);
    }

    /**
     * 애플리케이션 초기화
     */
    init() {
        // 기본 투자자 설정
        this.investorManager.setDefaultInvestors();

        // UI 초기화
        this.uiController.updateSelectedInvestors();
        this.uiController.displayComparison();
        this.uiController.updateGenerateButton();

        // 자동완성 핸들러 초기화
        this.autocompleteHandler.init();

        // 이벤트 리스너 등록
        this.attachEventListeners();
    }

    /**
     * 이벤트 리스너 등록
     */
    attachEventListeners() {
        // 투자자 제거 버튼 (이벤트 위임)
        document.getElementById('selected-investors').addEventListener('click', (event) => {
            if (event.target.classList.contains('remove-btn')) {
                const investorId = event.target.dataset.investorId;
                this.removeInvestor(investorId);
            }
        });

        // 투자자 추가 버튼 (이벤트 위임)
        document.getElementById('selected-investors').addEventListener('click', (event) => {
            if (event.target.id === 'add-investor-btn' || event.target.classList.contains('add-investor')) {
                this.uiController.openModal();
            }
        });

        // 모달 투자자 선택 (이벤트 위임)
        document.getElementById('investor-list').addEventListener('click', (event) => {
            const option = event.target.closest('.investor-option');
            if (option) {
                const investorId = option.dataset.investorId;
                this.addInvestor(investorId);
            }
        });

        // 모달 닫기 버튼
        document.querySelector('.close').addEventListener('click', () => {
            this.uiController.closeModal();
        });

        // 모달 외부 클릭
        window.addEventListener('click', (event) => {
            this.uiController.handleModalOutsideClick(event);
        });

        // 포트폴리오 생성 버튼
        document.getElementById('generateBtn').addEventListener('click', () => {
            this.generatePortfolio();
        });

        // 검색 버튼
        document.getElementById('searchBtn').addEventListener('click', () => {
            this.handleSearch();
        });

        // 자동완성 제안 클릭 (이벤트 위임)
        document.getElementById('suggestions').addEventListener('click', (event) => {
            const suggestionItem = event.target.closest('.suggestion-item');
            if (suggestionItem) {
                const investorId = suggestionItem.dataset.investorId;
                this.handleSuggestionSelect(investorId);
            }
        });
    }

    /**
     * 투자자 추가
     */
    addInvestor(investorId) {
        const result = this.investorManager.addInvestor(investorId);

        if (!result.success) {
            alert(result.message);
            return;
        }

        this.uiController.updateSelectedInvestors();
        this.uiController.displayComparison();
        this.uiController.updateGenerateButton();
        this.uiController.closeModal();
    }

    /**
     * 투자자 제거
     */
    removeInvestor(investorId) {
        this.investorManager.removeInvestor(investorId);
        this.uiController.updateSelectedInvestors();
        this.uiController.displayComparison();
        this.uiController.updateGenerateButton();
    }

    /**
     * 포트폴리오 생성
     */
    async generatePortfolio() {
        await this.uiController.generatePortfolio();
    }

    /**
     * 검색 처리
     */
    async handleSearch() {
        const result = await this.autocompleteHandler.searchInvestor();

        if (result && result.success) {
            this.uiController.updateSelectedInvestors();
            this.uiController.displayComparison();
            this.uiController.updateGenerateButton();
            this.uiController.closeModal();
        }
    }

    /**
     * 자동완성 제안 선택 처리
     */
    handleSuggestionSelect(investorId) {
        const result = this.autocompleteHandler.selectSuggestion(investorId);

        if (result) {
            this.uiController.updateSelectedInvestors();
            this.uiController.displayComparison();
            this.uiController.updateGenerateButton();
            this.uiController.closeModal();
        }
    }
}

// DOM이 로드되면 애플리케이션 초기화
document.addEventListener('DOMContentLoaded', () => {
    const app = new PortfolioComparisonApp();
    app.init();
});