let chart = null;
let candlestickSeries = null;
let sma20Series = null;
let sma60Series = null;
let sma120Series = null;
let currentTicker = null;
let currentTimeframe = 'D';

// 차트 초기화
function initChart() {
    const chartContainer = document.getElementById('chart-container');
    chartContainer.innerHTML = '';

    chart = LightweightCharts.createChart(chartContainer, {
        width: chartContainer.clientWidth,
        height: 600,
        layout: {
            background: { color: '#ffffff' },
            textColor: '#333',
        },
        grid: {
            vertLines: { color: '#f0f0f0' },
            horzLines: { color: '#f0f0f0' },
        },
        crosshair: {
            mode: LightweightCharts.CrosshairMode.Normal,
        },
        rightPriceScale: {
            borderColor: '#e0e0e0',
        },
        timeScale: {
            borderColor: '#e0e0e0',
            timeVisible: true,
        },
    });

    // 캔들스틱 시리즈
    candlestickSeries = chart.addCandlestickSeries({
        upColor: '#ef5350',
        downColor: '#26a69a',
        borderVisible: false,
        wickUpColor: '#ef5350',
        wickDownColor: '#26a69a',
    });

    // 이동평균선 시리즈
    sma20Series = chart.addLineSeries({
        color: '#2962FF',
        lineWidth: 2,
        title: 'SMA 20',
    });

    sma60Series = chart.addLineSeries({
        color: '#FF6D00',
        lineWidth: 2,
        title: 'SMA 60',
    });

    sma120Series = chart.addLineSeries({
        color: '#9C27B0',
        lineWidth: 2,
        title: 'SMA 120',
    });

    // 반응형 크기 조정
    window.addEventListener('resize', () => {
        chart.applyOptions({
            width: chartContainer.clientWidth,
        });
    });
}

// 종목 로드
async function loadStock(ticker) {
    currentTicker = ticker.toUpperCase();
    document.getElementById('ticker-input').value = currentTicker;

    try {
        // 종목 정보 로드
        const infoResponse = await fetch(`/stock/api/info/${currentTicker}`);
        if (!infoResponse.ok) {
            throw new Error('종목을 찾을 수 없습니다.');
        }
        const stockInfo = await infoResponse.json();
        displayStockInfo(stockInfo);

        // 차트 데이터 로드
        await loadChartData();

    } catch (error) {
        console.error('종목 로드 실패:', error);
        resetChart();  // 에러 시 차트 변수 리셋
        document.getElementById('chart-container').innerHTML = `
            <div class="error">${error.message}</div>
        `;
    }
}

// 차트 변수 리셋 함수
function resetChart() {
    chart = null;
    candlestickSeries = null;
    sma20Series = null;
    sma60Series = null;
    sma120Series = null;
}

// 차트 데이터 로드
async function loadChartData() {
    if (!currentTicker) return;

    try {
        const response = await fetch(
            `/stock/api/candles/${currentTicker}?timeframe=${currentTimeframe}&count=120`
        );

        if (!response.ok) {
            throw new Error('차트 데이터를 불러올 수 없습니다.');
        }

        const candles = await response.json();

        // 차트가 없거나 DOM이 제거된 경우 재초기화
        const chartContainer = document.getElementById('chart-container');
        if (!chart || !candlestickSeries || !chartContainer.querySelector('canvas')) {
            initChart();
        }

        // 캔들 데이터 변환
        const candleData = candles.map(c => ({
            time: c.date,
            open: c.open,
            high: c.high,
            low: c.low,
            close: c.close,
        }));

        // 이동평균 데이터 변환
        const sma20Data = candles
            .filter(c => c.sma20)
            .map(c => ({ time: c.date, value: c.sma20 }));

        const sma60Data = candles
            .filter(c => c.sma60)
            .map(c => ({ time: c.date, value: c.sma60 }));

        const sma120Data = candles
            .filter(c => c.sma120)
            .map(c => ({ time: c.date, value: c.sma120 }));

        // 차트 업데이트
        candlestickSeries.setData(candleData);
        sma20Series.setData(sma20Data);
        sma60Series.setData(sma60Data);
        sma120Series.setData(sma120Data);

        chart.timeScale().fitContent();

    } catch (error) {
        console.error('차트 로드 실패:', error);
        resetChart();  // 에러 시 차트 변수 리셋
        document.getElementById('chart-container').innerHTML = `
            <div class="error">${error.message}</div>
        `;
    }
}

