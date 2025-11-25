/**
 * UI 렌더링 및 DOM 조작 담당 모듈
 */
export class UIController {
    constructor(investorManager, apiService) {
        this.investorManager = investorManager;
        this.apiService = apiService;
    }

    /**
     * 선택된 투자자 UI 업데이트
     */
    updateSelectedInvestors() {
        const container = document.getElementById('selected-investors');
        container.innerHTML = '';

        const selectedInvestors = this.investorManager.getSelectedInvestors();

        selectedInvestors.forEach(investor => {
            const card = document.createElement('div');
            card.className = 'investor-card selected';
            card.innerHTML = `
                <button class="remove-btn" data-investor-id="${investor.id}">✕</button>
                <div class="investor-name">${investor.name}</div>
                <div class="investor-style">${investor.style || '커스텀 투자자'}</div>
            `;
            container.appendChild(card);
        });

        // 4명 미만이면 추가 버튼 표시
        if (this.investorManager.canAddMore()) {
            const addCard = document.createElement('div');
            addCard.className = 'add-investor';
            addCard.innerHTML = '+ 투자자 추가';
            addCard.id = 'add-investor-btn';
            container.appendChild(addCard);
        }
    }

    /**
     * 투자자 모달 열기
     */
    openModal() {
        const modal = document.getElementById('investorModal');
        const list = document.getElementById('investor-list');
        list.innerHTML = '';

        const availableInvestors = this.investorManager.getAvailableInvestors();

        availableInvestors.forEach(investor => {
            const option = document.createElement('div');
            option.className = 'investor-option';
            option.innerHTML = `
                <h3>${investor.name}</h3>
                <p>${investor.description}</p>
            `;
            option.dataset.investorId = investor.id;
            list.appendChild(option);
        });

        modal.style.display = 'block';
    }

    /**
     * 투자자 모달 닫기
     */
    closeModal() {
        document.getElementById('investorModal').style.display = 'none';
    }

    /**
     * 비교 데이터 표시
     */
    async displayComparison() {
        const container = document.getElementById('philosophy-container');

        if (this.investorManager.getSelectedCount() === 0) {
            container.innerHTML = '<div class="loading">투자자를 선택하면 비교 분석이 시작됩니다</div>';
            return;
        }

        try {
            const data = await this.apiService.fetchComparison(this.investorManager.selectedInvestors);

            container.innerHTML = '';

            data.forEach(investorData => {
                const investor = this.investorManager.getInvestorById(investorData.investorId);
                const card = document.createElement('div');
                card.className = 'investor-philosophy';

                let philosophyHtml = `
                    <h3>${investor ? investor.name : investorData.investorId}</h3>
                `;

                investorData.philosophy.forEach(item => {
                    philosophyHtml += `
                        <div class="philosophy-item">
                            <div class="philosophy-label">
                                <span>${item.category}</span>
                                <span>${item.percentage}%</span>
                            </div>
                            <div class="philosophy-bar">
                                <div class="philosophy-fill" style="width: ${item.percentage}%">
                                    ${item.percentage}%
                                </div>
                            </div>
                        </div>
                    `;
                });

                if (investorData.insights) {
                    philosophyHtml += `
                        <div class="insights-section">
                            <h4>💡 AI 인사이트</h4>
                            <div class="insight-text">${investorData.insights}</div>
                        </div>
                    `;
                }

                card.innerHTML = philosophyHtml;
                container.appendChild(card);
            });
        } catch (error) {
            console.error('비교 데이터 표시 오류:', error);
            container.innerHTML = '<div class="loading">데이터를 불러오는데 실패했습니다.</div>';
        }
    }

    /**
     * 포트폴리오 생성 버튼 상태 업데이트
     */
    updateGenerateButton() {
        const generateBtn = document.getElementById('generateBtn');
        generateBtn.disabled = !this.investorManager.canGeneratePortfolio();
    }

    /**
     * 포트폴리오 생성 및 표시
     */
    async generatePortfolio() {
        if (!this.investorManager.canGeneratePortfolio()) {
            alert('4명의 투자자를 모두 선택해주세요.');
            return;
        }

        const generateBtn = document.getElementById('generateBtn');
        const container = document.getElementById('portfolio-container');

        // 버튼 비활성화 및 로딩 표시
        generateBtn.disabled = true;
        generateBtn.textContent = 'AI가 포트폴리오를 생성 중입니다...';
        container.innerHTML = '<div class="loading">GPT API를 통해 추천 포트폴리오를 생성하고 있습니다. 잠시만 기다려주세요...</div>';

        try {
            const data = await this.apiService.generatePortfolio(this.investorManager.selectedInvestors);
            this.displayPortfolio(data);
        } catch (error) {
            console.error('포트폴리오 생성 오류:', error);
            container.innerHTML = '<div class="loading">포트폴리오 생성에 실패했습니다. API 키를 확인하거나 나중에 다시 시도해주세요.</div>';
        } finally {
            generateBtn.disabled = false;
            generateBtn.textContent = '포트폴리오 생성';
        }
    }

    /**
     * 포트폴리오 데이터 표시
     */
    displayPortfolio(data) {
        const container = document.getElementById('portfolio-container');

        const investorNames = this.investorManager.getSelectedInvestors()
            .map(inv => inv.name)
            .join(', ');

        container.innerHTML = `
            <div class="portfolio-content">
                <div class="portfolio-section">
                    <h3>📌 선택된 투자자</h3>
                    <div class="portfolio-text">${investorNames}</div>
                </div>
                <div class="portfolio-section">
                    <h3>💼 추천 포트폴리오</h3>
                    <div class="portfolio-text">${data.rationale || data.combinedPhilosophy}</div>
                </div>
            </div>
        `;
    }

    /**
     * 모달 외부 클릭 핸들러
     */
    handleModalOutsideClick(event) {
        const modal = document.getElementById('investorModal');
        if (event.target === modal) {
            this.closeModal();
        }
    }

    /**
     * 초기 포트폴리오 컨테이너 메시지 표시
     */
    showInitialPortfolioMessage() {
        const container = document.getElementById('portfolio-container');
        container.innerHTML = '<div class="loading">4명의 투자자를 선택하면 추천 포트폴리오를 생성할 수 있습니다</div>';
    }
}