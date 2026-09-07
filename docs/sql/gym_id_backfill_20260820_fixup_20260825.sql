-- gym_id_backfill_20260820.sql 실행 시 마지막 단계(LF02용 gym_setting 시드 행 INSERT)에서
-- "Duplicate entry '1' for key 'PRIMARY'" 에러로 멈춘 것을 이어서 완료하는 보완 스크립트.
-- 작성일: 2026-08-25
--
-- 원인: gym_setting의 PK `id`가 auto_increment가 아니라 고정값(레거시 단일 행 테이블
--       구조를 그대로 유지)이라, INSERT ... SELECT가 원본 행(id=1)의 id 값을 그대로
--       복사하려다 PK 충돌이 남. id를 명시적으로 지정해야 함.
--
-- 적용 전: 원본 gym_id_backfill_20260820.sql을 먼저 그대로 실행할 것(이 파일은 그 뒤에
--         이어서 실행하는 보완용). 원본이 이미 다른 13개 테이블의 FK/컬럼 추가까지는
--         전부 성공하고 이 마지막 INSERT 한 줄에서만 멈추므로, 재실행 시 앞부분에서
--         "Duplicate key name" 에러가 날 수 있음 — 그 경우 이 파일만 단독 실행하면 됨.

INSERT INTO gym_setting (
  id, gym_id, gym_name, gym_phone, gym_address, is_open,
  mon_open, mon_close, mon_closed, tue_open, tue_close, tue_closed,
  wed_open, wed_close, wed_closed, thu_open, thu_close, thu_closed,
  fri_open, fri_close, fri_closed, sat_open, sat_close, sat_closed,
  sun_open, sun_close, sun_closed, notice
)
SELECT
  2, 101, 'LINK_Fit 강남점', gym_phone, gym_address, is_open,
  mon_open, mon_close, mon_closed, tue_open, tue_close, tue_closed,
  wed_open, wed_close, wed_closed, thu_open, thu_close, thu_closed,
  fri_open, fri_close, fri_closed, sat_open, sat_close, sat_closed,
  sun_open, sun_close, sun_closed, notice
FROM gym_setting
WHERE gym_id = 1
  AND NOT EXISTS (SELECT 1 FROM gym_setting WHERE gym_id = 101);

-- 검증
SELECT id, gym_id, gym_name FROM gym_setting;  -- 기대값: (1, 1, ...), (2, 101, ...) 2행
