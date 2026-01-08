/**
 * 관리자 - 뉴스 관리 JavaScript
 */

let currentDeleteTarget = null; // 삭제 대상 저장

/**
 * 뉴스 목록 로드
 */
async function loadNewsList() {
    try {
        const response = await fetch('/api/admin/news');
        if (!response.ok) {
            throw new Error('뉴스 목록을 불러올 수 없습니다.');
        }

        const newsList = await response.json();
        renderNewsList(newsList);
        updateNewsStats(newsList);

    } catch (error) {
        console.error('뉴스 목록 로딩 실패:', error);
        document.getElementById('news-list').innerHTML = `
            <tr>
                <td colspan="6" style="text-align: center; padding: 40px; color: #dc3545;">
                    오류 발생: ${error.message}
                </td>
            </tr>
        `;
    }
}

/**
 * 뉴스 목록 렌더링
 */
function renderNewsList(newsList) {
    const tbody = document.getElementById('news-list');

    if (!newsList || newsList.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" style="text-align: center; padding: 40px; color: #999;">
                    등록된 뉴스가 없습니다.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = newsList.map(news => {
        const isDeleted = news.isDeleted || news.deleted;
        const createdDate = formatDate(news.createdAt);
        const statusBadge = isDeleted
            ? '<span style="color: #dc3545; font-weight: bold;">삭제됨</span>'
            : '<span style="color: #28a745; font-weight: bold;">정상</span>';

        const deleteButton = isDeleted
            ? '<button disabled style="opacity: 0.5; cursor: not-allowed;" class="btn-sm btn-danger">삭제됨</button>'
            : `<button onclick="openDeleteNewsModal(${news.id})" class="btn-sm btn-danger">삭제</button>`;

        return `
            <tr style="${isDeleted ? 'background-color: #f8f9fa; opacity: 0.7;' : ''}">
                <td>${news.id}</td>
                <td style="max-width: 400px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                    ${escapeHtml(news.title)}
                </td>
                <td>${news.source || '-'}</td>
                <td>${createdDate}</td>
                <td>${statusBadge}</td>
                <td>${deleteButton}</td>
            </tr>
        `;
    }).join('');
}

/**
 * 통계 업데이트
 */
function updateNewsStats(newsList) {
    const totalCount = newsList.length;
    const deletedCount = newsList.filter(news => news.isDeleted || news.deleted).length;

    document.getElementById('news-total-count').textContent = totalCount;
    document.getElementById('news-deleted-count').textContent = deletedCount;
}

/**
 * 삭제 모달 열기 (뉴스)
 */
function openDeleteNewsModal(newsId) {
    currentDeleteTarget = {
        type: 'news',
        id: newsId
    };

    document.getElementById('delete-reason').value = '';
    document.getElementById('deleteModal').style.display = 'block';
}

/**
 * 삭제 모달 닫기
 */
function closeDeleteModal() {
    document.getElementById('deleteModal').style.display = 'none';
    currentDeleteTarget = null;
}

/**
 * 삭제 확인
 */
async function confirmDelete() {
    if (!currentDeleteTarget) {
        alert('삭제 대상이 선택되지 않았습니다.');
        return;
    }

    const deleteReason = document.getElementById('delete-reason').value.trim();

    if (!deleteReason) {
        alert('삭제 사유를 입력해주세요.');
        return;
    }

    const { type, id, title } = currentDeleteTarget;

    if (type === 'news') {
        await deleteNews(id, deleteReason);
    } else if (type === 'content') {
        await deleteContentReview(id, deleteReason);
    }
}

/**
 * 뉴스 삭제 API 호출
 */
async function deleteNews(newsId, deleteReason) {
    try {
        const response = await fetch(`/api/admin/news/${newsId}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ deleteReason })
        });

        const result = await response.json();

        if (result.success) {
            alert('뉴스가 삭제되었습니다.');
            closeDeleteModal();
            loadNewsList(); // 목록 새로고침
        } else {
            alert('삭제 실패: ' + result.message);
        }

    } catch (error) {
        console.error('뉴스 삭제 실패:', error);
        alert('삭제 중 오류가 발생했습니다.');
    }
}

/**
 * 날짜 포맷팅
 */
function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

/**
 * HTML 이스케이프 처리
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * 번역 안 된 뉴스 개수 확인
 */
async function checkUntranslatedNews() {
    try {
        const response = await fetch('/api/news/admin/untranslated-count');
        const result = await response.json();

        if (result.success) {
            document.getElementById('news-untranslated-count').textContent = result.count;
            alert(result.message);
        } else {
            alert('오류: ' + result.message);
        }

    } catch (error) {
        console.error('번역 안 된 뉴스 개수 조회 실패:', error);
        alert('번역 안 된 뉴스 개수 조회 중 오류가 발생했습니다.');
    }
}

/**
 * 번역 안 된 뉴스 재번역 실행
 */
async function retranslateNews() {
    // 확인 메시지
    if (!confirm('번역 안 된 뉴스를 재번역하시겠습니까?\n\n번역에는 시간이 소요될 수 있습니다.')) {
        return;
    }

    try {
        // 로딩 표시
        const originalText = document.getElementById('news-untranslated-count').textContent;
        document.getElementById('news-untranslated-count').textContent = '번역 중...';
        document.getElementById('news-untranslated-count').style.color = '#0d6efd';

        const response = await fetch('/api/news/admin/retranslate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();

        if (result.success) {
            alert(result.message);

            // 통계 업데이트
            document.getElementById('news-untranslated-count').textContent = result.failCount || 0;
            document.getElementById('news-untranslated-count').style.color = '#fd7e14';

            // 뉴스 목록 새로고침
            loadNewsList();
        } else {
            alert('재번역 실패: ' + result.message);
            document.getElementById('news-untranslated-count').textContent = originalText;
            document.getElementById('news-untranslated-count').style.color = '#fd7e14';
        }

    } catch (error) {
        console.error('재번역 실패:', error);
        alert('재번역 중 오류가 발생했습니다.');
        document.getElementById('news-untranslated-count').textContent = '오류';
        document.getElementById('news-untranslated-count').style.color = '#dc3545';
    }
}

/**
 * 수동 크롤링 실행
 */
async function manualCrawlNews() {
    if (!confirm('수동 크롤링을 실행하시겠습니까?\n\n크롤링에는 시간이 소요될 수 있습니다.')) {
        return;
    }

    try {
        const response = await fetch('/api/admin/news/manual-crawl', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();

        if (result.success) {
            alert(result.message);
            loadNewsList();
        } else {
            alert('크롤링 실패: ' + result.message);
        }

    } catch (error) {
        console.error('수동 크롤링 실패:', error);
        alert('크롤링 중 오류가 발생했습니다.');
    }
}

/**
 * 뉴스 탭 활성화 시 자동 로드
 */
document.addEventListener('DOMContentLoaded', function() {
    // 탭 전환 이벤트 리스너
    const newsTabBtn = document.querySelector('[data-tab="news"]');
    if (newsTabBtn) {
        newsTabBtn.addEventListener('click', function() {
            // 뉴스 탭 클릭 시 목록 로드 및 번역 안 된 뉴스 개수 조회
            setTimeout(() => {
                loadNewsList();
                checkUntranslatedNews();
            }, 100);
        });
    }
});