-- 운영(prod) 적용용: gym_banner 테이블 생성
-- 작성일: 2026-08-24 (뒤늦게 작성 — 최초 생성 시 마이그레이션 파일을 안 남겨서
--       운영에 반영이 누락된 채로 있었음, docs/todo.md의 "공지사항 배너 등록·삭제·활성화"
--       항목이 이 테이블을 씀)
-- 배경: 지점 구분 없이 전 지점 공유(gym_id 컬럼 없음) — docs/multi-branch-expansion-issues.md
--       §"gym_banner: 전 지점 동일 배너 공유" 참고, 2번째 지점 확장 시 재검토 대상.

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

-- 적용 후 검증
SHOW TABLES LIKE 'gym_banner';
SELECT COUNT(*) FROM gym_banner;  -- 기대값: 0 (신규 테이블)
