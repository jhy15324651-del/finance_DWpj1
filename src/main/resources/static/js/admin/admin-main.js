// ======================
// 탭 전환 로직
// ======================

document.addEventListener('DOMContentLoaded', function() {
    // 탭 버튼 클릭 이벤트
    const tabBtns = document.querySelectorAll('.tab-btn');
    tabBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const tabName = this.dataset.tab;
            switchTab(tabName);
        });
    });

    // 초기 데이터 로드
    loadTwitterData();
});

function switchTab(tabName) {
    // 모든 탭 버튼 비활성화
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });

    // 모든 탭 컨텐츠 숨기기
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.remove('active');
    });

    // 선택한 탭 활성화
    document.querySelector(`[data-tab="${tabName}"]`).classList.add('active');
    document.getElementById(`${tabName}-tab`).classList.add('active');

    // 탭별 초기화
    if (tabName === 'twitter') {
        loadTwitterData();
    } else if (tabName === 'portfolio') {
        // 포트폴리오 통계 로드 (필요시)
    }
}

// ======================
// 트위터 관리 기능
// ======================

function loadTwitterData() {
    fetch('/api/twitter/admin/tweets')
        .then(response => response.json())
        .then(tweets => {
            document.getElementById('total-count').textContent = tweets.length;
            displayTweets(tweets);
        })
        .catch(error => {
            console.error('트윗 로드 실패:', error);
            document.getElementById('tweet-list').innerHTML =
                '<div class="error">트윗을 불러오는데 실패했습니다.</div>';
        });
}

function displayTweets(tweets) {
    const tweetList = document.getElementById('tweet-list');

    if (tweets.length === 0) {
        tweetList.innerHTML = '<div class="empty-state">등록된 트윗이 없습니다.</div>';
        return;
    }

    tweetList.innerHTML = tweets.map(tweet => `
        <div class="tweet-card">
            <div class="tweet-header">
                <div class="tweet-author">
                    <strong>${tweet.name}</strong>
                    <span class="handle">@${tweet.handle}</span>
                </div>
                <div class="tweet-actions">
                    <button class="btn-sm btn-edit" onclick="editTweet(${tweet.tweetId})">수정</button>
                    <button class="btn-sm btn-delete" onclick="deleteTweet(${tweet.tweetId})">삭제</button>
                </div>
            </div>
            <div class="tweet-content">
                <p>${tweet.translated}</p>
            </div>
        </div>
    `).join('');
}

function openAddModal() {
    document.getElementById('modal-title').textContent = '새 트윗 추가';
    document.getElementById('tweet-form').reset();
    document.getElementById('tweet-id').value = '';
    document.getElementById('tweetModal').classList.add('show');
}

function closeModal() {
    document.getElementById('tweetModal').classList.remove('show');
}

function editTweet(tweetId) {
    // 트윗 데이터 로드 및 모달 열기
    console.log('트윗 수정:', tweetId);
    // 실제 구현은 기존 twitter-admin.js 참고
}

function deleteTweet(tweetId) {
    if (!confirm('정말 이 트윗을 삭제하시겠습니까?')) {
        return;
    }

    fetch(`/api/twitter/admin/tweets/${tweetId}`, {
        method: 'DELETE'
    })
    .then(response => response.json())
    .then(data => {
        alert(data.message);
        loadTwitterData();
    })
    .catch(error => {
        console.error('트윗 삭제 실패:', error);
        alert('트윗 삭제에 실패했습니다.');
    });
}

function initializeDummyData() {
    if (!confirm('더미 데이터를 초기화하시겠습니까?')) {
        return;
    }

    fetch('/api/twitter/admin/initialize-dummy')
        .then(response => response.json())
        .then(data => {
            alert(data.message);
            if (data.success) {
                loadTwitterData();
            }
        })
        .catch(error => {
            console.error('더미 데이터 초기화 실패:', error);
            alert('더미 데이터 초기화에 실패했습니다.');
        });
}

// ======================
// 포트폴리오 관리 기능
// ======================

/**
 * 전체 투자자 13F 데이터 수집
 */
