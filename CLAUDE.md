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
JWT에 `role`(super_admin/gym_admin/trainer/manager/employee) 클레임이 있고 `ROLE_xxx`
GrantedAuthority까지 만들지만, `SecurityConfig`는 `anyRequest().authenticated()`뿐이라
`@PreAuthorize`/`hasRole` 같은 진짜 역할 검사는 코드베이스에 없다. 로그인만 하면 역할 무관하게
모든 API 호출 가능 — **단, `employee` role 하나만 예외**: `LockedCategoryInterceptor`가
role을 직접 확인해서 `LockableCategories` 전체를 하드코딩으로 차단한다(2026-08-24, 아래
"매니저/직원 계정" 항목 참고). Spring Security의 역할 기반 인가는 여전히 미구현이고, 이건
그 인터셉터 하나에 한정된 별도의 임시방편 차단이다 — 다른 곳에 role 검사를 추가하고 싶으면
이 인터셉터를 참고하되, 진짜 `hasRole` 기반 인가가 필요해지면 별도로 설계해야 함.

### 3. 지점(gym) 스코핑이 일부 컨트롤러에서만 적용됨
`Crm*Mapper` 계열(2026-06-08 이후 작성분)은 쿼리에 `gym_id` 필터가 있지만, `MemberApiController`/
`RevenueApiController`/`SettingApiController` 등 기존 컨트롤러는 `gymId` 파라미터 자체가
없어 전체 지점 데이터를 필터 없이 조회한다. 현재 지점이 `LF01` 1개뿐이라 실사용 영향은 없지만,
2번째 지점이 생기면 지점 간 데이터가 섞인다.
**2026-07-21에 `docs/multi-branch-expansion-issues.md`로 전수 재검증함 — 2번째 지점 추가 전 필독.**
(테스트용으로 `docs/sql/gym_lf02_seed_20260726.sql`에 LF02 시드 데이터가 있음.)

**2026-08-24, 실제 2번째 지점 발급 후 재현/수정** — potal에서 실제로 지점코드를 발급해 로그인해보니
대시보드(`/api/dashboard/members`)가 새 지점(회원 0명)인데도 기존 지점과 완전히 동일한 숫자를 보여줌.
`DashboardApiController.memberStats()`가 principal 자체를 안 받고, `DashboardMapper.xml`의
`memberStats`/`memberJoinStats` 쿼리가 `user_gym` 조인 없이 `users`/`user_profiles`만 봐서 전
지점 회원을 다 셌던 것 — 컨트롤러에 principal 추가 → 서비스/매퍼에 `gymId` 관통시켜 `user_gym`
조인(`gym_id=#{gymId} AND is_active=1`)을 추가해 수정. 실제로 신규 지점 계정으로 로그인해
0명으로 나오는 것, 기존 지점은 그대로 정상 집계되는 것까지 확인함.
**2026-08-24 시점엔 `classStats`/`revenueStats`/`revenueDetail`/`attendanceStats`/`appUsageCount`/
`routineComplianceStats`와 `Attendance`/`Consult`/`Revenue`/`Product`/`Class`/`Staff`/`Pt`/
`ReRegistration`/`Membership` 컨트롤러 전체를 "확인만 하고 손대지 않음"으로 남겨뒀었는데,
2026-08-25에 전부 마저 고침** — 아래 2026-08-25 항목 참고. `Setting`(`gym_setting`/`gym_holiday`/
`gym_banner`)만 여전히 스키마 자체에 gym_id가 없어 미해결로 남음(아래 참고).

**2026-08-24, "담당 트레이너 지정" 드롭박스가 전 지점 트레이너를 다 보여주던 문제** — 위와
같은 지점 발급 재현 과정에서 함께 발견. `MemberApiController.trainerOptions()`가
`staffService.findAll("TRAINER", 0, 200)`을 호출하는데 이 메서드 자체에 gymId 파라미터가
없어 시스템 전체 트레이너를 다 내려주고 있었음 — 신규 지점 관리자가 다른 지점 트레이너를
자기 회원의 담당으로 잘못 지정할 수 있는 상태였음. `StaffService.findTrainerOptions(gymId)`를
새로 추가(`user_gym` 조인, 기존 `findAll`/`count`는 손대지 않음 — `/staff` 페이지의 다른 용도와
섞이지 않도록 전용 메서드로 분리)하고 컨트롤러가 `principal.getGymId()`를 넘기도록 수정.
실제로 신규 지점은 빈 목록, 기존 지점은 정상 노출되는 것까지 확인함.

**2026-08-25, 위 2026-08-24 항목에서 "확인만 함"으로 남겨뒀던 나머지 전부 수정** — 로그아웃
버튼(위 6번), 신규 지점 대시보드 회원 수(위 3번 2026-08-24 항목), 트레이너 드롭박스(바로 위)를
고치는 과정에서 지점 간 데이터가 전혀 분리되지 않는 컨트롤러가 훨씬 많다는 게 드러나서, "지점코드가
다른 로그인의 경우 관리자페이지가 겹치면 안 된다"는 원칙에 따라 발견된 전부를 마저 고쳤다.

1. **`docs/sql/gym_id_backfill_20260820.sql` 적용** — 작성만 되고 실행되지 않은 채 남아있던
   마이그레이션을 로컬에 실제로 적용. `product`/`product_package`/`membership`/`member_tickets`/
   `trainer_schedules`/`attendance`/`consult`/`sale`/`class_session`/`member_freeze`/
   `staff_attendance`/`gym_banner`/`gym_holiday`/`gym_setting` 14개 테이블에 직접 `gym_id` 컬럼
   (FK + 인덱스)을 추가하고 기존 행은 `user_gym` 기준으로 백필. 마지막 단계(`gym_setting` LF02
   시드 행 INSERT)에서 `id`가 auto_increment가 아니라 고정값 PK라 `Duplicate entry '1'` 에러로
   멈춰서, `id`를 명시적으로 지정하는 보완 스크립트(`..._fixup_20260825.sql`)를 추가로 작성/적용함.
   (`gym_banner`/`gym_holiday`는 컬럼은 생겼지만 이번 라운드에서 컨트롤러 코드는 아직 안 고침 —
   `Setting` 컨트롤러 자체가 이 두 테이블을 아직 안 쓰고 있어서 실사용 영향 없음.)
2. **`Attendance`/`Consult`/`Revenue`(+ Dashboard 위젯)/`Product`/`Class`(+ Dashboard 위젯)
   컨트롤러 전체(9+6+10+5+13=43개 엔드포인트)** — gymId 관련 코드가 전혀 없던 상태에서 컨트롤러
   →서비스→매퍼 전 계층에 `principal.getGymId()`를 관통시킴. 위 백필로 직접 `gym_id`가 생긴
   테이블(`attendance`/`consult`/`sale`/`product`/`class_session`/`member_freeze`/
   `trainer_schedules`)은 `WHERE gym_id=#{gymId}`로, 아직 없는 테이블(`ticket_purchases`/
   `onepoint_requests`/`class_attendee`)은 `user_gym` 조인 또는 상위 엔티티(class_session) 검증으로
   스코핑. `DashboardMapper.xml`이 `revenueStats`/`revenueDetail`/`classStats` 쿼리를 `SaleMapper`/
   `ClassMapper`와 별도로 중복 보유하고 있어(위젯 전용), 두 곳 다 고쳐야 했음.
3. **`Staff` 컨트롤러(트레이너 CRUD 5개 + 출퇴근 5개)** — `findAll`/`count`/`findById`/`update`/
   `revokeTrainer`/`updateRole`에 `gymId` 추가(`user_gym` 조인). 트레이너 지정(`promoteToTrainer`)은
   원래 "대상자 본인이 앱에서 선택한 지점"을 그대로 쓰도록 설계돼 있었는데(승격시키는 관리자의
   세션 지점을 쓰면 트레이너 본인이 가입 안 한 지점 코드로 계정이 발급되는 문제가 있었어서), 정작
   **승격을 요청한 관리자의 지점과 대상자의 지점이 같은지 검사하는 코드가 없어서** A지점 관리자가
   B지점 회원을 B지점 트레이너로 지정할 수 있는 구멍이 있었음 — `callerGymId` 파라미터를 추가해
   불일치 시 거부하도록 수정. `staff_attendance`(백필로 직접 gym_id 생김)도 5개 메서드 전부 스코핑.
4. **`Pt` 컨트롤러(2개) + 그 안에서 파생된 `Membership` 컨트롤러 by-id 쓰기 구멍 6개** — PT
   목록/세션조정(`findPtMembers`/`adjustPtSessions`/`adjustServicePtSessions`)에 gymId를 추가하다가,
   `MemberMapper.insertMembership()`이 애초에 `gym_id` 컬럼 자체를 INSERT문에 넣지 않는다는 걸
   발견 — 1번 백필로 `membership.gym_id`가 `NOT NULL`이 된 상태라, **이 시점부터 이용권/PT/락커/
   운동복 등록(신규회원 등록, 엑셀 일괄 등록 전부 포함)이 전부 INSERT 실패로 깨져있었음**(실제
   서버 기동 후 재현 확인함 — 이번 세션에서 만든 회귀가 아니라 백필 마이그레이션 부작용). 
   `insertMembership`이 `gym_id`를 받아 채우도록 고치면서, `findMembershipById`/
   `updateMembershipEndDate`/`updateMembershipAmounts`/`markMembershipTransferred`/
   `deleteMembership`/`deleteMembershipsByMemberAndPackage`/`findMembershipsByMemberId`도 전부
   gymId 스코핑 — `MembershipApiController`의 이용권 삭제/기간조정/양도/종료일변경(by-id, 6개
   엔드포인트)에 gymId 파라미터 자체가 없던 것까지 같이 드러나서 함께 고침(다른 지점 이용권 id를
   알면 조회/수정/삭제가 가능한 상태였음). `GET /api/members/{id}/memberships`(회원상세 이용권
   이력 조회)도 같은 이유로 스코핑 추가.
