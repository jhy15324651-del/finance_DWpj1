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
     * 포트폴리오 생성 및 표시 (합의형 포트폴리오 + 도넛 차트)
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
        generateBtn.textContent = 'AI가 합의형 포트폴리오를 생성 중입니다...';
        container.innerHTML = '<div class="loading">4명의 투자자가 회의 후 모두 동의한 합의 종목 10개를 AI가 선정하고 있습니다. 잠시만 기다려주세요...</div>';

        try {
            const data = await this.apiService.generateConsensusPortfolio(this.investorManager.selectedInvestors);
            this.displayConsensusPortfolio(data);
        } catch (error) {
            console.error('합의형 포트폴리오 생성 오류:', error);
            container.innerHTML = '<div class="loading">포트폴리오 생성에 실패했습니다. API 키를 확인하거나 나중에 다시 시도해주세요.</div>';
        } finally {
            generateBtn.disabled = false;
            generateBtn.textContent = '포트폴리오 생성';
        }
    }

    /**
     * 합의형 포트폴리오 데이터 표시 (도넛 차트 포함)
     */
    displayConsensusPortfolio(data) {
        const container = document.getElementById('portfolio-container');

        const investorNames = this.investorManager.getSelectedInvestors()
            .map(inv => inv.name)
            .join(', ');

        // HTML 구조 생성
        container.innerHTML = `
            <div class="consensus-portfolio">
                <div class="portfolio-section">
                    <h3>📌 참여 투자자</h3>
                    <div class="portfolio-text">${investorNames}</div>
                </div>

                <div class="portfolio-section">
                    <h3>💡 합의 위원회 철학</h3>
                    <div class="portfolio-text">${data.committeePhilosophy || '선정된 투자자들의 합의로 도출된 포트폴리오입니다.'}</div>
                </div>

                <div class="portfolio-section">
                    <h3>📊 포트폴리오 구성 (도넛 차트)</h3>
                    <div class="chart-container">
                        <canvas id="consensusChart"></canvas>
                    </div>
                </div>

                <div class="portfolio-section">
                    <h3>📋 종목 리스트</h3>
                    <div class="stock-list" id="stockList"></div>
                </div>

                <div class="portfolio-section">
                    <h3>🎯 포트폴리오 특성</h3>
                    <div class="portfolio-text">${data.portfolioCharacteristics || '합의된 종목들의 특성을 반영한 포트폴리오입니다.'}</div>
                </div>
            </div>
        `;

        // 도넛 차트 렌더링
        this.renderDonutChart(data.stocks);

        // 종목 리스트 렌더링
        this.renderStockList(data.stocks);
    }

    /**
     * Chart.js 도넛 차트 렌더링
     */
    renderDonutChart(stocks) {
        const ctx = document.getElementById('consensusChart');

        // 기존 차트가 있다면 제거
        if (this.currentChart) {
            this.currentChart.destroy();
        }

        const labels = stocks.map(stock => `${stock.ticker} (${stock.weightPercent}%)`);
        const data = stocks.map(stock => stock.weightPercent);

        // 10개 색상 팔레트
        const colors = [
            '#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF',
            '#FF9F40', '#FF6384', '#C9CBCF', '#4BC0C0', '#FF9F40'
        ];

        this.currentChart = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: data,
                    backgroundColor: colors,
                    borderWidth: 2,
                    borderColor: '#ffffff'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: {
                        position: 'right',
                        labels: {
                            font: {
                                size: 12
                            },
                            padding: 15,
                            boxWidth: 15
                        }
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                const stock = stocks[context.dataIndex];
                                return [
                                    `${stock.companyName || stock.ticker}`,
                                    `비중: ${stock.weightPercent}%`,
                                    `섹터: ${stock.sector || 'N/A'}`
                                ];
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * 종목 리스트 테이블 렌더링
     */
    renderStockList(stocks) {
        const container = document.getElementById('stockList');

        let html = '<table class="stock-table">';
        html += '<thead><tr><th>티커</th><th>회사명</th><th>섹터</th><th>비중</th></tr></thead>';
        html += '<tbody>';

        stocks.forEach(stock => {
            html += `
                <tr>
                    <td><strong>${stock.ticker}</strong></td>
                    <td>${stock.companyName || '-'}</td>
                    <td>${stock.sector || '-'}</td>
                    <td><span class="weight-badge">${stock.weightPercent}%</span></td>
                </tr>
            `;
        });

        html += '</tbody></table>';
        container.innerHTML = html;
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