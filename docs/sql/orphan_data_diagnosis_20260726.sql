-- ============================================================================
-- 운영(prod) 진단용: 고아 데이터 현황 점검 (읽기 전용, SELECT/COUNT만 수행)
-- 작성일: 2026-07-26
-- 배경: "운영 DB에 고아 데이터가 너무 많다"는 요청에 따라, 실제로 어떤 테이블에
--       얼마나 있는지 먼저 파악하기 위한 진단 스크립트. 이 파일은 어떤 것도
--       삭제/변경하지 않는다 (DELETE/UPDATE 없음, 전부 SELECT).
--
-- 사용법: 이 스크립트를 운영 DB에서 실행한 뒤 각 섹션의 결과(건수)를 확인한다.
--         건수가 0보다 큰 항목이 실제 정리 대상이며, 그 결과를 바탕으로
--         orphan_data_cleanup_*.sql (백업 후 DELETE) 을 별도로 작성한다.
--
-- 참고: linkfit DB는 information_schema 조회 결과 대부분의 테이블에 FK 제약이
--       걸려 있어 신규 orphan이 거의 생기지 않는다. 아래는 FK 제약이 "없는"
--       컬럼들 위주로 점검한다 (여기가 orphan이 조용히 쌓일 수 있는 지점).
--       crm_* 테이블은 로컬 DB에만 적용되어 있고 운영 DB에는 아직 없을 수 있다
--       (docs/db.md 2026-07-15 항목 참고). 해당 테이블이 없다면 [B] 섹션에서
--       "Table doesn't exist" 에러가 나는 것이 정상이니 그 부분만 건너뛴다.
-- ============================================================================


-- -----------------------------------------------------------------------------
-- [A] 관리자(admin) 도메인 테이블 — FK가 없는 참조 컬럼 점검
-- -----------------------------------------------------------------------------

-- A-1. membership.package_id 가 이미 삭제된 product_package를 가리키는 경우
SELECT 'membership.package_id -> product_package' AS check_name, COUNT(*) AS orphan_count
FROM membership m
WHERE m.package_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM product_package p WHERE p.id = m.package_id);

-- A-2. ticket_purchases.user_id 가 존재하지 않는 회원을 가리키는 경우
SELECT 'ticket_purchases.user_id -> users' AS check_name, COUNT(*) AS orphan_count
FROM ticket_purchases t
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = t.user_id);

-- A-3. member_tickets.user_id 가 존재하지 않는 회원을 가리키는 경우
SELECT 'member_tickets.user_id -> users' AS check_name, COUNT(*) AS orphan_count
FROM member_tickets t
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = t.user_id);

-- A-4. ticket_logs.user_id 가 존재하지 않는 회원을 가리키는 경우
SELECT 'ticket_logs.user_id -> users' AS check_name, COUNT(*) AS orphan_count
FROM ticket_logs t
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = t.user_id);


-- -----------------------------------------------------------------------------
-- [B] CRM 도메인 테이블 — FK가 전혀 없음 (테이블 자체가 없으면 에러 발생, 정상)
-- -----------------------------------------------------------------------------

-- B-1. member_id 계열 (-> users.user_id)
SELECT 'crm_member_assignments.member_id -> users' AS check_name, COUNT(*) AS orphan_count
FROM crm_member_assignments t WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = t.member_id);

SELECT 'crm_member_notes.member_id -> users' AS check_name, COUNT(*) AS orphan_count
FROM crm_member_notes t WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = t.member_id);

SELECT 'crm_member_tags.member_id -> users' AS check_name, COUNT(*) AS orphan_count
FROM crm_member_tags t WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = t.member_id);

SELECT 'crm_membership_history.member_id -> users' AS check_name, COUNT(*) AS orphan_count
FROM crm_membership_history t WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = t.member_id);

SELECT 're_registration.member_id -> users' AS check_name, COUNT(*) AS orphan_count
FROM crm_re_registration t WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = t.member_id);

SELECT 'crm_sales.member_id -> users' AS check_name, COUNT(*) AS orphan_count
FROM crm_sales t WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = t.member_id);

SELECT 'crm_feedback_requests.member_id -> users' AS check_name, COUNT(*) AS orphan_count
FROM crm_feedback_requests t WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = t.member_id);

SELECT 'crm_feedback_tickets.member_id -> users' AS check_name, COUNT(*) AS orphan_count
FROM crm_feedback_tickets t WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = t.member_id);

SELECT 'crm_cs_tickets.member_id -> users' AS check_name, COUNT(*) AS orphan_count
FROM crm_cs_tickets t WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = t.member_id);

-- B-2. trainer_id/author_id/처리자 계열 (-> crm_users.id, NULL 허용 컬럼은 NULL 제외)
SELECT 'crm_member_assignments.trainer_id -> crm_users' AS check_name, COUNT(*) AS orphan_count
FROM crm_member_assignments t WHERE NOT EXISTS (SELECT 1 FROM crm_users u WHERE u.id = t.trainer_id);

SELECT 'crm_member_notes.author_id -> crm_users' AS check_name, COUNT(*) AS orphan_count
FROM crm_member_notes t WHERE t.author_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM crm_users u WHERE u.id = t.author_id);

