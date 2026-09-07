-- ============================================================================
-- 운영 DB 긴급 복구: locker_zones_20260810.sql이 35번째 줄(FK 제약 추가)에서
-- 실패하며 중단된 상태를 마저 완료한다.
-- 작성일: 2026-08-24
-- 배경: 운영 DB에 locker(gym_id 있는 구형 스키마) 100건이 남아있는 상태로
--       locker_zones_20260810.sql을 실행 → 33번째 줄(zone_id 컬럼 추가, NOT NULL)
--       까지는 성공해 기존 100개 행이 전부 zone_id=0(유효하지 않은 값)으로
--       채워졌고, 35번째 줄의 FK 제약 추가에서 "zone_id=0을 참조하는
--       locker_zone 행이 없다"는 이유로 실패해 멈춤.
--
-- 복구 근거(진단 완료, 2026-08-24):
--   - locker: 100건, 전부 zone_id=0
--   - locker_layout: 정확히 1건(gym_id=1, rows_count=100, cols_count=10, total_count=100)
--     → gym_id 컬럼이 지워지긴 했지만, 지점이 하나뿐이었다는 근거가 명확해
--       100건 전부 gym_id=1 소속이었음을 그대로 복구할 수 있음
--   - locker_zone: 0건 (아직 비어있음)
--   - membership(type='LOCKER', 배정중): 0건 → 실사용자 라커 배정 데이터 없음,
--     안전하게 진행 가능
-- ============================================================================

-- ── STEP 1. 유실된 gym_id를 그대로 살려 구역(zone) 하나를 새로 만든다 ──
--    이름은 임시로 "기본구역"으로 둠 — 필요하면 라커 관리 화면에서 나중에 수정 가능.
INSERT INTO locker_zone (gym_id, name, rows_count, cols_count, total_count, display_order)
VALUES (1, '기본구역', 100, 10, 100, 1);

-- ── STEP 2. 기존 100개 라커의 zone_id(현재 잘못된 값 0)를 방금 만든 구역으로 연결 ──
UPDATE locker SET zone_id = LAST_INSERT_ID() WHERE zone_id = 0;

-- ── STEP 3. 원래 마이그레이션에서 실패했던 FK 제약을 마저 추가 ──
ALTER TABLE locker ADD CONSTRAINT fk_locker_zone FOREIGN KEY (zone_id) REFERENCES locker_zone (id) ON DELETE CASCADE;

-- ── STEP 4. 원래 마이그레이션의 남은 마무리 작업(중단 전엔 도달하지 못했던 부분) ──
DROP TABLE IF EXISTS locker_layout;

-- ── STEP 5. 검증 ──
SELECT COUNT(*) AS zone_count FROM locker_zone;                              -- 기대값: 1
SELECT zone_id, COUNT(*) FROM locker GROUP BY zone_id;                       -- 기대값: 방금 만든 zone id로 100건
SELECT COUNT(*) AS remaining_zero FROM locker WHERE zone_id = 0;             -- 기대값: 0
SHOW TABLES LIKE 'locker%';                                                  -- 기대값: locker, locker_zone (locker_layout 없어짐)
