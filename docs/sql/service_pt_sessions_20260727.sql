-- ============================================================================
-- 운영(prod) 적용용: 서비스 PT(무료 특별지급) 세션 컬럼 신설
-- 작성일: 2026-07-27
-- 배경: PT와 피드백 티켓은 서로 다른 상품 개념이라(피드백은 매달 자동 지급 + 구독권별
--       차등이 있는 반면, 서비스 PT는 CRM에서 관리자가 그때그때 특별 지급하는 것) 기존
--       member_tickets/ticket_logs 체계를 재사용하지 않고 user_profiles에 컬럼을 하나
--       더 두는 방식으로 간다. 기존 pt_sessions_left(구매분)는 그대로 두고,
--       service_pt_sessions_left(서비스/무료 지급분)를 신설한다.
--       실제 PT 소진(POST /api/pt/consume) 시 서비스분을 먼저 차감하도록 lof-backend
--       쪽 로직도 함께 수정한다 (선차감).
--
--       이 작업을 계기로 "PT 관리"(/pt) 페이지가 실제로는 원포인트(ONE_POINT) 티켓을
--       관리하고 있던 오랜 버그도 같이 바로잡는다 — 원포인트는 이미 /feedback 페이지에서
--       완전히 독립적으로 관리되고 있어(MemberApiController의 /tickets/charge 경로) 겹치는
--       기능이 없으므로, /pt 페이지를 진짜 PT 세션(pt_sessions_left + service_pt_sessions_left)
--       기준으로 새로 구성해도 기존 기능 손실이 없다.
-- ============================================================================

SELECT COUNT(*) AS user_profiles_total FROM user_profiles;

ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS service_pt_sessions_left INT NOT NULL DEFAULT 0 AFTER pt_sessions_left;

SELECT COUNT(*) AS total,
       SUM(pt_sessions_left > 0) AS has_purchased_pt,
       SUM(service_pt_sessions_left > 0) AS has_service_pt
FROM user_profiles;

-- 롤백 (필요 시):
-- ALTER TABLE user_profiles DROP COLUMN service_pt_sessions_left;
