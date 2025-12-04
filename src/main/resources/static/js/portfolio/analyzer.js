/**
 * 포트폴리오 분석기 - 프론트엔드 로직
 */

// 전역 변수
let uploadedImageFiles = [];  // 배열로 변경 (최대 5개)
let stockCounter = 0;
const MAX_IMAGES = 5;
const BATCH_SIZE = 2;  // 배치 크기 (2개씩 처리)

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    initializeImageUpload();
    initializeParticles();
});

/**
 * 이미지 업로드 초기화 (다중 이미지 지원)
 */
function initializeImageUpload() {
    const dropZone = document.getElementById('drop-zone');
    const imageInput = document.getElementById('image-input');

    // 드롭존 클릭으로 파일 선택
    dropZone.addEventListener('click', () => {
        if (uploadedImageFiles.length < MAX_IMAGES) {
            imageInput.click();
        } else {
            alert(`최대 ${MAX_IMAGES}개까지만 업로드할 수 있습니다.`);
        }
    });

    // 파일 선택 이벤트 (multiple 지원)
    imageInput.addEventListener('change', (e) => {
        handleMultipleImages(Array.from(e.target.files));
        e.target.value = ''; // 같은 파일 재선택 가능하도록
    });

    // 드래그 오버
    dropZone.addEventListener('dragover', (e) => {
        e.preventDefault();
        e.stopPropagation();
        dropZone.classList.add('drag-over');
    });

    // 드래그 떠남
    dropZone.addEventListener('dragleave', (e) => {
        e.preventDefault();
        e.stopPropagation();
        dropZone.classList.remove('drag-over');
    });

    // 드롭
    dropZone.addEventListener('drop', (e) => {
        e.preventDefault();
        e.stopPropagation();
        dropZone.classList.remove('drag-over');

        const files = Array.from(e.dataTransfer.files).filter(file =>
            file.type.startsWith('image/')
        );

        if (files.length > 0) {
            handleMultipleImages(files);
        }
    });
}

/**
 * 다중 이미지 처리 (최대 5개)
 */
function handleMultipleImages(files) {
    if (!files || files.length === 0) return;

    // 현재 이미지 개수 + 새로운 파일 개수가 MAX_IMAGES를 초과하는지 확인
    const remainingSlots = MAX_IMAGES - uploadedImageFiles.length;

    if (remainingSlots <= 0) {
        alert(`최대 ${MAX_IMAGES}개까지만 업로드할 수 있습니다.`);
        return;
    }

    // 초과하는 파일은 제외
    const filesToAdd = files.slice(0, remainingSlots);

    if (files.length > remainingSlots) {
        alert(`${MAX_IMAGES}개를 초과하여 처음 ${remainingSlots}개만 추가됩니다.`);
    }

    // 파일 배열에 추가
    uploadedImageFiles.push(...filesToAdd);

    // 미리보기 그리드 표시
    const previewGrid = document.getElementById('preview-grid');
    const dropZone = document.getElementById('drop-zone');

    if (uploadedImageFiles.length > 0) {
        previewGrid.style.display = 'grid';
        dropZone.style.display = 'none';
    }

    // 각 파일의 미리보기 추가
    const startIndex = uploadedImageFiles.length - filesToAdd.length;
    filesToAdd.forEach((file, idx) => {
        addImagePreview(file, startIndex + idx);
    });

    // 이미지 개수 업데이트 및 분석 버튼 표시
    updateImageCount();
}

/**
 * 이미지 미리보기 추가
 */
function addImagePreview(file, index) {
    const previewGrid = document.getElementById('preview-grid');

    const previewItem = document.createElement('div');
    previewItem.className = 'preview-item';
    previewItem.dataset.index = index;

    // 이미지 읽기
    const reader = new FileReader();
    reader.onload = (e) => {
        previewItem.innerHTML = `
            <img src="${e.target.result}" alt="Preview ${index + 1}">
            <button class="remove-preview-btn" onclick="removeImage(${index})">
                <i class="fas fa-times"></i>
            </button>
            <div class="preview-number">${index + 1}</div>
        `;
    };
    reader.readAsDataURL(file);

    previewGrid.appendChild(previewItem);
}

/**
 * 이미지 제거
 */
function removeImage(index) {
    // 배열에서 제거
    uploadedImageFiles.splice(index, 1);

    // 미리보기 그리드 전체 재구성
    const previewGrid = document.getElementById('preview-grid');
    previewGrid.innerHTML = '';

    if (uploadedImageFiles.length === 0) {
        // 모든 이미지가 제거되면 드롭존 다시 표시
        previewGrid.style.display = 'none';
        document.getElementById('drop-zone').style.display = 'flex';
        updateImageCount();
        return;
    }

    // 남은 이미지들 다시 추가 (인덱스 재정렬)
    uploadedImageFiles.forEach((file, idx) => {
        addImagePreview(file, idx);
    });

    updateImageCount();
}

/**
 * 이미지 개수 업데이트 및 버튼 표시/숨김
 */
function updateImageCount() {
    const imageCount = document.getElementById('image-count');
    const startAnalysisBtn = document.getElementById('start-analysis-btn');

    imageCount.textContent = uploadedImageFiles.length;

    if (uploadedImageFiles.length > 0) {
        startAnalysisBtn.style.display = 'block';
    } else {
        startAnalysisBtn.style.display = 'none';
    }
}

/**
 * 분석 시작 버튼 클릭 시 호출 (다중 이미지 배치 처리)
 */
