-- ============================================================================
-- 운영(prod) 적용용: membership 테이블에 할인금액(discount_amount) 컬럼 추가
-- 작성일: 2026-07-26
-- 배경: 회원상세 > 이용권 등록에서 상품 금액을 할인해서 판매할 수 있도록
--       할인금액 입력란을 추가. 미납금 계산도 (price - discount_amount) - paid_amount로 변경.
--       기존 행은 할인 없음(0)으로 백필.
-- ============================================================================

SELECT COUNT(*) AS membership_total FROM membership;

ALTER TABLE membership ADD COLUMN discount_amount INT NOT NULL DEFAULT 0 AFTER price;

SELECT COUNT(*) AS total, SUM(discount_amount = 0) AS no_discount_count FROM membership;

-- 롤백 (필요 시):
-- ALTER TABLE membership DROP COLUMN discount_amount;
