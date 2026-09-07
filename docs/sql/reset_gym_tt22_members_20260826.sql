-- ============================================================
-- TT22 지점 회원정보 초기화 (앱 실사용자는 보존)
-- ============================================================
-- 대상: gym.branch_code = 'TT22' 소속 user_gym 중 role='MEMBER'이면서
--       user_auth에 로그인 정보가 없는(=한 번도 앱에 실가입한 적 없는, 대개 엑셀로
--       이관해온 CRM 전용 placeholder) 회원만 삭제한다.
--       - 실제 앱 사용자(user_auth 존재)는 계정/이용권/메모 등 전부 그대로 둔다.
--       - 트레이너/관리자(role != MEMBER)는 애초에 대상이 아니다("회원정보"만 초기화).
--       - 삭제 대상이 TT22 말고 다른 지점에도 소속돼 있다면(user_gym이 여러 개면) 계정
--         자체는 지우지 않고 TT22 관련 데이터만 지운다 — 다른 지점 소속까지 깨지 않기 위함.
--
-- 2026-08-26 1차 실행 중 crm_reregistration_notes 테이블이 운영 DB에 없어서(로컬에만
-- 적용된 최근 마이그레이션) 에러 발생 — 트랜잭션이라 자동 롤백되어 실제 삭제된 데이터는
-- 없었음. 이 버전은 각 테이블 존재 여부를 먼저 확인하고 없으면 건너뛰도록 방어적으로 수정함
-- (운영 DB가 로컬보다 스키마가 뒤처져 있는 경우가 있어 — 이 프로젝트에서 반복적으로 있었던
-- 드리프트 이슈).
--
-- 실행 전 반드시:
--   1) "영향 범위 확인" SELECT로 대상 인원수·목록을 먼저 확인
--   2) 가능하면 백업 이후 실행
--   3) COMMIT 전에 "실행 후 확인" 쿼리로 결과를 검증하고, 이상하면 ROLLBACK
-- ============================================================

-- ── 0. 영향 범위 확인 (실행 전 반드시 확인) ──────────────────────
SELECT u.user_id, up.name, up.contact,
       (SELECT COUNT(*) FROM user_gym ug2 WHERE ug2.user_id = u.user_id) AS gym_count
  FROM user_gym ug
  JOIN users u ON u.user_id = ug.user_id
  LEFT JOIN user_profiles up ON up.user_id = u.user_id
 WHERE ug.gym_id = (SELECT id FROM gym WHERE branch_code = 'TT22')
   AND u.role = 'MEMBER'
   AND NOT EXISTS (SELECT 1 FROM user_auth ua WHERE ua.user_id = u.user_id);

-- ============================================================
START TRANSACTION;

SET @target_gym_id = (SELECT id FROM gym WHERE branch_code = 'TT22');

-- 삭제 대상 회원 목록 임시 저장 (계정 자체를 지울지=full, TT22 데이터만 지울지=partial 구분)
DROP TEMPORARY TABLE IF EXISTS tmp_reset_targets;
CREATE TEMPORARY TABLE tmp_reset_targets AS
SELECT u.user_id,
       ((SELECT COUNT(*) FROM user_gym ug2 WHERE ug2.user_id = u.user_id) = 1) AS full_delete
  FROM user_gym ug
  JOIN users u ON u.user_id = ug.user_id
 WHERE ug.gym_id = @target_gym_id
   AND u.role = 'MEMBER'
   AND NOT EXISTS (SELECT 1 FROM user_auth ua WHERE ua.user_id = u.user_id);

-- 존재하지 않는 테이블은 건너뛰도록 하는 헬퍼: @sql을 실행 전 항상 이 매크로로 감싼다.
-- (운영 DB에 아직 안 올라간 최근 마이그레이션이 있을 수 있어 방어적으로 처리)

-- ── 1. TT22 소속 데이터 삭제 (FK 캐스케이드가 없는 테이블들 — 대상 전원 공통) ──

-- 재등록 메모 → 재등록 레코드 (crm_reregistration_notes는 2026-08-25 신규 테이블 — 없을 수 있음)
SET @tbl := (SELECT COUNT(*) FROM information_schema.tables
              WHERE table_schema = DATABASE() AND table_name = 'crm_reregistration_notes');
SET @sql := IF(@tbl > 0,
  'DELETE crn FROM crm_reregistration_notes crn
     JOIN crm_re_registration r ON r.id = crn.reregistration_id
    WHERE r.gym_id = @target_gym_id
      AND r.member_id IN (SELECT user_id FROM tmp_reset_targets)',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @tbl := (SELECT COUNT(*) FROM information_schema.tables
              WHERE table_schema = DATABASE() AND table_name = 'crm_re_registration');
SET @sql := IF(@tbl > 0,
  'DELETE FROM crm_re_registration
    WHERE gym_id = @target_gym_id
      AND member_id IN (SELECT user_id FROM tmp_reset_targets)',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @tbl := (SELECT COUNT(*) FROM information_schema.tables
              WHERE table_schema = DATABASE() AND table_name = 'crm_member_notes');
SET @sql := IF(@tbl > 0,
  'DELETE FROM crm_member_notes
    WHERE gym_id = @target_gym_id
      AND member_id IN (SELECT user_id FROM tmp_reset_targets)',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @tbl := (SELECT COUNT(*) FROM information_schema.tables
              WHERE table_schema = DATABASE() AND table_name = 'crm_member_tags');
