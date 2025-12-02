let currentEditId = null;

// 페이지 로드 시 트윗 목록 로드
window.addEventListener('DOMContentLoaded', () => {
    loadTweets();
});

// 트윗 목록 로드
async function loadTweets() {
    try {
        const response = await fetch('/api/twitter/admin/tweets');
        const tweets = await response.json();

        displayTweets(tweets);
        updateStats(tweets.length);

    } catch (error) {
        console.error('트윗 로드 실패:', error);
        document.getElementById('tweet-list').innerHTML = '<div class="loading">트윗을 불러오는데 실패했습니다.</div>';
    }
}

// 트윗 표시
function displayTweets(tweets) {
    const container = document.getElementById('tweet-list');

    if (!tweets || tweets.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <h3>등록된 트윗이 없습니다</h3>
                <p>새 트윗을 추가하거나 더미 데이터를 초기화하세요.</p>
                <button class="btn-primary" onclick="initializeDummyData()">더미 데이터 초기화</button>
            </div>
        `;
        return;
    }

    container.innerHTML = tweets.map(tweet => `
        <div class="tweet-item">
            <div class="tweet-header">
                ${tweet.avatar ? `<img src="${tweet.avatar}" alt="${tweet.name}" class="tweet-avatar">` : ''}
                <div class="tweet-user-info">
                    <div>
                        <span class="tweet-name">${tweet.name}</span>
                        ${tweet.verified ? '<span class="tweet-verified">✓</span>' : ''}
                    </div>
                    <div class="tweet-handle">@${tweet.handle}</div>
                </div>
                <span class="tweet-source">${getSourceLabel(tweet.source)}</span>
            </div>
            <div class="tweet-content">
                <div class="tweet-original">${tweet.originalText}</div>
                <div class="tweet-translated">${tweet.translatedText}</div>
            </div>
            <div class="tweet-meta">
                <span>${tweet.date || '날짜 없음'}</span>
                <div class="tweet-actions">
                    <button class="btn-edit" onclick="openEditModal(${tweet.id})">수정</button>
                    <button class="btn-danger" onclick="deleteTweet(${tweet.id})">삭제</button>
                </div>
            </div>
        </div>
    `).join('');
}

// 출처 라벨 변환
function getSourceLabel(source) {
    const labels = {
        'DUMMY': '더미',
        'MANUAL': '수동',
        'API': 'API'
    };
    return labels[source] || source;
}

// 통계 업데이트
function updateStats(count) {
    document.getElementById('total-count').textContent = count;
}

// 추가 모달 열기
function openAddModal() {
    currentEditId = null;
    document.getElementById('modal-title').textContent = '새 트윗 추가';
    document.getElementById('tweet-form').reset();
    document.getElementById('tweet-id').value = '';
    document.getElementById('tweetModal').style.display = 'block';
}

// 수정 모달 열기
async function openEditModal(tweetId) {
    try {
        const response = await fetch('/api/twitter/admin/tweets');
        const tweets = await response.json();
        const tweet = tweets.find(t => t.id === tweetId);

        if (!tweet) {
            alert('트윗을 찾을 수 없습니다.');
            return;
        }

        currentEditId = tweetId;
        document.getElementById('modal-title').textContent = '트윗 수정';
        document.getElementById('tweet-id').value = tweet.id;
        document.getElementById('tweet-name').value = tweet.name;
        document.getElementById('tweet-handle').value = tweet.handle;
        document.getElementById('tweet-avatar').value = tweet.avatar || '';
        document.getElementById('tweet-verified').checked = tweet.verified;
        document.getElementById('tweet-original').value = tweet.originalText;
        document.getElementById('tweet-translated').value = tweet.translatedText;
        document.getElementById('tweet-date').value = tweet.date || '';
        document.getElementById('tweet-url').value = tweet.url || '';
        document.getElementById('tweetModal').style.display = 'block';

    } catch (error) {
        console.error('트윗 로드 실패:', error);
        alert('트윗을 불러오는데 실패했습니다.');
    }
}

// 모달 닫기
function closeModal() {
    document.getElementById('tweetModal').style.display = 'none';
    currentEditId = null;
}

// 트윗 폼 제출
document.getElementById('tweet-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const tweetData = {
        name: document.getElementById('tweet-name').value,
        handle: document.getElementById('tweet-handle').value,
        avatar: document.getElementById('tweet-avatar').value,
        verified: document.getElementById('tweet-verified').checked,
        originalText: document.getElementById('tweet-original').value,
        translatedText: document.getElementById('tweet-translated').value,
        date: document.getElementById('tweet-date').value,
        url: document.getElementById('tweet-url').value,
        source: 'MANUAL'
    };

    try {
        let response;
        if (currentEditId) {
            // 수정
            response = await fetch(`/api/twitter/admin/tweets/${currentEditId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(tweetData)
            });
        } else {
            // 추가
            response = await fetch('/api/twitter/admin/tweets', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(tweetData)
            });
        }

        if (response.ok) {
            alert(currentEditId ? '트윗이 수정되었습니다.' : '트윗이 추가되었습니다.');
            closeModal();
            loadTweets();
        } else {
            alert('저장에 실패했습니다.');
        }
    } catch (error) {
        console.error('트윗 저장 실패:', error);
        alert('저장에 실패했습니다.');
    }
});

// 트윗 삭제
async function deleteTweet(tweetId) {
    if (!confirm('정말로 이 트윗을 삭제하시겠습니까?')) {
        return;
    }

    try {
        const response = await fetch(`/api/twitter/admin/tweets/${tweetId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            alert('트윗이 삭제되었습니다.');
            loadTweets();
        } else {
            alert('삭제에 실패했습니다.');
        }
    } catch (error) {
        console.error('트윗 삭제 실패:', error);
        alert('삭제에 실패했습니다.');
    }
}

// 더미 데이터 초기화
async function initializeDummyData() {
    if (!confirm('더미 데이터를 초기화하시겠습니까?\n(이미 데이터가 있으면 초기화되지 않습니다)')) {
        return;
    }

    try {
        const response = await fetch('/api/twitter/admin/initialize-dummy');
        const result = await response.json();

        alert(result.message);

        if (result.success && result.count > 0) {
            loadTweets();
        }
    } catch (error) {
        console.error('더미 데이터 초기화 실패:', error);
        alert('초기화에 실패했습니다.');
    }
}

// 모달 외부 클릭 시 닫기
window.addEventListener('click', (event) => {
    const modal = document.getElementById('tweetModal');
    if (event.target === modal) {
        closeModal();
    }
});