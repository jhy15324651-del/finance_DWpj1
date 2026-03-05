-- ========================================
-- Info 게시글 삭제 사유 컬럼 추가 마이그레이션
-- 작성일: 2025-12-29
-- 목적: News/ContentReview 패턴과 동일하게 감사 로그 기록을 위한 delete_reason 컬럼 추가
-- DB: MariaDB
-- ========================================

-- 1. info_posts 테이블에 delete_reason 컬럼 추가
ALTER TABLE info_posts
ADD COLUMN delete_reason TEXT COMMENT '삭제 사유 (관리자 입력)';

-- 2. 확인: 컬럼이 추가되었는지 확인 (MariaDB)
-- SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_COMMENT
-- FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_SCHEMA = 'finance_db'
--   AND TABLE_NAME = 'info_posts'
--   AND COLUMN_NAME = 'delete_reason';

-- ========================================
-- 롤백 스크립트 (필요시 사용)
-- ========================================
-- ALTER TABLE info_posts DROP COLUMN delete_reason;