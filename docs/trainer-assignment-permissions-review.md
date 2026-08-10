# 트레이너 지정 및 권한(Authorization) 검토

> 점검일: 2026-07-26
> 배경: 매출 연동 작업 중 `crm_sales.trainer_id`를 채우려다, 트레이너를 가리키는 ID가
> 두 군데(`users.user_id` / `crm_users.id`)로 나뉘어 있고 서로 연결할 방법이 없다는 것을
> 발견. 이를 계기로 "트레이너 지정"과 "권한" 전반을 코드 기준으로 재검증했다.

---

## 요약

| 영역 | 상태 | 심각도 |
|---|---|---|
| 회원 ↔ 트레이너 지정 (`user_profiles.trainer_id`) | 정상 동작 | - |
| 회원 ↔ 트레이너 지정 (`crm_member_assignments`, 중복 시스템) | 죽은 코드 + 실사용 시 항상 실패 | 중 |
| PT 관리 페이지 "담당 트레이너" 표시 | **항상 공백으로 표시되는 실제 버그** | 높음 |
| `crm_users`(CRM 로그인 계정) 생성 기능 | **존재하지 않음** — 트레이너는 CRM에 로그인 자체가 불가능 | 높음 |
| 트레이너 CRM 대시보드 피드백/재등록 카운트 | 항상 0으로 표시됨 (원인: 위 항목) | 중 |
| 역할 기반 인가(Authorization) | **전혀 구현되지 않음** — 로그인만 하면 전체 API 호출 가능 | 높음 |
| 2차 비밀번호(카테고리 잠금) 기능 | 본인 계정으로만 스코핑되어 안전 확인 | - |

---

## 1. "트레이너 지정"이 서로 다른 두 테이블로 이중 구현되어 있음

`MemberApiController`에 담당 트레이너를 지정하는 엔드포인트가 **두 개** 존재한다.

```java
// 149줄 부근 — 실제 앱이 쓰는 필드
@PatchMapping("/{id}/assigned-trainer")   // → user_profiles.trainer_id

// 280줄 부근 — CRM 전용 별도 테이블
@PutMapping("/{id}/trainer")              // → crm_member_assignments
```

- `PATCH .../assigned-trainer` → `user_profiles.trainer_id` 갱신. lof-front(모바일 앱)가
  PT/OT 일정, 루틴 배정 등에서 실제로 참조하는 **유일한 진짜 값**. `staff.html`의 "회원 배정"
  기능이 이 엔드포인트를 호출하며 정상 동작한다.
- `PUT .../trainer` → `crm_member_assignments` 테이블에 upsert. 이 테이블은
  `trainer_id`가 `crm_users.id`(CRM 로그인 계정)를 가리키도록 설계되어 있는데, 아래 2절에서
  보듯 **트레이너용 crm_users 계정 자체가 생성될 방법이 없어 이 값을 채울 수 있는 트레이너가
  없다.** 실제로 프론트엔드 어디에서도 이 엔드포인트를 호출하는 코드가 없음(grep 결과 0건) —
  완전히 죽은 코드다.

### 실제 버그: PT 관리 페이지의 "담당 트레이너" 컬럼이 항상 비어있음

`MemberMapper.xml`의 `findPtMembers` (PT 관리 페이지가 사용)는 여전히 죽은 `crm_member_assignments`
경로로 트레이너 이름을 조회한다.

```xml
<!-- MemberMapper.xml:364 -->
LEFT JOIN crm_member_assignments cma ON cma.member_id = mt.user_id
LEFT JOIN crm_users cu  ON cu.id = cma.trainer_id
LEFT JOIN user_profiles tup ON cu.app_user_id = tup.user_id
```

`crm_member_assignments`가 항상 비어있으므로 `trainerName`은 항상 NULL이다. **PT 관리
페이지에서 회원별 담당 트레이너가 실제로는 지정되어 있어도 화면에는 안 보인다.**

같은 문제를 이미 한 번 겪고 고친 흔적이 `StaffMapper.xml`에 남아있다 (트레이너 대시보드의
담당 회원 수 집계):

```xml
<!-- 담당 회원 수/목록은 user_profiles.trainer_id 기준(앱에서 실제로 쓰는 필드)으로 집계.
     crm_member_assignments는 트레이너의 crm_users 행이 있어야 매칭되는데, 현재 트레이너
     계정은 crm_users에 대응 행이 없어 항상 0/빈 목록만 나오는 문제가 있었음. -->
```

→ 이 수정과 동일한 패턴(`user_profiles.trainer_id` + `users`/`user_profiles` 직접 조인)을
`findPtMembers`에도 적용하면 해결된다.

---

## 2. `crm_users`(트레이너 CRM 로그인) 계정을 만드는 기능이 없음

- `CrmUserMapper.insert()`가 매퍼에 정의는 되어 있으나, 어떤 서비스/컨트롤러도 호출하지 않는다
  (grep 결과 호출부 0건). 즉 **애플리케이션을 통해서는 새 CRM 계정을 만들 방법이 아예 없다.**
