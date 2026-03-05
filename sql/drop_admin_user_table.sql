-- ========================================
-- 사용하지 않는 테이블 삭제 마이그레이션
-- 작성일: 2026-01-07
-- 목적: 사용하지 않는 admin_user, newsletter 테이블 제거
-- DB: MariaDB
-- ========================================

-- 1. 삭제 대상 테이블이 존재하는지 확인
-- SELECT TABLE_NAME
-- FROM INFORMATION_SCHEMA.TABLES
-- WHERE TABLE_SCHEMA = 'finance_db'
--   AND TABLE_NAME IN ('admin_user', 'newsletter');

-- 2. 사용하지 않는 테이블 삭제
DROP TABLE IF EXISTS admin_user;
DROP TABLE IF EXISTS newsletter;

-- ========================================
-- 실행 확인
-- ========================================
-- 테이블이 정상적으로 삭제되었는지 확인
-- SELECT TABLE_NAME
-- FROM INFORMATION_SCHEMA.TABLES
-- WHERE TABLE_SCHEMA = 'finance_db'
--   AND TABLE_NAME IN ('admin_user', 'newsletter');

-- ========================================
-- 참고: 삭제된 테이블 정보
-- ========================================
-- 1. admin_user: InsightsAdminUser 엔티티의 테이블 (사용 안 함)
-- 2. newsletter: 관련 엔티티 없음 (사용 안 함)