SET @sql := IF(@tbl > 0,
  'DELETE FROM crm_member_tags
    WHERE gym_id = @target_gym_id
      AND member_id IN (SELECT user_id FROM tmp_reset_targets)',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 회원<->헬스장 쪽지(양방향, 2026-08-26 신규 기능 — 없을 수 있음)
SET @tbl := (SELECT COUNT(*) FROM information_schema.tables
              WHERE table_schema = DATABASE() AND table_name = 'crm_messages');
SET @sql := IF(@tbl > 0,
  'DELETE FROM crm_messages
    WHERE gym_id = @target_gym_id
      AND (sender_id IN (SELECT user_id FROM tmp_reset_targets)
        OR receiver_id IN (SELECT user_id FROM tmp_reset_targets))',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 이용권/PT/락커/운동복 (membership.locker_id는 ON DELETE SET NULL이라 락커 자체는 안 지워짐 —
-- 락커는 membership 행이 없어지는 순간 자동으로 "빈 락커" 취급됨, 별도 처리 불필요)
DELETE FROM membership
 WHERE gym_id = @target_gym_id
   AND user_id IN (SELECT user_id FROM tmp_reset_targets);

-- 티켓 잔량(gym_id 있음 — 지점별로 관리됨)
DELETE FROM member_tickets
 WHERE gym_id = @target_gym_id
   AND user_id IN (SELECT user_id FROM tmp_reset_targets);

-- 출석 / 유증(정지)
DELETE FROM attendance
 WHERE gym_id = @target_gym_id
   AND user_id IN (SELECT user_id FROM tmp_reset_targets);

DELETE FROM member_freeze
 WHERE gym_id = @target_gym_id
   AND user_id IN (SELECT user_id FROM tmp_reset_targets);

-- 지점 소속 링크
DELETE FROM user_gym
 WHERE gym_id = @target_gym_id
   AND user_id IN (SELECT user_id FROM tmp_reset_targets);

-- ── 2. 계정 자체 삭제 (TT22가 유일한 소속이었던 회원만 — full_delete=1) ──
-- 티켓 사용내역은 gym_id 컬럼이 없어(지점 무관 이력) 계정을 통째로 지우는 경우에만 같이 삭제.
-- 다른 지점에도 소속된 회원(full_delete=0)은 그 지점 이력이 섞여 있을 수 있어 건드리지 않음.
DELETE FROM ticket_logs
 WHERE user_id IN (SELECT user_id FROM tmp_reset_targets WHERE full_delete = 1);

-- users 삭제 시 CASCADE로 자동 정리됨: user_profiles, membership(위에서 이미 지웠지만 다른 지점
-- 몫이 남아있었다면 그것까지 전부), member_freeze, attendance, message_conversation/chat_message,
-- user_auth(대상은 애초에 없음) 등. sale/crm_sales는 ON DELETE SET NULL이라 매출 이력은 보존됨
-- (회원정보 초기화 요청 범위가 매출 삭제는 아니라고 판단해 의도적으로 안 건드림).
DELETE FROM users
 WHERE user_id IN (SELECT user_id FROM tmp_reset_targets WHERE full_delete = 1);

DROP TEMPORARY TABLE IF EXISTS tmp_reset_targets;

-- ── 실행 후 확인 ──────────────────────────────────────────────
SELECT COUNT(*) AS remaining_non_app_members
  FROM user_gym ug
  JOIN users u ON u.user_id = ug.user_id
 WHERE ug.gym_id = @target_gym_id
   AND u.role = 'MEMBER'
   AND NOT EXISTS (SELECT 1 FROM user_auth ua WHERE ua.user_id = u.user_id);
-- 위 값이 0이어야 정상 (다른 지점 겸용 회원은 user_gym에서 TT22만 빠지고 계정은 남아있을 수 있음 —
-- 그 경우 이 COUNT엔 안 잡힘. user_gym.gym_id=TT22 조건 자체가 이미 걸려있기 때문)

SELECT COUNT(*) AS remaining_app_members
  FROM user_gym ug
  JOIN users u ON u.user_id = ug.user_id
 WHERE ug.gym_id = @target_gym_id
   AND u.role = 'MEMBER'
   AND EXISTS (SELECT 1 FROM user_auth ua WHERE ua.user_id = u.user_id);
-- 위 값은 초기화 전과 동일해야 정상 (앱 실사용 회원은 그대로 보존)

-- mysql < file 로 파일 전체를 한 번에 실행하는 경우, 이 안에 COMMIT이 없으면 파일 실행이
-- 끝나고 연결이 종료되는 순간 서버가 커밋 안 된 트랜잭션을 자동으로 롤백해버린다(1차 실행 때
-- 실제로 이렇게 됐음 — 삭제는 됐지만 연결 종료로 전부 원상복구됨). 그래서 이번엔 바로 커밋되게
-- 활성화해둠 — 위 "실행 후 확인" 결과가 이상하면 이 파일을 실행하기 전에 COMMIT을 ROLLBACK으로
-- 바꿔서 다시 실행할 것.
COMMIT;
-- 문제가 있으면 위 COMMIT을 지우고 대신:
-- ROLLBACK;
