# 다지점(멀티 브랜치) 확장 시 문제점 점검

> 점검일: 2026-07-21
> 배경: 헬스장 휴무/영업중 상태가 항상 특정 값으로 보이는 이슈를 확인하던 중,
> `gym_setting` 테이블이 지점 구분 없이 단일 행(`id=1`)만 사용하는 것을 발견.
> 이를 계기로 lof-backend / lof-admin 전체에서 "2번째 지점이 생기면 무엇이 깨지는지"를
> DB 스키마 + 컨트롤러/매퍼 코드 기준으로 재검증했다.
>
> ~~현재는 지점이 `LF01` 1개뿐이라 아래 문제들이 실사용에 영향을 주지 않는다.~~
> **[2026-08-20 갱신] 더 이상 사실이 아님 — 이 문서 작성 5일 뒤인 2026-07-26에
> `docs/sql/gym_lf02_seed_20260726.sql`로 LF02(강남점, `gym.id=101`)가 실제로 생성되어
> 아래 문제들이 이미 실사용 조건에 들어와 있다.** 2026-08-20 DB 전수조사로 아래 §1의
> 10개 테이블이 여전히 gym_id 없음을 재확인했고, 이 문서 작성 이후 신설된 테이블 중
> 같은 문제를 가진 4개(`product_package`/`class_session`/`member_freeze`/`staff_attendance`)를
> 추가로 발견했다. **§5-1의 스키마 마이그레이션 SQL을 `docs/sql/gym_id_backfill_20260820.sql`로
> 작성해둠 — 아직 실행 전, 검토 후 적용 필요.**

---

## 1. DB 스키마 자체에 지점 구분 컬럼이 없는 영역 (가장 심각)

코드 수정만으로 해결 불가 — 스키마 마이그레이션(컬럼 추가 + 백필)이 선행되어야 함.

`INFORMATION_SCHEMA.COLUMNS`에서 `gym_id`/`branch_code` 컬럼을 전수 조사한 결과,
실제로 지점 구분이 가능한 테이블은 아래 두 종류뿐이다.

- `user_gym` (회원-지점 매핑 테이블)
- `crm_*` 계열 테이블 전체 (2026-06-08 이후 신규 도입분: `crm_users`, `crm_sales`,
  `crm_re_registration`, `crm_feedback_requests` 등)
- `admin_user.gym_id` (어드민 로그인 계정)
- `gym.branch_code` (지점 마스터 테이블 자신)

**반대로 `gym_id` 컬럼이 전혀 없는 테이블** (= 앱의 핵심 레거시 도메인 대부분):

| 테이블 | 용도 | 영향 |
|---|---|---|
| `product` | 상품/이용권 카탈로그 | 지점별로 다른 상품 구성·가격 불가, 전 지점 동일 카탈로그 공유 |
| `membership` | 회원권 구매 이력 | 어느 지점에서 결제했는지 구분 불가 |
| `member_tickets` | 티켓(피드백/원포인트/사진/영상) 잔량 | 지점 구분 없이 회원 단위로만 관리됨 (회원이 한 지점 소속이면 실질 영향은 적음) |
| `trainer_schedules` | 트레이너 PT/OT 일정 | 트레이너가 여러 지점 소속일 경우 일정이 지점 무관하게 섞임 |
| `attendance` | 출석 기록 | 지점별 출석 집계 불가 |
| `consult` | 상담 기록 | 지점별 상담 집계 불가 |
| `sale` | 매출 내역 | 지점별 매출 정산 불가 |
| `message_conversation` / `chat_message` | 회원-트레이너 쪽지 | 1:1 대화라 실질 영향은 적으나 지점 통계 집계 시 구분 불가 |
| `gym_setting` | 헬스장 운영시간/공지/영업상태 | 아래 2절 참고 — 단일 행 고정이라 지점 간 데이터가 아예 공유됨 |
| `gym_banner` | 배너 관리 | 전 지점 동일 배너 공유 |
| `gym_holiday` | 휴일 설정 | 전 지점 동일 휴일 공유 |

---

## 2. 회원 앱(lof-backend) — 오늘 새로 발견한 실제 오동작

### 2-1. `gym_setting` 단일 행(`id=1`) 고정

- 관련 파일:
  - `lof-backend/src/main/resources/mapper/GymSettingMapper.xml` (`findOne` — `WHERE id = 1`)
  - `lof-admin/src/main/resources/mapper/GymSettingMapper.xml` (`find`/`upsert`/`updateOpenStatus` 전부 `id = 1` 하드코딩, `upsert`의 INSERT문도 `VALUES (1, ...)`로 고정)
  - `lof-backend/src/main/java/com/linkfit/service/GymSettingService.java` — `getSetting(gymCode)`가 `gymCode`로 **지점명만** 바꿔치기하고, 운영시간/공지/영업상태는 그대로 `id=1` 행을 반환
- **문제**: 2번째 지점이 생기면 두 지점이 운영시간·공지·휴무 상태를 **그대로 공유**한다. 한쪽 관리자가 "영업중"으로 토글하면 다른 지점 회원 화면에도 동일하게 반영된다.
- **함정**: `getSetting(gymCode)`가 파라미터를 받아 지점명은 정확히 바꿔주기 때문에 "지점별로 이미 처리돼 있다"고 착각하기 쉽다. 실제로는 이름만 다르고 운영시간/휴무 데이터는 동일 행을 참조하는 반쪽짜리 구현이다.
- 라이브 테스트로 확인: `PATCH /api/settings/gym/open`(lof-admin)으로 토글 → `GET /api/gym/setting`(lof-backend)이 즉시 반영됨을 확인 — 이 자체는 정상 동작이지만, 지점이 여러 개가 되는 순간 이 하나의 값을 모든 지점이 나눠 쓰게 된다는 뜻이다.