5. **`ReRegistration` 컨트롤러 by-id 4개** — 목록/집계는 이미 `gym_id`로 스코핑돼 있었는데
   `findById`/`updateStatus`/`updateMemo`/`assign`엔 gymId 파라미터 자체가 없었음(다른 지점 UUID를
   알면 상태변경/메모/담당자배정이 가능).
6. **Dashboard `app-usage`/`routine-compliance` 위젯** — `exercise_records`/`trainer_members`엔
   gym_id가 없어 `user_gym` 조인으로 스코핑.
7. **실제 검증** — 로컬 admin 비밀번호를 임시로 알려진 값으로 바꾸고(검증 후 원복), 기존 지점
   LF01과 비어있는 LF02(2026-07-26에 만든 테스트용 지점, 회원/이용권/출근 데이터 전부 0건)
   양쪽 계정으로 실제 로그인해 위 1~6번 전부를 실제 HTTP 호출로 대조 검증함: 목록류(Staff/PT/
   ReRegistration/Dashboard 위젯)는 LF01엔 데이터가 보이고 LF02는 빈 목록/0건인 것, by-id
   쓰기류(이용권 기간조정·삭제, PT 세션조정, ReRegistration 상태변경, 트레이너 정보수정·역할변경)는
   LF02 계정으로 LF01의 실제 id를 넣어 호출해도 DB에 아무 변화가 없는 것(no-op)까지 확인. 신규
   이용권 등록(`insertMembership`)도 실제로 호출해 `gym_id`가 올바르게 채워지는 것과 매출
   기록(`recordSale`)까지 확인 후 테스트로 만든 행은 삭제.

**2026-08-15에 이 카테고리로 발견/수정한 것들** (gymId를 받으면서 실제 쿼리에서 안 쓰거나, by-id
엔드포인트에 gym_id 필터가 아예 없던 경우):
- `StaffApiController` 대시보드/담당회원 — gymId 파라미터는 받으면서 SQL에서 실제로 안 씀(체크하는
  척만 하고 있었음) → `StaffMapper.xml`에 `user_gym` 조인 추가
- CRM 받은메시지함/공지사항 by-id 조회·수정·삭제(`CrmMessageMapper`, `CrmAnnouncementMapper`) —
  list 쿼리는 gym_id로 필터링하면서 by-id 쿼리엔 없었음(다른 지점 UUID를 알면 읽기/수정/삭제 가능)
- 공지·이벤트 일괄발송 대상 조회(`ConversationMapper.findMemberTargets/findTrainerTargets/countTarget`) —
  지점 구분 없이 전체 회원/트레이너 대상이었음. 이건 read-leak이 아니라 **쓰기 경로**라 더 심각 —
  다른 지점 관리자가 공지를 보내면 전체 지점에 뿌려짐

**2026-08-25(2회차), 실제로 신규 지점코드를 발급해 로그인해보고 나서 위 `Setting`/`ProductPackage`
잔여 문제까지 마저 수정** — 1회차(위 항목)에서 "Setting은 스키마부터 다시 손봐야 하는 문제라 보류"로
남겨뒀었는데, 실사용 재현 결과 이 결정을 뒤집고 마저 고쳤다. 또한 1회차에서 손대지 않았던
`ProductPackageApiController`가 gymId 관련 코드가 아예 없어서 신규 지점에서도 기존 지점 상품이
그대로 보이는 걸 발견/수정함.
1. **`ProductPackageApiController`(상품 등록 관리의 실제 데이터, `product_package` 테이블)** —
   1회차 감사에서 `Product`(단순 이용권)만 확인하고 `ProductPackage`(이용권/PT/락커/운동복을
   조합하는 실제 "상품 등록" 기능)는 놓쳤던 컨트롤러. gymId 관련 코드가 전혀 없어 신규 지점에서도
   기존 지점 상품이 그대로 보였음 — 전 계층에 gymId 스코핑 추가. `MyBatisMemberService.addMembership`이
   패키지로 회원권을 등록할 때도 `productPackageService.findById(packageId)`를 gymId 없이 호출하고
   있어서, 다른 지점 상품 id를 알면 자기 회원에게 등록시킬 수 있는 구멍도 같이 막음.
2. **`gym_setting`(기본정보/공지) 하드코딩 `id=1` 조회 구조 자체를 gym_id 기준으로 교체** — PK `id`가
   `tinyint` 고정값(auto_increment 아님)이라 단순 WHERE절 추가로는 안 되고, `find`/`upsert`(INSERT
   ON DUPLICATE KEY UPDATE)/`updateOpenStatus`를 전부 `gym_id` 기준으로 재작성. 신규 지점의 첫
   저장(INSERT)에서 쓸 `id` 값은 `SELECT COALESCE(MAX(id),0)+1` 서브쿼리로 직접 계산(`gym_id`엔
   `UNIQUE KEY uq_gym_setting_gym`이 이미 있어 두 번째 저장부터는 정상적으로 UPDATE로 빠짐).
3. **`gym_holiday`/`gym_banner`도 컨트롤러·매퍼 전체 gymId 스코핑** — 추가로 `gym_holiday`에
   지점 구분 없는 전역 `UNIQUE KEY uq_holiday_date_type(holiday_date, type)`이 걸려있던 걸 발견 —
   설날/추석처럼 대부분 지점이 같은 날짜를 쓰는 휴일은 한 지점이 등록하면 다른 지점은 등록 자체가
   막히는 버그였음. `(gym_id, holiday_date, type)` 복합 유니크로 교체
   (`docs/sql/gym_holiday_unique_key_fix_20260825.sql`, 로컬 적용 완료 — 운영 DB는 별도 적용 필요).
4. **2차 비밀번호 최초 설정 UX** — `POST /api/auth/second-password`가 이미 설정된 2차 비밀번호를
   "바꾸는" 상황과 아직 없어서 "처음 만드는" 상황을 구분하지 않고 항상 1차(로그인) 비밀번호 재확인을
   요구했음. 최초 로그인 강제 온보딩(`/change-password-first`)에서 이미 1/2차를 함께 설정하긴 하지만,
   그 이후에도 사용자가 헷갈릴 수 있어 — 계정에 2차 비밀번호가 아직 없으면(`second_password_hash IS
   NULL`) 재확인 없이 바로 설정 가능하게, 이미 있으면 기존처럼 1차 비밀번호 재확인을 요구하도록 분기.
   `settings.html`도 이 상태에 따라 "설정"/"변경" 라벨과 현재 비밀번호 입력란 노출 여부를 동적으로
   바꾸도록 수정.
5. **검증**: LF01(기존 데이터 있음)과 완전히 새로 만든 지점(gym_setting/product_package/gym_holiday
   행이 하나도 없는 상태)의 실제 로그인 계정으로 상품 목록/설정 조회가 서로 분리되는 것, 신규 지점의
   설정 저장이 LF01 행을 건드리지 않는 것(별도 `id` 채번 확인), 같은 날짜의 휴일을 두 지점이 각자
   등록할 수 있는 것, 2차 비밀번호 없는 계정은 확인 없이 설정→있는 계정은 확인 요구로 바뀌는 것까지
   전부 실제 HTTP 호출로 확인 후 테스트 데이터 삭제·`admin` 비밀번호 원복함.

**여전히 미해결**: 없음 (위 5번 검증 시점 기준 `Setting`/`ProductPackage`까지 포함해 지점 스코핑
감사 항목 전부 해소). 향후 새 기능을 추가할 때 gymId 스코핑을 빠뜨리지 않는 것이 관건.

**⚠️ 위 로컬 코드 수정 전부가 운영 DB에는 아직 미적용 — 2026-08-25 실제 배포 서버에서
"Unknown column 'gym_id'" 500 에러로 재현/확인됨.** 이 시점까지 로컬에 적용한 3개 마이그레이션
(`gym_id_backfill_20260820.sql` → `..._fixup_20260825.sql` → `gym_holiday_unique_key_fix_20260825.sql`)을
순서 맞춰 하나로 합치고, 운영 DB가 이미 일부 테이블(예: `product`)에 별도 경로로 gym_id를
갖고 있을 가능성까지 고려해 전체를 재실행 안전(idempotent)하게 다시 작성한 것이
`docs/sql/PROD_DEPLOY_gym_scoping_20260825.sql`이다 — **운영 반영 시 개별 파일 3개가 아니라
이 파일 하나만 실행하면 됨.** 실제 운영 DB 백업(`all_backup_260823.sql`, 워크스페이스 루트)을
복원한 스크래치 DB에 두 번 연속 실행해 완전히 검증 완료(에러 없음, 재실행해도 중복 적용 없음).
운영 DB 실제 적용은 아직 하지 않음 — 백업 먼저 뜨고 트래픽 적은 시간대에 실행할 것.

### 3-1. 트레이너 지정 방식이 두 갈래로 나뉘어 있음
`user_profiles.trainer_id`(정상 동작)와 `crm_member_assignments`(중복 시스템, 죽은 코드에 가까움 + 실사용 시
항상 실패)가 공존한다. 2026-07-26에 `docs/trainer-assignment-permissions-review.md`로 코드 기준 전수 검토함.

### 3-1-1. 트레이너 등록 — 이름+전화번호 검색 → 회원 드롭박스 선택으로 교체 (2026-08-25)
**원인**: `StaffMapper.findAppUserByNameAndPhone`이 `up.name = ? AND up.contact = ?` **완전 일치**로만
찾았다. 관리자가 입력한 전화번호가 저장값과 공백/하이픈 등 포맷이 조금만 달라도(또는 이름에
공백 차이가 있어도) 실제로는 이 지점에 승인된 회원인데 "해당 이름/전화번호로 가입된 앱 사용자를
찾을 수 없습니다"가 떴다 — 어떤 값이 왜 안 맞는지 관리자가 알 방법도 없었음.
**수정**: 완전 일치 검색을 없애고, 이름 일부로 검색해 **이 지점에 승인된**(`user_gym.status='APPROVED'`)
회원만 후보로 보여주는 드롭박스로 교체:
- `GET /api/staff/member-candidates?keyword=` — 이름 LIKE 검색, 이미 TRAINER/ADMIN인 계정은 제외,
  최대 20건. 결과에 이름+전화번호를 함께 내려줘 화면에서 바로 구분 가능.
