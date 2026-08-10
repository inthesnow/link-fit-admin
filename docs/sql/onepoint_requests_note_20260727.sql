-- ============================================================================
-- 운영(prod) 적용용: 원포인트 신청(onepoint_requests) 처리 메모 컬럼 신설
-- 작성일: 2026-07-27
-- 배경: 트레이너 관리 > 원포인트 신청 탭에서 신청 건을 승인/거절 처리할 때
--       담당자가 남기는 처리 메모 컬럼. 회원이 신청 시 남기는 요청 메모인
--       기존 notes(복수형) 컬럼과는 별개 — notes는 회원이 신청 시 입력, note는
--       트레이너/관리자가 상태 변경(updateStatus) 시 입력하는 값이다.
--       로컬 DB에는 이미 반영돼 있었지만 운영에는 마이그레이션 파일이 없어
--       "Unknown column 'op.note'" 500 에러가 발생했다.
-- ============================================================================

SELECT COUNT(*) AS onepoint_requests_total FROM onepoint_requests;

ALTER TABLE onepoint_requests
    ADD COLUMN IF NOT EXISTS note TEXT NULL AFTER selected_time;

SELECT COUNT(*) AS total,
       SUM(note IS NOT NULL) AS has_note
FROM onepoint_requests;

-- 롤백 (필요 시):
-- ALTER TABLE onepoint_requests DROP COLUMN note;
