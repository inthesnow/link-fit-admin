-- ============================================================================
-- 운영(prod) 적용용: 두 번째 지점(LF02) 가상 데이터 세팅
-- 작성일: 2026-07-26
-- 배경: 멀티 지점 기능(트레이너 소속 필터링, 앱 회원가입 헬스장 선택 등)을 실제로
--       테스트하려면 지점이 2개 이상 있어야 한다. LF01(LINK_Fit 본점) 외에
--       두 번째 지점을 하나 만들어둔다. 이름은 가상 데이터이므로 실제 지점명이
--       정해지면 UPDATE로 바꾸면 된다.
-- ============================================================================

SELECT COUNT(*) AS existing_lf02 FROM gym WHERE branch_code = 'LF02';

INSERT INTO gym (branch_code, name, is_active)
SELECT 'LF02', 'LINK_Fit 강남점', 1
WHERE NOT EXISTS (SELECT 1 FROM gym WHERE branch_code = 'LF02');

SELECT * FROM gym ORDER BY branch_code;

-- 롤백 (필요 시 — LF02에 연결된 회원/트레이너가 없을 때만 안전):
-- DELETE FROM gym WHERE branch_code = 'LF02';
