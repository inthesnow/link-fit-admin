-- ============================================================================
-- 운영(prod) 적용용: sale 테이블에 환불(refund) 처리용 컬럼 추가
-- 작성일: 2026-07-26
-- 배경: 매출관리 > 결제 내역의 "환불" 버튼이 기존에는 sale 행을 완전히 삭제(DELETE)하는
--       방식이라 환불 이력이 전혀 남지 않았음. 이를 개선해 환불 시 행을 지우지 않고
--       refund_amount/refunded_at/refund_reason에 기록만 남기도록 변경 (전액/부분 환불 지원).
--       매출 통계(revenueStats/revenueDetail/monthlyTrend 등)는 (amount - refund_amount)로 집계.
--       회원관리의 "회수"(이용권 삭제, membership 테이블)와는 완전히 별개의 기능이며 서로 영향을 주지 않음.
-- ============================================================================

SELECT COUNT(*) AS sale_total FROM sale;

ALTER TABLE sale
    ADD COLUMN IF NOT EXISTS refund_amount INT NOT NULL DEFAULT 0 AFTER amount,
    ADD COLUMN IF NOT EXISTS refunded_at DATETIME NULL AFTER memo,
    ADD COLUMN IF NOT EXISTS refund_reason VARCHAR(255) NULL AFTER refunded_at;

SELECT COUNT(*) AS total, SUM(refunded_at IS NOT NULL) AS refunded_count FROM sale;

-- 롤백 (필요 시):
-- ALTER TABLE sale DROP COLUMN refund_amount, DROP COLUMN refunded_at, DROP COLUMN refund_reason;
