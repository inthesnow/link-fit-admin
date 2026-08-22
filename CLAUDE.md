# LINK_Fit Admin — CLAUDE.md

헬스장 관리자 웹 어드민 프로젝트. LINK_Fit 앱의 백오피스로, 회원/직원/수업/출석/상담/매출/상품/메시지/CRM/피드백 등을 통합 관리한다.

---

## 기술 스택

| 항목 | 내용 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.4 |
| Security | Spring Security 7.x (JWT, Stateless) |
| Template | Thymeleaf (SSR) |
| Persistence | MyBatis + MariaDB (DB 연동 완료) |
| Build | Gradle 8.14 |
| Port | **17579** |
| 계정 | `admin` / `admin1234` (DB 기반, BCrypt 해싱) |

---

## DB 연결 정보

| 항목 | 값 |
|---|---|
| DBMS | MariaDB 10.11.14 |
| Host | `localhost` |
| Port | `3306` |
| Database | `linkfit` |
| Username | `linkfit` |
| Password | `link_fit!` |
| Profile | `dev` (기본값, `application.yml`에 설정됨) |

> 연결 설정 파일: `src/main/resources/application-dev.yml`
> 실제 앱 서비스 DB(`linkfit`)를 공유하여 사용한다.
> DDL 상세는 `docs/sql.md` / `docs/database.md` 참고.

### 어드민 전용 테이블

| 테이블 | 설명 |
|---|---|
| `admin_user` | 어드민 로그인 계정 |
| `product` | 상품/이용권 |
| `membership` | 회원권 구매 이력 |
| `member_freeze` | 유증(정지) 기록 |
| `class_session` | 그룹/PT 수업 (어드민 독립 사용) |
| `class_attendee` | 수업 신청자 |
| `attendance` | 출석 기록 |
| `consult` | 상담 기록 |
| `sale` | 매출 내역 |
| `gym_setting` | 헬스장 운영 설정 |

### 앱 DB 테이블 (재사용)

| 테이블 | 어드민 도메인 매핑 |
|---|---|
| `users` (role=MEMBER) | `member` |
| `users` (role=TRAINER) | `staff` |
| `user_profiles` | member/staff 상세 정보 (tier, member_type, trainer_id 등) |
| `member_tickets` | 티켓 잔량 (ONE_POINT / FEEDBACK / PHOTO / VIDEO) |
| `ticket_logs` | 티켓 지급·차감 이력 |
| `ticket_purchases` | 티켓 구매 이력 |
| `trainer_schedules` | 트레이너 PT·OT 일정 |
| `message_conversation` + `chat_message` | 실제 앱 쪽지/공지 (category: '공지'/'이벤트') |

---

## 프로젝트 구조

