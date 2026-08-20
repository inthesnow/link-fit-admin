-- ============================================================================
-- 다지점(멀티 브랜치) 스키마 마이그레이션: gym_id 컬럼 추가 + 백필
-- 작성일: 2026-08-20
-- 배경: docs/multi-branch-expansion-issues.md(2026-07-21 작성)의 "정리 — 확장 전
--       우선순위 1번"이 아직 미적용 상태였다. 그런데 그 문서가 "지점을 추가하기
--       전에 반드시 확인/조치가 필요하다"고 경고한 바로 그 조건 — 2번째 지점
--       추가 — 이 이미 2026-07-26 `gym_lf02_seed_20260726.sql`로 실행되어 LF02(강남점,
--       gym.id=101)가 실존한다. 즉 이 마이그레이션은 "예방"이 아니라 "이미 늦은 채무"다.
--
--       2026-08-20 전수조사(`information_schema.columns`에서 gym_id 컬럼 존재 여부 재확인)
--       결과 기존 문서의 10개 테이블 목록이 여전히 전부 gym_id 없음으로 확인됐고,
--       문서 작성 이후(2026-07-26 이후) 신설된 테이블 중에도 같은 문제를 가진
--       4개(product_package/class_session/member_freeze/staff_attendance)를 추가로 발견했다.
--
-- 적용 대상 (14개, user_id/trainer_id 보유 테이블은 user_gym 조인으로 백필,
--            없는 테이블은 LF01(gym.id=1)로 백필 — 현재 user_gym 전 행이 gym_id=1뿐이라
--            결과는 동일하지만, 조인 방식이 재실행에도 안전함):
--   product, product_package, membership, member_tickets, trainer_schedules,
--   attendance, consult, sale, class_session, member_freeze, staff_attendance,
--   gym_setting, gym_banner, gym_holiday
--
-- 적용 대상에서 제외 (직접 컬럼 불필요 — 상위 테이블을 통해 이미/곧 스코핑됨):
--   - locker            → zone_id로 locker_zone(이미 gym_id 있음, 2026-08-10)을 통해 스코핑됨
--   - class_attendee     → class_session_id로 이 파일에서 gym_id를 추가하는 class_session을 통해 스코핑됨
--
-- 보류 (스키마 변경 여부를 임의로 결정하지 않음 — 별도 논의 필요):
--   - payment_method → 결제수단 목록이 전 지점 공통 정책일 수도, 지점별 상이할 수도 있어
--     이번 조사만으로는 정책을 판단할 수 없음. 필요해지면 별도 마이그레이션으로.
--
-- 주의: 이 파일은 스키마 변경(ALTER TABLE)을 포함한다. 로컬에서 먼저 검증 후 운영 적용할 것.
--       모든 `ADD COLUMN gym_id`에 `IF NOT EXISTS`를 넣어 컬럼 추가 자체는 재실행해도
--       에러가 나지 않는다(MariaDB 10.0.2+, 로컬 12.3.2/운영 10.11.14 둘 다 지원 확인).
--       단, FK/UNIQUE/INDEX 추가(`ADD CONSTRAINT`)는 동일 보호가 없어 이미 적용된 테이블에서
--       재실행하면 "Duplicate key name" 류 에러가 난다 — 부분 실패 후 재실행 시 하단
--       "재실행 안전성" 절차대로 어디까지 적용됐는지 먼저 확인할 것.
-- ============================================================================


-- ── 0. 사전 확인 (실행 전 눈으로 확인) ─────────────────────────────────────
SELECT id, branch_code, name FROM gym ORDER BY id;
-- 기대값: id=1(LF01, 본점), id=101(LF02, 강남점)


-- ── 1. product (상품/이용권 카탈로그) — 0 rows, 백필 불필요 ────────────────
ALTER TABLE product
  ADD COLUMN IF NOT EXISTS gym_id BIGINT UNSIGNED NULL AFTER id;
ALTER TABLE product
  MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL DEFAULT 1,
  ADD CONSTRAINT fk_product_gym FOREIGN KEY (gym_id) REFERENCES gym (id),
  ADD INDEX idx_product_gym (gym_id);
ALTER TABLE product ALTER COLUMN gym_id DROP DEFAULT;


-- ── 2. product_package (상품 패키지) — 0 rows, 백필 불필요 ─────────────────
ALTER TABLE product_package
  ADD COLUMN IF NOT EXISTS gym_id BIGINT UNSIGNED NULL AFTER id;
