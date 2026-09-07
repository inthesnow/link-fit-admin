-- ============================================================================
-- 운영(prod) DB 배포용 통합 스크립트: 지점(gym) 스코핑을 위한 gym_id 백필
-- 작성일: 2026-08-25
--
-- 배경: 2번째 지점이 이미 발급된 상태에서 상담관리 등 여러 화면이
--       "Unknown column 'gym_id' in 'where clause'" 500 에러를 내는 게 실제
--       배포 서버에서 확인됨 — 로컬 DB에는 이미 적용된 마이그레이션이 운영
--       DB에는 아직 반영되지 않은 것이 원인. 로컬에 적용/검증 완료한 아래 3개
--       마이그레이션을 순서를 맞춰 하나로 통합했다:
--         1) gym_id_backfill_20260820.sql        (14개 테이블에 gym_id 컬럼 추가+백필)
--         2) gym_id_backfill_20260820_fixup_20260825.sql (1번의 gym_setting 시드 INSERT 버그 수정)
--         3) gym_holiday_unique_key_fix_20260825.sql     (gym_holiday 전역 유니크 제약 → 지점별로 수정)
--
-- ⚠️ 실행 전 필수:
--   1. 운영 DB 전체 백업 (mysqldump 등). 이 스크립트는 ALTER TABLE(스키마 변경)을
--      다수 포함하고, DDL은 트랜잭션으로 묶어 한 번에 롤백할 수 없다.
--   2. 가능하면 트래픽이 적은 시간대에 실행.
--   3. 이 파일 전체를 그대로 실행하면 된다 — 개별 파일 3개를 따로 실행할 필요 없음.
--
-- 재실행/부분적용 안전성: 실제 검증 결과, 운영 DB는 이 마이그레이션과 무관하게
-- 이미 일부 테이블(예: product)에 gym_id가 먼저 들어가 있을 수 있다는 게
-- 확인됐다(별도 경로로 먼저 추가된 것으로 추정). 그래서 이 스크립트는 모든
-- 컬럼/제약(FK)/인덱스 추가를 "이미 있으면 건너뛰기"로 처리한다 — 몇 번을
-- 다시 실행해도, 어떤 테이블까지 이미 적용돼 있어도 안전하게 나머지만 적용된다.
-- (`ADD COLUMN`/`ADD INDEX`는 MariaDB의 `IF NOT EXISTS`로, FK 제약은 MariaDB가
--  `ADD CONSTRAINT ... FOREIGN KEY IF NOT EXISTS` 문법을 지원하지 않아
--  information_schema를 직접 확인하는 프로시저로 처리했다 — 로컬 12.3.2에서
--  전체 스크립트를 처음부터 두 번 연속 실행해 완전히 검증함.)
-- ============================================================================


-- ── 0. 사전 확인 (실행 전 눈으로 확인) ─────────────────────────────────────
SELECT id, branch_code, name FROM gym ORDER BY id;


-- ── 0-1. 이번 마이그레이션 전용 헬퍼 프로시저 (끝나면 자동 삭제됨) ─────────
DELIMITER $$

DROP PROCEDURE IF EXISTS _gym_ensure_column$$
CREATE PROCEDURE _gym_ensure_column(IN p_table VARCHAR(64), IN p_after_col VARCHAR(64))
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = 'gym_id'
  ) THEN
    SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN gym_id BIGINT UNSIGNED NULL AFTER `', p_after_col, '`');
    PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
END$$

DROP PROCEDURE IF EXISTS _gym_ensure_fk_index$$
CREATE PROCEDURE _gym_ensure_fk_index(IN p_table VARCHAR(64), IN p_fk VARCHAR(64), IN p_idx VARCHAR(64))
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = p_table AND constraint_name = p_fk
  ) THEN
    SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD CONSTRAINT `', p_fk, '` FOREIGN KEY (gym_id) REFERENCES gym (id)');
    PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = p_table AND index_name = p_idx
  ) THEN
    SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_idx, '` (gym_id)');
    PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
END$$

DELIMITER ;