```
src/main/java/com/linkfit/admin/
├── LinkFitAdminApplication.java
├── common/
│   └── ApiResponse.java              ← 공통 REST 응답 래퍼 record
├── config/
│   ├── SecurityConfig.java           ← JWT Stateless 보안 설정 (CSRF 비활성, CORS 설정)
│   └── MyBatisConfig.java            ← Spring Boot 4.x MyBatis 수동 설정
├── controller/
│   ├── LoginController.java          ← GET /login, GET /dashboard (페이지 반환)
│   ├── PageController.java           ← 나머지 메뉴 페이지 GetMapping 모음
│   └── api/                          ← @RestController (JSON 반환)
│       ├── AuthApiController.java    ← POST /api/auth/login, /api/auth/logout
│       ├── DashboardApiController.java
│       ├── MemberApiController.java
│       ├── StaffApiController.java
│       ├── ClassApiController.java
│       ├── AttendanceApiController.java
│       ├── ConsultApiController.java
│       ├── ProductApiController.java
│       ├── MessageApiController.java
│       ├── RevenueApiController.java
│       ├── MembershipApiController.java
│       ├── PtApiController.java
│       ├── ReRegistrationApiController.java
│       ├── SettingApiController.java
│       ├── StatsApiController.java
│       ├── FeedbackApiController.java
│       ├── CrmInboxApiController.java
│       ├── CrmSalesApiController.java
│       ├── CsTicketApiController.java
│       ├── AnnouncementApiController.java
│       ├── LockerApiController.java        ← 라커 구역/배치/배정 (신규)
│       ├── GymJoinRequestApiController.java ← 헬스장 가입 승인 (신규)
│       ├── PaymentMethodApiController.java  ← 결제수단 관리 (신규)
│       └── ProductPackageApiController.java ← 상품 패키지 구성 (신규)
├── domain/                           ← VO/DTO (getter/setter 방식)
│   ├── AdminUser.java
│   ├── Member.java / Staff.java / ClassSession.java / ClassAttendee.java
│   ├── Attendance.java / Consult.java / Product.java / Message.java / Sale.java
│   ├── Membership.java / MemberFreeze.java / MemberTicket.java
│   ├── PtMember.java / ReRegistration.java / GymSetting.java / TicketSettings.java
│   ├── CrmUser.java / CrmAnnouncement.java / CrmCsTicket.java / CrmDailyStats.java
│   ├── CrmMemberNote.java / CrmMemberTag.java / CrmMembershipHistory.java / CrmMessage.java / CrmSale.java
│   ├── FeedbackRequest.java / FeedbackTicket.java
│   ├── TicketLog.java                ← 티켓 지급/차감 이력 (구독권/티켓 관리 "사용내역" 탭)
│   ├── Locker.java / LockerZone.java ← 라커 구역(zone)·개별 라커
│   ├── GymJoinRequest.java / GymJoinRequestLog.java ← 헬스장 가입 승인
│   ├── PaymentMethod.java            ← 결제수단 관리
│   └── ProductPackage.java           ← 상품 패키지(이용권/PT/락커/운동복 구성)
├── service/                          ← 서비스 인터페이스
├── service/mybatis/                  ← MyBatis 구현체 (현재 사용 중)
│   ├── MyBatisMemberService.java
│   ├── MyBatisStaffService.java
│   └── ... (전 도메인 구현 완료)
├── mapper/                           ← MyBatis @Mapper 인터페이스 (28개)
├── security/
│   ├── JwtUtil.java                  ← JWT 생성/검증 (JJWT)
│   ├── JwtCookieFilter.java          ← 쿠키에서 crm_token 추출 → SecurityContext 세팅
│   └── CrmUserDetails.java           ← UserDetails 구현체
├── service/
│   └── AdminUserDetailsService.java  ← (미사용 — 아래 "알려진 이슈" 참고)
├── scheduler/
│   └── DailyStatsScheduler.java      ← 일별 통계 자동 집계
└── exception/
    └── GlobalExceptionHandler.java   ← @ControllerAdvice, 404/500 처리

src/main/resources/
├── application.yml                   ← server.port=17579, profiles.active=dev
├── application-dev.yml               ← 로컬 DB 접속 정보, JWT 설정, CORS 설정
├── application-prod.yml              ← 환경변수 ${DB_URL} 방식
├── mapper/                           ← MyBatis XML (28개, 전 도메인 작성 완료)
├── static/css/
│   ├── common.css                    ← 사이드바/레이아웃/테이블/모달 공통 스타일
│   ├── dashboard.css                 ← 대시보드 전용 + 스켈레톤 UI
│   └── login.css                     ← 로그인 전용
└── templates/
    ├── fragments/sidebar.html        ← Thymeleaf 재사용 사이드바
    ├── login.html / dashboard.html
    ├── members.html / staff.html / classes.html / attendance.html
    ├── consults.html / revenue.html / products.html / messages.html
    ├── pt.html / settings.html / reregistration.html / feedback.html
    ├── inbox.html / cs.html / crm-sales.html / announcements.html
    ├── lockers.html                  ← 라커 관리 (구역별 배치/배정)
    ├── gym-requests.html             ← 헬스장 가입 승인
    └── error/404.html / error/500.html
```

---

## 아키텍처 패턴

### 인증 방식 — JWT (Stateless)
Spring Security 세션 없이 JWT 쿠키(`crm_token`) 기반으로 동작한다.
`JwtCookieFilter`가 모든 요청에서 쿠키를 추출해 `SecurityContextHolder`에 인증 정보를 세팅한다.
로그인/로그아웃은 `POST /api/auth/login`, `POST /api/auth/logout`으로 처리한다.

