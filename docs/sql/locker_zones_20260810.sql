-- ============================================================================
-- 라커 관리 - 구역(zone) 단위 지원으로 전환
-- 작성일: 2026-08-10
-- 배경: 기존엔 헬스장(gym)당 하나의 라커 그리드만 있었는데, 실제로는 "1층 남자라커",
--       "2층 여자라커"처럼 서로 다른 물리적 라커실(뱅크)이 여러 개 있는 경우가 많다.
--       이를 위해 헬스장 1:1이던 locker_layout을 헬스장 1:N인 locker_zone으로 바꾸고,
--       각 구역이 자기만의 가로/세로/총개수와 라커 번호(1번부터, 구역 내에서만 유일)를
--       독립적으로 갖게 한다. 배정(membership.locker_id → locker.id)은 그대로 재사용.
--       가로/세로 개념 도입에 맞춰 rows_count=세로, cols_count=가로로 의미는 그대로 두되
--       세로(rows_count)는 서비스 계층에서 최대 10으로 제한한다(컬럼 자체 제약은 아님).
--       아직 실서비스에 구역/라커 데이터가 없는 상태(로컬 확인 완료)라 무중단 마이그레이션
--       고민 없이 바로 구조를 교체한다.
-- ============================================================================

CREATE TABLE IF NOT EXISTS locker_zone (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    gym_id         BIGINT UNSIGNED NOT NULL,
    name           VARCHAR(50) NOT NULL,
    rows_count     INT NOT NULL,
    cols_count     INT NOT NULL,
    total_count    INT NOT NULL,
    display_order  INT NOT NULL DEFAULT 0,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_locker_zone_gym (gym_id),
    CONSTRAINT fk_locker_zone_gym FOREIGN KEY (gym_id) REFERENCES gym (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE locker DROP FOREIGN KEY fk_locker_gym;
ALTER TABLE locker DROP INDEX uq_locker_gym_number;
ALTER TABLE locker DROP COLUMN gym_id;
ALTER TABLE locker ADD COLUMN zone_id BIGINT UNSIGNED NOT NULL AFTER id;
ALTER TABLE locker ADD UNIQUE KEY uq_locker_zone_number (zone_id, locker_number);
ALTER TABLE locker ADD CONSTRAINT fk_locker_zone FOREIGN KEY (zone_id) REFERENCES locker_zone (id) ON DELETE CASCADE;

DROP TABLE IF EXISTS locker_layout;

-- 롤백 (필요 시, 순서 주의):
-- CREATE TABLE locker_layout (gym_id BIGINT UNSIGNED NOT NULL PRIMARY KEY, rows_count INT NOT NULL,
--   cols_count INT NOT NULL, total_count INT NOT NULL, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
--   ON UPDATE CURRENT_TIMESTAMP, CONSTRAINT fk_locker_layout_gym FOREIGN KEY (gym_id) REFERENCES gym(id) ON DELETE CASCADE);
-- ALTER TABLE locker DROP FOREIGN KEY fk_locker_zone;
-- ALTER TABLE locker DROP INDEX uq_locker_zone_number;
-- ALTER TABLE locker DROP COLUMN zone_id;
-- ALTER TABLE locker ADD COLUMN gym_id BIGINT UNSIGNED NOT NULL AFTER id;
-- ALTER TABLE locker ADD UNIQUE KEY uq_locker_gym_number (gym_id, locker_number);
-- ALTER TABLE locker ADD CONSTRAINT fk_locker_gym FOREIGN KEY (gym_id) REFERENCES gym(id) ON DELETE CASCADE;
-- DROP TABLE locker_zone;
