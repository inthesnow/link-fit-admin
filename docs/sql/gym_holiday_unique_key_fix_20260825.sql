-- gym_id_backfill_20260820.sql로 gym_holiday에 gym_id 컬럼이 생겼지만,
-- 기존 UNIQUE KEY uq_holiday_date_type(holiday_date, type)이 지점 구분 없이 전역으로
-- 걸려있어서, 2번째 지점이 같은 날짜에 같은 유형(예: 설날/추석처럼 대부분 지점이 같은
-- 달력 날짜를 쓰는 휴일)을 등록하려 하면 다른 지점이 이미 등록했다는 이유로 막히는 버그가 있었음.
-- 작성일: 2026-08-25

ALTER TABLE gym_holiday DROP INDEX uq_holiday_date_type;
ALTER TABLE gym_holiday ADD UNIQUE KEY uq_gym_holiday_gym_date_type (gym_id, holiday_date, type);

-- 검증
SHOW CREATE TABLE gym_holiday;
