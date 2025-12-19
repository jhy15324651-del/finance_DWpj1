/**
 * 관리자 - 콘텐츠 리뷰 관리 JavaScript
 */

/**
 * 콘텐츠 리뷰 목록 로드
 */
async function loadContentReviewList() {
    try {
        const response = await fetch('/api/admin/content-reviews');
        if (!response.ok) {
            throw new Error('콘텐츠 리뷰 목록을 불러올 수 없습니다.');
        }

        const reviewList = await response.json();
        renderContentReviewList(reviewList);
        updateContentStats(reviewList);

    } catch (error) {
        console.error('콘텐츠 리뷰 목록 로딩 실패:', error);
        document.getElementById('content-list').innerHTML = `
            <tr>
                <td colspan="6" style="text-align: center; padding: 40px; color: #dc3545;">
                    오류 발생: ${error.message}
                </td>
            </tr>
        `;
    }
}

/**
 * 콘텐츠 리뷰 목록 렌더링
 */
function renderContentReviewList(reviewList) {
    const tbody = document.getElementById('content-list');

    if (!reviewList || reviewList.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" style="text-align: center; padding: 40px; color: #999;">
                    등록된 콘텐츠 리뷰가 없습니다.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = reviewList.map(review => {
        const isDeleted = review.isDeleted || review.deleted;
        const createdDate = formatDate(review.createdDate);
        const statusBadge = isDeleted
            ? '<span style="color: #dc3545; font-weight: bold;">삭제됨</span>'
            : '<span style="color: #28a745; font-weight: bold;">정상</span>';

        const deleteButton = isDeleted
            ? '<button disabled style="opacity: 0.5; cursor: not-allowed;" class="btn-sm btn-danger">삭제됨</button>'
            : `<button onclick="openDeleteContentModal(${review.id})" class="btn-sm btn-danger">삭제</button>`;

        return `
            <tr style="${isDeleted ? 'background-color: #f8f9fa; opacity: 0.7;' : ''}">
                <td>${review.id}</td>
                <td style="max-width: 400px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                    ${escapeHtml(review.title)}
                </td>
                <td>${review.writer || '-'}</td>
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
function updateContentStats(reviewList) {
    const totalCount = reviewList.length;
    const deletedCount = reviewList.filter(review => review.isDeleted || review.deleted).length;

    document.getElementById('content-total-count').textContent = totalCount;
    document.getElementById('content-deleted-count').textContent = deletedCount;
}

/**
 * 삭제 모달 열기 (콘텐츠 리뷰)
 */
function openDeleteContentModal(reviewId) {
    currentDeleteTarget = {
        type: 'content',
        id: reviewId
    };

    document.getElementById('delete-reason').value = '';
    document.getElementById('deleteModal').style.display = 'block';
}

/**
 * 콘텐츠 리뷰 삭제 API 호출
 */
async function deleteContentReview(reviewId, deleteReason) {
    try {
        const response = await fetch(`/api/admin/content-reviews/${reviewId}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ deleteReason })
        });

        const result = await response.json();

        if (result.success) {
            alert('콘텐츠 리뷰가 삭제되었습니다.');
            closeDeleteModal();
            loadContentReviewList(); // 목록 새로고침
        } else {
            alert('삭제 실패: ' + result.message);
        }

    } catch (error) {
        console.error('콘텐츠 리뷰 삭제 실패:', error);
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
 * 콘텐츠 탭 활성화 시 자동 로드
 */
document.addEventListener('DOMContentLoaded', function() {
    // 탭 전환 이벤트 리스너
    const contentTabBtn = document.querySelector('[data-tab="content"]');
    if (contentTabBtn) {
        contentTabBtn.addEventListener('click', function() {
            // 콘텐츠 탭 클릭 시 목록 로드
            setTimeout(() => {
                loadContentReviewList();
            }, 100);
        });
    }
});