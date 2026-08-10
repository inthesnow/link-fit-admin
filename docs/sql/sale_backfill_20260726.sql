-- ============================================================================
-- 운영(prod) 적용용: 매출 연동 배포 이전에 등록된 상품/이용권을 sale 테이블로 소급 반영
-- 작성일: 2026-07-26
-- 배경: 회원관리에서 상품 등록 시 sale 테이블에도 자동으로 기록되도록 하는 기능이
--       배포되기 전에 이미 등록된 membership 행들은 sale 테이블에 대응 행이 없어서
--       매출관리 페이지에 전혀 나타나지 않는다 (예: 킬킬 회원 - 50만원 상품, 10만원
--       할인, 30만원 납부 건, membership.id=9). 이 스크립트는 금액을 들고 있는
--       (price/discount_amount/paid_amount 중 하나라도 0보다 큰) 대표(anchor) 행들을
--       찾아 아직 sale에 없는 것만 골라 1회성으로 채워 넣는다.
--       이후 신규 등록 건은 애플리케이션이 자동으로 sale에 기록하므로 이 스크립트는
--       1회만 실행하면 된다 (재실행해도 NOT EXISTS 조건으로 중복 삽입되지는 않음).
-- ============================================================================

-- 1. 사전 확인: 백필 대상 미리보기
SELECT m.id AS membershipId, m.user_id, m.type, m.price, m.discount_amount, m.paid_amount,
       m.payment_method, m.start_date, m.memo,
       COALESCE(pkg.name, p.name) AS productName
FROM membership m
LEFT JOIN product_package pkg ON pkg.id = m.package_id
LEFT JOIN product p ON p.id = m.product_id
WHERE (m.price > 0 OR m.discount_amount > 0 OR m.paid_amount > 0)
  AND NOT EXISTS (
      SELECT 1 FROM sale s
      WHERE s.user_id = m.user_id
        AND s.sale_date = m.start_date
        AND s.amount = m.paid_amount
        AND s.product_type = m.type
  );

-- 2. 백필 실행
INSERT INTO sale (user_id, product_id, product_name, product_type, amount, payment_method, sale_date, memo)
SELECT m.user_id, NULL, COALESCE(pkg.name, p.name), m.type, m.paid_amount,
       COALESCE(m.payment_method, '미지정'), m.start_date, m.memo
FROM membership m
LEFT JOIN product_package pkg ON pkg.id = m.package_id
LEFT JOIN product p ON p.id = m.product_id
WHERE (m.price > 0 OR m.discount_amount > 0 OR m.paid_amount > 0)
  AND NOT EXISTS (
      SELECT 1 FROM sale s
      WHERE s.user_id = m.user_id
        AND s.sale_date = m.start_date
        AND s.amount = m.paid_amount
        AND s.product_type = m.type
  );

-- 3. 사후 확인 (백필된 행 확인)
SELECT s.id, s.user_id, up.name AS memberName, s.product_name, s.product_type,
       s.amount, s.payment_method, s.sale_date
FROM sale s
LEFT JOIN user_profiles up ON up.user_id = s.user_id
ORDER BY s.id DESC;

-- 롤백 (필요 시 — 이 스크립트로 새로 들어간 행만 지우려면 created_at 기준으로 제한할 것):
-- DELETE FROM sale WHERE created_at >= '2026-07-26 00:00:00' AND refund_amount = 0 AND refunded_at IS NULL;