`AuthApiController`가 `CrmUserService`(→ `crm_users` 테이블)로 직접 아이디/비밀번호를 검증하고
JWT를 발급하는 구조 — Spring Security의 `AuthenticationManager`/`UserDetailsService` 경로를 타지 않는다.
`AdminUserDetailsService`(`admin_user` 테이블 기반)는 과거 로그인 방식의 잔재로, 현재는 어디서도
호출되지 않는 미사용 코드다.

```java
// JWT 클레임 구조
sub: userId (crm_users.id, CHAR(36) UUID)
branchCode, username, role, gymId
```

### REST API 응답 형식
모든 API는 `ApiResponse<T>` record를 사용한다.

```java
ApiResponse.ok(data)       // { success: true, message: "ok", data: ... }
ApiResponse.ok()           // data: null
ApiResponse.error("메시지") // { success: false, message: "...", data: null }
```

### 사이드바 fragment 사용법
모든 페이지는 `fragments/sidebar.html`을 `th:replace`로 삽입한다.

```html
<aside th:replace="~{fragments/sidebar :: sidebar('members')}"></aside>
```

activePage 값: `dashboard` / `members` / `staff` / `classes` / `attendance` / `consults` / `revenue` / `products` / `messages` / `pt` / `reregistration` / `feedback` / `lockers` / `gym-requests` / `inbox` / `cs` / `crm-sales` / `announcements` / `settings`

### 프론트엔드 데이터 흐름
페이지 렌더링은 Controller → Thymeleaf SSR.
동적 데이터(날짜 변경, 탭 전환, 테이블 갱신 등)는 각 페이지의 인라인 JS에서 `fetch()`로 REST API를 호출한다.
인증은 쿠키로 자동 전달되므로 `credentials: 'include'` 설정 필요.

---

## ⚠️ 알려진 이슈 (2026-07-15 검토 기준)

### 1. CRM 테이블 17개 — ✅ 로컬 DB 적용 완료, 운영(prod) DB는 아직 미적용
`docs/sql.md`에 CRM 전용 테이블(`crm_*`) 18개 DDL이 작성돼 있었는데, 로컬 `linkfit` DB엔
`crm_users` 1개만 실제로 존재하는 상태였다 (문서엔 "적용 완료"로 잘못 기록돼 있었음).
2026-07-15에 dry-run으로 충돌 여부를 검증(문제 없음 확인)한 뒤 **로컬 DB에 나머지 17개를 실제로 적용**했다
(90개 → 107개 테이블). 회원 담당 트레이너 지정, CRM 메모/태그, 피드백 요청·티켓, 재등록 관리, CRM 매출,
CS 티켓, 공지사항, CRM 쪽지함, 일별 통계 배치 등은 이제 로컬에서 정상 동작할 것으로 예상된다
(실제 기능 동작 재검증은 아직 안 함).
**운영 DB는 아직 미적용 — 공유 DB이므로 배포 일정과 조율 후 별도 적용 필요.**
상세 내역은 `docs/db.md`의 2026-07-15 항목들 참고.

### 2. 역할 기반 인가(Authorization) 미구현
JWT에 `role`(super_admin/gym_admin/trainer) 클레임이 있고 `ROLE_xxx` GrantedAuthority까지 만들지만,
`SecurityConfig`는 `anyRequest().authenticated()`뿐이라 실제로 역할을 검사하는 코드가 없다
(`@PreAuthorize`/`hasRole` 등 전체 코드베이스에 0건). 로그인만 하면 역할 무관하게 모든 API 호출 가능.

