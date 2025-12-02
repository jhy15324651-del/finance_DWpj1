let currentNewsId = null;
let currentComments = [];
let displayedCommentsCount = 0;
const COMMENTS_PER_PAGE = 10;
let currentReplyParentId = null;
const isAdmin = false; // 실제 환경에서는 서버에서 관리자 여부를 받아와야 함

// 페이지 로드 시 데일리 뉴스 로드
window.addEventListener('DOMContentLoaded', () => {
    loadDailyNews();
    setupModalClose();

    // URL 파라미터에서 newsId 확인하여 자동으로 모달 열기
    const urlParams = new URLSearchParams(window.location.search);
    const newsId = urlParams.get('newsId');
    if (newsId) {
        showNewsDetail(parseInt(newsId));
    }
});

// 모달 닫기 설정
function setupModalClose() {
    // 닫기 버튼 클릭
    document.querySelectorAll('.close').forEach(btn => {
        btn.addEventListener('click', () => {
            document.getElementById('newsModal').style.display = 'none';
            document.getElementById('replyModal').style.display = 'none';
        });
    });

    // 모달 외부 클릭
    window.addEventListener('click', (event) => {
        const newsModal = document.getElementById('newsModal');
        const replyModal = document.getElementById('replyModal');
        if (event.target === newsModal) {
            newsModal.style.display = 'none';
        }
        if (event.target === replyModal) {
            replyModal.style.display = 'none';
        }
    });
}

// 탭 전환 (데일리/금주/아카이브)
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
    } else if (tabName === 'twitter') {
        loadTwitterInsights();
    }
}

