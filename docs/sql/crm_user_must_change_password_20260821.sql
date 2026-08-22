-- 지점코드 발급으로 생성된 gym_admin 계정(기본 비밀번호 공유)이 최초 로그인 시
-- 1차 비밀번호 변경 + 2차 비밀번호 생성을 강제하도록 하는 플래그.
-- lof-potal의 지점코드 발급(POST /api/gyms)이 계정 생성 시 1로 세팅하고,
-- lof-admin의 POST /api/auth/change-password-first 완료 시 0으로 내려간다.
ALTER TABLE crm_users
  ADD COLUMN must_change_password TINYINT(1) NOT NULL DEFAULT 0 AFTER second_password_hash;

-- 확인 시점(2026-08-21)에 기존 gym_admin 계정이 하나도 없어 소급 UPDATE는 하지 않음.
-- 이후 이미 발급된 gym_admin 계정 중 기본 비밀번호(linkonfit)를 아직 안 바꾼 계정이 있다면
-- 필요시 개별적으로 `UPDATE crm_users SET must_change_password = 1 WHERE id = '...'`로 표시할 것.