ALTER TABLE product_package
  MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL DEFAULT 1,
  ADD CONSTRAINT fk_product_package_gym FOREIGN KEY (gym_id) REFERENCES gym (id),
  ADD INDEX idx_product_package_gym (gym_id);
ALTER TABLE product_package ALTER COLUMN gym_id DROP DEFAULT;


-- ── 3. membership (회원권 구매 이력) — 1 row, user_id 기준 백필 ────────────
ALTER TABLE membership
  ADD COLUMN IF NOT EXISTS gym_id BIGINT UNSIGNED NULL AFTER user_id;
UPDATE membership m
  LEFT JOIN user_gym ug ON ug.user_id = m.user_id
  SET m.gym_id = COALESCE(ug.gym_id, 1)
  WHERE m.gym_id IS NULL;
ALTER TABLE membership
  MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL,
  ADD CONSTRAINT fk_membership_gym FOREIGN KEY (gym_id) REFERENCES gym (id),
  ADD INDEX idx_membership_gym (gym_id);


-- ── 4. member_tickets (티켓 잔량) — 5 rows, user_id 기준 백필 ──────────────
ALTER TABLE member_tickets
  ADD COLUMN IF NOT EXISTS gym_id BIGINT UNSIGNED NULL AFTER user_id;
UPDATE member_tickets mt
  LEFT JOIN user_gym ug ON ug.user_id = mt.user_id
  SET mt.gym_id = COALESCE(ug.gym_id, 1)
  WHERE mt.gym_id IS NULL;
ALTER TABLE member_tickets
  MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL,
  ADD CONSTRAINT fk_member_tickets_gym FOREIGN KEY (gym_id) REFERENCES gym (id),
  ADD INDEX idx_member_tickets_gym (gym_id);


-- ── 5. trainer_schedules (PT/OT 일정) — 19 rows, trainer_id 기준 백필 ──────
ALTER TABLE trainer_schedules
  ADD COLUMN IF NOT EXISTS gym_id BIGINT UNSIGNED NULL AFTER trainer_id;
UPDATE trainer_schedules ts
  LEFT JOIN user_gym ug ON ug.user_id = ts.trainer_id
  SET ts.gym_id = COALESCE(ug.gym_id, 1)
  WHERE ts.gym_id IS NULL;
ALTER TABLE trainer_schedules
  MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL,
  ADD CONSTRAINT fk_trainer_schedules_gym FOREIGN KEY (gym_id) REFERENCES gym (id),
  ADD INDEX idx_trainer_schedules_gym (gym_id);


-- ── 6. attendance (출석) — 0 rows, user_id 기준 백필(향후 재실행 대비) ─────
ALTER TABLE attendance
  ADD COLUMN IF NOT EXISTS gym_id BIGINT UNSIGNED NULL AFTER user_id;
UPDATE attendance a
  LEFT JOIN user_gym ug ON ug.user_id = a.user_id
  SET a.gym_id = COALESCE(ug.gym_id, 1)
  WHERE a.gym_id IS NULL;
ALTER TABLE attendance
  MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL,
  ADD CONSTRAINT fk_attendance_gym FOREIGN KEY (gym_id) REFERENCES gym (id),
  ADD INDEX idx_attendance_gym (gym_id);


-- ── 7. consult (상담 기록) — 0 rows, user_id는 NULL 허용이라 기본값 1로 ───
ALTER TABLE consult
  ADD COLUMN IF NOT EXISTS gym_id BIGINT UNSIGNED NULL AFTER user_id;
UPDATE consult c
  LEFT JOIN user_gym ug ON ug.user_id = c.user_id
  SET c.gym_id = COALESCE(ug.gym_id, 1)
  WHERE c.gym_id IS NULL;
ALTER TABLE consult
  MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL,
  ADD CONSTRAINT fk_consult_gym FOREIGN KEY (gym_id) REFERENCES gym (id),
  ADD INDEX idx_consult_gym (gym_id);


-- ── 8. sale (매출 내역) — 0 rows, user_id는 NULL 허용이라 기본값 1로 ──────
ALTER TABLE sale
  ADD COLUMN IF NOT EXISTS gym_id BIGINT UNSIGNED NULL AFTER user_id;
UPDATE sale s
  LEFT JOIN user_gym ug ON ug.user_id = s.user_id
  SET s.gym_id = COALESCE(ug.gym_id, 1)
  WHERE s.gym_id IS NULL;
ALTER TABLE sale
  MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL,
  ADD CONSTRAINT fk_sale_gym FOREIGN KEY (gym_id) REFERENCES gym (id),
  ADD INDEX idx_sale_gym (gym_id);


