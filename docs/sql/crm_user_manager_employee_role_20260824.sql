-- 운영(prod) 적용용: 관리자 페이지 전용 계정(매니저/직원) 역할 추가
-- 작성일: 2026-08-24
-- 배경: 지점 관리자(gym_admin)가 lof-admin에서 직접 추가 계정을 만들 수 있게 됨.
--       매니저(manager) = gym_admin과 동등한 권한(코드상 역할별 인가 자체가 없어 자동으로 동등).
--       직원(employee)  = LockableCategories에 정의된 2차 비밀번호 잠금 대상 카테고리 전체를
--       영구적으로 볼 수 없음(본인이 2차 비밀번호를 설정해도 해제 불가 — LockedCategoryInterceptor
--       에서 role='employee'면 무조건 차단하도록 처리).

ALTER TABLE crm_users MODIFY COLUMN role ENUM('super_admin','gym_admin','trainer','manager','employee') NOT NULL;
