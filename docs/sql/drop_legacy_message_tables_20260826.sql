-- 레거시 메시지 발송 기능(message/message_recipient) 완전 삭제.
-- 어떤 컨트롤러도 호출하지 않던 죽은 코드였고, 매퍼 SQL이 실제 테이블에 없는
-- 컬럼(target_type, recipient_count, sender_id, sender_name)을 참조해 호출돼도
-- 에러가 나는 상태였다. 현재 실제로 쓰이는 관리자<->회원 쪽지 기능은 crm_messages,
-- 회원<->트레이너 쪽지는 message_conversation/chat_message로 완전히 대체됨.
DROP TABLE IF EXISTS message_recipient;
DROP TABLE IF EXISTS message;