-- ── 9. class_session (그룹/PT 수업) — 0 rows, trainer_id 기준 백필 ────────
-- class_attendee는 class_session_id를 통해 자동으로 스코핑되므로 별도 컬럼 불필요.
ALTER TABLE class_session
  ADD COLUMN IF NOT EXISTS gym_id BIGINT UNSIGNED NULL AFTER trainer_id;
UPDATE class_session cs
  LEFT JOIN user_gym ug ON ug.user_id = cs.trainer_id
  SET cs.gym_id = COALESCE(ug.gym_id, 1)
  WHERE cs.gym_id IS NULL;
ALTER TABLE class_session
  MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL,
  ADD CONSTRAINT fk_class_session_gym FOREIGN KEY (gym_id) REFERENCES gym (id),
  ADD INDEX idx_class_session_gym (gym_id);


-- ── 10. member_freeze (유증/정지) — 0 rows, user_id 기준 백필 ─────────────
ALTER TABLE member_freeze
  ADD COLUMN IF NOT EXISTS gym_id BIGINT UNSIGNED NULL AFTER user_id;
UPDATE member_freeze mf
  LEFT JOIN user_gym ug ON ug.user_id = mf.user_id
  SET mf.gym_id = COALESCE(ug.gym_id, 1)
  WHERE mf.gym_id IS NULL;
ALTER TABLE member_freeze
  MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL,
  ADD CONSTRAINT fk_member_freeze_gym FOREIGN KEY (gym_id) REFERENCES gym (id),
  ADD INDEX idx_member_freeze_gym (gym_id);


-- ── 11. staff_attendance (트레이너 출퇴근) — 0 rows, user_id 기준 백필 ────
ALTER TABLE staff_attendance
  ADD COLUMN IF NOT EXISTS gym_id BIGINT UNSIGNED NULL AFTER user_id;
UPDATE staff_attendance sa
  LEFT JOIN user_gym ug ON ug.user_id = sa.user_id
  SET sa.gym_id = COALESCE(ug.gym_id, 1)
  WHERE sa.gym_id IS NULL;
ALTER TABLE staff_attendance
  MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL,
  ADD CONSTRAINT fk_staff_attendance_gym FOREIGN KEY (gym_id) REFERENCES gym (id),
  ADD INDEX idx_staff_attendance_gym (gym_id);


-- ── 12. gym_banner (배너) — 0 rows, 백필 불필요 ────────────────────────────
ALTER TABLE gym_banner
  ADD COLUMN IF NOT EXISTS gym_id BIGINT UNSIGNED NULL AFTER id;
ALTER TABLE gym_banner
  MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL DEFAULT 1,
  ADD CONSTRAINT fk_gym_banner_gym FOREIGN KEY (gym_id) REFERENCES gym (id),
  ADD INDEX idx_gym_banner_gym (gym_id);
ALTER TABLE gym_banner ALTER COLUMN gym_id DROP DEFAULT;


-- ── 13. gym_holiday (휴일) — 0 rows, 백필 불필요 ───────────────────────────
ALTER TABLE gym_holiday
  ADD COLUMN IF NOT EXISTS gym_id BIGINT UNSIGNED NULL AFTER id;
ALTER TABLE gym_holiday
  MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL DEFAULT 1,
  ADD CONSTRAINT fk_gym_holiday_gym FOREIGN KEY (gym_id) REFERENCES gym (id),
  ADD INDEX idx_gym_holiday_gym (gym_id);
ALTER TABLE gym_holiday ALTER COLUMN gym_id DROP DEFAULT;


-- ── 14. gym_setting (운영시간/공지/영업상태) — 특수 케이스 ─────────────────
-- 기존엔 PK가 tinyint 고정값 1인 단일 행 테이블. gym_id를 추가하고 UNIQUE로
-- "지점당 1행"을 보장하는 구조로 바꾼다. id 컬럼/PK 자체는 레거시 호환을 위해
-- 그대로 둔다(애플리케이션 코드가 id=1로 하드코딩된 부분은 이 마이그레이션이
-- 아니라 GymSettingMapper.xml 수정에서 별도로 처리해야 함 — 이 파일은 스키마만).
ALTER TABLE gym_setting
  ADD COLUMN IF NOT EXISTS gym_id BIGINT UNSIGNED NULL AFTER id;