- `POST /api/staff`(트레이너 등록)가 더 이상 `{name, phone}`이 아니라 드롭박스에서 고른
  `{memberId, hireDate, workStatus, resignationDate}`을 받는다 — 이름/전화번호를 다시 타이핑할 일
  자체가 없어져 포맷 불일치로 인한 오탐이 구조적으로 불가능해짐. 선택된 `memberId`는
  `findMemberCandidateById`로 서버측에서 다시 한번 "이 지점 승인 회원"인지 확인(위조 방지).
- **입사일/근무상태/퇴사일 신설** — 기존엔 "입사일"이 앱 회원가입일(`users.created_at`)을 그대로
  보여준 것이었고 실제 근무 여부를 나타내는 값이 전혀 없었다. `crm_users`(지점별 트레이너 CRM
  계정)에 `hire_date DATE`/`work_status ENUM('ACTIVE','LEAVE','RESIGNED')
  DEFAULT 'ACTIVE'`/`resignation_date DATE`를 추가(`docs/sql/crm_user_trainer_employment_20260825.sql`,
  로컬 적용 완료·운영 미적용)하고 트레이너 등록/수정 화면에서 입사일(날짜)·근무상태(재직/휴직/퇴사
  드롭박스)·퇴사일(근무상태가 퇴사일 때만 노출, 필수)을 입력받도록 함. `StaffMapper.staffSelect`가
  더 이상 `DATE(u.created_at)`을 입사일로 계산하지 않고 `crm_users`를 LEFT JOIN해서 실제 값을 읽음
  — 이 마이그레이션 이전에 만들어진 기존 트레이너는 `crm_users`에 해당 값이 없어 목록에서
  입사일 `-`/근무상태 "재직"(기본값 폴백)으로 보이는 게 정상.
- 실제 로컬 DB의 승인 회원(jisoo_kim, LF01)으로 이름 검색 → 드롭박스 선택 → 입사일/근무상태
  입력 → 트레이너 승격까지, 그리고 근무상태를 휴직→퇴사(퇴사일 없이 시도 시 거부 확인)로 바꾸는
  수정까지 전부 실제 HTTP 호출로 검증. LF02 계정으로 같은 이름을 검색하면 결과가 0건인 것(지점
  스코핑 유지)까지 확인 후 테스트 데이터는 원상 복구함.

### 3-1-2. 재등록 관리 → "만료 및 예정 회원 관리"로 개편, 상태 변경 제거 + 메모 스택화 (2026-08-25)
- **사이드바**: `이용권/PT`와 `재등록 관리` 순서를 바꿔 `회원 관리 → 만료 및 예정 회원 관리 →
  이용권/PT` 순으로 변경(`fragments/sidebar.html`). 라우트/권한 카테고리 키는 그대로 `reregistration`
  (URL도 `/reregistration` 그대로) — 표시 라벨만 "만료 및 예정 회원 관리"로 변경(`settings.html`의
  2차 비밀번호 잠금 카테고리 목록 라벨도 함께 갱신).
- **상태(워크플로) 삭제**: `crm_re_registration.status`(pending/in_progress/success/failed/hold)를
  관리자가 직접 바꾸던 "상태 변경" 드롭박스와 목록 상단 상태 필터를 화면에서 제거하고, 관련
  API(`PATCH /api/reregistration/{id}/status`)와 서비스/매퍼 메서드를 전부 삭제함. 목록의 "상태"
  컬럼은 원래 이 워크플로 상태가 아니라 **회원권 만료 상태**(만료/만료예정/유효, `expiryBadge()`가
  계산)를 보여주고 있었던 것이라 그대로 남기고 헤더만 "만료상태"로 명확히 함(이 페이지가 이제
  만료일 기준 관리 화면이 됐으니 혼동 소지 없앰). `status` 컬럼 자체와 `autoClassify()`의 중복 방지
  로직(`existsByMemberAndReason`이 `status NOT IN ('success','failed')`로 체크)은 DB/백엔드에
  그대로 둠 — 변경 수단이 없어져 항상 `pending`으로 남지만, 그래도 같은 회원을 중복 생성하지
  않는 용도로는 계속 정상 동작함.
- **메모 스택화**: 기존엔 `crm_re_registration.memo` 컬럼 하나에 계속 덮어쓰는 방식(`PATCH
  /api/reregistration/{id}/memo`)이라 이전 상담 이력이 사라졌다. 회원 메모(`crm_member_notes`)와
  동일한 패턴으로 `crm_reregistration_notes` 테이블을 신설(`docs/sql/
  crm_reregistration_notes_20260825.sql`, 로컬 적용 완료·운영 미적용)하고 `GET/POST
  /api/reregistration/{id}/notes`로 시간순(최신순) 스택 조회/추가하도록 교체. "메모보기" 버튼과
  모달은 그대로 남기되(요청대로), 모달 내용을 상태 변경 드롭박스 없이 메모 목록+입력창만 있는
  형태로 바꿈. 기존 `memo` 컬럼은 로컬에 데이터가 없어 마이그레이션 없이 그대로 방치(사용 안 함).
- 컴파일 + 기존 테스트(`MyBatisReRegistrationServiceTest`, updateStatus 테스트를 addNote/findNotes
  테스트로 교체) 통과 확인. 라이브 인증 검증은 이번엔 생략(비로그인 상태로 각 페이지가 302로 정상
  리다이렉트되는 것 — 즉 템플릿 파싱 에러가 없는 것만 확인) — 실제 CRUD 동작은 코드 리뷰로 검증.

### 3-1-3. 엑셀 회원 일괄등록 — 이용종료일 공란 = 이미 만료된 회원 (2026-08-25)
타사 CRM 이관 엑셀(`MyBatisMemberImportService.importMembership`)에서 "이용종료일"(I열,
인덱스 8) 셀이 비어있는 행은 원본 CRM에서 이용권 만료일을 따로 안 남기는 관행 때문이며, "만료일이
없는 회원"이 아니라 **이미 만료된 회원**을 뜻한다. 기존엔 이 경우 이용권 등록 자체를 건너뛰어서
관리자 화면엔 그냥 이용권이 없는 것처럼("-") 보였다.
- `membership.end_date`는 DB에 `NOT NULL` 제약이 있어 실제로 비워둘 수는 없다 — 임의의 미래
  종료일을 추정하는 대신 `LocalDate.now().minusDays(1)`(항상 오늘 이전)을 넣어, 화면의 만료
  판정 로직(`expiryInfo()`류의 `diffDays < 0` 분기)이 시작일 값과 무관하게 무조건 "만료"로
  표시하도록 함. `status` 컬럼은 건드리지 않음(원래 null=정상/TRANSFERRED만 쓰는 용도라 새 값을
  추가하지 않고 기존 만료 판정 방식에 그대로 올라타는 쪽을 선택).
- PT 타입은 기존 로직(종료일 대신 횟수로 관리, 종료일 공란 시 무기한 처리) 그대로 유지 — 이번
  변경은 MEMBERSHIP/GROUP 등 비-PT 타입에만 적용됨.
- 등록 결과 메시지에 "(이용종료일 미기재 — 이미 만료된 회원으로 분류)"를 덧붙여 관리자가 왜
  만료로 등록됐는지 알 수 있게 함.
- `MyBatisMemberImportServiceTest`(신규) — 비-PT 종료일 공란 시 만료 처리되는지, PT는 영향받지
  않는지 검증(인메모리 XSSFWorkbook으로 실제 `importExcel()` 경로를 태움). `./gradlew test` 통과.

### 3-1-4. 쪽지함 — 3개의 병렬/단절 구현 정리 → 회원<->헬스장(관리자) 쪽지 신규 연결 (2026-08-26)
설계 검토 결과 쪽지 관련 코드가 서로 연결 안 된 3갈래로 존재했음: ① `message`/`message_recipient`
(죽은 코드 + 실제 테이블과 스키마 불일치, 삭제함 — `docs/sql/drop_legacy_message_tables_20260826.sql`),
② `crm_messages`/`/inbox`(CRM 내부 쪽지함, 완성도는 높았지만 앱과 전혀 연결 안 돼 0건), ③
`message_conversation`/`chat_message`(회원<->**트레이너** 1:1 쪽지, 유일하게 실제 동작하지만 발신자가
"헬스장"이 아니라 특정 트레이너 개인 — 이건 완전 별개 기능이라 이번 작업에서 건드리지 않음).
- **②를 앱과 연결**: `crm_messages`를 회원<->헬스장 쪽지의 실제 저장소로 채택. 헬스장 "계정"을
  `users`에 별도로 만들지 않음 — `crm_messages.sender_id`/`receiver_id`가 애초에 FK 없는 varchar라
  (③의 `message_conversation`/`chat_message`와 달리 `users.user_id` FK 제약이 없음) 필요 없었고,
  발신자가 admin인 메시지는 표시할 때 `gym.name`(회원의 `user_gym` 소속 지점)을 그때그때 붙이는
  방식으로 충분함. 회원이 보낼 때는 특정 관리자가 아니라 지점 전체에 보내는 것이므로
  `receiver_id='GYM'` 고정 문자열 사용(lof-backend `GymMessageMapper` 참고).
- **회원 상세 "쪽지" 탭 신설** (`members.html`, `dpane-msg`) — `GET/POST /api/members/{id}/messages`
  (`MemberApiController`, `CrmMessageMapper.findThreadWithMember`/`markMemberMessagesRead` 신규).
  관리자↔이 회원 간 전체 대화를 시간순 채팅 버블로 표시, 조회 시 회원이 보낸 메시지를 자동 읽음
  처리. 기존 `/inbox`(원포인트 ID 직접 입력) 대신 이미 회원이 선택된 상세 화면에서 바로 보내는
  구조라 UX가 훨씬 자연스러움 — `/inbox` 자체는 건드리지 않음(직원 간 내부 쪽지 용도로 계속 사용).