async function startOCRAnalysis() {
    if (uploadedImageFiles.length === 0) {
        alert('이미지를 먼저 선택해주세요.');
        return;
    }

    // 로딩 화면 표시
    showSection('loading-section');

    try {
        // 배치 처리로 OCR 실행
        const allStocks = await processImagesInBatches(uploadedImageFiles, BATCH_SIZE);

        if (allStocks.length > 0) {
            // OCR 성공 - 수동 입력 섹션으로 이동하고 데이터 채우기
            populateStocksFromOCR(allStocks);
            showSection('input-section');
        } else {
            // OCR 실패 - 빈 입력 폼으로 이동
            alert('이미지에서 포트폴리오를 추출하지 못했습니다. 직접 입력해주세요.');
            skipToManualInput();
        }

    } catch (error) {
        console.error('OCR 배치 처리 에러:', error);
        alert('이미지 처리 중 오류가 발생했습니다. 직접 입력해주세요.');
        skipToManualInput();
    }
}

/**
 * 다중 이미지 배치 처리 (2개씩)
 * @param {File[]} imageFiles - 처리할 이미지 파일 배열
 * @param {number} batchSize - 배치 크기 (기본값: 2)
 * @returns {Promise<Array>} 모든 이미지에서 추출된 종목 리스트
 */
async function processImagesInBatches(imageFiles, batchSize = 2) {
    const allStocks = [];
    const totalBatches = Math.ceil(imageFiles.length / batchSize);

    console.log(`총 ${imageFiles.length}개 이미지를 ${totalBatches}개 배치로 처리 시작`);

    // 배치 단위로 처리
    for (let i = 0; i < imageFiles.length; i += batchSize) {
        const batch = imageFiles.slice(i, i + batchSize);
        const batchNumber = Math.floor(i / batchSize) + 1;

        console.log(`[배치 ${batchNumber}/${totalBatches}] ${batch.length}개 이미지 처리 중...`);

        // 배치 내 이미지들은 병렬 처리
        const batchPromises = batch.map(file => extractPortfolioFromImage(file));

        try {
            const batchResults = await Promise.all(batchPromises);

            // 각 결과에서 종목 추출
            batchResults.forEach((result, idx) => {
                if (result && result.stocks && result.stocks.length > 0) {
                    console.log(`  ✓ 이미지 ${i + idx + 1}: ${result.stocks.length}개 종목 추출`);
                    allStocks.push(...result.stocks);
                } else {
                    console.warn(`  ✗ 이미지 ${i + idx + 1}: 추출 실패`);
                }
            });

        } catch (error) {
            console.error(`배치 ${batchNumber} 처리 중 오류:`, error);
            // 에러가 발생해도 다음 배치 계속 진행
        }

        // 다음 배치 전 짧은 대기 (서버 부하 방지)
        if (i + batchSize < imageFiles.length) {
            await new Promise(resolve => setTimeout(resolve, 500));
        }
    }

    console.log(`✓ 전체 처리 완료: 총 ${allStocks.length}개 종목 추출`);

    // 중복 제거 및 비중 합산 (같은 ticker는 비중 합산)
    const mergedStocks = mergeStocks(allStocks);

    return mergedStocks;
}

/**
 * 중복 종목 병합 (같은 티커의 비중 합산)
 * @param {Array} stocks - 종목 배열
 * @returns {Array} 병합된 종목 배열
 */
function mergeStocks(stocks) {
    const stockMap = new Map();

    stocks.forEach(stock => {
        const ticker = stock.ticker.toUpperCase();

        if (stockMap.has(ticker)) {
            // 이미 존재하면 비중 합산
            const existing = stockMap.get(ticker);
            existing.weight += stock.weight;
        } else {
            // 새로운 종목 추가
            stockMap.set(ticker, { ticker, weight: stock.weight });
        }
    });

    const merged = Array.from(stockMap.values());

    // 비중 재조정 (합계를 100%로 맞춤)
    const totalWeight = merged.reduce((sum, s) => sum + s.weight, 0);

    if (totalWeight > 0) {
        merged.forEach(stock => {
            stock.weight = (stock.weight / totalWeight) * 100;
        });
    }

    console.log(`종목 병합: ${stocks.length}개 → ${merged.length}개 (비중 재조정 완료)`);

    return merged;
}

/**
 * OCR로 포트폴리오 추출 (단일 이미지)
 * @param {File} file - 처리할 이미지 파일
 * @returns {Promise<Object>} OCR 결과 객체
 */
async function extractPortfolioFromImage(file) {
    const formData = new FormData();
    formData.append('image', file);

    // 드롭다운에서 선택된 증권사 값 가져오기
    const brokerSelect = document.getElementById('broker-select');
    const selectedBroker = brokerSelect ? brokerSelect.value : 'TOSS'; // 기본값: TOSS
    formData.append('broker', selectedBroker);

    console.log(`선택된 증권사: ${selectedBroker}`);

    try {
        const response = await fetch('/api/portfolio/extract-from-image', {
            method: 'POST',
            body: formData
        });

        const result = await response.json();
        return result;

    } catch (error) {
        console.error('OCR 에러:', error);
        return { success: false, stocks: [] };
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
    uploadedImageFiles = [];
    stockCounter = 0;

    // 이미지 초기화
    const previewGrid = document.getElementById('preview-grid');
    const dropZone = document.getElementById('drop-zone');
    const startAnalysisBtn = document.getElementById('start-analysis-btn');

    previewGrid.innerHTML = '';
    previewGrid.style.display = 'none';
    dropZone.style.display = 'flex';
    startAnalysisBtn.style.display = 'none';

    // 이미지 카운트 초기화
    updateImageCount();

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

