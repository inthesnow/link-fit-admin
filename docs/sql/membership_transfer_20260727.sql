-- ============================================================================
-- 운영(prod) 적용용: 회원 간 상품(이용권/PT/락커/운동복) 양도 기능 - status 컬럼 추가
-- 작성일: 2026-07-27
-- 배경: 이용권/락커/운동복을 다른 회원에게 양도할 때, 원 회원의 membership 행을
--       삭제하지 않고 이력을 보존하기 위해 status='TRANSFERRED' 로만 표시한다.
--       (PT는 회원 전체 잔여 풀 단위로 이전하므로 이 컬럼과 무관 — user_profiles의
--       pt_sessions_left/service_pt_sessions_left를 직접 옮긴다.)
--       status가 있으면 "사용 가능한 상품" 관련 조회(만료예정, 이용중인 상품 등)에서
--       제외해야 하므로 MemberMapper.xml 여러 쿼리도 함께 수정한다(코드 배포 필요).
-- ============================================================================

SELECT COUNT(*) AS membership_total FROM membership;

ALTER TABLE membership
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NULL AFTER session_count;

SELECT COUNT(*) AS total, SUM(status IS NOT NULL) AS with_status FROM membership;

-- 롤백 (필요 시):
-- ALTER TABLE membership DROP COLUMN status;
