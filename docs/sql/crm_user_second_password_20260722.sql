-- ============================================================================
-- 운영(prod) 적용용: 관리자 2차 비밀번호 / 카테고리 잠금 기능
-- 작성일: 2026-07-22
-- 배경: 관리자 계정 개별로 2차 비밀번호를 설정해, 매출관리 등 특정 사이드바
--       카테고리 진입 시 재확인을 요구하는 기능. 신규 테이블 없이 기존
--       crm_users 테이블에 컬럼만 추가.
-- ============================================================================

-- ── 사전 점검 ──
SELECT COUNT(*) AS crm_users_total FROM crm_users;

-- ── 컬럼 추가 ──
ALTER TABLE crm_users
  ADD COLUMN second_password_hash VARCHAR(255) NULL AFTER password_hash,
  ADD COLUMN locked_categories TEXT NULL AFTER role;

-- ── 적용 후 검증 (기대값: 두 컬럼 모두 전 계정 NULL로 시작) ──
SELECT COUNT(*) AS total,
       SUM(second_password_hash IS NOT NULL) AS has_second_password,
       SUM(locked_categories IS NOT NULL) AS has_locked_categories
FROM crm_users;

-- 롤백 (필요 시):
-- ALTER TABLE crm_users DROP COLUMN second_password_hash, DROP COLUMN locked_categories;
