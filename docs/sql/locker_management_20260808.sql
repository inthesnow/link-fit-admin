-- ============================================================================
-- 라커 관리 기능 - 물리적 라커 재고(locker) + 배치 설정(locker_layout) 신설,
--                membership에 locker_id 연결 컬럼 추가
-- 작성일: 2026-08-08
-- 배경: 기존에도 product/membership에 type='LOCKER'가 있었지만 "결제/기간 기록"만
--       있었을 뿐, 실제 물리적 라커 번호·배치(행/열)를 관리하는 개념이 없었다.
--       헬스장마다 라커 개수/배치가 다르므로 총 개수+행+열을 입력받아 라커 번호를
--       순차 생성하고, 관리자 페이지에서 회원을 특정 라커 번호에 배정할 수 있게 한다.
--       배정은 기존 상품/결제(membership) 흐름을 그대로 재사용한다 — 라커 배정 시
--       membership에 type='LOCKER' 행이 생성되고, 그 행의 locker_id로 실제 라커에
--       연결된다. 해제는 기존 "이용권 삭제"(DELETE /api/memberships/{id})를 그대로
--       재사용한다(별도 API 불필요).
--       행/열 위치는 저장하지 않는다 — locker_number 순서 + locker_layout.cols_count로
--       화면에서 그때그때 계산해서 그리므로, cols_count가 나중에 바뀌어도 재계산 없이 반영된다.
-- ============================================================================

CREATE TABLE IF NOT EXISTS locker (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    gym_id         BIGINT UNSIGNED NOT NULL,
    locker_number  INT NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_locker_gym_number (gym_id, locker_number),
    CONSTRAINT fk_locker_gym FOREIGN KEY (gym_id) REFERENCES gym (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS locker_layout (
    gym_id       BIGINT UNSIGNED NOT NULL,
    rows_count   INT NOT NULL,
    cols_count   INT NOT NULL,
    total_count  INT NOT NULL,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (gym_id),
    CONSTRAINT fk_locker_layout_gym FOREIGN KEY (gym_id) REFERENCES gym (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE membership
    ADD COLUMN IF NOT EXISTS locker_id BIGINT UNSIGNED NULL AFTER package_id;

ALTER TABLE membership
    ADD CONSTRAINT fk_membership_locker FOREIGN KEY (locker_id) REFERENCES locker (id) ON DELETE SET NULL;

-- 롤백 (필요 시):
-- ALTER TABLE membership DROP FOREIGN KEY fk_membership_locker;
-- ALTER TABLE membership DROP COLUMN locker_id;
-- DROP TABLE locker_layout;
-- DROP TABLE locker;
