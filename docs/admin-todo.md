# LINK_Fit Admin — 개발 진행 현황

> 마지막 업데이트: 2026-06-09 (2026-07-15 — CRM 테이블 DB 적용 상태 항목만 실제 확인 후 수정, 나머지는 미검증)  
> 기준 브랜치: master  
> 인증 방식: JWT (HttpOnly 쿠키 `crm_token`, crm_users 테이블)

---

## 범례
- ✅ 완료 (API + UI 모두)
- 🔧 백엔드만 완료 (UI 미구현 또는 일부 구현)
- ⬜ 미구현

---

## 1. 인증 / 보안

| 항목 | 상태 | 비고 |
|---|---|---|
| JWT 로그인 (`POST /api/auth/login`) | ✅ | branchCode + username + password |
| JWT 로그아웃 (`POST /api/auth/logout`) | ✅ | HttpOnly 쿠키 만료 |
| `crm_users` 테이블 기반 계정 관리 | ✅ | CrmUserMapper, CrmUserService |
| 역할 구분 (super_admin / gym_admin / trainer) | 🔧 | DB 컬럼 존재, 권한 분기 UI 미구현 |
| 관리자 계정 CRUD (웹 UI) | ⬜ | 직원 관리 페이지와 통합 예정 |

---

## 2. 회원 관리 (Sector 2-3)

| 항목 | 상태 | 비고 |
|---|---|---|
| 회원 목록 조회·검색·필터 | ✅ | keyword / status / tier 필터 |
| 회원 상세 조회·수정 | ✅ | `GET/PUT /api/members/{id}` |
| 회원 등록 / 탈퇴 처리 | ✅ | users + user_profiles 동시 처리 |
| 구독 등급 변경 | ✅ | `PATCH /api/members/{id}/tier` |
| 회원 구분 변경 (PT/OT/일반) | ✅ | `PATCH /api/members/{id}/member-type` |
| 상태 변경 (유효 / 정지) | ✅ | `PATCH /api/members/{id}/status` |
| 유증(정지) 기록 | ✅ | `POST /api/members/{id}/freeze` |
| 이용권 부여·조회 | ✅ | `GET/POST /api/members/{id}/tickets/charge` |
| 담당 트레이너 지정 | ✅ | `PUT /api/members/{id}/trainer` — crm_member_assignments |
| CRM 메모 기록 | ✅ | `GET/POST /api/members/{id}/notes` — crm_member_notes |
| 상태 태그 관리 | ✅ | `GET/POST/DELETE /api/members/{id}/tags` — crm_member_tags |
| 만료 예정 이용권 목록 | ✅ | `GET /api/memberships/expiring?days=7\|14\|30` |
| 이용권 변경 이력 기록 | ✅ | `POST /api/memberships/member/{id}/actions` — crm_membership_history |

---

## 3. 직원(트레이너) 관리 (Sector 13)

| 항목 | 상태 | 비고 |
|---|---|---|
| 트레이너 목록 조회·검색 | ✅ | `GET /api/staff` — users 테이블 role=TRAINER |
| 트레이너 등록·수정·삭제 | ✅ | `POST/PUT/DELETE /api/staff` |
| 역할 변경 | ✅ | `PATCH /api/staff/{id}/role` |
| 트레이너 CRM 대시보드 (담당회원·피드백·재등록) | ✅ | `GET /api/staff/{id}/dashboard` + 모달 UI |
| 담당 회원 목록 | ✅ | `GET /api/staff/{id}/members` |
| 출근 여부 현황판 | ⬜ | attendance 테이블 연동 필요 |
| CRM 트레이너 계정 (crm_users) CRUD UI | ⬜ | 직원 관리에 통합 예정 |

---

## 4. 수업 관리

| 항목 | 상태 | 비고 |
|---|---|---|
| 수업 목록 조회 | ✅ | `GET /api/classes` |
| 수업 등록·수정·삭제 | ✅ | `POST/PUT/DELETE /api/classes` |
| 수업 신청자 등록·취소 | ✅ | `POST/DELETE /api/classes/{id}/attendees` |
| 수업 일정 캘린더 UI | 🔧 | HTML 존재, 상세 캘린더 뷰 미구현 |
| 원포인트 레슨 신청 승인·거절 | ⬜ | |
| OT 신규 회원 트레이너 배정 | ⬜ | |

