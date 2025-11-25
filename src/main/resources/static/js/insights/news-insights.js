let currentNewsId = null;
const isAdmin = false; // 실제 환경에서는 서버에서 관리자 여부를 받아와야 함

// 페이지 로드 시 데일리 뉴스 로드
window.addEventListener('DOMContentLoaded', () => {
    loadDailyNews();
});

// 탭 전환
function switchTab(tabName) {
    // 모든 탭 비활성화
    document.querySelectorAll('.tab').forEach(tab => tab.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));

    // 선택된 탭 활성화
    document.querySelector(`button[onclick="switchTab('${tabName}')"]`).classList.add('active');
    document.getElementById(`${tabName}-tab`).classList.add('active');

    // 데이터 로드
    if (tabName === 'daily') {
        loadDailyNews();
    } else if (tabName === 'weekly') {
        loadWeeklyNews();
    } else if (tabName === 'archive') {
        loadArchiveNews();
    }
}

// 데일리 뉴스 로드
async function loadDailyNews() {
    try {
        const response = await fetch('/api/news/daily');
        const newsList = await response.json();
        displayNews(newsList, 'daily-news');
    } catch (error) {
        console.error('데일리 뉴스 로드 실패:', error);
        document.getElementById('daily-news').innerHTML = '<div class="loading">뉴스를 불러오는데 실패했습니다.</div>';
    }
}

// 금주의 뉴스 로드
async function loadWeeklyNews() {
    try {
        const response = await fetch('/api/news/weekly-top');
        const newsList = await response.json();
        displayWeeklyNews(newsList);
    } catch (error) {
        console.error('금주의 뉴스 로드 실패:', error);
        document.getElementById('weekly-news').innerHTML = '<div class="loading">뉴스를 불러오는데 실패했습니다.</div>';
    }
}

// 금주의 뉴스 표시 (랭킹 포함)
function displayWeeklyNews(newsList) {
    const container = document.getElementById('weekly-news');

    if (!newsList || newsList.length === 0) {
        container.innerHTML = '<div class="loading">등록된 뉴스가 없습니다.</div>';
        return;
    }

    // 최대 10개로 제한
    const top10 = newsList.slice(0, 10);

    // 랭킹 아이콘 함수
    function getRankBadge(rank) {
        if (rank === 1) return '👑'; // 금관
        if (rank === 2) return '🥈'; // 은관
        if (rank === 3) return '🥉'; // 동관
        return rank; // 4-10등은 숫자
    }

    // 랭크 클래스 함수
    function getRankClass(rank) {
        if (rank === 1) return 'rank-1';
        if (rank === 2) return 'rank-2';
        if (rank === 3) return 'rank-3';
        return '';
    }

    // 배지 클래스 함수
    function getBadgeClass(rank) {
        if (rank === 1) return 'gold';
        if (rank === 2) return 'silver';
        if (rank === 3) return 'bronze';
        return 'number';
    }

    container.innerHTML = top10.map((news, index) => {
        const rank = index + 1;
        return `
            <div class="weekly-news-card ${getRankClass(rank)}" onclick="showNewsDetail(${news.id})">
                <div class="rank-badge ${getBadgeClass(rank)}">
                    ${getRankBadge(rank)}
                </div>
                <div class="news-header" style="margin-top: 15px;">
                    <span class="news-badge" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">TOP ${rank}</span>
                    <span class="news-date">${news.createdAt}</span>
                </div>
                <div class="news-title" style="font-size: 1rem; min-height: 60px;">${news.title}</div>
                <div class="news-source">📰 ${news.source}</div>
                <div class="news-stats">
                    <div class="stat-item">👁️ ${news.viewCount || 0}회</div>
                    <div class="stat-item">💬 ${news.commentCount || 0}개</div>
                </div>
            </div>
        `;
    }).join('');
}

// 아카이브 뉴스 로드
async function loadArchiveNews() {
    try {
        const response = await fetch('/api/news/archive?page=0&size=20');
        const data = await response.json();
        displayNews(data.content, 'archive-news');
    } catch (error) {
        console.error('아카이브 로드 실패:', error);
        document.getElementById('archive-news').innerHTML = '<div class="loading">뉴스를 불러오는데 실패했습니다.</div>';
    }
}

// 뉴스 목록 표시
function displayNews(newsList, containerId) {
    const container = document.getElementById(containerId);

    if (!newsList || newsList.length === 0) {
        container.innerHTML = '<div class="loading">등록된 뉴스가 없습니다.</div>';
        return;
    }

    container.innerHTML = newsList.map(news => `
        <div class="news-card" onclick="showNewsDetail(${news.id})">
            <div class="news-header">
                <span class="news-badge">${news.status === 'DAILY' ? '데일리' : '아카이브'}</span>
                <span class="news-date">${news.createdAt}</span>
            </div>
            <div class="news-title">${news.title}</div>
            <div class="news-source">📰 ${news.source}</div>
            ${news.summary ? `<div class="summary">${news.summary}</div>` : ''}
            <div class="news-stats">
                <div class="stat-item">👁️ ${news.viewCount || 0}회</div>
                <div class="stat-item">💬 ${news.commentCount || 0}개</div>
            </div>
        </div>
    `).join('');
}

