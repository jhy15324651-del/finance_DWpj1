/**
 * 포트폴리오 분석기 - 프론트엔드 로직
 */

// 전역 변수
let uploadedImageFile = null;
let stockCounter = 0;

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    initializeImageUpload();
    initializeParticles();
});

/**
 * 이미지 업로드 초기화
 */
function initializeImageUpload() {
    const uploadArea = document.getElementById('upload-area');
    const imageInput = document.getElementById('image-input');
    const previewImage = document.getElementById('preview-image');

    // 클릭으로 파일 선택
    uploadArea.addEventListener('click', () => {
        imageInput.click();
    });

    // 파일 선택 이벤트
    imageInput.addEventListener('change', (e) => {
        handleImageSelect(e.target.files[0]);
    });

    // 드래그 앤 드롭
    uploadArea.addEventListener('dragover', (e) => {
        e.preventDefault();
        uploadArea.classList.add('drag-over');
    });

    uploadArea.addEventListener('dragleave', () => {
        uploadArea.classList.remove('drag-over');
    });

    uploadArea.addEventListener('drop', (e) => {
        e.preventDefault();
        uploadArea.classList.remove('drag-over');

        const file = e.dataTransfer.files[0];
        if (file && file.type.startsWith('image/')) {
            handleImageSelect(file);
        }
    });
}

/**
 * 이미지 선택 처리
 */
function handleImageSelect(file) {
    if (!file) return;

    uploadedImageFile = file;
    const previewImage = document.getElementById('preview-image');
    const uploadPlaceholder = document.querySelector('.upload-placeholder');

    // 이미지 미리보기
    const reader = new FileReader();
    reader.onload = (e) => {
        previewImage.src = e.target.result;
        previewImage.style.display = 'block';
        uploadPlaceholder.style.display = 'none';

        // OCR 자동 실행
        extractPortfolioFromImage(file);
    };
    reader.readAsDataURL(file);
}

/**
 * OCR로 포트폴리오 추출
 */
async function extractPortfolioFromImage(file) {
    const formData = new FormData();
    formData.append('image', file);

    try {
        showSection('loading-section');

        const response = await fetch('/api/portfolio/extract-from-image', {
            method: 'POST',
            body: formData
        });

        const result = await response.json();

        if (result.success && result.stocks && result.stocks.length > 0) {
            // OCR 성공 - 수동 입력 섹션으로 이동하고 데이터 채우기
            populateStocksFromOCR(result.stocks);
            showSection('input-section');
        } else {
            // OCR 실패 - 빈 입력 폼으로 이동
            alert('이미지에서 포트폴리오를 추출하지 못했습니다. 직접 입력해주세요.');
            skipToManualInput();
        }

    } catch (error) {
        console.error('OCR 에러:', error);
        alert('이미지 처리 중 오류가 발생했습니다. 직접 입력해주세요.');
        skipToManualInput();
    }
}

/**
 * OCR 결과를 입력 폼에 채우기
 */
function populateStocksFromOCR(stocks) {
    const stockList = document.getElementById('stock-list');
    stockList.innerHTML = ''; // 기존 행 제거

    stocks.forEach(stock => {
        addStockRow(stock.ticker, stock.weight);
    });

    updateTotalWeight();
}

/**
 * 직접 입력으로 건너뛰기
 */
function skipToManualInput() {
    showSection('input-section');

    // 빈 행 3개 추가
    const stockList = document.getElementById('stock-list');
    if (stockList.children.length === 0) {
        for (let i = 0; i < 3; i++) {
            addStockRow();
        }
    }
}

/**
 * 종목 행 추가
 */
function addStockRow(ticker = '', weight = 0) {
    const stockList = document.getElementById('stock-list');
    const row = document.createElement('div');
    row.className = 'stock-row';
    row.dataset.id = stockCounter++;

    row.innerHTML = `
        <input type="text"
               class="stock-ticker"
               placeholder="AAPL"
               value="${ticker}"
               maxlength="10">
        <input type="number"
               class="stock-weight"
               placeholder="비중 (%)"
               value="${weight || ''}"
               min="0"
               max="100"
               step="0.1">
        <button class="btn-remove" onclick="removeStockRow(this)">
            <i class="fas fa-times"></i>
        </button>
    `;

    stockList.appendChild(row);

    // 비중 변경 시 자동 계산
    const weightInput = row.querySelector('.stock-weight');
    weightInput.addEventListener('input', updateTotalWeight);

    updateTotalWeight();
}

/**
 * 종목 행 제거
 */