-- ── 1. gym_banner (배너) — 운영에 테이블 자체가 없을 수 있어 먼저 생성 ─────
CREATE TABLE IF NOT EXISTS gym_banner (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    image_url   VARCHAR(512) NOT NULL,
    title       VARCHAR(100) NOT NULL DEFAULT '',
    is_active   TINYINT(1) NOT NULL DEFAULT 1,
    sort_order  INT NOT NULL DEFAULT 0,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_banner_active (is_active),
    KEY idx_banner_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ── 2. gym_holiday (휴일) — 마찬가지로 먼저 생성 (구버전 전역 UNIQUE 그대로 —
--      뒤 섹션에서 지점별 UNIQUE로 교체한다) ─────────────────────────────────
CREATE TABLE IF NOT EXISTS gym_holiday (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    holiday_date  DATE NOT NULL,
    type          ENUM('PUBLIC','CLOSURE') NOT NULL DEFAULT 'CLOSURE' COMMENT 'PUBLIC=공휴일, CLOSURE=임시휴관',
    name          VARCHAR(100) NOT NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_holiday_date_type (holiday_date, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ── 3. product ───────────────────────────────────────────────────────────
CALL _gym_ensure_column('product', 'id');
UPDATE product SET gym_id = 1 WHERE gym_id IS NULL;
ALTER TABLE product MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL;
CALL _gym_ensure_fk_index('product', 'fk_product_gym', 'idx_product_gym');


-- ── 4. product_package ───────────────────────────────────────────────────
CALL _gym_ensure_column('product_package', 'id');
UPDATE product_package SET gym_id = 1 WHERE gym_id IS NULL;
ALTER TABLE product_package MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL;
CALL _gym_ensure_fk_index('product_package', 'fk_product_package_gym', 'idx_product_package_gym');


-- ── 5. membership — user_id 기준 백필 ───────────────────────────────────
CALL _gym_ensure_column('membership', 'user_id');
UPDATE membership m
  LEFT JOIN user_gym ug ON ug.user_id = m.user_id
  SET m.gym_id = COALESCE(ug.gym_id, 1)
  WHERE m.gym_id IS NULL;
ALTER TABLE membership MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL;
CALL _gym_ensure_fk_index('membership', 'fk_membership_gym', 'idx_membership_gym');


-- ── 6. member_tickets — user_id 기준 백필 ───────────────────────────────
CALL _gym_ensure_column('member_tickets', 'user_id');
UPDATE member_tickets mt
  LEFT JOIN user_gym ug ON ug.user_id = mt.user_id
  SET mt.gym_id = COALESCE(ug.gym_id, 1)
  WHERE mt.gym_id IS NULL;
ALTER TABLE member_tickets MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL;
CALL _gym_ensure_fk_index('member_tickets', 'fk_member_tickets_gym', 'idx_member_tickets_gym');


-- ── 7. trainer_schedules — trainer_id 기준 백필 ─────────────────────────
CALL _gym_ensure_column('trainer_schedules', 'trainer_id');
UPDATE trainer_schedules ts
  LEFT JOIN user_gym ug ON ug.user_id = ts.trainer_id
  SET ts.gym_id = COALESCE(ug.gym_id, 1)
  WHERE ts.gym_id IS NULL;
ALTER TABLE trainer_schedules MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL;
CALL _gym_ensure_fk_index('trainer_schedules', 'fk_trainer_schedules_gym', 'idx_trainer_schedules_gym');


-- ── 8. attendance — user_id 기준 백필 ───────────────────────────────────
CALL _gym_ensure_column('attendance', 'user_id');
UPDATE attendance a
  LEFT JOIN user_gym ug ON ug.user_id = a.user_id
  SET a.gym_id = COALESCE(ug.gym_id, 1)
  WHERE a.gym_id IS NULL;
ALTER TABLE attendance MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL;
CALL _gym_ensure_fk_index('attendance', 'fk_attendance_gym', 'idx_attendance_gym');


-- ── 9. consult — user_id는 NULL 허용이라 기본값 1로 ─────────────────────
CALL _gym_ensure_column('consult', 'user_id');
UPDATE consult c
  LEFT JOIN user_gym ug ON ug.user_id = c.user_id
  SET c.gym_id = COALESCE(ug.gym_id, 1)
  WHERE c.gym_id IS NULL;
ALTER TABLE consult MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL;
CALL _gym_ensure_fk_index('consult', 'fk_consult_gym', 'idx_consult_gym');


-- ── 10. sale — user_id는 NULL 허용이라 기본값 1로 ───────────────────────
CALL _gym_ensure_column('sale', 'user_id');
UPDATE sale s
  LEFT JOIN user_gym ug ON ug.user_id = s.user_id
  SET s.gym_id = COALESCE(ug.gym_id, 1)
  WHERE s.gym_id IS NULL;
ALTER TABLE sale MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL;
CALL _gym_ensure_fk_index('sale', 'fk_sale_gym', 'idx_sale_gym');


-- ── 11. class_session — trainer_id 기준 백필 (class_attendee는 class_session_id로 자동 스코핑) ──
CALL _gym_ensure_column('class_session', 'trainer_id');
UPDATE class_session cs
  LEFT JOIN user_gym ug ON ug.user_id = cs.trainer_id
  SET cs.gym_id = COALESCE(ug.gym_id, 1)
  WHERE cs.gym_id IS NULL;
ALTER TABLE class_session MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL;
CALL _gym_ensure_fk_index('class_session', 'fk_class_session_gym', 'idx_class_session_gym');


-- ── 12. member_freeze — user_id 기준 백필 ───────────────────────────────
CALL _gym_ensure_column('member_freeze', 'user_id');
UPDATE member_freeze mf
  LEFT JOIN user_gym ug ON ug.user_id = mf.user_id
  SET mf.gym_id = COALESCE(ug.gym_id, 1)
  WHERE mf.gym_id IS NULL;
ALTER TABLE member_freeze MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL;
CALL _gym_ensure_fk_index('member_freeze', 'fk_member_freeze_gym', 'idx_member_freeze_gym');


-- ── 13. staff_attendance — user_id 기준 백필 ────────────────────────────
CALL _gym_ensure_column('staff_attendance', 'user_id');
UPDATE staff_attendance sa
  LEFT JOIN user_gym ug ON ug.user_id = sa.user_id
  SET sa.gym_id = COALESCE(ug.gym_id, 1)
  WHERE sa.gym_id IS NULL;
ALTER TABLE staff_attendance MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL;
CALL _gym_ensure_fk_index('staff_attendance', 'fk_staff_attendance_gym', 'idx_staff_attendance_gym');


-- ── 14. gym_banner — gym_id 컬럼 추가 (테이블은 1번에서 이미 생성/존재 확인) ──
CALL _gym_ensure_column('gym_banner', 'id');
UPDATE gym_banner SET gym_id = 1 WHERE gym_id IS NULL;
ALTER TABLE gym_banner MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL;
CALL _gym_ensure_fk_index('gym_banner', 'fk_gym_banner_gym', 'idx_gym_banner_gym');


-- ── 15. gym_holiday — gym_id 컬럼 추가 (테이블은 2번에서 이미 생성/존재 확인) ─
CALL _gym_ensure_column('gym_holiday', 'id');
UPDATE gym_holiday SET gym_id = 1 WHERE gym_id IS NULL;
ALTER TABLE gym_holiday MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL;
CALL _gym_ensure_fk_index('gym_holiday', 'fk_gym_holiday_gym', 'idx_gym_holiday_gym');

-- 2026-08-25 후속 수정: 원래 UNIQUE KEY uq_holiday_date_type(holiday_date, type)이
-- 지점 구분 없이 전역으로 걸려있어서, 다른 지점이 같은 날짜에 같은 유형의 휴일을
-- 등록하려 하면 막히는 버그가 있었다(예: 설날/추석처럼 대부분 지점이 같은 달력
-- 날짜를 쓰는 휴일). gym_id를 포함한 복합 UNIQUE로 교체 (재실행 안전).
ALTER TABLE gym_holiday DROP INDEX IF EXISTS uq_holiday_date_type;
ALTER TABLE gym_holiday ADD UNIQUE KEY IF NOT EXISTS uq_gym_holiday_gym_date_type (gym_id, holiday_date, type);


-- ── 16. gym_setting (운영시간/공지/영업상태) — 특수 케이스 ─────────────────
-- 기존엔 PK가 tinyint 고정값 1인 단일 행 테이블. gym_id를 추가하고 UNIQUE로
-- "지점당 1행"을 보장하는 구조로 바꾼다. id 컬럼/PK는 애플리케이션이
-- (GymSettingMapper.xml 수정으로) 더 이상 하드코딩 조회하지 않으므로 레거시
-- 호환을 위해 그대로 둔다.
CALL _gym_ensure_column('gym_setting', 'id');
UPDATE gym_setting SET gym_id = 1 WHERE id = 1 AND gym_id IS NULL;
ALTER TABLE gym_setting MODIFY COLUMN gym_id BIGINT UNSIGNED NOT NULL;

-- UNIQUE(gym_id)도 같은 방식으로 존재 확인 후 추가
CALL _gym_ensure_fk_index('gym_setting', 'fk_gym_setting_gym', 'idx_gym_setting_gym_unused');
ALTER TABLE gym_setting ADD UNIQUE KEY IF NOT EXISTS uq_gym_setting_gym (gym_id);
-- 위 _gym_ensure_fk_index 호출은 FK만 추가 목적으로 재사용한 것 — 같이 만들어지는
-- 일반 INDEX(idx_gym_setting_gym_unused)는 UNIQUE KEY가 이미 인덱스 역할을 하므로
-- 중복이라 바로 제거한다.
ALTER TABLE gym_setting DROP INDEX IF EXISTS idx_gym_setting_gym_unused;

-- LF01(gym_id=1) 제외 나머지 전 지점에 기본 설정 행 신설 — 없으면 지점별 조회로
-- 바뀐 뒤 그 지점은 "설정 없음"으로 나온다. LF01 현재값을 그대로 복사해
-- 시작점으로 삼는다. id는 auto_increment가 아닌 고정값 PK라 명시적으로 다음
-- 값을 계산해서 넣어야 한다. 지점 수가 많아도 안전하도록 반복 처리한다.
DELIMITER $$
DROP PROCEDURE IF EXISTS _gym_seed_settings$$
CREATE PROCEDURE _gym_seed_settings()
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE v_gym_id BIGINT UNSIGNED;
  DECLARE v_gym_name VARCHAR(100);
  DECLARE cur CURSOR FOR
    SELECT id, name FROM gym
    WHERE id <> 1
      AND NOT EXISTS (SELECT 1 FROM gym_setting WHERE gym_setting.gym_id = gym.id);
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO v_gym_id, v_gym_name;
    IF done THEN
      LEAVE read_loop;
    END IF;

    INSERT INTO gym_setting (
      id, gym_id, gym_name, gym_phone, gym_address, is_open,
      mon_open, mon_close, mon_closed, tue_open, tue_close, tue_closed,
      wed_open, wed_close, wed_closed, thu_open, thu_close, thu_closed,
      fri_open, fri_close, fri_closed, sat_open, sat_close, sat_closed,
      sun_open, sun_close, sun_closed, notice
    )
    SELECT
      (SELECT COALESCE(MAX(id), 0) + 1 FROM gym_setting),
      v_gym_id, v_gym_name, gym_phone, gym_address, is_open,
      mon_open, mon_close, mon_closed, tue_open, tue_close, tue_closed,
      wed_open, wed_close, wed_closed, thu_open, thu_close, thu_closed,
      fri_open, fri_close, fri_closed, sat_open, sat_close, sat_closed,
      sun_open, sun_close, sun_closed, notice
    FROM gym_setting WHERE gym_id = 1 LIMIT 1;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

CALL _gym_seed_settings();
DROP PROCEDURE _gym_seed_settings;


-- ── 17. 헬퍼 프로시저 정리 ──────────────────────────────────────────────
DROP PROCEDURE IF EXISTS _gym_ensure_column;
DROP PROCEDURE IF EXISTS _gym_ensure_fk_index;


-- ── 18. 검증 쿼리 (실행 후 확인용) ─────────────────────────────────────────
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
-- 기대값: 전부 null_gym_id = 0

SELECT id, gym_id, gym_name FROM gym_setting ORDER BY gym_id;
-- 기대값: gym 테이블의 모든 지점에 대해 1행씩

SHOW CREATE TABLE gym_holiday\G
-- 기대값: UNIQUE KEY uq_gym_holiday_gym_date_type (gym_id, holiday_date, type) 존재,
-- uq_holiday_date_type은 더 이상 없어야 함


-- ============================================================================
-- 롤백 (주의 — FK/데이터 유실 가능, 필요할 때만):
--   ALTER TABLE <table> DROP FOREIGN KEY <fk_name>, DROP COLUMN gym_id;
--   DELETE FROM gym_setting WHERE gym_id <> 1;
--   DROP TABLE gym_banner;  -- 원래 운영에 없던 테이블이었다면
--   DROP TABLE gym_holiday; -- 원래 운영에 없던 테이블이었다면
-- ============================================================================
