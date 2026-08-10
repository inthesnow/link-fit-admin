-- ============================================================================
-- 운영(prod) 적용용: user_profiles.contact(연락처) 하이픈 제거
-- 작성일: 2026-07-26
-- 배경: 회원 등록/수정 시 연락처에 하이픈이 들어간 값과 안 들어간 값이 섞여
--       있었음. 이제부터는 저장 시 항상 숫자만 저장하고(백엔드에서 자동 정규화),
--       화면 표시할 때만 3-4-4 형식으로 하이픈을 붙여 보여준다.
--       기존에 하이픈이 섞여 저장된 데이터를 이 스크립트로 일괄 정리한다.
-- ============================================================================

-- 사전 점검
SELECT COUNT(*) AS total, SUM(contact LIKE '%-%') AS with_hyphen
FROM user_profiles WHERE contact IS NOT NULL;

UPDATE user_profiles
   SET contact = REPLACE(contact, '-', '')
 WHERE contact LIKE '%-%';

-- 적용 후 검증 (기대값: with_hyphen = 0)
SELECT COUNT(*) AS total, SUM(contact LIKE '%-%') AS with_hyphen
FROM user_profiles WHERE contact IS NOT NULL;
