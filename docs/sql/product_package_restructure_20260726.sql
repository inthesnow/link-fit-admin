-- ============================================================================
-- 운영(prod) 적용용: 상품 등록(패키지) 기능 — 카테고리 인라인 구성 방식으로 정리
-- 작성일: 2026-07-26 (2차 수정 — 이용권/락커/운동복 개월+일 입력, PT 기간 제거)
-- 배경: 이전에 "기존 상품을 참조해 패키지 구성" 방식의 초안 SQL이 이미 운영에
--       적용되어 product_package 테이블(참조 방식 컬럼)이 존재하는 상태.
--       최종 방식은 "상품 하나에 이용권/PT/락커/운동복 구성을 바로 지정"하는
--       것으로 확정. 이용권/락커/운동복은 기간을 개월+일 두 값으로 입력받고,
--       PT는 기간 없이 횟수만 관리한다(등록 시 만료일 없이 무기한 처리).
--       기존 테이블은 그대로 두고 필요한 컬럼만 추가/삭제하도록 멱등성 있게
--       (이미 있으면 건너뛰는 방식) 작성함.
--       product_package_item(참조용 중간 테이블)은 더 이상 쓰지 않아 삭제.
--
-- ⚠️ product_package_item / product_package에 이미 등록된 데이터가 있다면
--    변경 전 반드시 백업할 것.
-- ============================================================================

-- 사전 점검
SELECT COUNT(*) AS package_total FROM product_package;

-- 참조 방식용 중간 테이블 삭제 (더 이상 사용 안 함) — 존재하지 않아도 에러 없이 넘어감
DROP TABLE IF EXISTS product_package_item;

-- 카테고리 인라인 구성 컬럼 추가 (이미 있으면 건너뜀)
ALTER TABLE product_package ADD COLUMN IF NOT EXISTS membership_enabled TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE product_package ADD COLUMN IF NOT EXISTS membership_duration_months INT NULL;
ALTER TABLE product_package ADD COLUMN IF NOT EXISTS membership_duration_days INT NULL;
ALTER TABLE product_package ADD COLUMN IF NOT EXISTS pt_enabled TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE product_package ADD COLUMN IF NOT EXISTS pt_session_count INT NULL;
ALTER TABLE product_package ADD COLUMN IF NOT EXISTS locker_enabled TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE product_package ADD COLUMN IF NOT EXISTS locker_duration_months INT NULL;
ALTER TABLE product_package ADD COLUMN IF NOT EXISTS locker_duration_days INT NULL;
ALTER TABLE product_package ADD COLUMN IF NOT EXISTS uniform_enabled TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE product_package ADD COLUMN IF NOT EXISTS uniform_duration_months INT NULL;
ALTER TABLE product_package ADD COLUMN IF NOT EXISTS uniform_duration_days INT NULL;

-- 이전 초안(참조 방식과 함께 만들어졌던 PT 기간 컬럼)은 더 이상 쓰지 않아 삭제 — 없어도 에러 없이 넘어감
ALTER TABLE product_package DROP COLUMN IF EXISTS pt_duration_days;

-- membership.package_id (없을 때만 추가)
ALTER TABLE membership ADD COLUMN IF NOT EXISTS package_id BIGINT UNSIGNED NULL AFTER product_id;

-- 적용 후 검증
SHOW COLUMNS FROM product_package;
SELECT COUNT(*) AS membership_total, SUM(package_id IS NOT NULL) AS package_linked_count FROM membership;

-- 롤백 (필요 시):
-- ALTER TABLE membership DROP COLUMN package_id;
-- ALTER TABLE product_package
--   DROP COLUMN membership_enabled, DROP COLUMN membership_duration_months, DROP COLUMN membership_duration_days,
--   DROP COLUMN pt_enabled, DROP COLUMN pt_session_count,
--   DROP COLUMN locker_enabled, DROP COLUMN locker_duration_months, DROP COLUMN locker_duration_days,
--   DROP COLUMN uniform_enabled, DROP COLUMN uniform_duration_months, DROP COLUMN uniform_duration_days;
