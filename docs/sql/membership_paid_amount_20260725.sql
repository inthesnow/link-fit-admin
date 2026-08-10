-- ============================================================================
-- 운영(prod) 적용용: membership 테이블에 납부액(paid_amount) 컬럼 추가
-- 작성일: 2026-07-25
-- 배경: 회원관리 목록에 "이용중인상품(가격)"/"미납금" 컬럼을 추가하면서,
--       미납금 = price - paid_amount 계산을 위해 납부액 컬럼이 필요해짐.
--       기존 행은 실제 분납 이력을 알 수 없으므로 전액 납부로 백필(미납 0 처리).
-- ============================================================================

SELECT COUNT(*) AS membership_total FROM membership;

ALTER TABLE membership ADD COLUMN paid_amount INT NOT NULL DEFAULT 0 AFTER price;
UPDATE membership SET paid_amount = price;

SELECT COUNT(*) AS total,
       SUM(paid_amount = price) AS fully_paid_count
FROM membership;

-- 롤백 (필요 시):
-- ALTER TABLE membership DROP COLUMN paid_amount;
