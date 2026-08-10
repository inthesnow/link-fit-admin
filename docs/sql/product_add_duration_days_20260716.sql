-- product 테이블: duration_days 컬럼 누락 버그 수정
-- ProductMapper.xml이 duration_days/active 컬럼을 참조하는데 실제 테이블엔
-- duration_days가 아예 없고 active도 is_active로 존재 (이름 불일치).
-- 그 결과 이용권/PT 등록(POST /api/products)이 항상 500 에러 —
-- "Unknown column 'duration_days' in 'INSERT INTO'" — product 테이블이 0건이었음.
-- 여기서는 duration_days만 추가하고, active↔is_active 불일치는
-- ProductMapper.xml 쪽 컬럼명을 is_active로 고쳐서 해결(테이블 변경 없음).

ALTER TABLE product
    ADD COLUMN duration_days INT NOT NULL DEFAULT 0 COMMENT '이용 기간(일), 0=무기한' AFTER price;