### 3. 지점(gym) 스코핑이 일부 컨트롤러에서만 적용됨
`Crm*Mapper` 계열(2026-06-08 이후 작성분)은 쿼리에 `gym_id` 필터가 있지만, `MemberApiController`/
`RevenueApiController`/`SettingApiController` 등 기존 컨트롤러는 `gymId` 파라미터 자체가
없어 전체 지점 데이터를 필터 없이 조회한다. 현재 지점이 `LF01` 1개뿐이라 실사용 영향은 없지만,
2번째 지점이 생기면 지점 간 데이터가 섞인다.
**2026-07-21에 `docs/multi-branch-expansion-issues.md`로 전수 재검증함 — 2번째 지점 추가 전 필독.**
(테스트용으로 `docs/sql/gym_lf02_seed_20260726.sql`에 LF02 시드 데이터가 있음.)

**2026-08-15에 이 카테고리로 발견/수정한 것들** (gymId를 받으면서 실제 쿼리에서 안 쓰거나, by-id
엔드포인트에 gym_id 필터가 아예 없던 경우):
- `StaffApiController` 대시보드/담당회원 — gymId 파라미터는 받으면서 SQL에서 실제로 안 씀(체크하는
  척만 하고 있었음) → `StaffMapper.xml`에 `user_gym` 조인 추가
- CRM 받은메시지함/공지사항 by-id 조회·수정·삭제(`CrmMessageMapper`, `CrmAnnouncementMapper`) —
  list 쿼리는 gym_id로 필터링하면서 by-id 쿼리엔 없었음(다른 지점 UUID를 알면 읽기/수정/삭제 가능)
- 공지·이벤트 일괄발송 대상 조회(`ConversationMapper.findMemberTargets/findTrainerTargets/countTarget`) —
  지점 구분 없이 전체 회원/트레이너 대상이었음. 이건 read-leak이 아니라 **쓰기 경로**라 더 심각 —
  다른 지점 관리자가 공지를 보내면 전체 지점에 뿌려짐

**여전히 미해결 (스키마 자체에 gym_id가 없어서, 임의로 스키마를 바꾸지 않고 보류)**:
`gym_setting`(단일 행, PK가 `tinyint` 기본값 1로 하드코딩됨)/`gym_holiday`/`gym_banner` — gym_id
컬럼 자체가 없어 2번째 지점이 생기면 운영시간/공지/휴무일/배너가 전 지점에서 공유된다. 이건 WHERE절
하나 빠뜨린 수준이 아니라 스키마 설계부터 다시 해야 하는 문제라, 2번째 지점 추가 계획이 구체화될 때
`docs/multi-branch-expansion-issues.md`와 함께 별도로 다룰 것.

### 3-1. 트레이너 지정 방식이 두 갈래로 나뉘어 있음
`user_profiles.trainer_id`(정상 동작)와 `crm_member_assignments`(중복 시스템, 죽은 코드에 가까움 + 실사용 시
항상 실패)가 공존한다. 2026-07-26에 `docs/trainer-assignment-permissions-review.md`로 코드 기준 전수 검토함.

### 3-2. 최초 로그인 비밀번호 변경 강제 (2026-08-21)
지점코드 발급으로 만들어진 gym_admin 계정은 전 지점이 `admin`/`linkonfit` 기본 비밀번호를
공유한다(`docs/admin-todo.md`에도 "보류"로 남아있던 항목). 이제 최초 로그인 시 1차(로그인)
비밀번호 변경 + 2차 비밀번호 생성을 강제한다.
- `crm_users.must_change_password` 컬럼(`docs/sql/crm_user_must_change_password_20260821.sql`) —
  lof-potal의 지점코드 발급(`GymService.issueBranchCode`)이 `1`로 세팅. 트레이너 승격 계정(앱
  비밀번호 위임 검증)은 기본 비밀번호를 쓰지 않으므로 대상 아님(컬럼 기본값 0).
- `MustChangePasswordInterceptor`(`WebConfig`에 `LockedCategoryInterceptor`보다 먼저 등록) —
  이 플래그가 켜진 계정은 `/change-password-first`/로그인/로그아웃/정적 리소스를 제외한 모든
  페이지·API를 차단한다(페이지는 리다이렉트, `/api/**`는 423). `LockedCategoryInterceptor`와
  동일한 "principal 없으면 통과 → 인터셉터가 최종 방어선" 패턴을 그대로 따름.