- **앱 쪽 연결**은 lof-backend `GET/POST /api/gym-messages` + lof-front `GymMessageDetailPage`가
  담당 — 자세한 내용은 두 리포의 CLAUDE.md 참고.
- **`staff.html` 트레이너 관리영역에 "쪽지내역보기" 추가** (③에 대한 관리자 오버사이트) — 트레이너
  CRM 대시보드 모달(`openDashboard`)에 `GET /api/staff/{id}/messages`
  (`ConversationMapper.findMessagesByTrainer`, 읽기전용) 섹션을 추가해, 그 트레이너가 회원들과
  주고받은 `chat_message` 전체를 관리자가 확인할 수 있게 함. `message_conversation`엔 `gym_id`가
  없어 이 테이블 자체로는 지점 스코핑이 안 되는데, `staffMapper.findById(id, gymId)`로 "이 id가
  실제 이 지점 소속 트레이너인가"부터 먼저 확인해 우회함.
- 검증: 컴파일 + 기존 테스트 통과, lof-backend를 통해 실제 JWT로 양방향 메시지 왕복(관리자→회원
  DB 직접 삽입 → `GET /api/gym-messages`로 확인 → `POST /api/gym-messages`로 회원 답장 → 동일 SQL로
  두 메시지 모두 시간순 확인) 및 `findMessagesByTrainer` 쿼리를 실제 대화 데이터로 확인, 트레이너
  회원 자유텍스트 발신 차단(③, 아래 3-1-5)까지 전부 실제 호출로 검증 후 테스트 데이터 삭제.

### 3-1-5. 회원<->트레이너 쪽지, 자유 텍스트는 트레이너→회원 단방향으로 제한 (2026-08-26)
3-1-4(회원<->헬스장 쪽지) 도입과 함께 결정된 사항 — `message_conversation`/`chat_message`(③, 회원
<->트레이너 1:1)에서 회원이 직접 텍스트를 입력해 트레이너에게 보내는 것을 막았다. 회원이 트레이너에게
먼저 말을 거는 경로는 원포인트 신청/피드백 요청 등 지정된 액션(lof-backend
`sendOnepointNotification`/`sendFeedbackNotification`)뿐이고, 자유 텍스트는 트레이너만 보낼 수 있다.
실제 차단은 lof-backend `MessageService.sendMessage()`에서 처리(자세한 내용은 lof-backend
CLAUDE.md 참고) — lof-admin 쪽은 이 채널에 쓰기 기능이 없어 영향 없음.

### 3-1-6. 헬스장 가입 승인(`gym-requests.html`) "승인됨" 카운터 = 실제 앱가입자만 (2026-08-26)
`user_gym.status='APPROVED'`인 행에는 두 종류가 섞여 있음: ① 회원이 앱에서 자율적으로 가입 신청해서
관리자가 승인한 진짜 앱 사용자, ② 엑셀 일괄등록 등 CRM 쪽 경로로 만들어져 `user_gym`엔 기본값
APPROVED로 들어가지만 실제로 앱에 로그인해본 적 없는(=`user_auth`에 행이 없는) 회원. 요약 카드
"승인됨"이 이 둘을 합쳐서 보여주고 있었는데, 실제 앱가입자만 세도록 변경.
- `GymJoinRequestMapper.countApprovedAppUsers` — `user_gym.status='APPROVED'` AND
  `EXISTS (SELECT 1 FROM user_auth WHERE user_auth.user_id = user_gym.user_id)`.
- 새 전용 엔드포인트 `GET /api/gym-join-requests/approved-app-count`로 노출하고 `gym-requests.html`의
  "승인됨" 요약 카드만 이걸 쓰도록 변경(카드 라벨도 "승인됨 (앱가입)"으로 명확히 함).
- **의도적으로 목록/페이징(`count`/`findAll`, status=APPROVED 필터)은 건드리지 않음** — 요청 범위가
  "카운터"였고, 목록까지 같이 필터링하면 관리자가 CRM 등록 회원의 승인 상태를 조회/관리할 방법이
  없어짐. 그 결과 요약 카드 숫자(앱가입자만)와 목록에서 APPROVED로 필터링했을 때 나오는 행 수(전체)가
  다를 수 있음 — 의도된 차이이며 버그 아님.
- 검증: 로컬 데이터로 실제 값 확인(`user_gym` APPROVED 6건 중 `user_auth` 있는 건 2건) → 새
  엔드포인트가 정확히 2를 반환하는 것까지 실제 HTTP 호출로 확인(관리자 비밀번호는 건드리지 않고
  로컬 JWT 시크릿으로 직접 서명한 토큰 사용).

### 3-1-7. 회원관리 목록에 성별 컬럼 추가 (2026-08-26)
`members.html` 회원 목록 테이블의 연락처↔가입일 사이에 "성별" 컬럼 추가. 백엔드 변경 없음 —
`/api/members` 응답에 `gender`가 이미 내려오고 있었고(`up.gender`, "남자"/"여자" 원본값), 테이블
렌더링(`renderTable`)에서만 라벨 매핑("남자"→"남성" 등) 후 표시.

### 3-1-8. 출석 관리 — "출석 현황" 탭 단순화(오늘 고정), 일별/주간/월별은 "회원별 현황"으로 이동 (2026-08-26)
"출석 현황" 탭에 있던 기간 탭(일별/주간/월별)과 날짜 선택 UI(주간 달력/월 네비게이션)를 완전히
제거하고, 항상 **오늘** 출석자 리스트만 보여주도록 단순화. 대신 그 기간 선택 기능 자체는
"회원별 현황" 탭으로 옮겨서, 이제 회원별 집계(출석횟수/이행률/마지막출석)를 일별/주간/월별
단위로 조회할 수 있게 됨(기존엔 월별 고정이었음).
- `attendance.html`: 상태 변수(`msCurrentPeriod`/`msSelectedDate`/`msWeekOffset`/`msMonthOffset`/
  `msWeekCountMap`)와 주간달력·월네비 마크업/함수(`setMemberPeriod`/`renderMemberWeekGrid`/
  `moveMemberWeek`/`moveMemberMonth` 등)를 패널1→패널2로 통째로 이동, id에 `ms` 접두어 부여해 충돌
  방지. 이행률 기준 회차도 기간에 따라 동적으로 바뀜(일별 1회/주간 7회/월별 30회 기준).
- `AttendanceMapper.memberMonthlyStats(yearMonth)`를 `memberPeriodStats(date, period)`로 교체 —
  `findAll`의 `periodFilter`(daily/weekly/monthly 날짜범위 계산)와 동일한 로직을 LEFT JOIN
  attendance의 ON 조건에 적용(`periodFilterA`, `a.` 접두 버전). `GET /api/attendance/member-stats`도
  `yearMonth` 파라미터 대신 `date`+`period`를 받도록 계약 변경(다른 소비처 없어 하위호환 불필요).
- `/api/attendance` 자체(패널1이 쓰는 목록 API)와 `periodFilter`는 변경 없음 — 패널1은 이제 항상
  `period=daily`, `date=오늘`로만 호출.
- 검증: 로컬 DB에 오늘 날짜 출석 1건을 임시로 넣어 daily/weekly/monthly 세 파라미터 조합 모두 실제
  HTTP 호출로 attendCount가 올바르게 집계되는 것 확인 후 삭제.

### 3-1-9. 대시보드 KPI 패널 → 상세 페이지 바로가기 링크 (2026-08-26)
`dashboard.html`의 KPI 카드/위젯에 각 지표를 자세히 볼 수 있는 페이지로 이동하는 `.kpi-card-link`
링크 추가. 백엔드 변경 없음(순수 템플릿).
- 전체 회원 수 → `/members`, 신규 회원(이번달) → `/members`(기존에 이미 있던 링크, 변경 없음)
- 만료예정 회원 → `/reregistration`("만료 및 예정 회원 관리"), 정지 회원 → `/reregistration`
  (같은 페이지로 연결 — 요청대로)
- 루틴 이행률 → `/attendance`("루틴/출석" 페이지)
- 피드백 대기 → 기존엔 `/feedback`(사실 "구독권/티켓 관리" 페이지라 이름과 안 맞았음)로 잘못
  연결돼 있던 것을 `/staff`("트레이너 관리")로 수정
- 실 어플사용 회원 → 요청대로 링크 보류(추가 안 함)
- 검증: 로컬 JWT로 인증된 상태에서 `/dashboard`를 실제로 렌더링해 6개 링크의 href가 모두 의도한
  경로로 나오는 것 확인.

### 3-1-10. 대시보드 "유효 회원" KPI 카드 신설 (2026-08-26)
"만료예정 회원"과 "정지 회원" 사이에 "유효 회원" 카드 추가, `/members`로 링크.
- **정의**: `membership` 테이블에 헬스장 이용권/라커이용권/운동복이용권(`type` = MEMBERSHIP/GROUP/
  PT/LOCKER/ITEM 전체 — 이 5개 타입이 곧 "헬스장 이용권+라커+운동복"이라 타입 필터가 따로 필요
  없음) 중 하나라도 `status IS NULL`(양도되지 않음) AND `end_date >= 오늘`인 게 있으면 유효회원.
- `DashboardMapper.memberStats`(기존 전체/정지 회원 수를 함께 계산하던 쿼리)에 `valid` 컬럼을
  상관 서브쿼리(`EXISTS`)로 추가 — 별도 엔드포인트 없이 기존 `/api/dashboard/members` 응답에
  얹었음(만료예정 회원과 달리 기간 선택 드롭다운이 없는 "지금 이 순간" 스냅샷이라 이 쪽이 자연스러움).
