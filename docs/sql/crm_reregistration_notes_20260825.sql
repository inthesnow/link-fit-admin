-- 재등록 관리(→ 만료 및 예정 회원 관리) 메모를 단일 텍스트 덮어쓰기에서
-- 회원 메모(crm_member_notes)와 동일한 "스택형 메모" 방식으로 교체.
-- 작성일: 2026-08-25
--
-- 배경: 기존 crm_re_registration.memo는 컬럼 하나에 계속 덮어쓰는 방식이라 이전 상담
-- 이력이 남지 않았음. crm_member_notes와 동일한 구조로 별도 테이블을 만들어 메모를
-- 시간순으로 쌓이게 한다. 기존 crm_re_registration.memo 컬럼은 데이터 손실 방지를 위해
-- 그대로 남겨두되(로컬 기준 데이터 없음), 더 이상 API/화면에서 쓰지 않는다.

CREATE TABLE IF NOT EXISTS crm_reregistration_notes (
  id CHAR(36) NOT NULL,
  reregistration_id CHAR(36) NOT NULL,
  gym_id BIGINT UNSIGNED NOT NULL,
  author_id CHAR(36) DEFAULT NULL COMMENT 'crm_users.id',
  content TEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_crn_reregistration (reregistration_id),
  KEY idx_crn_gym (gym_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 검증
DESCRIBE crm_reregistration_notes;
