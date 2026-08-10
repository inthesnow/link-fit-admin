-- ============================================================================
-- 운영(prod) 적용용: 결제수단(payment_method) 테이블 신설 + membership.payment_method 컬럼 추가
-- 작성일: 2026-07-26
-- 배경: 회원상세 > 이용권 등록에서 결제수단(신용카드/계좌이체/현금 기본 제공)을 선택할 수 있게 하고,
--       관리자가 "기타결제수단추가" 버튼으로 새 결제수단을 직접 추가할 수 있도록 함.
--       결제수단은 상품 구성(이용권/PT/락커/운동복) 중 금액을 들고 있는 대표(anchor) 행에만 기록됨
--       (기존 price/discount_amount/paid_amount와 동일한 처리 방식).
-- ============================================================================

SELECT COUNT(*) AS product_package_total FROM product_package;
SELECT COUNT(*) AS membership_total FROM membership;

CREATE TABLE IF NOT EXISTS payment_method (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_method_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO payment_method (name, sort_order)
SELECT '신용카드', 1 WHERE NOT EXISTS (SELECT 1 FROM payment_method WHERE name = '신용카드');
INSERT INTO payment_method (name, sort_order)
SELECT '계좌이체', 2 WHERE NOT EXISTS (SELECT 1 FROM payment_method WHERE name = '계좌이체');
INSERT INTO payment_method (name, sort_order)
SELECT '현금', 3 WHERE NOT EXISTS (SELECT 1 FROM payment_method WHERE name = '현금');

ALTER TABLE membership ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50) NULL AFTER paid_amount;

SELECT * FROM payment_method ORDER BY sort_order ASC, id ASC;
SELECT COUNT(*) AS total, SUM(payment_method IS NULL) AS no_payment_method_count FROM membership;

-- 롤백 (필요 시):
-- ALTER TABLE membership DROP COLUMN payment_method;
-- DROP TABLE payment_method;