// 뉴스 상세 보기
async function showNewsDetail(newsId) {
    currentNewsId = newsId;

    try {
        // 뉴스 상세 정보 가져오기 (조회수 증가)
        const response = await fetch(`/api/news/${newsId}`);
        const news = await response.json();

        // 모달에 뉴스 정보 표시
        document.getElementById('modal-title').textContent = news.title;
        document.getElementById('modal-meta').innerHTML = `
            <div style="display: flex; justify-content: space-between; color: #718096;">
                <span>📰 ${news.source}</span>
                <span>${news.createdAt}</span>
            </div>
        `;
        document.getElementById('modal-summary').textContent = news.summary || '';
        document.getElementById('modal-content').textContent = news.content || '내용이 없습니다.';

        // 관리자 버튼 표시
        document.getElementById('admin-buttons').style.display = isAdmin ? 'flex' : 'none';

        // 댓글 로드
        loadComments(newsId);

        // 모달 열기
        document.getElementById('newsModal').style.display = 'block';
    } catch (error) {
        console.error('뉴스 상세 로드 실패:', error);
        alert('뉴스를 불러오는데 실패했습니다.');
    }
}

// 댓글 로드
async function loadComments(newsId) {
    try {
        const response = await fetch(`/api/news/${newsId}/comments`);
        const comments = await response.json();

        const commentList = document.getElementById('comment-list');
        if (comments.length === 0) {
            commentList.innerHTML = '<div style="text-align: center; color: #718096;">첫 댓글을 작성해보세요!</div>';
            return;
        }

        commentList.innerHTML = comments.map(comment => `
            <div class="comment-item">
                <div class="comment-author">${comment.userName}</div>
                <div class="comment-date">${comment.createdAt}</div>
                <div class="comment-content">${comment.content}</div>
                ${isAdmin ? `<button class="btn-delete" onclick="deleteComment(${comment.id})">삭제</button>` : ''}
            </div>
        `).join('');
    } catch (error) {
        console.error('댓글 로드 실패:', error);
    }
}

// 댓글 작성
async function submitComment() {
    const userName = document.getElementById('comment-user-name').value.trim();
    const content = document.getElementById('comment-content').value.trim();

    if (!userName || !content) {
        alert('이름과 댓글 내용을 입력해주세요.');
        return;
    }

    try {
        const response = await fetch(`/api/news/${currentNewsId}/comments`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                userName: userName,
                content: content
            })
        });

        if (response.ok) {
            alert('댓글이 작성되었습니다.');
            document.getElementById('comment-user-name').value = '';
            document.getElementById('comment-content').value = '';
            loadComments(currentNewsId);
        } else {
            alert('댓글 작성에 실패했습니다.');
        }
    } catch (error) {
        console.error('댓글 작성 실패:', error);
        alert('댓글 작성에 실패했습니다.');
    }
}

// 뉴스 삭제 (관리자 전용)
async function deleteNews() {
    if (!confirm('정말 이 뉴스를 삭제하시겠습니까?')) {
        return;
    }

    try {
        const response = await fetch(`/api/news/admin/${currentNewsId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            alert('뉴스가 삭제되었습니다.');
            closeModal();
            loadDailyNews();
        } else {
            alert('뉴스 삭제에 실패했습니다.');
        }
    } catch (error) {
        console.error('뉴스 삭제 실패:', error);
        alert('뉴스 삭제에 실패했습니다.');
    }
}

// 댓글 삭제 (관리자 전용)
async function deleteComment(commentId) {
    if (!confirm('정말 이 댓글을 삭제하시겠습니까?')) {
        return;
    }

    try {
        const response = await fetch(`/api/news/admin/comments/${commentId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            alert('댓글이 삭제되었습니다.');
            loadComments(currentNewsId);
        } else {
            alert('댓글 삭제에 실패했습니다.');
        }
    } catch (error) {
        console.error('댓글 삭제 실패:', error);
        alert('댓글 삭제에 실패했습니다.');
    }
}

// 모달 닫기
function closeModal() {
    document.getElementById('newsModal').style.display = 'none';
}

// 모달 외부 클릭 시 닫기
window.onclick = function(event) {
    const modal = document.getElementById('newsModal');
    if (event.target === modal) {
        closeModal();
    }
}

// 닫기 버튼 클릭
document.querySelector('.close').addEventListener('click', closeModal);