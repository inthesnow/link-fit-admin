-- ============================================================================
-- 운영(prod) 적용용: 실제 "매출 관리"(crm_sales) 화면에 과거 상품 등록 건을 소급 반영
-- 작성일: 2026-07-26
-- 배경: 사이드바의 "매출 관리" 메뉴는 /crm-sales 페이지이며 crm_sales 테이블을 사용한다.
--       (sale 테이블을 쓰는 /revenue 페이지는 사이드바에서 "통계 / 리포트"로 연결되어 있어
--       서로 다른 화면/테이블임 — sale_backfill_20260726.sql과는 별개의 작업이다.)
--       회원관리에서 상품 등록 시 crm_sales에도 자동 기록되는 기능이 배포되기 전에
--       이미 등록된 membership 행들은 crm_sales에 대응 행이 없어 "매출 관리" 화면에
--       나타나지 않는다 (예: 킬킬 회원 - 50만원 상품, 10만원 할인, 30만원 납부 건).
--       이 스크립트는 금액을 들고 있는(price/discount_amount/paid_amount 중 하나라도
--       0보다 큰) 대표(anchor) 행들을 찾아 아직 crm_sales에 없는 것만 1회성으로 채운다.
--       이후 신규 등록 건은 애플리케이션이 자동으로 crm_sales에도 기록하므로 이 스크립트는
--       1회만 실행하면 된다 (재실행해도 NOT EXISTS 조건으로 중복 삽입되지는 않음).
--
-- 참고: crm_sales.sales_type은 'membership'/'pt'/'feedback_ticket'만 허용하므로,
--       membership.type이 PT인 것만 'pt'로, 나머지(이용권/락커/운동복)는 전부
--       'membership'으로 매핑한다. reg_type(신규/재등록/추천)은 구분할 근거가 없어
--       전부 'new'(신규)로 채운다. gym_id는 현재 지점이 1개뿐이라 gym 테이블의
--       첫 행을 사용한다.
-- ============================================================================

-- 1. 사전 확인: 백필 대상 미리보기
SELECT m.id AS membershipId, m.user_id, m.type, m.paid_amount,
       CASE WHEN m.type = 'PT' THEN 'pt' ELSE 'membership' END AS salesType,
       m.start_date,
       COALESCE(pkg.name, p.name) AS productName, m.memo
FROM membership m
LEFT JOIN product_package pkg ON pkg.id = m.package_id
LEFT JOIN product p ON p.id = m.product_id
WHERE (m.price > 0 OR m.discount_amount > 0 OR m.paid_amount > 0)
  AND NOT EXISTS (
      SELECT 1 FROM crm_sales s
      WHERE s.member_id = m.user_id
        AND s.sale_date = m.start_date
        AND s.amount = m.paid_amount
        AND s.sales_type = (CASE WHEN m.type = 'PT' THEN 'pt' ELSE 'membership' END)
  );

-- 2. 백필 실행
INSERT INTO crm_sales (id, gym_id, member_id, sales_type, reg_type, trainer_id, amount, sale_date, note, created_at)
SELECT UUID(),
       (SELECT id FROM gym ORDER BY id LIMIT 1),
       m.user_id,
       CASE WHEN m.type = 'PT' THEN 'pt' ELSE 'membership' END,
       'new',
       NULL,
       m.paid_amount,
       m.start_date,
       TRIM(BOTH ' · ' FROM CONCAT(COALESCE(pkg.name, p.name, ''), IF(m.memo IS NOT NULL AND m.memo <> '', CONCAT(' · ', m.memo), ''))),
       m.created_at
FROM membership m
LEFT JOIN product_package pkg ON pkg.id = m.package_id
LEFT JOIN product p ON p.id = m.product_id
WHERE (m.price > 0 OR m.discount_amount > 0 OR m.paid_amount > 0)
  AND NOT EXISTS (
      SELECT 1 FROM crm_sales s
      WHERE s.member_id = m.user_id
        AND s.sale_date = m.start_date
        AND s.amount = m.paid_amount
        AND s.sales_type = (CASE WHEN m.type = 'PT' THEN 'pt' ELSE 'membership' END)
  );

-- 3. 사후 확인 (백필된 행 확인)
SELECT s.id, s.member_id, up.name AS memberName, s.sales_type, s.reg_type,
       s.amount, s.sale_date, s.note
FROM crm_sales s
LEFT JOIN user_profiles up ON up.user_id = s.member_id
ORDER BY s.created_at DESC;

-- 롤백 (필요 시 — 이 스크립트로 새로 들어간 행만 지우려면 created_at 기준으로 제한할 것):
-- DELETE FROM crm_sales WHERE reg_type = 'new' AND created_at >= '2026-07-26 00:00:00' AND trainer_id IS NULL;
