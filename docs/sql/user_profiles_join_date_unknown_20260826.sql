-- 타사 CRM 이관(엑셀 일괄등록) 회원은 실제 가입일을 알 수 없다. users.created_at은
-- "이관 작업을 실행한 시각"일 뿐인데 이걸 "가입일"로 잘못 보여주고 있었음(members.html).
-- 이 플래그가 1이면 화면엔 가입일을 공란으로 표시한다(MyBatisMemberImportService에서만 1로 설정).
-- 2026-09-08: 재실행 안전을 위해 IF NOT EXISTS 추가(원래 없어서 이미 적용된 DB에
-- 재실행하면 "Duplicate column name" 에러가 났음).
ALTER TABLE user_profiles
  ADD COLUMN IF NOT EXISTS join_date_unknown TINYINT(1) NOT NULL DEFAULT 0 AFTER birth_date;