function removeStockRow(button) {
    const row = button.closest('.stock-row');
    row.remove();
    updateTotalWeight();
}

/**
 * 총 비중 계산 및 업데이트
 */
function updateTotalWeight() {
    const weightInputs = document.querySelectorAll('.stock-weight');
    let total = 0;

    weightInputs.forEach(input => {
        const value = parseFloat(input.value) || 0;
        total += value;
    });

    const totalElement = document.getElementById('total-weight');
    const statusElement = document.getElementById('weight-status');

    totalElement.textContent = total.toFixed(1);

    // 상태 표시
    if (Math.abs(total - 100) < 0.1) {
        statusElement.textContent = '✓ 완벽합니다!';
        statusElement.className = 'weight-status valid';
    } else if (total > 100) {
        statusElement.textContent = '⚠ 100%를 초과했습니다';
        statusElement.className = 'weight-status invalid';
    } else if (total > 0) {
        statusElement.textContent = `⚠ ${(100 - total).toFixed(1)}% 부족합니다`;
        statusElement.className = 'weight-status warning';
    } else {
        statusElement.textContent = '';
        statusElement.className = 'weight-status';
    }
}

/**
 * 포트폴리오 분석 실행
 */
async function analyzePortfolio() {
    // 입력 검증
    const stockRows = document.querySelectorAll('.stock-row');

    if (stockRows.length === 0) {
        alert('최소 1개 이상의 종목을 입력해주세요.');
        return;
    }

    // 포트폴리오 데이터 수집
    const portfolio = {};
    let hasError = false;

    stockRows.forEach(row => {
        const ticker = row.querySelector('.stock-ticker').value.trim().toUpperCase();
        const weight = parseFloat(row.querySelector('.stock-weight').value);

        if (!ticker || isNaN(weight) || weight <= 0) {
            hasError = true;
            return;
        }

        portfolio[ticker] = weight;
    });

    if (hasError) {
        alert('모든 종목의 티커와 비중을 올바르게 입력해주세요.');
        return;
    }

    if (Object.keys(portfolio).length === 0) {
        alert('최소 1개 이상의 종목을 입력해주세요.');
        return;
    }

    // 비중 합계 검증 (95~105% 허용)
    const totalWeight = Object.values(portfolio).reduce((sum, w) => sum + w, 0);
    if (totalWeight < 95 || totalWeight > 105) {
        const confirm = window.confirm(
            `총 비중이 ${totalWeight.toFixed(1)}%입니다.\n` +
            `계속 진행하시겠습니까?`
        );
        if (!confirm) return;
    }

    // 로딩 표시
    showSection('loading-section');

    try {
        // API 호출
        const response = await fetch('/api/portfolio/analyze', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ portfolio })
        });

        if (!response.ok) {
            throw new Error('분석 요청 실패');
        }

        const result = await response.json();

        // 결과 표시
        displayResults(result);
        showSection('result-section');

    } catch (error) {
        console.error('분석 에러:', error);
        alert('포트폴리오 분석 중 오류가 발생했습니다. 다시 시도해주세요.');
        showSection('input-section');
    }
}

/**
 * 분석 결과 표시
 */
function displayResults(result) {
    displayTopMatches(result.topMatches);
    displayRecommendedContents(result.recommendedContents);
}

/**
 * TOP 3 투자대가 매칭 결과 표시
 */
function displayTopMatches(matches) {
    const container = document.getElementById('top-matches');
    container.innerHTML = '';

    matches.forEach((match, index) => {
        const card = createMatchCard(match, index);
        container.appendChild(card);

        // 순차 애니메이션
        setTimeout(() => {
            card.classList.add('appear');
        }, index * 200);
    });
}

/**
 * 매칭 결과 카드 생성
 */
