-- ============================================================================
-- 운영(prod) 적용용: 이용권/PT 등록 시 신규/재유입/재등록/단품결제 자동 분류
-- 작성일: 2026-07-27
-- 배경: 회원관리에서 이용권을 등록할 때, 해당 회원의 과거 이용권/PT 이력을 기준으로
--       아래 규칙에 따라 자동으로 등록 구분을 매겨 membership.reg_type에 저장하고,
--       매출 관리(crm_sales.reg_type)에도 그대로 반영한다.
--         - 신규(new): 이 회원의 최초 이용권/PT 구매
--         - 재유입(re_inflow): 이용권/PT 만료일로부터 1개월을 초과해서 재결제
--         - 재등록(re): 만료 전, 또는 만료 후 1개월 이내에 재결제
--         - 단품결제(single_item): 이용권/PT 없이 락커/운동복만 구매한 경우 (위 3분류와 무관)
--       기존 crm_sales.reg_type은 new/re/referral만 있었는데(referral은 매출관리 화면에서
--       수동 등록 시에만 쓰는 값, 이번 자동분류와 무관하게 계속 사용), re_inflow/single_item
--       두 값을 추가한다.
-- ============================================================================

-- 1. membership.reg_type 컬럼 추가
SELECT COUNT(*) AS membership_total FROM membership;

ALTER TABLE membership
    ADD COLUMN IF NOT EXISTS reg_type VARCHAR(20) NULL AFTER payment_method;

-- 2. crm_sales.reg_type enum 확장 (기존 값 new/re/referral은 그대로 유지)
ALTER TABLE crm_sales
    MODIFY COLUMN reg_type ENUM('new','re','re_inflow','single_item','referral') NULL;

SELECT COUNT(*) AS total, SUM(reg_type IS NULL) AS no_reg_type_count FROM membership;
SHOW COLUMNS FROM crm_sales LIKE 'reg_type';

-- 롤백 (필요 시):
-- ALTER TABLE membership DROP COLUMN reg_type;
-- ALTER TABLE crm_sales MODIFY COLUMN reg_type ENUM('new','re','referral') NULL;