- 검증: 로컬 회원(`jisoo_kim`, PT 이용권 1건 2026-12-31 만료)으로 `valid:1` 확인 → 그 이용권의
  `end_date`를 임시로 과거로 바꿔 `valid:0`으로 바뀌는 것까지 실제 HTTP 호출로 확인 후 원상복구.

### 3-1-11. 회원관리 페이지 상단 집계 패널 신설 (2026-08-26)
`members.html` 상단(헤더 바로 아래, 검색바 위)에 전체회원/유효회원/만료회원/만료예정회원 4개
카드 추가 — 필터와 연동되지 않는 순수 표시용 스냅샷(대시보드의 2x2 요약카드 패턴과 동일한
자체 CSS, 클릭 인터랙션 없음).
- **유효/만료 정의는 대시보드 "유효 회원"(3-1-10)과 동일**: `membership` 전 타입(MEMBERSHIP/GROUP/
  PT/LOCKER/ITEM) 중 `status IS NULL AND end_date >= 오늘`인 게 하나라도 있으면 유효, 없으면 만료 —
  즉 **전체 = 유효 + 만료로 정확히 나뉜다**(상호 배타).
- **만료예정**은 유효회원의 부분집합(만료일이 오늘~+30일 이내) — 대시보드의 `countExpiringMemberships`
  로직과 같은 30일 기준을 그대로 씀(별도 기간 선택 없이 고정).
- **모집단은 이 페이지의 목록/전체건수(`MemberMapper.count`/`findAll`)와 동일하게 `role IN
  (MEMBER,TRAINER)`** — 대시보드(`role='MEMBER'`만)와는 모집단이 달라 숫자가 서로 다를 수 있음
  (의도된 차이 — 이 페이지의 "전체"는 이 페이지 목록의 전체 건수와 일치해야 하므로).
- `MemberMapper.summaryCounts(gymId)` 신규(단일 쿼리, 회원별 EXISTS/NOT EXISTS 합산이라
  DISTINCT 문제 없음) → `GET /api/members/summary`(`MemberApiController`) → `loadSummary()`.
- **부수 수정**: 같은 김에 `countExpiringMemberships`(대시보드가 쓰는 기존 쿼리)의
  `COUNT(*)`를 `COUNT(DISTINCT ms.user_id)`로 수정 — 원래는 한 회원이 이용권 여러 개(예: 이용권+
  라커)를 동시에 만료 앞두고 있으면 그 회원을 중복 카운트하던 잠재 버그였음. 이 페이지의 새
  "만료예정" 로직과 개념을 통일하는 김에 같이 바로잡음(대시보드 쪽 표시값도 이제 정확한 헤드카운트).
- 검증: `/api/members/summary` 실호출로 `total=2, valid=1, expired=1, expiring=0`이 로컬 DB의
  실제 회원 구성(회원 1명 유효 + 트레이너 1명 이용권 없음)과 정확히 일치하는 것, `valid+expired=
  total`인 것까지 확인.

### 3-1-12. 사이드바 "회원" 카테고리 4개 페이지에 엑셀 내보내기 추가 (2026-08-26)
회원관리(`members.html`)엔 이미 있던 "엑셀 내보내기"(`exportMembers()`→`GET /api/members/export`)를
같은 사이드바 카테고리의 나머지 3개 페이지에도 동일한 패턴으로 추가: 헤더 버튼 클릭 →
`window.location.href`로 새 export 엔드포인트 이동(화면의 현재 필터값을 쿼리파라미터로 그대로 전달,
페이지네이션 없이 최대 10만건) → 서버가 XSSFWorkbook으로 즉석 생성해 스트리밍.
- **만료 및 예정 회원 관리** — `GET /api/reregistration/export`(`ReRegistrationApiController`),
  현재 만료상태/기간 필터 반영. 컬럼: 회원명/전화번호/만료상태/만료일/등록상품/구독권등급.
  만료상태는 화면 JS `expiryInfo()`와 동일한 규칙(만료일 없음 또는 경과=만료, 30일 이내=만료예정,
  그 외=유효)을 서버에도 `expiryLabel()`로 동일하게 구현(회원관리 집계 패널·대시보드 "유효
  회원"과 컨셉은 같지만 계산 위치가 각각 달라 로직 자체는 파일별로 별도 구현돼 있음 — 기준을
  바꿀 일이 생기면 세 곳: `MemberMapper.summaryCounts`/`DashboardMapper.memberStats`/이 클래스의
  `expiryLabel` 모두 확인 필요).
- **이용권/PT**(`/pt`) — `GET /api/pt/export`(`PtApiController`), `lowStockFilter` 체크박스 상태
  반영. 컬럼: 회원명/전화번호/등급/담당트레이너/구매PT/서비스PT/PT잔여합계.
- **헬스장 가입 승인** — `GET /api/gym-join-requests/export`(`GymJoinRequestApiController`),
  현재 상태(승인대기/승인됨/거절됨/전체) 필터 반영. 컬럼: 회원명/전화번호/지점명/상태/요청일시.
- 검증: 4개 엔드포인트(회원관리는 기존) 중 신규 3개를 실제 HTTP 호출로 받아 유효한 OOXML(xlsx)
  파일인 것과 헤더/실데이터(PT 회원 "김지수"/"이현우", 가입승인 "LINK_Fit 본점"/"승인됨" 등)가
  올바르게 들어있는 것 확인 후 임시 파일 삭제.

### 3-1-13. 엑셀 회원 일괄등록 — 가입일 공란 처리 + "만료" 표기 개선 (2026-08-26)
3-1-3(이용종료일 공란=이미 만료)의 후속 보완. 두 가지를 고쳤다:
1. **가입일**: 엑셀엔 애초에 가입일 컬럼이 없는데, `members.html`의 "가입일"은 `DATE(u.created_at)`
   라 이관 작업을 실행한 "오늘"이 무조건 가입일처럼 표시되고 있었다(실제 가입일을 모르는 채로
   오늘 가입한 것처럼 왜곡). `user_profiles.join_date_unknown`(신규 컬럼,
   `docs/sql/user_profiles_join_date_unknown_20260826.sql`) 플래그를 추가해 `MyBatisMemberImportService`가
   새 회원을 만들 때만 1로 설정 — `MemberMapper.memberSelect`의 `join_date`가 이 경우 `NULL`을
   반환해 화면엔 공란("-")으로 표시됨. 수동 "회원 등록"(`MyBatisMemberService.save`)은 `Member`
   객체를 새로 만들 때 이 필드를 안 건드리므로 기본값 `false`가 유지되어 기존처럼 오늘 날짜가 정상
   표시됨(같은 `insertProfile` 매퍼를 공유하지만 호출부별로 값이 달라 영향 없음).
2. **"만료" 표기**: 3-1-3에서 "status 컬럼은 안 건드린다"고 했던 결정을 **번복** — 이용종료일
   미기재로 만든 행에 `status='EXPIRED_UNKNOWN'`을 부여하도록 바꿨다. 이 값은 TRANSFERRED처럼
   `status IS NULL` 조건(유효 이용권 판정에 코드베이스 전역에서 쓰이는 필터)에서 자동으로
   제외되므로, `MemberMapper.memberSelect`의 `membership_end` 서브쿼리가 이 행을 안 잡고
   `NULL`을 반환 → `members.html`의 "회원권만료일(잔여일)" 컬럼이 기존 `-` 대신 `(만료)`를
   표시하도록 폴백 문구 변경. 부수효과로 "이용중인상품(가격)"도 함께 `-`로 빠지는데(같은 서브쿼리
   패턴), 이건 "이 회원은 현재 추적 중인 유효 이용권이 없다"는 의미상 올바른 결과라 별도 처리
   안 함. 회원상세의 이용권 목록 탭에선 이 행이 `status !== 'TRANSFERRED'`라 평범한 구성요소로
   보이되(연장/양도/회수 버튼 정상 노출), 날짜 옆에 기존 로직(`ms.endDate < today`)이 그대로
   "(만료)"를 붙여줘 이미 일관돼 있었음.
   **주의**: `ReRegistrationMapper.xml`의 `membership_end`(`me` 서브쿼리, "만료 및 예정 회원 관리"
   페이지가 씀)는 `status IS NULL` 필터가 없어 이 변경의 영향을 안 받는다 — 그 페이지에서도
   이 행을 "이용권 없음"으로 취급하고 싶다면 그 서브쿼리에도 같은 필터를 추가해야 함(이번 요청
   범위 밖이라 건드리지 않음).
- `MyBatisMemberImportServiceTest`에 검증 추가: `status=='EXPIRED_UNKNOWN'`, `Member.joinDateUnknown
  ==true`.
- 검증: 실제 xlsx(openpyxl로 생성)를 `POST /api/members/import`로 업로드 → `GET /api/members`
  응답에서 `joinDate:null`, `membershipEnd:null` 확인, `GET /api/members/summary`에서 이 회원이
  `expired`로 집계되고 `valid`엔 안 잡히는 것까지 확인 후 테스트 데이터 전부 삭제.

### 3-1-14. 회원관리 목록 기본 정렬 — 유효회원 우선 + 이름 가나다순 (2026-08-27)
`members.html`의 회원 목록(`/api/members`, 필터 없는 최초 조회 포함)의 기본 정렬을
`ORDER BY u.created_at DESC`(최근 등록순)에서 **1순위 유효회원 우선, 2순위 이름 가나다순**으로
변경. 유효회원 판정은 3-1-10/3-1-13과 동일한 기준(`membership` 아무 타입이나 `status IS NULL
AND end_date >= CURDATE()`인 게 하나라도 있으면 유효) — `EXISTS(...)`를 정렬 키로 그대로 사용.
Hangul 완성형 음절은 utf8mb4 콜레이션에서 코드포인트 순서가 이미 가나다 순서와 일치해서
`up.name ASC`만으로 충분(별도 COLLATE 불필요).
- 검증: 이름이 다른 4명(유효 2명 "가영"/"라희", 무효 2명 "나리"/"다현")을 임시로 만들어
  `GET /api/members` 실호출로 `가영, 김지수, 라희, 나리, 다현, 이현우` 순서(유효자 먼저 가나다순,
  그다음 무효자 가나다순)가 정확히 나오는 것 확인 후 테스트 데이터 삭제.