---

## 5. 출석 관리 (Sector 6)

| 항목 | 상태 | 비고 |
|---|---|---|
| 출석 기록 조회 | ✅ | `GET /api/attendance` |
| 출석 체크 | ✅ | `POST /api/attendance` |
| 출석 삭제 | ✅ | `DELETE /api/attendance/{id}` |
| 휴회(freeze) 목록 조회 | ✅ | `GET /api/attendance/freeze` |
| 루틴 이행 이력 조회 | ⬜ | Sector 6 — routine_logs 앱 테이블 연동 필요 |
| 장기 미출석 회원 알림 목록 | ⬜ | |
| 출석 30일 일별 트렌드 차트 | ✅ | `GET /api/attendance/trend` + Chart.js — attendance.html |
| 이행률 낮은 회원 자동 분류 | ⬜ | Sector 6 — 임계값 70% |

---

## 6. PT 관리 (Sector 4)

| 항목 | 상태 | 비고 |
|---|---|---|
| PT 회원 목록 (잔여 횟수·담당 트레이너) | ✅ | `GET /api/pt/members` — member_tickets 연동 |
| 잔여 5회 미만 필터 | ✅ | `?lowStock=true` |
| PT 횟수 충전·차감 | ✅ | `PUT /api/pt/members/{id}/tickets` + 이력 기록 |
| PT 재등록 대상자 목록 | 🔧 | 재등록 관리 페이지에서 pt_low 필터 사용 |
| 신규/재등록/소개 구분 기록 (crm_pt_registration_type) | ⬜ | CRM 매출 등록으로 대체 가능 |

---

## 7. 피드백 관리 (Sector 7-8-10)

| 항목 | 상태 | 비고 |
|---|---|---|
| 피드백 요청 목록 (status 필터) | ✅ | `GET /api/feedback/requests` |
| 피드백 요청 상세 조회 | ✅ | `GET /api/feedback/requests/{id}` |
| 트레이너 배정 | ✅ | `PATCH /api/feedback/requests/{id}/assign` |
| 답변 등록 (완료 처리) | ✅ | `POST /api/feedback/requests/{id}/respond` |
| 요청 상태 변경 (보류 등) | ✅ | `PATCH /api/feedback/requests/{id}/status` |
| 피드백 티켓 목록 (월·상태 필터) | ✅ | `GET /api/feedback/tickets` |
| 티켓 수동 발급 | ✅ | `POST /api/feedback/tickets/issue` |
| 티켓 만료 자동 처리 배치 | ✅ | 매일 00:05 @Scheduled — FeedbackTicketMapper.expireOverdue() |
| 당월 티켓 사용 요약 (카드) | ✅ | `GET /api/feedback/tickets/summary` |
| 헬스장별 발행 설정 | ✅ | `GET/PUT /api/feedback/settings` |
| 사진·영상 첨부 미리보기 | ⬜ | attachments JSON 저장만, 뷰어 미구현 |
| 자동 월초 티켓 발급 배치 | ✅ | 매달 1일 00:10 @Scheduled — DailyStatsScheduler.issueMonthlyTickets() |
| 티켓 구매 및 재고 관리 (Sector 9) | ⬜ | 정식 출시 후 구현 예정 |

---

## 8. 상담 관리

| 항목 | 상태 | 비고 |
|---|---|---|
| 상담 목록 조회·필터 | ✅ | `GET /api/consults` |
| 상담 등록 | ✅ | `POST /api/consults` |
| 상담 삭제 | ✅ | `DELETE /api/consults/{id}` |
| 기존 회원 상담 등록 | 🔧 | endpoint 존재 (`/api/consults/existing`) |
| 상담→회원 등록 전환 UI | ⬜ | |

---

## 9. 재등록 관리 (Sector 15)

