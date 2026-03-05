-- ========================================
-- 관리자 콘텐츠 삭제 기능 DB 마이그레이션
-- ========================================

-- 1. InsightsNews 테이블에 삭제 관련 필드 추가
ALTER TABLE news
    ADD COLUMN deleted_at DATETIME COMMENT '삭제된 시간',
    ADD COLUMN deleted_by VARCHAR(100) COMMENT '삭제한 관리자 이메일',
    ADD COLUMN delete_reason TEXT COMMENT '삭제 사유';

-- 2. ContentReview 테이블에 삭제 관련 필드 추가
-- (deleted_at은 이미 존재하므로 나머지만 추가)
ALTER TABLE content_review
    ADD COLUMN deleted_by VARCHAR(100) COMMENT '삭제한 관리자 이메일',
    ADD COLUMN delete_reason TEXT COMMENT '삭제 사유';

-- 3. 감사 로그 테이블 생성
CREATE TABLE audit_delete_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 삭제 행위 정보
    action VARCHAR(20) NOT NULL COMMENT 'SOFT_DELETE / HARD_DELETE',
    target_type VARCHAR(50) NOT NULL COMMENT 'NEWS / CONTENT_REVIEW',
    target_id BIGINT NOT NULL COMMENT '삭제 대상 PK',
    target_title VARCHAR(500) COMMENT '삭제 당시 제목 스냅샷',

    -- 행위자 정보
    actor_user_id BIGINT COMMENT '관리자 PK (있다면)',
    actor_email VARCHAR(100) COMMENT '관리자 계정 이메일',
    actor_role VARCHAR(20) COMMENT 'ROLE_ADMIN',

    -- 삭제 사유 및 추적 정보
    reason TEXT COMMENT '삭제 사유 (관리자 입력 또는 AUTO_PURGE_AFTER_30_DAYS)',
    ip_address VARCHAR(50) COMMENT 'IP 주소',
    user_agent VARCHAR(500) COMMENT 'User-Agent',

    -- 생성 시간
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '감사 로그 기록 시간',

    -- 인덱스
    INDEX idx_target_type_id (target_type, target_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='콘텐츠 삭제 감사 로그';

-- 4. 기존 데이터 마이그레이션 (필요시)
-- 이미 삭제된 데이터가 있다면 기본값 설정
UPDATE news
SET deleted_by = 'SYSTEM',
    delete_reason = '기존 삭제 데이터 (마이그레이션)',
    deleted_at = IFNULL(deleted_at, created_at)
WHERE is_deleted = TRUE AND deleted_by IS NULL;

UPDATE content_review
SET deleted_by = 'SYSTEM',
    delete_reason = '기존 삭제 데이터 (마이그레이션)'
WHERE is_deleted = TRUE AND deleted_by IS NULL;