- `POST /api/auth/change-password-first` — `{currentPassword, newPassword, newSecondPassword}`.
  1차는 최소 4자리, 2차는 영문+숫자+특수문자 혼합 최소 8자리(대소문자 무관, 정규식
  `^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$`) — 검증 실패 시 에러, 성공 시
  `password_hash`/`second_password_hash`/`must_change_password=0`을 한 UPDATE로 원자적으로 갱신.
  이미 완료된 계정이 다시 호출하면 거부(이 엔드포인트는 최초 1회 전용 — 이후 비밀번호 변경은
  기존 "2차 비밀번호 변경"(`/api/auth/second-password`)과는 별도로, 1차 비밀번호만 다시 바꾸는
  일반 기능은 아직 없음, 필요시 별도 요청).
- 실제 로그인(`admin/linkonfit`) → `/dashboard` 리다이렉트 → `/api/dashboard/**` 423 →
  `POST /api/auth/change-password-first` 검증 실패 3종 + 성공 → 플래그 해제 후 `/dashboard`
  정상 접근 → 새 1차 비밀번호로 재로그인까지 end-to-end 실제 호출로 검증함(테스트용 지점
  LF99는 검증 후 삭제).
- 확인 시점 기존 gym_admin 계정 0건이라 소급 UPDATE는 하지 않음 — 이미 발급된 계정 중
  기본 비밀번호를 안 바꾼 게 있다면 개별적으로 플래그를 켜야 함(마이그레이션 파일 주석 참고).

### 4. 로그인 쿠키에 `Secure`/`SameSite` 미설정 + CSRF 전역 비활성
`AuthApiController`에서 쿠키에 `HttpOnly`만 설정하고 `Secure`/`SameSite`는 없음. `SecurityConfig`도
CSRF를 전역 비활성화(`csrf.disable()`)한 상태라 브라우저 기본 동작에만 의존하고 있다.

### 5. 미사용(dead) 코드
- ~~`service/mock/` 패키지~~ — 2026-07-15 삭제 완료 (아무 곳에서도 참조하지 않는 것 확인 후 제거, 빌드 통과 확인)
- `service/AdminUserDetailsService.java` — 위 인증 방식 참고, 현재 아무 데서도 호출되지 않음 (정리 대상)

---

## 개발 진행 현황

> 세부 항목별 체크리스트는 `docs/admin-todo.md` 참고 (섹터별로 훨씬 상세함).
> 아래는 그 내용을 기준으로 한 요약. CRM 관련 기능은 2026-07-15 로컬 DB 적용 완료로 로컬에서는
> 정상 동작 예상되나, **운영 DB는 아직 미적용**이라 운영 환경에서는 여전히 500 에러가 난다 (위 "알려진 이슈 1번" 참고).

### ✅ 완료 (코드 기준)

**보안 / 인증**
- Spring Security 7.x + JWT Stateless 인증 (`JwtCookieFilter`, `JwtUtil`)
- `crm_users` 기반 로그인 (`AuthApiController`, BCrypt 해싱) — 위 "알려진 이슈" 2·3·4번 참고
- CORS 설정 (`application-dev.yml`의 `app.cors.allowed-origins`)
- 로그아웃 쿠키 삭제 처리
- 지점코드 발급 계정 최초 로그인 시 1/2차 비밀번호 변경 강제 (위 "알려진 이슈 3-2번" 참고)

**백엔드**
- `ApiResponse<T>` 공통 응답 포맷, `GlobalExceptionHandler`(404/500)
- REST API Controller 20개, 도메인 클래스 33개, MyBatis Mapper 인터페이스+XML 28개
- `DailyStatsScheduler` — 일별 통계 집계(01:00) / 재등록 자동분류(06:00) — 로컬은 2026-07-15부터 정상 동작 예상 (운영은 아직 실패)
  **주의(2026-08-14 정정)**: "티켓 만료(00:05)"는 실제로 구현된 적이 없는 항목이었음(잘못된 문서 기록).
  `member_tickets` 테이블 자체에 만료일 컬럼이 없어(`remaining` 잔량만 존재) 시간 기준 자동 만료를
  걸 데이터가 없다. 무료 피드백 티켓의 월별 리셋은 lof-backend `TicketResetScheduler`가 별도로
  처리하는 것과는 다른 개념 — 그쪽은 "매달 초기화"고 여기서 말하던 건 "구매 후 N일 경과 만료"인데
  후자는 스키마부터 없다. 실제로 티켓 만료 기능이 필요하면 만료일 컬럼 추가부터 하는 스키마 설계가
  먼저 필요 — 임의로 만료 정책(기간 등)을 정해 구현하지 말 것.