| 항목 | 상태 | 비고 |
|---|---|---|
| 재등록 대상자 목록 (상태·사유 필터) | ✅ | `GET /api/reregistration` |
| 상태 변경 (대기→진행→성공/실패/보류) | ✅ | `PATCH /api/reregistration/{id}/status` |
| 담당자 배정 | ✅ | `PATCH /api/reregistration/{id}/assign` |
| 상담 메모 기록 | ✅ | `PATCH /api/reregistration/{id}/memo` |
| 상태별 요약 카드 | ✅ | `GET /api/reregistration/summary` |
| 이탈 사유 자동 분류 배치 | ✅ | 매일 06:00 @Scheduled — membership_expiry 자동 등록 |
| 수동 자동 분류 실행 버튼 | ✅ | `POST /api/reregistration/auto-classify` |

---

## 10. CS 티켓 (Sector 12)

| 항목 | 상태 | 비고 |
|---|---|---|
| CS 티켓 목록 (상태·카테고리 필터) | ✅ | `GET /api/cs/tickets` |
| 티켓 등록 | ✅ | `POST /api/cs/tickets` |
| 상태 변경 | ✅ | `PATCH /api/cs/tickets/{id}/status` |
| 담당자 배정 | ✅ | `PATCH /api/cs/tickets/{id}/assign` |
| 답변 등록 (answered 자동 전환) | ✅ | `PATCH /api/cs/tickets/{id}/respond` |
| 상태별 요약 카드 | ✅ | `GET /api/cs/tickets/summary` |

---

## 11. CRM 매출 (Sector 14)

| 항목 | 상태 | 비고 |
|---|---|---|
| 매출 내역 목록 (유형·기간 필터) | ✅ | `GET /api/crm-sales` |
| 매출 등록 (신규/재등록/소개 구분) | ✅ | `POST /api/crm-sales` |
| 매출 삭제 | ✅ | `DELETE /api/crm-sales/{id}` |
| 월간 요약 (유형별 합계·달성률) | ✅ | `GET /api/crm-sales/summary` |
| 월 목표 설정 | ✅ | `PUT /api/crm-sales/target` |
| 목표 대비 달성률 바 시각화 | ✅ | crm-sales.html |
| 매출 리포트 엑셀 내보내기 | ✅ | `GET /api/crm-sales/export` — Apache POI, crm-sales.html 버튼 |
| 트레이너별 매출 현황 | ⬜ | |

---

## 12. 공지사항 (Sector 16)

| 항목 | 상태 | 비고 |
|---|---|---|
| 공지사항 목록 (대상 필터) | ✅ | `GET /api/announcements` |
| 공지 작성 (전체/회원/트레이너/특정 대상) | ✅ | `POST /api/announcements` |
| 발송 완료 처리 | ✅ | `PATCH /api/announcements/{id}/send` |
| 공지 삭제 | ✅ | `DELETE /api/announcements/{id}` |
| 실제 푸시 발송 연동 | ⬜ | send_push 컬럼 저장만, FCM 미연동 |
| 공지 수정 기능 | ⬜ | |

---

## 13. 내부 쪽지함 (Sector 11)

| 항목 | 상태 | 비고 |
|---|---|---|
| 받은 메시지 / 보낸 메시지 / 공지 탭 | ✅ | `GET /api/inbox/messages?type=received\|sent\|notice` |
| 메시지 발송 / 답장 | ✅ | `POST /api/inbox/messages` + parentId |
| 읽음 처리 / 전체 읽음 | ✅ | `PATCH /api/inbox/messages/{id}/read`, `PATCH /read-all` |
| 안읽음 배지 (사이드바 자동 표시) | ✅ | `GET /api/inbox/unread-count` + sidebar JS |
| 메시지 삭제 | ✅ | `DELETE /api/inbox/messages/{id}` |

---

## 14. 매출 대시보드 (기존)

| 항목 | 상태 | 비고 |
|---|---|---|
| 날짜 네비게이션 (일/주/월) | ✅ | |
| 신규 회원·매출·출석 요약 카드 | ✅ | |
| 만료 예정 회원 위젯 (30일) | ✅ | `GET /api/dashboard/crm-summary` |
| 피드백 미처리 위젯 | ✅ | |
| 재등록 대기 위젯 | ✅ | |