// 종목 정보 표시
function displayStockInfo(info) {
    document.getElementById('stock-info-container').style.display = 'block';

    const priceClass = info.changeRate >= 0 ? 'price-up' : 'price-down';
    const priceSymbol = info.changeRate >= 0 ? '▲' : '▼';

    document.getElementById('stock-info').innerHTML = `
        <div class="info-item">
            <span class="info-label">종목명</span>
            <span class="info-value">${info.name}</span>
        </div>
        <div class="info-item">
            <span class="info-label">현재가</span>
            <span class="info-value ${priceClass}">
                ${formatNumber(info.currentPrice)}
            </span>
        </div>
        <div class="info-item">
            <span class="info-label">전일대비</span>
            <span class="info-value ${priceClass}">
                ${priceSymbol} ${formatNumber(Math.abs(info.changeAmount))} (${info.changeRate.toFixed(2)}%)
            </span>
        </div>
        <div class="info-item">
            <span class="info-label">거래량</span>
            <span class="info-value">${formatNumber(info.tradingVolume)}</span>
        </div>
        <div class="info-item">
            <span class="info-label">시가총액</span>
            <span class="info-value">${formatMarketCap(info.marketCap)}</span>
        </div>
        <div class="info-item">
            <span class="info-label">52주 최고/최저</span>
            <span class="info-value" style="font-size: 1rem;">
                ${formatNumber(info.high52Week)} / ${formatNumber(info.low52Week)}
            </span>
        </div>
    `;
}

// 시간프레임 변경
function changeTimeframe(timeframe) {
    currentTimeframe = timeframe;

    // 버튼 활성화 상태 변경
    document.querySelectorAll('.timeframe-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    document.querySelector(`[data-timeframe="${timeframe}"]`).classList.add('active');

    // 차트 다시 로드
    loadChartData();
}

// 검색
function searchStock() {
    const ticker = document.getElementById('ticker-input').value.trim();
    if (ticker) {
        loadStock(ticker);
    }
}

// 엔터키 검색
function handleSearchKeyPress(event) {
    if (event.key === 'Enter') {
        searchStock();
    }
}

// 숫자 포맷
function formatNumber(num) {
    if (!num) return '0';
    return num.toLocaleString('ko-KR', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
    });
}

// 시가총액 포맷
function formatMarketCap(num) {
    if (!num) return '0';
    if (num >= 1e12) return (num / 1e12).toFixed(1) + '조';
    if (num >= 1e8) return (num / 1e8).toFixed(0) + '억';
    return formatNumber(num);
}

// 이평선 토글
function toggleSMA(period) {
    if (!chart) return;

    const isChecked = document.getElementById(`sma${period}-toggle`).checked;

    switch(period) {
        case 20:
            if (sma20Series) {
                sma20Series.applyOptions({ visible: isChecked });
            }
            break;
        case 60:
            if (sma60Series) {
                sma60Series.applyOptions({ visible: isChecked });
            }
            break;
        case 120:
            if (sma120Series) {
                sma120Series.applyOptions({ visible: isChecked });
            }
            break;
    }
}

// 날짜 범위 적용
function applyDateRange() {
    if (!chart) return;

    const startDate = document.getElementById('start-date').value;
    const endDate = document.getElementById('end-date').value;

    if (!startDate || !endDate) {
        alert('시작 날짜와 종료 날짜를 모두 선택해주세요.');
        return;
    }

    if (startDate > endDate) {
        alert('시작 날짜가 종료 날짜보다 늦을 수 없습니다.');
        return;
    }

    // 차트 시간 범위 설정
    chart.timeScale().setVisibleRange({
        from: startDate,
        to: endDate
    });
}

// 날짜 범위 초기화 (전체 데이터 표시)
function resetDateRange() {
    if (!chart) return;

    document.getElementById('start-date').value = '';
    document.getElementById('end-date').value = '';

    chart.timeScale().fitContent();
}

// 페이지 로드 시 기본 종목 표시 (삼성전자)
window.addEventListener('load', () => {
    // 날짜 입력 기본값 설정 (최근 6개월)
    const today = new Date();
    const sixMonthsAgo = new Date();
    sixMonthsAgo.setMonth(sixMonthsAgo.getMonth() - 6);

    document.getElementById('end-date').value = today.toISOString().split('T')[0];
    document.getElementById('start-date').value = sixMonthsAgo.toISOString().split('T')[0];

    loadStock('005930');
});