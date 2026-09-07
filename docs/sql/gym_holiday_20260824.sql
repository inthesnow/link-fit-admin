-- 운영(prod) 적용용: gym_holiday 테이블 생성
-- 작성일: 2026-08-24 (뒤늦게 작성 — gym_banner와 동일하게 최초 생성 시 마이그레이션
--       파일을 안 남겨서 운영 반영이 누락된 채로 있었음)
-- 배경: 지점 구분 없이 전 지점 공유(gym_id 컬럼 없음) — gym_banner와 동일하게
--       docs/multi-branch-expansion-issues.md에서 2번째 지점 확장 시 재검토 대상으로
--       분류된 테이블. "설정 > 휴일 설정" 화면(공휴일/임시휴관)이 이 테이블을 씀.

CREATE TABLE IF NOT EXISTS gym_holiday (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    holiday_date  DATE NOT NULL,
    type          ENUM('PUBLIC','CLOSURE') NOT NULL DEFAULT 'CLOSURE' COMMENT 'PUBLIC=공휴일, CLOSURE=임시휴관',
    name          VARCHAR(100) NOT NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_holiday_date_type (holiday_date, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 적용 후 검증
SHOW TABLES LIKE 'gym_holiday';
SELECT COUNT(*) FROM gym_holiday;  -- 기대값: 0 (신규 테이블)
