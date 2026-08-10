-- ============================================================================
-- 운영(prod) 적용용: 고아 데이터 정리
-- 작성일: 2026-07-26
-- 배경: orphan_data_diagnosis_20260726.sql을 운영 DB에서 실행한 결과를 바탕으로 작성.
--       실제 참조 무결성이 깨진 행은 crm_feedback_tickets 1건뿐이었고, 그 외
--       FK 없는 컬럼들은 전부 0건으로 깨끗했다. 대신 과거 정리 작업(운동/티켓/후기/회원
--       관련 cleanup)에서 안전을 위해 만들어둔 백업 테이블 6개가 남아있었는데,
--       lof-admin/lof-backend 코드 어디에서도 참조되지 않는 것을 grep으로 확인했다.
--       "고아 데이터"로 체감된 것의 상당수는 행 단위 orphan보다 이 방치된 백업
--       테이블들일 가능성이 높다.
--
-- 구성:
--   [1] 실제 고아 행 정리 (crm_feedback_tickets) — 백업 후 삭제, 안전
--   [2] 미사용 백업 테이블 정리 — DROP은 되돌릴 수 없으므로 기본적으로 주석 처리해둠.
--       각 테이블이 더 이상 필요 없다고 확인되면 해당 줄의 주석만 해제해서 실행할 것.
-- ============================================================================


-- =============================================================================
-- [1] crm_feedback_tickets 고아 행 정리 (member_id가 존재하지 않는 회원을 가리킴)
-- =============================================================================

-- 1-1. 사전 확인
SELECT * FROM crm_feedback_tickets t
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = t.member_id);

-- 1-2. 백업 (삭제 전 스냅샷)
CREATE TABLE IF NOT EXISTS crm_feedback_tickets_orphan_backup_20260726 AS
SELECT * FROM crm_feedback_tickets t
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = t.member_id);

-- 1-3. 삭제
DELETE t FROM crm_feedback_tickets t
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = t.member_id);

-- 1-4. 사후 확인 (0건이어야 정상)
SELECT COUNT(*) AS remaining_orphan_count FROM crm_feedback_tickets t
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = t.member_id);

-- 롤백 (필요 시, 삭제 직후에만 유효):
-- INSERT INTO crm_feedback_tickets SELECT * FROM crm_feedback_tickets_orphan_backup_20260726;


-- =============================================================================
-- [2] 미사용 백업 테이블 정리 (검토 후 주석 해제하여 실행 — 기본은 비활성)
-- =============================================================================
-- 아래 테이블들은 과거 정리 작업의 스냅샷으로, 현재 코드에서 참조되지 않는다.
-- 원본 정리가 문제없이 완료되었다고 확인되면 DROP 후보다. 실행 전 각 테이블을
-- 한 번 더 SELECT로 열어보고, 필요하면 별도 위치(로컬 dump 등)에 옮겨둔 뒤 지울 것.

-- 2-1. exercises_backup_20260718 / exercises_backup_20260721 (각 496건, 내용 거의 동일한 중복으로 보임)
--      두 백업 시점이 3일 차이인데 행 수가 같다는 것은 그 사이 변경이 없었다는 뜻 —
--      실행 전 아래 쿼리로 두 테이블이 실제로 동일한지 확인 권장:
-- SELECT (SELECT COUNT(*) FROM exercises_backup_20260718) AS cnt_0718,
--        (SELECT COUNT(*) FROM exercises_backup_20260721) AS cnt_0721;
-- DROP TABLE exercises_backup_20260718;
-- DROP TABLE exercises_backup_20260721;

-- 2-2. member_tickets_backup_20260721 (77건)
-- DROP TABLE member_tickets_backup_20260721;

-- 2-3. trainer_reviews_backup_20260721 (1605건)
-- DROP TABLE trainer_reviews_backup_20260721;

-- 2-4. users_backup_20260721 (41건) — 회원 개인정보 스냅샷이므로 특히 신중히 검토 후 삭제
-- DROP TABLE users_backup_20260721;

-- 2-5. exercise_record_sets_backup (0건, 빈 테이블)
-- DROP TABLE exercise_record_sets_backup;


-- =============================================================================
-- [3] 레거시 미사용 메시지 테이블 (참고 — 현재 0건이라 급하지 않음)
-- =============================================================================
-- message/message_recipient는 message_conversation/chat_message로 대체된 레거시 기능.
-- 현재 운영 DB에 0건이라 지워도 데이터 손실은 없으나, 코드에서 완전히 손을 뗀 것을
-- 재확인한 뒤 진행 권장 (급하지 않으므로 기본 비활성).
-- DROP TABLE message_recipient;
-- DROP TABLE message;