### 3-1-15. "만료 및 예정 회원 관리"에서 회원 행 클릭 시 회원관리 상세모달 재사용 (2026-08-27)
`reregistration.html`의 표 행을 클릭하면 `members.html`의 회원 상세정보 모달(기본정보/이용권/
티켓/담당트레이너/메모/쪽지 탭)을 그대로 열 수 있게 함. 모달을 통째로 복제하지 않고,
`/members?openId=<userId>`로 이동시켜서 `members.html`이 로드되자마자 `openDetailModal(openId)`를
자동 호출하도록 했음(쿼리파라미터 하나 읽는 코드 몇 줄 추가가 전부 — 향후 상세모달이 바뀌어도
한 곳만 유지하면 됨).
- `reregistration.html`: `<tr>`에 `onclick="openMemberDetail(r.memberId)"` 추가(행 전체 클릭
  가능, `cursor:pointer`). 기존 "메모보기" 버튼(재등록 메모 전용, 회원 상세와는 다른 모달)은
  `event.stopPropagation()`을 추가해 행 클릭과 겹치지 않게 함.
- `members.html`: 초기화 스크립트 맨 끝에 `URLSearchParams`로 `openId` 쿼리파라미터를 읽어
  있으면 `openDetailModal(openId)`를 호출하는 3줄 추가. 다른 페이지에서도 같은 방식으로
  재사용 가능(예: 다른 목록 페이지에서도 회원 상세를 보여주고 싶으면 `/members?openId=`로
  보내기만 하면 됨).
- 검증: `/members?openId=jisoo_kim` 렌더링 결과에 해당 스크립트가 포함된 것, `/api/members/
  jisoo_kim`이 정상 응답하는 것, `reregistration.html`에 `openMemberDetail` 함수가 정상
  렌더링되는 것을 각각 실제 HTTP 응답으로 확인(브라우저 클릭 자체는 headless 환경이라 직접
  테스트 못 함 — 각 구성요소가 올바르게 연결되는지로 대신 검증).
- **(수정, 2026-08-27, 3-1-18 참고)** 위 방식은 실제로는 `/members`로 풀 페이지 이동이라 URL이
  바뀌고 사이드바도 "회원관리"로 바뀌어버려 "만료 및 예정 회원 관리 화면 위에서 바로 상세를
  본다"는 느낌이 아니었음 — iframe 모달 방식으로 교체함.

### 3-1-18. "만료 및 예정 회원 관리" — 페이지 이동 없이 회원 상세모달을 iframe으로 표시 (2026-08-27)
3-1-15에서 만든 `/members?openId=` 풀 페이지 이동 방식은 실제로 "전체 회원관리 페이지로 넘어간"
것처럼 보여서(주소창/사이드바가 바뀜), 요청에 따라 페이지 이동 없이 현재 화면 위에 상세모달만
뜨도록 iframe 방식으로 교체. `members.html`의 상세모달(기본정보/이용권/티켓/담당트레이너/메모/
쪽지)을 그대로 재사용하고, **모달 자체를 복제하지 않음**(피드백 기능이 화면마다 다른 구현을
타다가 어긋났던 사례가 있어, 같은 실수를 반복하지 않기 위해 의도적으로 복제 대신 재사용 방식을
선택).
- `members.html`: `?embed=1` 쿼리파라미터를 새로 지원. embed=1이면 사이드바/헤더/집계패널/
  검색바/목록(즉 `<main>` 전체와 `aside.sidebar`)을 숨기고 `body` 배경을 투명하게 만든 뒤
  목록 관련 초기 로드(`loadTrainerList`/`loadMembers`/`loadSummary`)를 건너뛰어 불필요한 API
  호출도 없앰. `openId`가 있으면 기존과 동일하게 상세모달을 자동으로 염 — 모달 자체(HTML/JS)는
  전혀 안 건드림. `closeDetailModal()`에서 embed 모드일 때 `window.parent.postMessage(
  'memberDetailModalClosed', origin)`으로 부모 창에 닫힘을 알림.
- `reregistration.html`: 화면 전체를 덮는 `#memberDetailFrameWrap`(`position:fixed;inset:0`)
  안에 `<iframe id="memberDetailFrame">`을 추가. `openMemberDetail(memberId)`가 더 이상
  `location.href`로 이동하지 않고 iframe의 `src`를 `/members?openId=<id>&embed=1`로 설정하고
  래퍼를 보여주기만 함 — 현재 페이지 URL/상태 그대로 유지됨. `message` 이벤트로 iframe이 보낸
  닫힘 신호를 받으면 래퍼를 숨기고 `src`를 `about:blank`로 리셋(다음에 열 때 상태 초기화),
  이용권/상태가 바뀌었을 수 있으니 `loadList()`/`loadSummary()`로 목록도 함께 새로고침.
- **`SecurityConfig.java` 변경 필요했음**: Spring Security 기본값이 `X-Frame-Options: DENY`라서
  iframe 자체가 브라우저에서 차단되는 걸 실제로 확인(`curl -D -`로 헤더 확인). `.headers(headers
  -> headers.frameOptions(frame -> frame.sameOrigin()))`로 same-origin 프레이밍만 허용하도록
  변경 — 외부 사이트의 클릭재킹은 여전히 차단되고, 같은 앱 안에서의 iframe 재사용만 허용됨.
- 검증: `/members?openId=...&embed=1` 응답 헤더에 `X-Frame-Options: SAMEORIGIN` 확인, embed=1일
  때/아닐 때 각각 렌더링 결과 확인(embed 아닐 때는 기존과 100% 동일 — diff로 확인), 전체
  테스트 스위트 통과 확인.
- **(수정, 2026-08-27)** 배포 후 실제로 클릭해보니 화면 변화 없이 이후 클릭만 안 먹는 버그
  발생 — headless Chrome을 직접 띄워(DevTools Protocol, `websocket-client`로 스크립트 제어)
  실제 클릭을 재현해서 원인을 확인함. 콘솔에 `ReferenceError: Cannot access 'detailMemberId'
  before initialization`이 찍혔는데, `let detailMemberId = null;` 선언이 `openDetailModal()`
  호출부(자동 openId 처리)보다 **아래**에 있었던 게 원인 — `let`은 `function` 선언과 달리
  호이스팅되어도 초기화되지 않아서, 선언 이전 시점에 참조하면 TDZ(Temporal Dead Zone)
  에러로 던져진다. 이 버그는 사실 3-1-15에서 `openId` 자동 열기를 처음 추가했을 때부터 있었던
  것인데, 그때는 curl/코드리뷰로만 검증하고 실제 브라우저로 클릭을 재현해본 적이 없어서
  발견되지 못했다(모달 자체는 `/members`에서 직접 진입할 땐 이미 페이지가 다 로드된 뒤라
  `let` 초기화가 끝나 있어서 문제가 없었고, `openId`로 "로드되자마자 자동으로 여는" 경로에서만
  터지는 버그였음). `let detailMemberId`/`let detailMemberData` 선언을 `openDetailModal()`
  자동 호출부보다 위로 옮겨서 해결.
- 이후 headless Chrome으로 실제 클릭→모달 표시(스크린샷으로 육안 확인)→X 버튼으로 닫기→
  postMessage로 부모 창에 알림→iframe이 `about:blank`로 리셋되고 목록이 그대로 남는 것까지
  전체 흐름을 실제로 재현해서 확인함(이번 세션부터는 이런 종류의 클릭 상호작용 버그를 다시
  코드리뷰만으로 넘기지 않기 위해, 이 환경에 Chrome.app이 있다는 걸 활용해 CDP로 직접
  검증하는 방법을 확립함).

### 3-1-19. "만료 및 예정 회원 관리" 요약 카드 — 대상자/등록완료/만료 → 대상자/예정자/만료 (2026-08-27)
기존 요약 카드는 "등록완료"(재등록 대상자 중 이미 갱신해서 `membership.end_date`가 오늘 이후인
사람)를 보여줬는데, 요청에 따라 목록 자체의 만료상태 배지(`expiryInfo()`: 만료예정/만료/유효,
`ReRegistrationMapper.expiryFilter`와 동일 기준 — 만료예정=0~30일 이내, 만료=만료일 경과 또는
이용권 이력 없음)와 맞춰 "예정자"(만료예정 인원)로 교체하고, "대상자"는 예정자+만료의 합으로
재정의(즉 "유효"한 사람은 대상자에서 제외됨 — 원래는 재등록 대상 전체 건수였음).
- `ReRegistrationMapper.summaryByMembership`: `completed`(end_date>=오늘) 대신 `expiring`(만료
  예정 기준, `DATEDIFF(end_date, CURDATE()) <= 30`)을 계산하도록 변경. `target`은 더 이상 SQL의
  `COUNT(*)`가 아니라 서비스 레이어(`MyBatisReRegistrationService.membershipSummary`)에서
  `expiring + expired`로 계산.
- `reregistration.html`: `sc-completed` 카드를 `sc-expiring`으로 교체(라벨 "등록완료"→"예정자",
  색상도 success→warning으로 — 목록의 "만료예정" 배지 색과 통일), `loadSummary()`가 `d.completed`
  대신 `d.expiring`을 읽도록 변경.
- 검증: 로컬에 만료(이용권 이력 없음)/예정(만료일 D+10)/유효(만료일 D+126) 회원 각 1명씩 임시로
  넣어서 `GET /api/reregistration/membership-summary` 실호출 → `{target:2, expiring:1, expired:1}`
  정확히 확인(유효 1명은 대상자에서 제외됨), 렌더링된 페이지에 새 라벨 반영 확인 후 테스트 데이터
  삭제. 전체 테스트 스위트 통과.