function createMatchCard(match, index) {
    const card = document.createElement('div');
    card.className = 'match-card';

    // 순위별 메달 색상
    const rankColors = ['gold', 'silver', 'bronze'];
    const rankColor = rankColors[index] || 'gray';

    card.innerHTML = `
        <div class="rank-badge rank-${rankColor}">
            <i class="fas fa-medal"></i> ${match.rank}위
        </div>

        <div class="match-header">
            <h3 class="investor-name">${match.investorName}</h3>
            <div class="similarity-badge">${match.similarity}%</div>
        </div>

        <div class="match-details">
            <div class="detail-item">
                <i class="fas fa-chart-pie"></i>
                <strong>투자 철학:</strong>
                <p>${match.philosophy}</p>
            </div>

            <div class="detail-item">
                <i class="fas fa-check-circle" style="color: #4CAF50;"></i>
                <strong>장점:</strong>
                <p>${match.strengths}</p>
            </div>

            <div class="detail-item">
                <i class="fas fa-exclamation-triangle" style="color: #FF9800;"></i>
                <strong>주의할 점:</strong>
                <p>${match.weaknesses}</p>
            </div>

            <div class="detail-item">
                <i class="fas fa-lightbulb" style="color: #FFD700;"></i>
                <strong>개선 제안:</strong>
                <p>${match.suggestions}</p>
            </div>

            <div class="detail-item">
                <i class="fas fa-layer-group"></i>
                <strong>겹치는 종목:</strong>
                <div class="matched-stocks">
                    ${match.matchedStocks.map(stock =>
                        `<span class="stock-tag">${stock}</span>`
                    ).join('')}
                </div>
            </div>

            <div class="stats-row">
                <div class="stat">
                    <i class="fas fa-percentage"></i>
                    <span>종목 겹침률</span>
                    <strong>${match.overlapPercentage}%</strong>
                </div>
                <div class="stat">
                    <i class="fas fa-briefcase"></i>
                    <span>전체 보유 종목</span>
                    <strong>${match.totalHoldings}개</strong>
                </div>
            </div>
        </div>
    `;

    return card;
}

/**
 * 추천 콘텐츠 표시
 */
function displayRecommendedContents(contents) {
    const container = document.getElementById('recommended-contents');
    container.innerHTML = '';

    if (!contents || contents.length === 0) {
        container.innerHTML = '<p style="text-align: center; color: #999;">추천 콘텐츠가 없습니다.</p>';
        return;
    }

    contents.forEach((content, index) => {
        const card = createContentCard(content);
        container.appendChild(card);

        // 순차 애니메이션
        setTimeout(() => {
            card.classList.add('appear');
        }, index * 100);
    });
}

/**
 * 콘텐츠 카드 생성
 */
function createContentCard(content) {
    const card = document.createElement('div');
    card.className = 'content-card';

    card.innerHTML = `
        <div class="content-thumbnail">
            <img src="${content.thumbnailUrl}"
                 alt="${content.title}"
                 onerror="this.src='/images/placeholder.jpg'">
            <div class="content-category">${content.category}</div>
        </div>
        <div class="content-info">
            <h4>${content.title}</h4>
            <div class="content-meta">
                <span class="rating">
                    <i class="fas fa-star"></i> ${content.rating.toFixed(1)}
                </span>
                <span class="keyword">
                    <i class="fas fa-tag"></i> ${content.keyword}
                </span>
            </div>
        </div>
    `;

    // 클릭 이벤트 (추후 콘텐츠 상세 페이지 연결)
    card.addEventListener('click', () => {
        console.log('콘텐츠 클릭:', content.contentId);
        // TODO: 콘텐츠 상세 페이지로 이동
        // window.location.href = `/contents/${content.contentId}`;
    });

    return card;
}

/**
 * 섹션 전환
 */
function showSection(sectionId) {
    const sections = document.querySelectorAll('.section');
    sections.forEach(section => {
        section.classList.remove('active');
    });

    const targetSection = document.getElementById(sectionId);
    if (targetSection) {
        targetSection.classList.add('active');
        targetSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
}

/**
 * 다시 시작
 */
function restart() {
    // 모든 상태 초기화
    uploadedImageFile = null;
    stockCounter = 0;

    // 이미지 초기화
    const previewImage = document.getElementById('preview-image');
    const uploadPlaceholder = document.querySelector('.upload-placeholder');
    previewImage.style.display = 'none';
    previewImage.src = '';
    uploadPlaceholder.style.display = 'flex';

    // 입력 폼 초기화
    const stockList = document.getElementById('stock-list');
    stockList.innerHTML = '';

    // 결과 초기화
    document.getElementById('top-matches').innerHTML = '';
    document.getElementById('recommended-contents').innerHTML = '';

    // 업로드 섹션으로 이동
    showSection('upload-section');
}

/**
 * 금색 파티클 애니메이션 초기화
 */
function initializeParticles() {
    const particlesContainer = document.querySelector('.golden-particles');
    if (!particlesContainer) return;

    // 랜덤 파티클 생성
    for (let i = 0; i < 50; i++) {
        const particle = document.createElement('div');
        particle.className = 'particle';
        particle.style.left = Math.random() * 100 + '%';
        particle.style.top = Math.random() * 100 + '%';
        particle.style.animationDelay = Math.random() * 5 + 's';
        particle.style.animationDuration = (Math.random() * 10 + 10) + 's';
        particlesContainer.appendChild(particle);
    }
}
