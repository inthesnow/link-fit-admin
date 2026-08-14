# LINK_Fit Admin

헬스장 관리자 웹 어드민(백오피스). LINK_Fit 앱의 회원/직원/수업/출석/상담/매출/상품/메시지/CRM/피드백을
통합 관리한다.

## 기술 스택

| 항목 | 내용 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.4 |
| Security | Spring Security 7.x (JWT, Stateless, 쿠키 `crm_token`) |
| Template | Thymeleaf (SSR) |
| Persistence | MyBatis + MariaDB |
| Build | Gradle 8.14 |
| Port | 17579 |

## 프로젝트 구조

```
src/main/java/com/linkfit/admin/
├── controller/            # LoginController, PageController (SSR 페이지)
│   └── api/                # REST 컨트롤러 (@RestController, 20여개)
├── domain/                 # VO/DTO
├── service/, service/mybatis/  # 서비스 인터페이스 + MyBatis 구현체
├── mapper/                 # MyBatis @Mapper 인터페이스
├── security/                # JwtUtil, JwtCookieFilter, CrmUserDetails
└── scheduler/               # DailyStatsScheduler (일별 통계/재등록 자동분류)

src/main/resources/
├── application*.yml
├── mapper/**/*.xml          # MyBatis XML
├── static/css/
└── templates/                # Thymeleaf 페이지 + fragments/sidebar.html
```

세부 아키텍처 패턴, DB 스키마 매핑, 알려진 이슈, 개발 진행 현황은 `CLAUDE.md` 참고.

## DB 연결 정보 (로컬)

| 항목 | 값 |
|---|---|
| DBMS | MariaDB 10.11.14 |
| Host | `localhost:3306` |
| Database | `linkfit` |
| Username | `linkfit` |
| Password | `link_fit!` |

설정 파일: `src/main/resources/application-dev.yml` (실제 앱 서비스 DB를 공유해서 사용).
DDL 상세는 `docs/sql.md` / `docs/database.md` 참고.

## 실행 방법

```bash
# 빌드
./gradlew build -x test

# 실행 (dev 프로필 기본)
./gradlew bootRun

# 접속
http://localhost:17579
```

**Windows:** `gradlew.bat bootRun`

## 로그인 계정

`crm_users` 테이블 기반(BCrypt 해싱), 지점코드 + 아이디 + 비밀번호로 로그인한다.
로컬 개발용 기본 계정: `admin` / `admin1234` (지점코드 `LF01`).

> 트레이너로 승격된 계정은 별도 CRM 비밀번호 없이 앱 로그인 비밀번호를 그대로 위임 검증한다.

## 디자인 시스템

라이트 테마 + 사이드바 다크 네이비. `common.css`에 CSS 변수로 정의됨.

| 변수 | 값 | 용도 |
|---|---|---|
| `--bg` | `#F6F8FA` | 페이지 배경 |
| `--surface` | `#FFFFFF` | 카드, 헤더 |
| `--accent` | `#0969DA` | 활성 탭, 링크, 포커스 |
| `--success` | `#1A7F37` | 유효 상태, 매출 |
| `--error` | `#CF222E` | 에러, 만기 상태, 삭제 |
| `--warning` | `#9A6700` | 경고, 정지 상태 |
| 사이드바 배경 | `#1C2333` | 다크 네이비 |

## 주요 페이지

`/dashboard` `/members` `/staff` `/classes` `/attendance` `/consults` `/revenue` `/products`
`/messages` `/pt` `/reregistration` `/feedback` `/lockers` `/gym-requests` `/inbox` `/cs`
`/crm-sales` `/announcements` `/settings`

REST API 전체 목록·아키텍처 패턴·알려진 이슈·개발 진행 현황은 `CLAUDE.md`를 참고할 것 —
이 프로젝트는 기능이 빠르게 늘어나는 중이라 README보다 CLAUDE.md가 더 최신 상태로 유지된다.