### 3-1-16. 내부 쪽지함(inbox.html) — 단체쪽지 발송 + 새 메시지 회원 이름검색 (2026-08-27)
- **단체쪽지 보내기**: 헤더에 버튼 추가, 성별(전체/남자/여자)·이용상태(전체/유효/만료) 두 필터를
  AND로 조합해 대상 인원을 실시간 미리보기(`GET /api/inbox/broadcast-count`)한 뒤, 확인
  다이얼로그로 인원수를 보여주고 `POST /api/inbox/broadcast`로 대상 전원에게 개별 `crm_messages`
  행을 하나씩 발송(반복 insert — 대상 인원이 수백 명 수준이라 배치 최적화 없이도 충분).
  `CrmMessageMapper.findBroadcastTargets`/`countBroadcastTargets` 신규 — "유효" 판정은 3-1-10/
  3-1-14와 동일 기준(membership 아무 타입이나 status IS NULL AND end_date>=오늘).
- **새 메시지 — 회원 이름 검색**: 수신자 유형을 "회원"으로 선택하면 기존 "수신자 ID 직접입력"
  칸이 숨겨지고 이름 검색창이 나타남(staff.html/feedback.html에서 이미 쓰던 검색-선택 패턴 재사용,
  `/api/members?keyword=` 그대로 활용). 검색 결과 목록에 이름과 함께 성별·전화번호를 보여주고,
  선택하면 내부적으로 `cReceiverId`(hidden 아님, 그냥 다른 그룹으로 안 보일 뿐)에 채워짐 — 발송
  로직(`sendMessage()`)은 그대로. "관리자"/"트레이너" 유형은 기존처럼 ID 직접입력 유지(요청
  범위가 회원 검색이었음).
- **확인 요청 사항 — 회원관리 "쪽지" 탭에서 보낸 쪽지가 쪽지함에서도 관리되는지**: 실제 발송 후
  대조한 결과,
  - **회원관리(`members.html`) "쪽지" 탭에서 보낸 메시지는 `/inbox`의 "보낸 메시지함"에 그대로
    보임** ✅ — 둘 다 같은 `sender_id`(로그인한 관리자의 crm_users.id)를 쓰기 때문. 실제로
    보내서 확인함.
  - **회원이 앱에서 보낸 답장은 `/inbox`의 "받은 메시지함"에 안 보임** ❌ — 앱 쪽(`GymMessageService`,
    2026-08-26 구현)이 회원 답장을 특정 관리자가 아니라 지점 전체 앞으로 보내는 개념이라
    `receiver_id='GYM'`(고정 문자열)로 저장하는데, `/inbox`의 "받은 메시지함"은 로그인한 그
    관리자 개인의 `receiver_id`(=crm_users.id)로 필터링해서 구조적으로 안 맞음. 회원 답장은
    현재 **회원상세(`members.html`)의 "쪽지" 탭에서만** 볼 수 있음(그쪽은 회원 기준으로 조회해서
    문제없음). 실제로 회원 답장을 흉내낸 테스트 메시지를 넣어 두 화면 모두 확인함 — `/inbox`엔
    0건, 회원상세 쪽지 탭엔 정상 표시.
  - 이 gap을 고칠지는 별도 요청이 필요 — 고치려면 `/inbox`의 "받은 메시지함"을 "이 관리자에게
    온 것"이 아니라 "이 지점 회원들이 보낸 것 전체"로 개념을 바꿔야 해서 설계 판단이 필요함
    (여러 관리자가 있는 지점이면 "받은 메시지함"이 공유 사서함이 되는 셈).

### 3-1-17. "받은 메시지함" → "읽지않은 쪽지함"으로 개편, 메신저 개념 도입 (2026-08-27)
- 3-1-16에서 확인된 gap(회원의 앱 답장이 `receiver_id='GYM'`으로 저장되어 로그인 관리자 개인
  기준 "받은 메시지함"에서 구조적으로 누락되던 문제)을 해결하면서, 아예 쪽지함을 메신저 개념으로
  개편. "받은 메시지함"이라는 탭 자체를 없애고 **"읽지않은 쪽지함"**으로 대체:
  - 특정 관리자 개인이 아니라 **이 지점 전체 기준**으로, 회원이 보낸 미확인(`is_read=0`) 쪽지를
    **회원별로 그룹핑**해서 대화목록(메신저 스타일, 1행=1회원)으로 보여줌 — 개별 메시지 리스트가
    아님. `CrmMessageMapper.findUnreadMemberThreads`(상관 서브쿼리로 회원별 최근 내용/시각/미확인
    개수 집계 — `crm_messages.id`가 시간순 아닌 UUID라 MAX(id) 조인 방식 대신 사용) 신규.
  - 행을 클릭하면 단일 메시지 팝업이 아니라 **회원관리 상세모달의 "쪽지" 탭으로 바로 이동**
    (`/members?openId=<id>&tab=msg`) — 3-1-15에서 만든 `openId` 네비게이션 패턴을 `tab` 파라미터로
    확장(`openDetailModal(id, tab)`이 하드코딩됐던 `switchDetailTab('info')`를 대체). 이동한
    화면(`GET /api/members/{id}/messages`)이 기존에도 열람 시 `markMemberMessagesRead`를 호출하고
    있어서, 스레드로 들어가는 것만으로 자연스럽게 읽음 처리됨 — 별도의 "읽음" API 연동 불필요.
  - 배지(`GET /api/inbox/unread-count`)와 "모두 읽음"(`PATCH /api/inbox/messages/read-all`)도 옛
    "로그인 관리자의 receiver_id" 기준에서 "이 지점의 미확인 회원 메시지 전체" 기준으로 변경
    (`countUnreadMemberMessages`/`markAllMemberMessagesRead`). 기존 `findReceived`/`countReceived`/
    `countUnread`/구버전 `markAllRead(gymId, receiverId)`는 이 3곳에서만 쓰이던 게 확인되어 완전히
    제거(다른 호출부 없음, grep으로 확인).
  - `markRead(id, gymId)`(단건 읽음)는 "보낸 메시지함"/"공지사항" 탭의 기존 단일 메시지 보기
    팝업(`viewMsg`)에서 계속 쓰여서 그대로 유지.
  - 실제 서버 기동 후 JWT로 인증해 회원(`jisoo_kim`) 답장 2건을 직접 INSERT하여 검증: 그룹핑된
    응답에 회원 1명·미확인 2건으로 정상 집계, 회원상세 쪽지 탭을 열람하는 API를 호출하니 미확인
    쪽지함에서 정상적으로 사라짐(0건) — 확인 후 테스트 데이터 삭제.

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
  이미 완료된 계정이 다시 호출하면 거부(이 엔드포인트는 최초 1회 전용).
- 실제 로그인(`admin/linkonfit`) → `/dashboard` 리다이렉트 → `/api/dashboard/**` 423 →
  `POST /api/auth/change-password-first` 검증 실패 3종 + 성공 → 플래그 해제 후 `/dashboard`
  정상 접근 → 새 1차 비밀번호로 재로그인까지 end-to-end 실제 호출로 검증함(테스트용 지점
  LF99는 검증 후 삭제).
- 확인 시점 기존 gym_admin 계정 0건이라 소급 UPDATE는 하지 않음 — 이미 발급된 계정 중
  기본 비밀번호를 안 바꾼 게 있다면 개별적으로 플래그를 켜야 함(마이그레이션 파일 주석 참고).
- **`/settings` 화면 — 온보딩 이후 1/2차 비밀번호 변경(2026-08-24)** — `change-password-first`는
  최초 1회 전용이라, 온보딩을 마친 뒤 비밀번호를 또 바꾸고 싶으면 방법이 없던 문제를 메움.
  `POST /api/auth/password`(신규, 1차 비밀번호만 변경, 최소 4자리) +
  `POST /api/auth/second-password`(기존 엔드포인트에 검증 강화 — 이전엔 빈 값만 아니면 통과했는데,
  이제 `change-password-first`와 동일한 정규식으로 2차 비밀번호 형식을 강제함). `settings.html`의
  "보안" 카드에 1차/2차 변경 폼을 나란히 둠. 로컬에서 오답/성공 케이스 실제 호출 + 변경된
  1차 비밀번호로 재로그인까지 검증함(테스트용으로 건드린 `admin` 계정의 비밀번호/2차 비밀번호는
  검증 후 원래 상태로 복원).

### 4. 로그인 쿠키에 `Secure`/`SameSite` 미설정 + CSRF 전역 비활성
`AuthApiController`에서 쿠키에 `HttpOnly`만 설정하고 `Secure`/`SameSite`는 없음. `SecurityConfig`도
CSRF를 전역 비활성화(`csrf.disable()`)한 상태라 브라우저 기본 동작에만 의존하고 있다.

### 5. 미사용(dead) 코드
- ~~`service/mock/` 패키지~~ — 2026-07-15 삭제 완료 (아무 곳에서도 참조하지 않는 것 확인 후 제거, 빌드 통과 확인)
- `service/AdminUserDetailsService.java` — 위 인증 방식 참고, 현재 아무 데서도 호출되지 않음 (정리 대상)