UPDATE gym_setting SET gym_id = 1 WHERE id = 1 AND gym_id IS NULL;
ALTER TABLE gym_setting
  MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL,
  ADD CONSTRAINT uq_gym_setting_gym UNIQUE (gym_id),
  ADD CONSTRAINT fk_gym_setting_gym FOREIGN KEY (gym_id) REFERENCES gym (id);

-- LF02(gym_id=101)용 기본 설정 행 신설 — 없으면 지점별 조회로 바뀐 뒤 LF02는
-- "설정 없음"으로 깨진다. LF01 현재값을 그대로 복사해 시작점으로 삼는다.
INSERT INTO gym_setting (
  gym_id, gym_name, gym_phone, gym_address, is_open,
  mon_open, mon_close, mon_closed, tue_open, tue_close, tue_closed,
  wed_open, wed_close, wed_closed, thu_open, thu_close, thu_closed,
  fri_open, fri_close, fri_closed, sat_open, sat_close, sat_closed,
  sun_open, sun_close, sun_closed, notice
)
SELECT
  101, 'LINK_Fit 강남점', gym_phone, gym_address, is_open,
  mon_open, mon_close, mon_closed, tue_open, tue_close, tue_closed,
  wed_open, wed_close, wed_closed, thu_open, thu_close, thu_closed,
  fri_open, fri_close, fri_closed, sat_open, sat_close, sat_closed,
  sun_open, sun_close, sun_closed, notice
FROM gym_setting
WHERE gym_id = 1
  AND NOT EXISTS (SELECT 1 FROM gym_setting WHERE gym_id = 101);


-- ── 15. 검증 쿼리 (실행 후 확인용) ─────────────────────────────────────────
SELECT 'product' t, COUNT(*) rows_, SUM(gym_id IS NULL) null_gym_id FROM product
UNION ALL SELECT 'product_package', COUNT(*), SUM(gym_id IS NULL) FROM product_package
UNION ALL SELECT 'membership', COUNT(*), SUM(gym_id IS NULL) FROM membership
UNION ALL SELECT 'member_tickets', COUNT(*), SUM(gym_id IS NULL) FROM member_tickets
UNION ALL SELECT 'trainer_schedules', COUNT(*), SUM(gym_id IS NULL) FROM trainer_schedules
UNION ALL SELECT 'attendance', COUNT(*), SUM(gym_id IS NULL) FROM attendance
UNION ALL SELECT 'consult', COUNT(*), SUM(gym_id IS NULL) FROM consult
UNION ALL SELECT 'sale', COUNT(*), SUM(gym_id IS NULL) FROM sale
UNION ALL SELECT 'class_session', COUNT(*), SUM(gym_id IS NULL) FROM class_session
UNION ALL SELECT 'member_freeze', COUNT(*), SUM(gym_id IS NULL) FROM member_freeze
UNION ALL SELECT 'staff_attendance', COUNT(*), SUM(gym_id IS NULL) FROM staff_attendance
UNION ALL SELECT 'gym_banner', COUNT(*), SUM(gym_id IS NULL) FROM gym_banner
UNION ALL SELECT 'gym_holiday', COUNT(*), SUM(gym_id IS NULL) FROM gym_holiday
UNION ALL SELECT 'gym_setting', COUNT(*), SUM(gym_id IS NULL) FROM gym_setting;
-- 기대값: 전부 null_gym_id = 0, gym_setting은 rows_ = 2(LF01/LF02 각 1행)


-- ============================================================================
-- 재실행 안전성: `ADD COLUMN IF NOT EXISTS`라서 컬럼 추가 자체는 재실행해도
-- 안전하지만, 바로 뒤따르는 `ADD CONSTRAINT`(FK/UNIQUE/INDEX)는 이미 적용된
-- 테이블에서 재실행하면 "Duplicate key name" 에러가 난다. 부분 실패 후
-- 재실행이 필요하면 아래로 어느 테이블까지 FK/INDEX까지 끝났는지 먼저 확인할 것:
--
--   SELECT table_name, constraint_name FROM information_schema.table_constraints
--   WHERE table_schema = 'linkfit' AND constraint_name LIKE 'fk_%_gym'
--   ORDER BY table_name;
--
-- 이미 FK/INDEX까지 끝난 테이블 블록은 건너뛰고 나머지부터 이어서 실행.
--
-- 롤백 (주의 — FK/데이터 유실 가능, 필요할 때만):
--   ALTER TABLE <table> DROP FOREIGN KEY <fk_name>, DROP COLUMN gym_id;
--   DELETE FROM gym_setting WHERE gym_id = 101;
-- ============================================================================
