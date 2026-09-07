-- 트레이너 근태 정보(입사일/근무상태/퇴사일) 컬럼 추가
-- 작성일: 2026-08-25
--
-- 배경: 트레이너 지정 시 기존엔 "입사일"을 앱 회원가입일(users.created_at)로 그냥 계산해서
-- 보여줬을 뿐, 실제 입사일이나 근무 상태(재직/휴직/퇴사)를 관리자가 직접 입력/관리할 방법이
-- 없었다. crm_users는 지점별 트레이너 CRM 계정이라 이 정보를 담기에 적합한 테이블.

ALTER TABLE crm_users
  ADD COLUMN IF NOT EXISTS hire_date DATE NULL AFTER role,
  ADD COLUMN IF NOT EXISTS work_status ENUM('ACTIVE','LEAVE','RESIGNED') NOT NULL DEFAULT 'ACTIVE' AFTER hire_date,
  ADD COLUMN IF NOT EXISTS resignation_date DATE NULL AFTER work_status;

-- 검증
DESCRIBE crm_users;