### 6. (수정 완료, 2026-08-24) 로그아웃 버튼이 전 페이지에서 실제로 동작하지 않던 문제
`fragments/sidebar.html`의 `th:fragment="sidebar(activePage)"`가 `<aside>...</aside>`까지만
감싸고 있었고, 그 뒤에 있는 `<script>`(`doLogout()`, `loadUnreadCount()` 등)와 2차 비밀번호
확인 모달(`#lockUnlockModal`)은 그 fragment 경계 밖에 있었다 — `th:replace="~{fragments/sidebar
:: sidebar(activePage)}"`로 포함하는 모든 페이지에 로그아웃 버튼 마크업은 보이지만 `doLogout()`
함수 자체가 없어(존재하지 않는 함수 호출, 브라우저 콘솔에만 조용히 에러) 클릭해도 아무 일도
안 일어났음. 2차 비밀번호 확인도 클라이언트 모달 없이 항상 `LockedCategoryInterceptor`의
서버 렌더링 폴백 페이지로만 떨어지고 있었던 것(기능은 동작하지만 UX가 원래 의도와 다름).
`<aside>`/모달/`<script>` 전체를 `<th:block th:fragment="sidebar(activePage)">`로 다시 감싸서
수정 — 실제로 렌더링된 페이지에 `function doLogout()`이 포함되는지, 여러 페이지(대시보드/회원
관리)에서 공통으로 되는지 확인함. 최소 `5f10142`(트레이너 계정 관리 개편) 커밋 이후부터 있던
버그로 추정 — 이번 세션에서 만든 코드가 원인은 아님.

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
- **관리자페이지 전용 계정 — 매니저/직원(2026-08-24)** — `/settings` "관리자페이지 계정 관리"
  카드에서 지점 관리자가 직접 추가 CRM 로그인 계정을 만들 수 있다. `crm_users.role`에
  `manager`/`employee` 추가(`docs/sql/crm_user_manager_employee_role_20260824.sql`).
  - **매니저** — 관리자(gym_admin)와 동등한 권한. 코드상 역할별 접근 제한 자체가 없어서
    (위 "알려진 이슈 2번" 참고) 별도 구현 없이 자동으로 동등해짐. 지점코드 발급 계정과
    동일하게 `must_change_password=1`로 생성돼 최초 로그인 시 1/2차 비밀번호를 직접 설정.
  - **직원** — `LockableCategories`에 정의된 2차 비밀번호 잠금 대상 카테고리 중 **대시보드만
    예외**(2026-08-24 수정 — 처음엔 대시보드도 막았다가, 랜딩 화면이라 항상 보여야 한다는
    요청으로 뺌)이고 나머지(회원/PT/재등록/출석/상품/피드백/상담/받은메시지함/CS/공지/직원
    관리/CRM매출/매출/설정)는 영구적으로 볼 수 없다. `LockedCategoryInterceptor`가 role이
    employee면 `category`가 "dashboard"가 아닌 한 `lockedCategorySet()`/언락 쿠키를 아예
    확인하지 않고 항상 차단 — 본인이 2차 비밀번호를 설정하고 실제로 검증에 성공해도
    해제되지 않는 것까지 실제로 확인함. 차단 시 메시지는 "권한이 없습니다."로 통일, 페이지
    직접 접근 시 뜨는 안내 화면의 이동 링크는 이제 안전하게 `/dashboard`를 가리킴(예전엔
    대시보드도 막혀있어서 `/lockers`로 대체했었음). 사이드바 쪽에서 막힌 메뉴를 아예 숨기는
    처리는 아직 안 함(서버 차단은 완전하지만, 클릭 후 막히는 UX라 프론트 폴리싱 여지가 있음).
  - `CrmUserMapper.insert()`에 `must_change_password` 컬럼을 추가했음 — 트레이너 승격 등
    이 메서드를 쓰는 다른 경로도 함께 영향받으니 새 호출부 추가 시 주의.
  - 계정 아이디 중복 확인은 `is_active` 무관하게 전체를 본다(`uq_crm_gym_username`이
    비활성 계정에도 걸려있어서) — `existsByGymIdAndUsernameAnyStatus`.
  - 실제 로그인 계정 생성 → 최초 로그인 강제 플로우 → 매니저 전체 접근/직원 전체 차단
    (언락 쿠키 우회 시도 포함) → 비활성화 시 로그인 차단 → 재활성화까지 로컬에서 전부
    실제 호출로 검증함(테스트 계정은 삭제).
- **회원 일괄 등록(엑셀, 2026-08-24)** — 타사 헬스장 CRM에서 회원 정보를 이관해올 때 사용.
  구독권/인앱 티켓 같은 앱 전용 데이터는 대상이 아니고, 순수 헬스장 정보(회원 기본정보 +
  이용권(회원권/PT/그룹) + 락커 + 운동복)만 다룬다. 두 가지 설계(① 타사 엑셀 그대로 받아 파싱
  vs ② 우리 템플릿 제공) 중 ②를 선택함 — 벤더마다 제각각인 포맷을 우리가 매번 파싱하는 것보다
  포맷 하나만 관리하는 쪽이 유지보수 비용이 훨씬 낮다고 판단.
  - `GET /api/members/import/template` — 헤더 21개 컬럼(A~U) + 예시 행 1개, 성별/회원권구분
    컬럼엔 엑셀 드롭다운(데이터 유효성 검사) 적용.
  - `POST /api/members/import` — 컬럼은 **헤더 텍스트가 아니라 위치(A~U)로 파싱**한다(사용자가
    헤더 문구를 바꿔도 안전하게 동작). 행 단위로 이름+연락처 중복(`existsByNameAndPhone`) 검사
    후 회원 생성, 이용권/락커/운동복은 각각 독립적으로 시도 — **회원 생성만 성공하면 이후
    단계가 일부 실패해도 그 행은 `success:true`로 보고한다**(재업로드 시 "이미 존재"로 막혀
    다시 시도할 방법이 없어지는 것을 방지, 실패한 항목은 회원 상세정보에서 수동으로 이어서
    등록). 완전 실패(이름/연락처 누락, 중복)만 `success:false`.
  - **이름+연락처가 기존 앱 회원과 일치하면 "앱 활성화"(2026-08-24 수정)** — 원래는
    `existsByNameAndPhone`(지점 무관 전역 매칭)이 true면 그냥 건너뛰기만 하고 끝이라, 이미
    앱에 가입한 회원이 새 지점 엑셀에 실려 있어도 그 지점에 전혀 연결되지 않는 문제가 있었음.
    지금은 `findIdByNameAndPhone`으로 실제 user_id를 찾아 `existsInGym(id, gymId)`으로
    "이 지점"에 이미 연결됐는지만 확인 — 아직 안 됐으면 새 회원을 만들지 않고 그 계정을
    `user_gym`에 연결(uq_user_gym이 (user_id, gym_id)라 한 회원이 여러 지점에 동시 소속 가능)
    한 뒤 이용권/락커/운동복은 그 기존 user_id로 이어서 등록. 이미 이 지점에 연결된 경우만
    진짜 중복으로 거부. 실제 회원(jisoo_kim)으로 신규 지점 업로드 → 연결 → 재업로드 시
    "이미 등록됨" 순서로 실제 검증함.
  - `MemberMapper.insertMembership()`을 `MemberService.addMembership()`(오늘 날짜 기준으로
    기간을 새로 계산하는 "신규 구매" 흐름)이 아니라 **직접** 호출한다 — 이관 데이터는 과거에
    이미 정해진 시작/종료일을 그대로 보존해야 하기 때문. PT는 종료일이 없으면
    `MyBatisMemberService.DEFAULT_UNLIMITED_DURATION_DAYS`와 동일한 3650일 무기한 처리를
    자체 상수로 복제.
  - 락커는 락커 **번호**만으로 매칭한다(구역까지는 못 맞춤) — `LockerMapper.findAvailableByGymAndNumber`
    로 해당 지점에서 그 번호의 미배정 라커를 찾고, 없거나 이미 배정돼 있으면 그 항목만 건너뛴다.
  - 담당 트레이너는 이름 텍스트로 받되 **자동 매칭하지 않는다** — 우리 트레이너 계정과 자동
    연결할 근거가 없어, 안내 메시지만 남기고 상세정보 화면에서 수동 지정하도록 함.
  - 실사용 시나리오(정상행/최소필드행/PT행/잘못된 값/필수값 누락/중복 재업로드)로 로컬에서
    실제 업로드 테스트 완료 — 처음엔 PT 행에서 `end_date NOT NULL` 제약 위반이 났었는데(PT는
    종료일 없이 올 수 있다는 걸 놓침) 위 무기한 처리로 수정 후 재확인함.
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
| `GET /inbox` | 내부 쪽지함 (CRM, 읽지않은/보낸/공지) |
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
| `POST /api/auth/password` | AuthApiController | 1차(로그인) 비밀번호 변경 — 설정 화면, 온보딩 이후 상시 사용 |
| `/api/dashboard/**` | DashboardApiController | 통계 (members/classes/attendance/revenue/consults/crm-summary) |
| `/api/members/**` | MemberApiController | 회원 CRUD, 상태·등급·유형 변경, 티켓 조회·충전, 메모·태그, 트레이너 지정 |
| `GET /api/members/import/template` | MemberApiController | 회원 일괄 등록용 엑셀 템플릿 다운로드 |
| `POST /api/members/import` | MemberApiController | 타사 CRM 이관 — 템플릿 엑셀 업로드로 회원+이용권/락커/운동복 일괄 등록 |
| `/api/crm-accounts/**` | CrmAccountApiController | 관리자페이지 전용 계정(매니저/직원) 목록·생성·활성화 토글 |
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
| `/api/inbox/**` | CrmInboxApiController | CRM 내부 쪽지 CRUD, 읽지않은 쪽지함(회원별 그룹), 단체쪽지 |
| `/api/crm-sales/**` | CrmSalesApiController | CRM 매출 목록, 요약, 목표 관리, 내보내기 |
| `/api/cs/tickets/**` | CsTicketApiController | CS 티켓 관리, 담당자·상태·응답 처리 |
| `/api/announcements/**` | AnnouncementApiController | 공지사항 CRUD, 발송 처리 |
| `/api/members/tickets/logs` | MemberApiController | 티켓(구독권/원포인트/피드백/사진/영상) 사용내역 조회 |
| `/api/lockers/zones/**` | LockerApiController | 라커 구역 CRUD(가로/세로/총개수) |
| `/api/lockers/**` | LockerApiController | 라커 목록 조회, 배정(assign) — 해제는 `/api/memberships/{id}` DELETE 재사용 |
| `/api/gym-join-requests/**` | GymJoinRequestApiController | 헬스장 가입 신청 목록, 승인/거절 |
| `/api/payment-methods/**` | PaymentMethodApiController | 결제수단 목록 조회·추가 |
| `/api/product-packages/**` | ProductPackageApiController | 상품 패키지 CRUD |
