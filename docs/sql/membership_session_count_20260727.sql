-- ============================================================================
-- 운영(prod) 적용용: membership 테이블에 session_count 컬럼 추가 (PT 세션 수 기록)
-- 작성일: 2026-07-27
-- 배경: PT 상품(패키지든 개별 등록이든)을 등록하면 이제 user_profiles.pt_sessions_left에
--       실제 세션 수를 충전한다 (이전에는 등록해도 앱에서 쓸 수 있는 PT 횟수가 늘지 않는
--       버그가 있었음). 나중에 해당 PT membership 행을 회수(삭제)할 때 충전했던 세션을
--       되돌리려면 "이 행이 몇 회를 충전했는지"가 남아있어야 해서 session_count를 추가한다.
--       PT가 아닌 행(이용권/락커/운동복)은 이 컬럼을 쓰지 않는다(NULL).
-- ============================================================================

SELECT COUNT(*) AS membership_total FROM membership;

ALTER TABLE membership
    ADD COLUMN IF NOT EXISTS session_count INT NULL AFTER reg_type;

SELECT COUNT(*) AS total, SUM(session_count IS NOT NULL) AS with_session_count FROM membership;

-- 롤백 (필요 시):
-- ALTER TABLE membership DROP COLUMN session_count;