### 2-2. `GET /api/trainers` (회원의 "트레이너 선택하기" 목록) 지점 필터 없음

- 관련 파일: `lof-backend/src/main/resources/mapper/TrainerMapper.xml` (`findAllTrainers`)
- 쿼리 조건이 `WHERE u.role = 'TRAINER' AND u.is_active = 1` 뿐이며 지점 조인/필터가 전혀 없다.
- **문제**: 2번째 지점이 생기면 **다른 지점 회원이 다른 지점 소속 트레이너를 그대로 조회·선택·배정받을 수 있다.**

---

## 3. lof-admin 백오피스 — 컨트롤러별 gymId 스코핑 재검증

기존 `CLAUDE.md`에 "일부 컨트롤러는 gymId 파라미터가 없다"고 기록되어 있었으나,
실제 코드 기준으로 컨트롤러별로 다시 확인한 결과는 아래와 같다.

| 컨트롤러 | gymId 스코핑 | 확인 결과 |
|---|---|---|
| `MemberApiController` | ✅ 적용됨 | `principal.getGymId()`를 전 메서드에서 사용 (`user_gym` JOIN 방식). 이전 세션 회원관리 개편 때 반영된 것으로 보이며, **CLAUDE.md 문서가 최신화되지 않은 상태**였다 — 이번에 문서 갱신 필요 |
| `StaffApiController` | ⚠️ 일부만 적용 | 대시보드(`GET /{id}/dashboard`), 담당 회원(`GET /{id}/members`)은 `principal.getGymId()` 사용. **정작 직원 목록 조회(`GET /api/staff`)는 gymId 파라미터 자체가 없어 전 지점 직원이 그대로 노출됨** |
| `RevenueApiController` | ❌ 전혀 없음 | 매출 요약/카테고리별 상세/추이/매출 내역/CSV 내보내기/구독권 통계/티켓 통계 등 모든 엔드포인트에 `CrmUserDetails`/gymId 파라미터가 없음 → **지점별 매출 정산이 원천적으로 불가능**, 2번째 지점이 생기면 전 지점 매출이 합산되어 보임 |
| `SettingApiController` | ❌ 전혀 없음 | 위 2-1절의 `gym_setting` 문제와 동일 컨트롤러. 휴일(`GymHoliday`)·배너(`GymBanner`) 관리도 같은 이유로 전 지점 공용 |

---

## 4. 권한(인가) 체계 미비 — 위 문제들을 더 취약하게 만드는 배경 요인

- JWT에 `role`(super_admin/gym_admin/trainer), `gymId` 클레임이 있지만, `SecurityConfig`는
  `anyRequest().authenticated()`뿐이라 실제로 역할이나 소속 지점을 검사하는 코드가 없다
  (`@PreAuthorize`/`hasRole` 등 전체 코드베이스에 0건 — 기존에 문서화된 이슈).
- 설령 각 API에 `gymId` 필터를 추가하더라도, "본인 소속 지점만" 조회 가능하도록 서버 단에서
  강제하는 안전장치가 없다면 클라이언트가 임의로 다른 `gymId`를 요청해 타 지점 데이터를
  열람할 수 있는 구조다. 현재는 지점이 1개뿐이라 실질적 위험이 드러나지 않을 뿐이다.

---

## 5. 정리 — 확장 전 우선순위

1. **스키마 마이그레이션**: `product`, `membership`, `member_tickets`, `trainer_schedules`,
   `attendance`, `consult`, `sale`, `gym_setting`, `gym_banner`, `gym_holiday`에 `gym_id`
   컬럼 추가 + 기존 데이터 백필(전부 `LF01`로).
   **[2026-08-20] SQL 작성 완료 — `docs/sql/gym_id_backfill_20260820.sql`.** 이 문서 작성 이후
   신설된 `product_package`/`class_session`/`member_freeze`/`staff_attendance` 4개도 같은 사유로
   포함시킴. `locker`는 `locker_zone.gym_id`(2026-08-10 기 반영)를 통해, `class_attendee`는
   `class_session.gym_id`(이번에 추가)를 통해 간접 스코핑되므로 제외. `payment_method`는 전
   지점 공통 정책일 가능성이 있어 임의로 포함하지 않고 보류. **아직 실행 전(로컬 미적용) —
   적용 후에는 2번 항목(코드 수정)이 뒤따라야 실제로 지점별로 갈라진다.**
2. **`gym_setting` 다지점화**: `id=1` 하드코딩을 `gym_id` 기준 조회/갱신으로 변경 (lof-backend,
   lof-admin 양쪽 매퍼 전부 수정 필요).
3. **`GET /api/trainers` 지점 필터 추가**: 회원의 소속 지점(`user_gym`) 기준으로 트레이너 목록
   필터링.
4. **lof-admin 컨트롤러 보완**: `StaffApiController` 목록 조회, `RevenueApiController` 전체,
   `SettingApiController` 전체에 `gymId` 스코핑 추가.
5. **권한 체계 구현**: `@PreAuthorize`/역할·지점 기반 접근 제어 도입 — 위 3~4번 작업과 함께
   가지 않으면 필터를 추가해도 우회 가능한 구조가 남는다.
6. **CLAUDE.md 갱신**: `MemberApiController`는 이미 gymId 스코핑이 적용된 상태이므로 문서의
   "알려진 이슈 3번" 항목에서 제외하고, `StaffApiController`(부분 적용)/`RevenueApiController`/
   `SettingApiController`(미적용)로 범위를 좁혀 다시 기록.