SELECT 'crm_membership_history.processed_by -> crm_users' AS check_name, COUNT(*) AS orphan_count
FROM crm_membership_history t WHERE t.processed_by IS NOT NULL AND NOT EXISTS (SELECT 1 FROM crm_users u WHERE u.id = t.processed_by);

SELECT 'crm_re_registration.assigned_to -> crm_users' AS check_name, COUNT(*) AS orphan_count
FROM crm_re_registration t WHERE t.assigned_to IS NOT NULL AND NOT EXISTS (SELECT 1 FROM crm_users u WHERE u.id = t.assigned_to);

SELECT 'crm_sales.trainer_id -> crm_users' AS check_name, COUNT(*) AS orphan_count
FROM crm_sales t WHERE t.trainer_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM crm_users u WHERE u.id = t.trainer_id);

SELECT 'crm_feedback_requests.trainer_id -> crm_users' AS check_name, COUNT(*) AS orphan_count
FROM crm_feedback_requests t WHERE t.trainer_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM crm_users u WHERE u.id = t.trainer_id);

SELECT 'crm_feedback_tickets.trainer_id -> crm_users' AS check_name, COUNT(*) AS orphan_count
FROM crm_feedback_tickets t WHERE t.trainer_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM crm_users u WHERE u.id = t.trainer_id);

SELECT 'crm_cs_tickets.assigned_to -> crm_users' AS check_name, COUNT(*) AS orphan_count
FROM crm_cs_tickets t WHERE t.assigned_to IS NOT NULL AND NOT EXISTS (SELECT 1 FROM crm_users u WHERE u.id = t.assigned_to);

SELECT 'crm_ticket_purchases.purchased_by -> crm_users' AS check_name, COUNT(*) AS orphan_count
FROM crm_ticket_purchases t WHERE t.purchased_by IS NOT NULL AND NOT EXISTS (SELECT 1 FROM crm_users u WHERE u.id = t.purchased_by);

SELECT 'crm_announcements.author_id -> crm_users' AS check_name, COUNT(*) AS orphan_count
FROM crm_announcements t WHERE t.author_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM crm_users u WHERE u.id = t.author_id);

-- B-3. gym_id 계열 (-> gym.id) — 현재 지점이 1개뿐이라 사실상 0건 예상
SELECT 'crm_users.gym_id -> gym' AS check_name, COUNT(*) AS orphan_count
FROM crm_users t WHERE NOT EXISTS (SELECT 1 FROM gym g WHERE g.id = t.gym_id);

SELECT 'crm_member_assignments.gym_id -> gym' AS check_name, COUNT(*) AS orphan_count
FROM crm_member_assignments t WHERE NOT EXISTS (SELECT 1 FROM gym g WHERE g.id = t.gym_id);

SELECT 'crm_sales.gym_id -> gym' AS check_name, COUNT(*) AS orphan_count
FROM crm_sales t WHERE NOT EXISTS (SELECT 1 FROM gym g WHERE g.id = t.gym_id);


-- -----------------------------------------------------------------------------
-- [C] 탈퇴/비활성 회원의 잔여 데이터 현황 (참고용 — 자동 삭제 대상 아님)
-- -----------------------------------------------------------------------------
-- users.deleted_at 은 소프트 삭제라서 탈퇴해도 행은 남아있고, 아래 테이블들의
-- user_id는 "고아"가 아니라 정상 참조 상태다. 다만 탈퇴 회원의 결제/운동 이력을
-- 실제로 지울지 보관할지는 정책 결정이 필요해 여기서는 건수만 확인한다.

SELECT COUNT(*) AS withdrawn_member_count FROM users WHERE deleted_at IS NOT NULL;

SELECT 'membership rows of withdrawn members' AS check_name, COUNT(*) AS cnt
FROM membership m JOIN users u ON u.user_id = m.user_id WHERE u.deleted_at IS NOT NULL;

SELECT 'sale rows of withdrawn members' AS check_name, COUNT(*) AS cnt
FROM sale s JOIN users u ON u.user_id = s.user_id WHERE u.deleted_at IS NOT NULL;


-- -----------------------------------------------------------------------------
-- [D] 개발 중 생성된 백업/레거시 테이블 목록 (참고용 — 자동 삭제 대상 아님)
-- -----------------------------------------------------------------------------
-- 과거 정리 작업 중 안전을 위해 만들어둔 스냅샷 테이블들이다. 해당 정리가
-- 문제없이 완료된 것이 확인되면 DROP 후보다. 로컬 DB 기준 아래 이름들이
-- 존재했으니 운영 DB에도 있는지 확인해본다.

SELECT TABLE_NAME, TABLE_ROWS, CREATE_TIME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'linkfit'
  AND (TABLE_NAME LIKE '%\_backup%' OR TABLE_NAME LIKE '%\_backup\_%');

-- 레거시(미사용) 메시지 테이블 — message_conversation/chat_message로 대체됨
SELECT 'message' AS table_name, COUNT(*) AS row_count FROM message
UNION ALL
SELECT 'message_recipient', COUNT(*) FROM message_recipient;