---

## 15. 통계 배치 (Sector 17)

| 항목 | 상태 | 비고 |
|---|---|---|
| 일별 통계 집계 배치 (매일 01:00) | ✅ | DailyStatsScheduler — crm_daily_stats upsert |
| 티켓 만료 배치 (매일 00:05) | ✅ | FeedbackTicketMapper.expireOverdue() |
| 재등록 자동 분류 배치 (매일 06:00) | ✅ | ReRegistrationService.autoClassify() |
| 수동 통계 집계 API | ✅ | `POST /api/stats/daily/aggregate` |
| 통계 조회 API | ✅ | `GET /api/stats/daily?startDate=&endDate=` |
| 대시보드 통계 차트 (trend graph) | ✅ | `GET /api/stats/daily` + Chart.js 활성 회원 30일 추이 — dashboard.html |

---

## 16. 설정

| 항목 | 상태 | 비고 |
|---|---|---|
| 헬스장 기본정보 조회·수정 | ✅ | `GET/PUT /api/settings/gym` |
| 운영 상태 변경 (오픈·마감) | ✅ | `PATCH /api/settings/gym/open` |
| 피드백 티켓 발행 설정 | ✅ | `GET/PUT /api/feedback/settings` — feedback.html 탭 |
| 운영시간 요일별 설정 | ⬜ | |
| 휴일·임시 휴관일 설정 | ⬜ | |

---

## 공통 인프라

| 항목 | 상태 | 비고 |
|---|---|---|
| Spring Boot 4.0.4 + Java 21 | ✅ | |
| Spring Security 7.x + JWT | ✅ | STATELESS, CSRF disabled |
| MyBatis 3.0.3 + MariaDB 10.11 | ✅ | linkfit DB 공유 |
| Thymeleaf SSR + 인라인 JS fetch | ✅ | |
| 공통 REST 응답 `ApiResponse<T>` | ✅ | |
| GlobalExceptionHandler (404/500) | ✅ | |
| 다크 테마 CSS 시스템 (GitHub 스타일) | ✅ | common.css CSS 변수 |
| 반응형 사이드바 (모바일 오버레이) | ✅ | |
| CRM 전용 테이블 DDL (`crm_*` 18개) | ✅ (로컬만) | 2026-07-15 로컬 DB에 17개 추가 적용 완료 (18개 전부 존재). **운영 DB는 아직 미적용** — docs/db.md 참고 |
| @Scheduled 배치 잡 3종 | ✅ | DailyStatsScheduler |
| @EnableScheduling 활성화 | ✅ | LinkFitAdminApplication |
| Logback 설정 | ⬜ | 로그 레벨만 설정됨 |
| 테스트 코드 | ✅ | DailyStatsSchedulerTest(8) + MyBatisReRegistrationServiceTest(9) |

---

## 잔여 구현 항목 (우선순위 순)

| 순위 | 항목 | 비고 |
|---|---|---|
| 1 | 루틴 이행 이력 조회 | routine_logs 앱 테이블 read-only |
| 2 | 출석 주간·월간 통계 (기간별 집계 뷰) | 일별 차트는 완료, 주·월 집계 UI 미구현 |
| 3 | Sector 9: 티켓 구매·재고 관리 | 정식 출시 후 |
| 4 | FCM 푸시 알림 연동 | 공지·만료 알림 실제 발송 |
| 5 | 트레이너별 매출 현황 | CRM 매출 집계 추가 쿼리 필요 |
| 6 | 동명이인 자동 구분 (전화번호 끝 4자리 접미사) | 회원 등록 시 이름은 같고 전화번호가 다른 회원이 이미 있으면 "홍길동(5678)"처럼 새 등록자 이름에 자동으로 전화번호 끝 4자리를 붙이는 방안 — 2026-07-26 논의, 추후로 보류 |
| 6 | 역할 권한 분기 UI | super_admin / gym_admin / trainer |