function fetchAll13FData() {
    const statusDiv = document.getElementById('fetch-status');
    const allButtons = document.querySelectorAll('.investor-btn, .btn-large');

    // 버튼 비활성화
    allButtons.forEach(btn => btn.disabled = true);

    // 로딩 상태 표시
    statusDiv.className = 'fetch-status show loading';
    statusDiv.innerHTML = `
        <strong>⏳ 데이터 수집 중...</strong><br>
        SEC API에서 전체 투자자의 13F 데이터를 가져오고 있습니다.<br>
        <small>※ 이 작업은 2-5분 정도 소요될 수 있습니다.</small>
    `;

    fetch('/api/portfolio/admin/fetch-13f-all', {
        method: 'POST'
    })
    .then(response => response.json())
    .then(data => {
        // 버튼 활성화
        allButtons.forEach(btn => btn.disabled = false);

        if (data.success) {
            statusDiv.className = 'fetch-status show success';
            statusDiv.innerHTML = `
                <strong>✅ 수집 완료!</strong><br>
                ${data.message}
            `;

            // 5초 후 상태 메시지 숨기기
            setTimeout(() => {
                statusDiv.classList.remove('show');
            }, 5000);
        } else {
            statusDiv.className = 'fetch-status show error';
            statusDiv.innerHTML = `
                <strong>❌ 수집 실패</strong><br>
                ${data.message}<br>
                <small>${data.error || ''}</small>
            `;
        }
    })
    .catch(error => {
        console.error('13F 데이터 수집 실패:', error);
        allButtons.forEach(btn => btn.disabled = false);

        statusDiv.className = 'fetch-status show error';
        statusDiv.innerHTML = `
            <strong>❌ 네트워크 오류</strong><br>
            서버와의 통신에 실패했습니다.<br>
            <small>${error.message}</small>
        `;
    });
}

/**
 * 특정 투자자 13F 데이터 수집
 */
function fetch13FData(investorId) {
    const statusDiv = document.getElementById('fetch-status');
    const button = event.target;

    // 버튼 비활성화
    button.disabled = true;
    button.textContent = '수집 중...';

    // 로딩 상태 표시
    statusDiv.className = 'fetch-status show loading';
    statusDiv.innerHTML = `
        <strong>⏳ ${investorId} 데이터 수집 중...</strong><br>
        SEC API에서 데이터를 가져오고 있습니다.
    `;

    fetch(`/api/portfolio/admin/fetch-13f/${investorId}`, {
        method: 'POST'
    })
    .then(response => response.json())
    .then(data => {
        // 버튼 복구
        button.disabled = false;
        button.textContent = button.textContent.replace('수집 중...', getInvestorName(investorId));

        if (data.success) {
            statusDiv.className = 'fetch-status show success';
            statusDiv.innerHTML = `
                <strong>✅ 수집 완료!</strong><br>
                ${data.message}
            `;

            // 3초 후 상태 메시지 숨기기
            setTimeout(() => {
                statusDiv.classList.remove('show');
            }, 3000);
        } else {
            statusDiv.className = 'fetch-status show error';
            statusDiv.innerHTML = `
                <strong>❌ 수집 실패</strong><br>
                ${data.message}<br>
                <small>${data.error || ''}</small>
            `;
        }
    })
    .catch(error => {
        console.error(`${investorId} 13F 데이터 수집 실패:`, error);
        button.disabled = false;
        button.textContent = getInvestorName(investorId);

        statusDiv.className = 'fetch-status show error';
        statusDiv.innerHTML = `
            <strong>❌ 네트워크 오류</strong><br>
            ${investorId} 데이터 수집에 실패했습니다.<br>
            <small>${error.message}</small>
        `;
    });
}

/**
 * 투자자 ID로 이름 가져오기
 */
function getInvestorName(investorId) {
    const names = {
        'buffett': 'Warren Buffett',
        'wood': 'Cathie Wood',
        'dalio': 'Ray Dalio',
        'soros': 'George Soros',
        'lynch': 'Peter Lynch',
        'graham': 'Benjamin Graham',
        'thiel': 'Peter Thiel',
        'fink': 'Larry Fink',
        'simons': 'Jim Simons',
        'ackman': 'Bill Ackman'
    };
    return names[investorId] || investorId;
}

// ======================
// 트윗 폼 제출 (트위터 탭용)
// ======================

document.getElementById('tweet-form')?.addEventListener('submit', function(e) {
    e.preventDefault();

    const tweetId = document.getElementById('tweet-id').value;
    const tweetData = {
        name: document.getElementById('tweet-name').value,
        handle: document.getElementById('tweet-handle').value,
        avatarUrl: document.getElementById('tweet-avatar').value,
        verified: document.getElementById('tweet-verified').checked,
        originalText: document.getElementById('tweet-original').value,
        translated: document.getElementById('tweet-translated').value,
        timeAgo: document.getElementById('tweet-date').value,
        tweetUrl: document.getElementById('tweet-url').value
    };

    const url = tweetId
        ? `/api/twitter/admin/tweets/${tweetId}`
        : '/api/twitter/admin/tweets';

    const method = tweetId ? 'PUT' : 'POST';

    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(tweetData)
    })
    .then(response => response.json())
    .then(data => {
        alert(tweetId ? '트윗이 수정되었습니다.' : '트윗이 추가되었습니다.');
        closeModal();
        loadTwitterData();
    })
    .catch(error => {
        console.error('트윗 저장 실패:', error);
        alert('트윗 저장에 실패했습니다.');
    });
});

// 모달 외부 클릭 시 닫기
window.onclick = function(event) {
    const modal = document.getElementById('tweetModal');
    if (event.target === modal) {
        closeModal();
    }
}