**프론트엔드 — 최근 전면 개편분 (2026-06 커밋 기준)**
- 메시지 시스템 — `message_conversation`+`chat_message` 기반 재설계 완료 (레거시 `message`/`message_recipient`는 미사용)
- 출석 관리 — 기간탭, 회원별 현황, 장기 미출석, 유증(freeze) 관리 전면 개편
- 매출 관리 — 결제 내역, 구독권 현황, 티켓 판매, CSV 내보내기 전면 개편
- 수업 관리 — 수업 수정, 신청자 목록, 트레이너 일정 캘린더, 원포인트 신청 처리 전면 개편
- 사이드바 카테고리 개편 + 구독권/티켓 관리 페이지 신설
- 회원 등급(tier)·OT/PT 유형·담당 트레이너 지정, 헬스장 설정(`/settings`) 등 기존 기능

**신규 기능 (2026-07 ~ 08)**
- **라커 관리(`/lockers`)** — 헬스장당 여러 구역(zone) 신설, 구역별 독립된 가로×세로 배치와 라커 번호
  자동 생성(세로 최대 10칸), 회원 배정은 기존 상품/결제(membership) 흐름 재사용, 그리드는 가로/세로
  각 20칸 타일 단위 페이지네이션
- **헬스장 가입 승인(`/gym-requests`)** — 앱에서 회원이 가입 신청(`user_gym.status=PENDING`)하면 승인/거절,
  대시보드 배너 + 사이드바 뱃지로 대기 건수 노출
- **트레이너 계정 관리 개편** — 기존 앱 회원을 이름+전화번호로 검색해 트레이너로 승격/권한회수, 승격된
  계정은 CRM 로그인 시 앱 비밀번호를 그대로 위임 검증. 관리자별 2차 비밀번호 + 사이드바 카테고리 잠금
  (`LockableCategories` + `LockedCategoryInterceptor`로 직접 URL 접근도 서버측 차단)
- **결제수단 관리** — 이용권 등록 시 신용카드/계좌이체/현금 기본 제공 + "기타결제수단추가"
- **상품 등록 관리 개편** — 상품 패키지(`ProductPackage`)로 이용권/PT/락커/운동복을 자유 조합, PT는
  기간 없이 횟수만 관리
- **회원권 등록 개편** — 할인금액/납부액(미납금)/결제수단, 이력 기준 자동분류(신규/재유입/재등록/단품결제,
  `membership.reg_type`), 이용권/락커/운동복 양도(`status='TRANSFERRED'`로 이력 보존)
- **PT 세션 실제 충전** — PT 상품 등록 시 `user_profiles.pt_sessions_left`가 실제로 충전되도록 수정
  (이전엔 등록해도 앱에서 못 쓰는 버그)
- **매출 관리 — 환불 이력 보존** — 환불 시 행을 삭제하지 않고 `refund_amount`/`refunded_at`/`refund_reason`에
  기록(전액/부분 환불), 통계는 `amount - refund_amount`로 집계
- **구독권/티켓 관리 — 사용내역 탭** — `ticket_logs`(그동안 기록만 되고 조회 화면이 없었음) 조회 UI,
  사진/영상 피드백권도 관리 대상에 포함
- **연락처 정규화** — 저장은 항상 숫자만, 화면 표시할 때만 3-4-4 하이픈 부여

**데이터 정합성/보안 수정 (2026-08-15, 전수 점검)**
- **환불** — 부분 환불 후 추가 환불이 영구 차단되던 버그 + `refund_amount`를 누적이 아니라 매번
  덮어써서 이전 부분환불액이 유실되던 버그 함께 수정 (`RevenueApiController`)