- 로그인(`AuthApiController.login`)은 기존 `crm_users` 행을 조회만 할 뿐 자동 생성(provisioning)
  로직이 없다.
- 로컬 DB 기준 `crm_users`에는 수동으로 심어둔 super_admin 계정 1건만 존재하고, `app_user_id`도
  NULL이다. 즉 지금 상태로는 **트레이너가 CRM에 로그인하는 것 자체가 불가능**하다.
- 이 때문에 `crm_users.app_user_id`(→ `users.user_id` 연결) 또는 `crm_users.role='trainer'`에
  의존하는 기능이 전부 실질적으로 죽어있다:
  - 트레이너 대시보드의 `pendingFeedback`/`pendingReregistration` 카운트 (`StaffMapper.xml`
    `findDashboard`) — 항상 0
  - `crm_sales.trainer_id`, `crm_feedback_requests.trainer_id`, `crm_re_registration.assigned_to`,
    `crm_cs_tickets.assigned_to` 등 CRM 테이블 전반의 담당자 필드 — 채울 방법이 없어 전부 NULL
  - (참고: 이번에 만든 회원관리→매출 자동 연동에서 `crm_sales.trainer_id`를 의도적으로 NULL로
    남긴 이유가 바로 이것)

**결론**: "트레이너가 CRM에 로그인해서 자기 대시보드/일정/피드백을 처리한다"는 설계 의도 자체는
스키마와 JWT 역할 클레임에 남아있지만, 실제로 그 계정을 만드는 관리 기능이 빠져 있어 지금은
**super_admin 1명만 CRM을 사용할 수 있는 상태**다.

---

## 3. 역할 기반 인가(Authorization)가 전혀 구현되어 있지 않음

`SecurityConfig`:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/login", "/api/auth/login", ...).permitAll()
    .anyRequest().authenticated()   // ← 이게 전부
)
```

- `@PreAuthorize`, `hasRole`, `hasAuthority`는 전체 코드베이스에서 0건 (grep 확인).
- `CrmUserDetails.getAuthorities()`가 `ROLE_SUPER_ADMIN`/`ROLE_GYM_ADMIN`/`ROLE_TRAINER`를
  만들어주긴 하지만 아무도 검사하지 않는다.
- `role` 값은 로그인 응답(`/api/auth/login`, `/api/auth/me`)에 그대로 내려줄 뿐, 서버 어디에서도
  분기 로직에 쓰이지 않는다(grep 결과 `getRole()` 호출부는 로그인/me 응답 조립부 3곳뿐).
- **실질적 의미**: (2번 항목 때문에 지금은 이론상의 문제지만) 만약 트레이너 CRM 계정이
  생긴다면, 그 트레이너는 로그인만 하면 직원 관리, 헬스장 설정, 다른 회원의 결제/매출 정보,
  2차 비밀번호 설정 화면(자기 것만 수정 가능하니 안전) 등 **super_admin과 동일하게 모든 API를
  호출할 수 있다.** 역할 구분이 UI 표시(role 뱃지)에만 쓰이고 실제 접근 제어에는 전혀 반영되지
  않는다.

CLAUDE.md의 "알려진 이슈 2번"에 이미 기록되어 있던 문제이며, 이번 검토로 실제 영향 범위
(트레이너 계정이 생기는 순간 바로 노출됨)를 재확인했다.

---

## 4. 확인 결과 문제없는 부분: 2차 비밀번호 / 카테고리 잠금

`AuthApiController`의 `/second-password`, `/second-password/verify`, `/security-settings`는
전부 `principal.getId()`(본인)로만 스코핑되어 있고 대상 사용자 id를 파라미터로 받지 않는다.
타인 계정의 2차 비밀번호를 조회/변경/우회할 수 있는 경로는 없음을 확인했다.

---

## 권장 조치 (우선순위순)

1. **`findPtMembers`의 담당 트레이너 조인을 `user_profiles.trainer_id` 기준으로 교체** — 가장
   작고 확실한 수정, PT 관리 페이지 버그 즉시 해결.
2. **`PUT /{id}/trainer` + `crm_member_assignments` 경로 정리** — 죽은 코드이므로 삭제하거나,
   `user_profiles.trainer_id`와 동기화되도록 재설계.
3. **`crm_users` 계정 생성 기능 추가 여부 결정** — 트레이너를 실제로 CRM에 로그인시킬 계획이
   있는지에 따라 (a) 계정 발급 UI를 새로 만들거나 (b) 현재처럼 super_admin 전용으로 남기고
   관련 미사용 코드(`crm_member_assignments`, 트레이너 대시보드의 피드백/재등록 카운트 등)를
   정리.
4. **역할 기반 인가 설계/구현** — 최소한 트레이너 계정이 생기기 전에 처리 필요. `hasRole`
   기반으로 관리 기능(직원관리/설정/타인 데이터)을 super_admin·gym_admin으로 제한하는 것부터
   시작 권장.