// 뉴스 내용 탭 전환 (요약/번역/원문)
function switchNewsTab(tabName) {
    // 모든 뉴스 탭 비활성화
    document.querySelectorAll('.news-tab').forEach(tab => tab.classList.remove('active'));
    document.querySelectorAll('.news-tab-pane').forEach(pane => pane.classList.remove('active'));

    // 선택된 탭 활성화
    document.querySelector(`button[onclick="switchNewsTab('${tabName}')"]`).classList.add('active');
    document.getElementById(`tab-${tabName}`).classList.add('active');
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

// 아카이브 뉴스 로드
async function loadArchiveNews() {
    try {
        const response = await fetch('/api/news/archive?page=0&size=20');
        const result = await response.json();
        displayNews(result.content, 'archive-news');
    } catch (error) {
        console.error('아카이브 뉴스 로드 실패:', error);
        document.getElementById('archive-news').innerHTML = '<div class="loading">뉴스를 불러오는데 실패했습니다.</div>';
    }
}

// 뉴스 표시
function displayNews(newsList, containerId) {
    const container = document.getElementById(containerId);

    if (!newsList || newsList.length === 0) {
        container.innerHTML = '<div class="loading">등록된 뉴스가 없습니다.</div>';
        return;
    }

    container.innerHTML = newsList.map(news => `
        <div class="news-card" onclick="showNewsDetail(${news.id})">
            <div class="news-header">
                <span class="news-badge">DAILY</span>
                <span class="news-date">${news.createdAt}</span>
            </div>
            <div class="news-title">${news.title}</div>
            <div class="news-source">📰 ${news.source || 'Yahoo Finance'}</div>
            <div class="summary">${news.summary || '요약이 없습니다.'}</div>
            <div class="news-stats">
                <span class="stat-item">👁 ${news.viewCount || 0} views</span>
                <span class="stat-item">💬 ${news.commentCount || 0} comments</span>
            </div>
        </div>
    `).join('');
}

// 금주의 뉴스 표시 (랭킹 포함)
function displayWeeklyNews(newsList) {
    const container = document.getElementById('weekly-news');

    if (!newsList || newsList.length === 0) {
        container.innerHTML = '<div class="loading">등록된 뉴스가 없습니다.</div>';
        return;
    }

    const top10 = newsList.slice(0, 10);

    function getRankBadge(rank) {
        if (rank === 1) return '👑';
        if (rank === 2) return '🥈';
        if (rank === 3) return '🥉';
        return rank;
    }

    function getRankClass(rank) {
        if (rank === 1) return 'rank-1';
        if (rank === 2) return 'rank-2';
        if (rank === 3) return 'rank-3';
        return '';
    }

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
                    <span class="news-badge">TOP ${rank}</span>
                    <span class="news-date">${news.createdAt}</span>
                </div>
                <div class="news-title" style="font-size: 1rem; min-height: 60px;">${news.title}</div>
                <div class="news-stats">
                    <span class="stat-item">👁 ${news.viewCount || 0}</span>
                    <span class="stat-item">💬 ${news.commentCount || 0}</span>
                </div>
            </div>
        `;
    }).join('');
}

// 뉴스 상세 모달 열기
async function showNewsDetail(newsId) {
    try {
        currentNewsId = newsId;

        // 뉴스 상세 정보 가져오기
        const response = await fetch(`/api/news/detail/${newsId}`);
        const news = await response.json();

        // 모달에 정보 표시
        document.getElementById('modal-title').textContent = news.title;
        document.getElementById('modal-source').textContent = `📰 출처: ${news.source || 'Yahoo Finance'} | ${news.createdAt}`;

        // 탭 내용 설정
        document.getElementById('modal-summary').textContent = news.summary || 'GPT API 키를 설정하면 자동 요약이 생성됩니다.';
        document.getElementById('modal-translated').textContent = news.content || '번역된 내용이 없습니다.';
        document.getElementById('modal-original').textContent = news.originalContent || '원문이 없습니다.';

        // 첫 번째 탭(요약)을 활성화
        switchNewsTab('summary');

        // 댓글 로드
        await loadComments(newsId);

        // 모달 표시
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

        // 좋아요 순으로 정렬
        currentComments = comments.sort((a, b) => (b.likeCount || 0) - (a.likeCount || 0));
        displayedCommentsCount = 0;

        // 댓글 개수 표시
        document.getElementById('comments-total').textContent = currentComments.length;

        // 처음 10개 댓글 표시
        displayComments();

    } catch (error) {
        console.error('댓글 로드 실패:', error);
        document.getElementById('comment-list').innerHTML = '<div class="loading">댓글을 불러오는데 실패했습니다.</div>';
    }
}

// 댓글 표시
function displayComments() {
    const commentList = document.getElementById('comment-list');
    const loadMoreContainer = document.getElementById('load-more-container');

    // 일반 댓글만 필터링 (답글 제외)
    const mainComments = currentComments.filter(c => !c.isReply);

    // 표시할 댓글 개수 계산
    const nextCount = displayedCommentsCount + COMMENTS_PER_PAGE;
    const commentsToShow = mainComments.slice(0, nextCount);
    displayedCommentsCount = commentsToShow.length;

    // 댓글 HTML 생성
    commentList.innerHTML = commentsToShow.map(comment => {
        // 해당 댓글의 답글 찾기
        const replies = currentComments.filter(c => c.parentCommentId === comment.id);

        return `
            <div class="comment-item">
                <div class="comment-header">
                    <div class="comment-user-info">
                        <span class="comment-author">${comment.userName}</span>
                        <span class="comment-date">${comment.createdAt}</span>
                    </div>
                </div>
                <div class="comment-content">${comment.content}</div>
                <div class="comment-actions">
                    <button class="btn-action" onclick="likeComment(${comment.id})">
                        👍 좋아요 ${comment.likeCount || 0}
                    </button>
                    <button class="btn-action" onclick="dislikeComment(${comment.id})">
                        👎 싫어요 ${comment.dislikeCount || 0}
                    </button>
                    <button class="btn-reply" onclick="openReplyModal(${comment.id}, '${comment.userName}')">
                        💬 답글
                    </button>
                </div>

                ${replies.length > 0 ? replies.map(reply => `
                    <div class="reply-item">
                        <div class="comment-header">
                            <div class="comment-user-info">
                                <span class="reply-badge">답글</span>
                                <span class="comment-author">${reply.userName}</span>
                                <span class="comment-date">${reply.createdAt}</span>
                            </div>
                        </div>
                        <div class="comment-content">${reply.content}</div>
                        <div class="comment-actions">
                            <button class="btn-action" onclick="likeComment(${reply.id})">
                                👍 ${reply.likeCount || 0}
                            </button>
                            <button class="btn-action" onclick="dislikeComment(${reply.id})">
                                👎 ${reply.dislikeCount || 0}
                            </button>
                        </div>
                    </div>
                `).join('') : ''}
            </div>
        `;
    }).join('');

    // 더보기 버튼 표시/숨김
    if (displayedCommentsCount < mainComments.length) {
        loadMoreContainer.style.display = 'block';
    } else {
        loadMoreContainer.style.display = 'none';
    }
}

// 더보기 버튼
function loadMoreComments() {
    displayComments();
}

// 댓글 폼으로 스크롤
function scrollToCommentForm() {
    document.getElementById('comment-form-section').scrollIntoView({ behavior: 'smooth' });
}

// 댓글 작성
async function submitComment() {
    const userName = document.getElementById('comment-user-name').value.trim();
    const content = document.getElementById('comment-content').value.trim();

    if (!userName || !content) {
        alert('이름과 의견을 모두 입력해주세요.');
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
            // 입력 필드 초기화
            document.getElementById('comment-user-name').value = '';
            document.getElementById('comment-content').value = '';

            // 댓글 목록 새로고침
            await loadComments(currentNewsId);

            alert('의견이 등록되었습니다.');
        } else {
            alert('의견 등록에 실패했습니다.');
        }
    } catch (error) {
        console.error('댓글 작성 실패:', error);
        alert('의견 등록에 실패했습니다.');
    }
}

// 댓글 좋아요
async function likeComment(commentId) {
    try {
        const response = await fetch(`/api/news/comments/${commentId}/like`, {
            method: 'POST'
        });

        if (response.ok) {
            // 댓글 목록 새로고침
            await loadComments(currentNewsId);
        }
    } catch (error) {
        console.error('좋아요 실패:', error);
    }
}

// 댓글 싫어요
async function dislikeComment(commentId) {
    try {
        const response = await fetch(`/api/news/comments/${commentId}/dislike`, {
            method: 'POST'
        });

        if (response.ok) {
            // 댓글 목록 새로고침
            await loadComments(currentNewsId);
        }
    } catch (error) {
        console.error('싫어요 실패:', error);
    }
}

// 답글 모달 열기
function openReplyModal(parentCommentId, parentUserName) {
    currentReplyParentId = parentCommentId;
    document.getElementById('reply-to-user').textContent = `@${parentUserName} 님에게 답글`;
    document.getElementById('replyModal').style.display = 'block';
}

// 답글 모달 닫기
function closeReplyModal() {
    document.getElementById('replyModal').style.display = 'none';
    document.getElementById('reply-user-name').value = '';
    document.getElementById('reply-content').value = '';
    currentReplyParentId = null;
}

// 답글 작성
async function submitReply() {
    const userName = document.getElementById('reply-user-name').value.trim();
    const content = document.getElementById('reply-content').value.trim();

    if (!userName || !content) {
        alert('이름과 답글을 모두 입력해주세요.');
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
                content: content,
                parentCommentId: currentReplyParentId
            })
        });

        if (response.ok) {
            // 답글 모달 닫기
            closeReplyModal();

            // 댓글 목록 새로고침
            await loadComments(currentNewsId);

            alert('답글이 등록되었습니다.');
        } else {
            alert('답글 등록에 실패했습니다.');
        }
    } catch (error) {
        console.error('답글 작성 실패:', error);
        alert('답글 등록에 실패했습니다.');
    }
}

// 트위터 인사이트 로드
async function loadTwitterInsights() {
    try {
        const response = await fetch('/api/twitter/insights');
        const tweets = await response.json();
        displayTwitterInsights(tweets);
    } catch (error) {
        console.error('트위터 인사이트 로드 실패:', error);
        document.getElementById('twitter-insights').innerHTML = '<div class="loading">트위터 인사이트를 불러오는데 실패했습니다.</div>';
    }
}

// 트위터 인사이트 표시
function displayTwitterInsights(tweets) {
    const container = document.getElementById('twitter-insights');

    if (!tweets || tweets.length === 0) {
        container.innerHTML = '<div class="loading">등록된 트윗이 없습니다.</div>';
        return;
    }

    container.innerHTML = tweets.map(tweet => `
        <div class="tweet-card">
            <div class="tweet-header">
                ${tweet.avatar ? `<img src="${tweet.avatar}" alt="${tweet.name}" class="tweet-avatar">` : ''}
                <div class="tweet-user-info">
                    <div class="tweet-name-row">
                        <span class="tweet-name">${tweet.name}</span>
                        ${tweet.verified ? '<span class="verified-badge">✓</span>' : ''}
                    </div>
                    <div class="tweet-handle">@${tweet.handle}</div>
                </div>
                <div class="tweet-date">${tweet.date || ''}</div>
            </div>
            <div class="tweet-content">
                <div class="tweet-original">${tweet.originalText}</div>
                <div class="tweet-translated">${tweet.translatedText}</div>
            </div>
            ${tweet.url ? `<a href="${tweet.url}" target="_blank" class="tweet-link">트윗 보기 →</a>` : ''}
        </div>
    `).join('');
}