- **회원권 삭제** — 금액 이전 + PT 세션 환원 + 삭제가 트랜잭션으로 묶여있지 않아 중간 실패 시
  데이터가 어긋날 수 있었음 → `MemberService.deleteMembership`로 이동, `@Transactional` 적용
- **티켓 충전/차감** — 트랜잭션 미적용 + 잔액 부족으로 clamp된 경우 로그가 실제 변동폭을 반영하지
  못하던 문제 수정 (`MyBatisMemberService.chargeTicket`)
- **소유권/지점 스코핑** — 위 "알려진 이슈 3번"의 2026-08-15 항목 참고
- JSON 숫자 필드 언가드 캐스팅(잘못된 타입 전송 시 500) 방어 코드 추가

**인프라**
- `application-dev.yml` / `application-prod.yml` 환경 분리, prod는 환경변수 기반
- HTTP 요청/에러 로깅 강화 (502 추적 대응), Logback 파일 로깅 + 일별 `.gz` 로테이트

### ⏳ 미완료 / 우선순위 낮은 항목

| 항목 | 비고 |
|---|---|
| ~~CRM 테이블 17개 DB 적용~~ | ✅ 로컬 완료 (2026-07-15). **운영 DB 적용은 아직 남음** — 위 "알려진 이슈 1번" 참고 |
| **역할 기반 인가, 지점 스코핑** | 위 "알려진 이슈 2·3번" 참고 |
| 티켓 구매·재고 관리 (Sector 9) | `crm_ticket_purchases`/`crm_ticket_inventory` — 정식 출시 후 구현 예정 |
| 루틴 이행 이력 조회 | 앱 `routines` 테이블 연동 필요 (read-only) |
| FCM 푸시 알림 실연동 | 공지사항 `send_push` 컬럼 저장만, 실제 발송 미구현 |
| 트레이너별 매출 현황 | CRM 매출 집계 쿼리 추가 필요 |
| 역할 권한 분기 UI | super_admin / gym_admin / trainer 화면 차등 없음 |
| 테스트 코드 | 2개뿐 (`DailyStatsSchedulerTest`, `MyBatisReRegistrationServiceTest`) — 컨트롤러 20개 대비 매우 부족 |
| `AdminUserDetailsService` | 미사용 dead code — 정리 대상 (`service/mock/`은 2026-07-15 삭제 완료) |

---

## 실행 방법

```bash
# 빌드
./gradlew build -x test

# 실행
./gradlew bootRun

# 접속
http://localhost:17579
```

**Windows:**
```bat
gradlew.bat bootRun
```

---

## 디자인 시스템

라이트 테마 + 사이드바 다크 네이비. `common.css`에 CSS 변수로 정의됨.

| 변수 | 값 | 용도 |
|---|---|---|
| `--bg` | `#F6F8FA` | 페이지 배경 |
| `--surface` | `#FFFFFF` | 카드, 헤더 |
| `--surface-up` | `#F0F2F5` | 입력 필드, 버튼 배경 |
| `--accent` | `#0969DA` | 활성 탭, 링크, 포커스 |
| `--success` | `#1A7F37` | 유효 상태, 매출 |
| `--error` | `#CF222E` | 에러, 만기 상태, 삭제 |
| `--warning` | `#9A6700` | 경고, 정지 상태 |
| `--border` | `#D0D7DE` | 테두리 |
| `--text-primary` | `#1F2328` | 주요 텍스트 |
| `--text-secondary` | `#636C76` | 보조 텍스트 |
| `--text-muted` | `#818B98` | 비활성 텍스트 |
| 사이드바 배경 | `#1C2333` | 다크 네이비 |

---

## 주요 URL 목록

### 페이지

