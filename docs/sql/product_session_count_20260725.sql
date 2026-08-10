-- ============================================================================
-- 운영(prod) 적용용: 상품(product) 테이블에 횟수(session_count) 컬럼 추가
-- 작성일: 2026-07-25
-- 배경: "이용권/PT 관리" 페이지를 "상품 등록 관리"로 개편하면서, 회원권/PT/락커/
--       운동복 등록 시 기간(duration_days)뿐 아니라 횟수(PT 세션 수 등)도
--       입력할 수 있도록 컬럼 추가.
-- ============================================================================

SELECT COUNT(*) AS product_total FROM product;

ALTER TABLE product ADD COLUMN session_count INT NULL AFTER duration_days;

SELECT COUNT(*) AS total, SUM(session_count IS NOT NULL) AS has_session_count FROM product;

-- 롤백 (필요 시):
-- ALTER TABLE product DROP COLUMN session_count;