| URL | 설명 |
|---|---|
| `GET /login` | 로그인 페이지 |
| `GET /change-password-first` | 최초 로그인 비밀번호 변경(강제) — `must_change_password` 계정만, 완료 계정은 `/dashboard`로 리다이렉트 |
| `GET /dashboard` | 대시보드 |
| `GET /members` | 회원 관리 |
| `GET /staff` | 직원 관리 |
| `GET /classes` | 수업 관리 |
| `GET /attendance` | 출석 관리 |
| `GET /consults` | 상담 관리 |
| `GET /revenue` | 매출 관리 |
| `GET /products` | 상품 관리 |
| `GET /messages` | 메시지 |
| `GET /pt` | PT 관리 |
| `GET /reregistration` | 재등록 관리 |
| `GET /feedback` | 구독권/티켓 관리 |
| `GET /lockers` | 라커 관리 |
| `GET /gym-requests` | 헬스장 가입 승인 |
| `GET /inbox` | 받은 메시지함 (CRM) |
| `GET /cs` | CS 티켓 |
| `GET /crm-sales` | CRM 영업 현황 |
| `GET /announcements` | 공지사항 관리 |
| `GET /settings` | 헬스장 설정 |

### REST API

| 경로 | 컨트롤러 | 주요 기능 |
|---|---|---|
| `POST /api/auth/login` | AuthApiController | JWT 로그인 |
| `POST /api/auth/logout` | AuthApiController | 쿠키 삭제 로그아웃 |
| `POST /api/auth/change-password-first` | AuthApiController | 최초 로그인 강제 변경(1차+2차 비밀번호 동시 설정) — `must_change_password` 계정 전용 |
| `/api/dashboard/**` | DashboardApiController | 통계 (members/classes/attendance/revenue/consults/crm-summary) |
| `/api/members/**` | MemberApiController | 회원 CRUD, 상태·등급·유형 변경, 티켓 조회·충전, 메모·태그, 트레이너 지정 |
| `/api/staff/**` | StaffApiController | 직원 CRUD, 역할 변경, 대시보드, 담당 회원 |
| `/api/classes/**` | ClassApiController | 수업 CRUD, 신청자 관리 |
| `/api/attendance/**` | AttendanceApiController | 출석 체크, 현황 조회, 유증 목록, 추이 |
| `/api/consults/**` | ConsultApiController | 상담 CRUD |
| `/api/products/**` | ProductApiController | 상품 CRUD |
| `/api/messages/**` | MessageApiController | 메시지 CRUD |
| `/api/revenue/**` | RevenueApiController | 매출 요약, 카테고리별 상세 |
| `/api/memberships/**` | MembershipApiController | 회원권 이력, 만료 예정, 액션 처리 |
| `/api/pt/**` | PtApiController | PT 회원 목록, 티켓 조회·수정 |
| `/api/reregistration/**` | ReRegistrationApiController | 재등록 목록, 상태·메모·담당자 변경, 자동 분류 |
| `/api/settings/gym` | SettingApiController | 헬스장 설정 조회·수정, 오픈 여부 토글 |
| `/api/stats/**` | StatsApiController | 일별 통계 조회, 수동 집계 |
| `/api/feedback/**` | FeedbackApiController | 피드백 요청·티켓 관리, 설정 |
| `/api/inbox/**` | CrmInboxApiController | CRM 받은 메시지 CRUD, 읽음 처리 |
| `/api/crm-sales/**` | CrmSalesApiController | CRM 매출 목록, 요약, 목표 관리, 내보내기 |
| `/api/cs/tickets/**` | CsTicketApiController | CS 티켓 관리, 담당자·상태·응답 처리 |
| `/api/announcements/**` | AnnouncementApiController | 공지사항 CRUD, 발송 처리 |
| `/api/members/tickets/logs` | MemberApiController | 티켓(구독권/원포인트/피드백/사진/영상) 사용내역 조회 |
| `/api/lockers/zones/**` | LockerApiController | 라커 구역 CRUD(가로/세로/총개수) |
| `/api/lockers/**` | LockerApiController | 라커 목록 조회, 배정(assign) — 해제는 `/api/memberships/{id}` DELETE 재사용 |
| `/api/gym-join-requests/**` | GymJoinRequestApiController | 헬스장 가입 신청 목록, 승인/거절 |
| `/api/payment-methods/**` | PaymentMethodApiController | 결제수단 목록 조회·추가 |
| `/api/product-packages/**` | ProductPackageApiController | 상품 패키지 CRUD |
