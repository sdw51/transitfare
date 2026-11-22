# 후쿠오카 대중교통 경비 계산기 구현 계획

> **에이전트 작업자용:** 필수 하위 스킬: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 이 계획을 태스크 단위로 구현한다. 단계는 체크박스(`- [ ]`)로 추적한다.

**목표:** 여행 일정을 입력하면 자체 요금 데이터로 구간 요금·총액을 계산하고, 패스·할인 승차권 중 가장 싼 조합을 추천하는 로컬 실행 웹사이트를 만든다.

**아키텍처:** MSA로 분리할 수 있게 만든 모듈러 모놀리스. Spring Boot 서버 하나 안에 기능 모듈 `transit`(지역·운영사·역·요금 데이터와 요금 계산), `pass`(승차권 상품과 최적화), `itinerary`(여행·일정·요약), `directions`(경로 후보·검증 로그)를 두고, 각 모듈은 `XxxFacade` 하나만 외부에 공개한다. 퍼사드 안쪽은 사용자 행동 하나당 유스케이스 클래스 하나, 계산 로직은 Spring에 의존하지 않는 `domain`. 모듈마다 DB 스키마를 따로 갖고 다른 모듈 테이블을 참조하지 않는다. React 프론트는 표시와 입력만 한다.

**기술 스택:** Java 25, Spring Boot 4.x(start.spring.io 최신 안정판), Gradle Kotlin DSL(wrapper), Spring Web MVC, Spring Data JPA, Lombok, Flyway, H2(개발 `dev` 프로필·테스트, `MODE=PostgreSQL`), PostgreSQL(운영 `prod` 프로필), springdoc-openapi, ArchUnit, JUnit 5 + AssertJ + MockMvc / Node 24, Vite + React + TypeScript, Mantine, React Router, TanStack Query, Vitest + Testing Library, `@vis.gl/react-google-maps`.

**설계 문서:** `docs/superpowers/specs/2026-11-17-fukuoka-transit-fare-design.md` — 실행자는 계획과 설계 문서를 함께 읽는다(특히 10-1장 아키텍처).

**진행 방식:** ① 0단계(약속 고정)를 한 세션에서 끝내고 커밋 → ② 1단계는 기능별 세션을 3개씩 두 번에 나눠 각자 git worktree에서 구현: 1차(transit·pass·데이터) → `dev`에 합침 → 2차(itinerary·directions·frontend, 갱신된 `dev`에서 시작해 실제 요금·승차권 모듈 사용) → ③ 2단계에서 2차를 `dev`에 합치고 연결 → 최종 확인 후 `dev`를 `main`에 머지. 브랜치·커밋 규칙은 아래 "커밋·브랜치 규칙", 진행은 "실행 단계".

## 전역 제약

- 완성 목표일: **2026-11-23까지 전부 구현.** 시간이 부족하면 뒤로 미루는 순서: 관리자 화면 → 인쇄용 요약 → 원화 환산 → Google 연동. 요금 데이터·요금 계산·일정 입력·승차권 비교는 반드시 먼저.
- 무료 운영. 유료 API 금지. Google Maps Platform은 무료 한도 안에서.
- **사용자 화면의 모든 금액은 자체 요금 데이터로 계산한 값.** Google 요금(transitFare)은 사용자 화면에 표시하지 않고 계산에 쓰지 않으며, 요금 불일치 검증에만 쓴다. 구간별 Google 요금으로 빈 구간을 채우지 않는다.
- 데이터에 없는 요금은 0으로 계산하지 않는다. 상태 `UNKNOWN`("미확인")으로 표시하고 총액에 "미확인 구간 포함" 경고.
- 모든 요금·승차권 시드 값에 `source_url`, `verified_on`을 기록한다. 확인 못 한 값은 넣지 않는다.
- **모듈 간 계약은 `docs/contracts.md`가 원본.** 0단계에서 계약 코드를 만들지 않는다. 각 모듈 세션이 자기 `contract` 타입과 퍼사드를 이 문서대로 만들고, 프론트는 `api/types.ts`를 이 문서로 작성. 다르게 바꿔야 하면 문서를 먼저 고치고 `[contract]`. **pass는 transit을 모른다** — 입력은 자체 타입 `TicketLeg`·`TicketParty`, transit 결과 변환은 trip 담당.
- **컨트롤러 → 퍼사드 (사용자 결정 2026-11-19):** 모든 모듈의 컨트롤러는 자기 모듈의 퍼사드만 부른다. itinerary도 `ItineraryFacade`를 둔다(자기 컨트롤러 전용). **예외 없음:** 관리자 컨트롤러는 관리 전용 퍼사드 `XxxAdminFacade`(자기 모듈 `admin` 컨트롤러 전용, 다른 모듈 사용 금지)를 거친다. CRUD 공통 클래스는 두지 않는다(사용자 결정). 컨트롤러 주석에는 누가·언제 부르는지와 저장/조회 여부(CLAUDE.md).
- **모듈 경계:** 모듈 밖에서는 그 모듈의 `XxxFacade`와 `contract` 패키지(공개 DTO)만 참조한다. 모듈 간에는 ID 값만 저장하고 FK·JOIN 금지. ArchUnit 테스트로 강제.
- **DB 스키마:** `transit`, `pass`, `itinerary`, `directions`. 마이그레이션 SQL은 H2와 PostgreSQL 모두에서 동작하는 문법만.
- **모듈별 Flyway:** 모듈마다 독립된 Flyway(자기 스키마, 자기 `flyway_schema_history`, 자기 폴더 `db/migration/{module}`). 버전 번호는 모듈 안에서만 유일. 같은 모듈에 두 세션이 마이그레이션을 추가할 때는 아래 번호 범위를 지킨다. 커밋된 마이그레이션은 수정 금지.
- **지역:** 운영사·역·승차권 상품·여행에 `region_code`. 첫 지역은 `JP-FUKUOKA`(통화 JPY, 시간대 Asia/Tokyo). 금액 컬럼은 지역 통화의 정수 최소 단위(`*_amount`). 원화 환산은 "현지 통화 1단위 = ○원"(`local_to_krw`).
- API는 REST + JSON, 경로 접두사 `/api/v1`. Swagger UI `/swagger-ui`.
- **이름:** 지역 중립. 패키지 `com.transitfare`, 메인 클래스 `TransitFareApplication`, 화면 제목 "여행 교통비 계산기"(지역 선택에 현재는 후쿠오카만), DB 이름 `transitfare`.
- 화면 언어 한국어. 역·운영사·승차권은 `name_ja` + `name_ko`.
- 프론트 화면 작업은 `frontend-design` 스킬로 디자인 방향을 정하고 Mantine 테마로 반영. 부가 정보(열차 종별·좌석·소요시간)는 배지가 아니라 보조 텍스트, "미확인"은 경고색 텍스트.
- 로컬 실행, PC 화면 기준, 인증 없음. E2E(Playwright) 없음 — 전체 흐름은 수동 확인.
- 일행은 항상 같이 움직인다. 인원(어른 수)은 여행 단위로 한 번.
- **어린이 요금은 다루지 않는다**(v1 비목표, 사용자 결정 2026-11-18). 금액은 어른 1명 기준, 어린이 컬럼·필드·테스트 없음.
- **코드 enum:** `contract`의 enum은 한글 `label`·`description` 필드, `@JsonProperty("code") getCode()`, 코드 문자열과 객체를 둘 다 읽는 `@JsonCreator from(Object)`를 enum마다 직접 둔다(공통 인터페이스 없음, `docs/contracts.md`의 "코드 enum" 모양 그대로). 응답 JSON은 `{code, label, description}` 객체. 목록과 라벨은 `docs/contracts.md`의 표. DB에는 코드만(`EnumType.STRING`).
- **에러 처리:** 모듈마다 `{module}/error/XxxErrorCode` enum(`ErrorCode` 구현: `HttpStatus status`, `String message`)에 에러를 모은다. 유스케이스는 `throw new BusinessException(ItineraryErrorCode.ITINERARY_OUT_OF_PERIOD, "travelDate")`. 응답은 HTTP 상태와 같은 값의 본문 `{"status":400,"code":"ITINERARY_OUT_OF_PERIOD","message":"여행 기간 밖의 날짜입니다","field":"travelDate"}`(`field`는 없으면 null). code 이름은 모듈 접두사(`TRANSIT_`, `PASS_`, `ITINERARY_`, `DIRECTIONS_`). `domain` 패키지는 Spring에 의존하지 않으므로 `BusinessException`을 던지지 않고, 유스케이스가 결과를 보고 던진다. 에러 테스트는 HTTP 상태와 `$.code`를 둘 다 확인. 아래 "에러 코드" 표의 코드는 그 코드를 처음 던지는 커밋에 추가.
- **주석:** 유스케이스 public 메서드마다 한글 Javadoc(하는 일·입력·결과·에러 코드), 퍼사드 메서드마다 하는 일과 연결된 유스케이스·그 유스케이스가 하는 일, domain 계산 규칙 메서드마다 규칙 요약. 자세한 형식은 CLAUDE.md.
- **엔티티 매핑:** 엔티티의 모든 필드에 `@Column(name = "snake_case 컬럼명")`을 명시하고, DB에서 NOT NULL인 컬럼은 `nullable = false`. id는 `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")`. 연관 필드는 `@JoinColumn(name = …)`, enum은 `@Enumerated(EnumType.STRING)`과 함께 `@Column`. 엔티티 클래스에는 `@Table(schema = "모듈", name = "테이블")`.
- **Lombok:** `@RequiredArgsConstructor`(유스케이스·퍼사드 구현·컨트롤러·어댑터의 `final` 필드 주입), 엔티티에 `@Getter` + `@NoArgsConstructor(access = AccessLevel.PROTECTED)` + `@Builder`, 로그는 `@Slf4j`. **`@Setter`·`@Data`는 쓰지 않는다** — 값 변경은 이름 있는 메서드(`itinerary.changePeriod(start, end)`). 공개 DTO(`contract` 패키지)와 도메인 값 객체는 Java record.
- **테스트 이름:** 메서드명은 계획에 적힌 영어 이름 그대로, 모든 테스트에 한글 `@DisplayName`(예: `@DisplayName("도보 구간은 요금이 0원이다") void walkLegIsFree()`). 프론트(Vitest)는 `it("도보 구간은 요금이 0원이다")`처럼 한글 설명.
- **코드는 먼저 보여주고 작성:** 커밋 단위("커밋·브랜치 규칙"의 한 단계)로 테스트·구현 코드를 사용자에게 보여주고 승인받은 뒤 파일에 쓴다. 그래서 사용자와 대화할 수 없는 하위 에이전트에게 구현을 맡기지 않고, 각 세션이 직접 구현한다.
- API 키는 커밋 금지. 백엔드 `GOOGLE_ROUTES_API_KEY`, 프론트 `VITE_GOOGLE_MAPS_API_KEY`(`frontend/.env.local`).

## 커밋·브랜치 규칙

**브랜치 구조**
```
main  ← 완성본. Task 1 커밋과 Task 19의 최종 머지만 들어간다
 └ dev  ← 통합 브랜치. Task 1 커밋 직후 main에서 만든다
    ├ feat/transit, feat/pass, feat/data         (1단계 1차, dev에서 분기 → 끝나면 dev에 머지)
    ├ feat/itinerary, feat/directions, feat/frontend      (1단계 2차, 1차가 합쳐진 dev에서 분기)
    ├ feat/wave1-fix, feat/data-region-N         (1차 합치기 수정, 지역 2~4 데이터)
    └ feat/integration, feat/admin, feat/final  (2단계, dev에서 분기)
```
- `main`, `dev`에는 직접 기능 커밋을 하지 않는다. 모든 작업은 `dev`에서 딴 `feat/*` 브랜치에서 한다.
- **`dev`·`main` 머지는 사용자가 직접 한다.** Claude 세션은 머지 직전까지(테스트 통과, 커밋, 머지 순서 안내) 준비하고, 머지 후 테스트 실행·충돌로 생긴 수정은 `feat/*` 브랜치에서 돕는다.
- `feat/*` → `dev`는 머지 커밋으로 합친다(`git merge --no-ff`, 스쿼시·리베이스 금지). 메시지 `merge: feat/transit → dev`.
- `dev` → `main`은 Task 19 최종 확인 뒤 한 번, `git merge --no-ff dev`, 메시지 `merge: dev → main (v1)`.

**커밋 4단계 (백엔드)** — 기능마다 아래 순서로 커밋해서 만드는 흐름이 기록에 보이게 한다.

| 순서 | 단계 | 들어가는 것 | 함께 넣는 테스트·확인 | 메시지 끝말 |
|---|---|---|---|---|
| ① | 도메인(테이블) | 마이그레이션(스키마), `entity` | `./gradlew test` — 마이그레이션 적용 + JPA `ddl-auto: validate`로 엔티티 매핑 검증 | `… 도메인 생성` |
| ② | 계산 로직 | `domain` 패키지(순수 Java: 계산 규칙, `FareData` 같은 포트 인터페이스), 로직이 쓰는 `contract` 타입 | 로직 단위 테스트 | `… 로직 구현` |
| ③ | 저장소·유스케이스·퍼사드 | `repository`, 외부 연동 어댑터(`provider`), `usecase`, `XxxFacadeImpl` | 리포지토리·유스케이스 테스트 | `… 유스케이스 구현` |
| ④ | 컨트롤러 | `controller`, `admin` 컨트롤러 | MockMvc 테스트 | `… API 구현` |

- 단계마다 TDD를 한 번씩 돈다: 그 단계의 테스트 작성 → 실패 확인 → 구현 → `./gradlew test` 통과 → 커밋. 태스크 본문의 "FAIL → 구현 → PASS" 단계는 이렇게 커밋 단계별로 나눠서 실행한다. 테스트가 실패하는 상태로 커밋하지 않는다.
- 해당 계층이 없는 기능은 그 단계를 건너뛴다(예: Task 3은 ②만, Task 5는 ③④만, Task 6은 ①③④). 각 태스크의 마지막 "커밋" 단계에 그 태스크의 커밋 목록이 있다.
- 프론트는 화면(또는 공용 컴포넌트) 단위로 커밋한다: `feat(frontend): 여행 요약·승차권 설정 화면 구현`.
- 데이터는 시드 마이그레이션과 골든 테스트를 따로 커밋한다: `data(transit): …`, `test(transit): … 골든 테스트 추가`.

**메시지 형식:** `type(module): 대상 + 한 일` — 단계 이름을 앞에 붙이지 않고, 문장 끝에 무엇을 했는지 쓴다.
  - ① 테이블·엔티티: `… 도메인 생성` (예: `feat(transit): 요금 기준 데이터 도메인 생성`)
  - ② 계산 규칙: `… 로직 구현` (예: `feat(transit): 요금 계산 로직 구현`)
  - ③ 저장소·유스케이스·퍼사드: `… 유스케이스 구현` (예: `feat(transit): 구간 요금 계산 유스케이스 구현`)
  - ④ 컨트롤러: `… API 구현` (예: `feat(transit): 구간 요금 계산 API 구현`)
  - 프론트 `… 화면 구현`, 데이터 `… 데이터 추가`, 테스트만 `… 테스트 추가`, 문서 `… 작성`
- type: `feat` 기능, `fix` 버그 수정, `data` 요금·승차권 데이터, `test` 테스트만, `docs` 문서, `chore` 설정·골격·통합, `refactor` 동작 변화 없는 정리
- module: `transit`, `pass`, `itinerary`, `directions`, `frontend`, `integration`(공통·골격은 생략)
- 수정 금지 공용 파일을 건드린 커밋은 메시지 끝에 `[contract]`.
- AI가 만든 커밋은 마지막 줄에 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## 리뷰 포커스

1. **인원 0명 또는 음수** — 400 "인원을 1명 이상 입력하세요"(`TRANSIT_INVALID_PASSENGERS`, `ITINERARY_INVALID_PASSENGERS`). → Task 5, Task 6 테스트.
2. **같은 운영사 거리제 구간이 연속될 때(지하철 공항선→나나쿠마선, JR 가고시마 본선→규다이 본선)** — 사이에 도보가 끼어도 거리를 합산해 1회 계산, 다른 운영사가 끼면 끊는다. → Task 3 테스트.
3. **승차권 가격을 원화로 입력했는데 환율이 비어 있음** — 저장 거부(400 "환율을 먼저 입력하세요"). → Task 8 테스트.
4. **여행 기간 밖 날짜의 구간, 여행 끝을 넘는 다일 패스** — 구간 날짜는 기간 안만(400). 다일 패스는 끝을 넘어도 구매 가능으로 계산. → Task 6, Task 7 테스트.
5. **Google 경로에 매칭 안 되는 역이 섞인 후보** — 후보는 유지, 그 구간만 `UNKNOWN`, 매칭 실패 로그 1건(같은 이름은 횟수 증가). → Task 10 테스트.

---

## 실행 단계

### 날짜별 일정

| 날짜 | 할 일 |
|---|---|
| 11/17 | 0단계 (Task 1) |
| 11/18~19 | 1단계 1차 (transit·pass·데이터) → 19일 저녁 1차 합치기 (Task M1) |
| 11/20~21 | 1단계 2차 (itinerary·directions·frontend) |
| 11/22 | 2단계 연결 (Task I1, Google 연동 확인 포함) + 관리자 (Task 15) |
| 11/23 | 최종 확인 (Task 19), 지역 2~4 데이터 (Task 16~18) 가능한 만큼 |

하루가 밀리면 뒤에서부터 뺀다: 관리자 화면 → 인쇄용 요약 → 원화 환산 → Google 연동. 매일 끝에 일정 대비 진행 상황을 사용자에게 알린다.

### 0단계: 약속 고정 (한 세션, 순차) — Task 1
골격과 **모듈 간 계약**을 코드로 만들어 `main`에 커밋하고, 바로 `dev` 브랜치를 만든다. 구현은 없다.
- 프로젝트 골격, 모듈별 Flyway 설정, ArchUnit 테스트, OpenAPI
- 모든 `contract` 패키지 타입(아래 "공개 타입")과 퍼사드 인터페이스(구현 없음)
- 공용 파일을 미리 채움: `build.gradle.kts`(모든 의존성), `application.yml`, `frontend/package.json`(모든 의존성), `frontend/src/App.tsx`(모든 라우트 + 빈 페이지), `frontend/src/api/types.ts`(모든 API 타입)
- `README.md` 뼈대: 프로젝트 소개, 실행 방법, **작업 규칙**(모듈 경계, 브랜치 이름, 마이그레이션 번호 범위, 수정 금지 공용 파일, 요금 출처 기록 규칙). 1단계 세션은 시작 전에 README를 읽는다.
- 1단계 세션은 위 공용 파일(README 포함)을 원칙적으로 수정하지 않는다. 꼭 필요하면 해당 세션 브랜치에서 수정하고 2단계 합칠 때 충돌을 해결한다.

### 1단계: 기능별 구현 (세션 3개씩 두 번, 각자 worktree·브랜치)

코드를 먼저 보여주고 승인받기 때문에 동시에 도는 세션은 최대 3개. 모든 브랜치는 그 시점의 `dev`에서 딴다(`git worktree add ../travel-transit -b feat/transit dev`).

**1차 (dev = 0단계 상태에서 시작, 세 세션 모두 처음부터 동시에)**

| 세션 | 브랜치 | 태스크 | 다른 모듈이 필요할 때 |
|---|---|---|---|
| transit | `feat/transit` | Task 2, 3, 5, 10-A(역 매칭), 13(가져오기 코드) | 없음 |
| pass | `feat/pass` | Task 7 (Step 5~6 시드·골든 제외) | 없음(transit을 모름. 입력은 자체 `TicketLeg`) |
| 데이터 | `feat/data` | Task 4, 7 Step 5~6 | 계획에 고정된 컬럼명으로 시드 SQL 작성. 골든 테스트는 작성만, 실행은 1차 합치기에서 |

**1차 합치기 (사용자가 머지, 메인 세션이 확인)** — Task M1
- `dev`에 머지 커밋으로 `feat/transit` → `./gradlew test` → `feat/pass` → test → `feat/data` → `./gradlew test goldenTest`.
- 골든 불일치는 `dev`에서 딴 `feat/wave1-fix`에서 데이터(새 번호 마이그레이션) 또는 엔진(Task 3 테스트에 재현 추가)을 고쳐 `dev`에 머지.
- 각 세션이 만든 `contract`·퍼사드가 `docs/contracts.md`와 일치하는지 확인(다르면 문서 또는 코드 수정).
- 커밋: 머지 커밋 외 고친 것마다 `fix(모듈): …` / `data(transit): …`.

**2차 (1차가 합쳐진 dev에서 시작)**

| 세션 | 브랜치 | 태스크 | 다른 모듈이 필요할 때 |
|---|---|---|---|
| itinerary | `feat/itinerary` | Task 6, 8 | **실제** `TransitFacade`·`PassFacade`와 시드 데이터 사용(@SpringBootTest). 유스케이스 단위 테스트에서 특정 값이 필요할 때만 가짜 허용 |
| directions | `feat/directions` | Task 10-B(후보·검증), 12-A(Google 제공자) | **실제** `TransitFacade.matchStep`·`priceLegs` 사용, Google은 가짜 제공자 |
| frontend | `feat/frontend` | Task 9, 11, 12-B(장소 입력·지도), 14 | itinerary·directions API가 아직 없으므로 `VITE_USE_MOCK=true` 가짜 API(`api/mock.ts`). 요금·승차권 API(`/api/v1/pricing`, `/stations`, `/products`)는 실제 백엔드로 확인 가능 |

- 지역 2~4 데이터(Task 16~18)는 2차가 끝나 세션 자리가 비면 `feat/data-region-N`으로 진행(동시 세션 최대 3개 유지).
- 각 세션은 자기 모듈 문서를 쓴다: `docs/modules/{transit,pass,itinerary,directions,frontend}.md`(모듈이 하는 일, 퍼사드 메서드, 주요 유스케이스, 테이블, 테스트 실행 방법). 데이터 세션은 `docs/data-sources.md`(조사한 공식 자료 목록: 운영사, URL, 확인 날짜, 다룬 구간).
- 각 세션은 자기 모듈 폴더와 자기 테스트만 수정한다. 데이터 세션은 `db/migration/*` 의 자기 번호 범위와 `seed/GoldenFareTest.java`만.
- 각 세션 종료 조건: 자기 모듈 문서(`docs/modules/*.md` 또는 `docs/data-sources.md`) 작성, 자기 범위의 테스트 통과(데이터 세션은 SQL 문법이 H2에서 적용되는지까지 — 0단계 스키마가 있는 상태에서 `./gradlew test`가 마이그레이션 단계에서 실패하지 않을 것), "커밋·브랜치 규칙"대로 단계별 커밋. `dev` 머지는 세션이 하지 않는다(1차 합치기·2단계에서).

### 2단계: 합치기와 연결 (한 세션, 순차) — Task I1, 15, 19
- `dev`에 머지 커밋으로 2차를 합치는 순서: `feat/itinerary` → `feat/directions` → `feat/frontend`. 매번 `./gradlew test`(프론트는 `npx vitest run`) 통과 확인 후 다음.
- 연결·관리자·최종 확인은 각각 `dev`에서 딴 `feat/integration`, `feat/admin`, `feat/final`에서 하고 끝나면 `dev`에 머지. 마지막에 `dev` → `main`.
- 연결(Task I1) → 관리자(Task 15) → 최종 확인(Task 19). 미루는 순서(관리자 → 인쇄 → 원화 → Google)는 그대로.

## 에러 코드

| enum | code | HTTP | message | 처음 쓰는 태스크 |
|---|---|---|---|---|
| `TransitErrorCode` | `TRANSIT_REGION_NOT_FOUND` | 404 | 지역을 찾을 수 없습니다 | Task 2 |
| | `TRANSIT_INVALID_PASSENGERS` | 400 | 인원을 1명 이상 입력하세요 | Task 5 |
| | `TRANSIT_OPERATOR_REGION_MISMATCH` | 400 | 다른 지역의 운영사가 섞여 있습니다 | Task 5 |
| | `TRANSIT_GTFS_INVALID` | 400 | GTFS 파일을 읽을 수 없습니다 | Task 13 |
| | `TRANSIT_SOURCE_REQUIRED` | 400 | 출처와 확인 날짜를 입력하세요 | Task 15 |
| `PassErrorCode` | `PASS_PRODUCT_NOT_FOUND` | 404 | 승차권 상품을 찾을 수 없습니다 | Task 15 |
| | `PASS_SOURCE_REQUIRED` | 400 | 출처와 확인 날짜를 입력하세요 | Task 15 |
| `ItineraryErrorCode` | `ITINERARY_NOT_FOUND` | 404 | 여행을 찾을 수 없습니다 | Task 6 |
| | `ITINERARY_SEGMENT_NOT_FOUND` | 404 | 일정을 찾을 수 없습니다 | Task 6 |
| | `ITINERARY_UNKNOWN_REGION` | 400 | 지원하지 않는 지역입니다 | Task 6 |
| | `ITINERARY_INVALID_PERIOD` | 400 | 종료일이 시작일보다 빠릅니다 | Task 6 |
| | `ITINERARY_OUT_OF_PERIOD` | 400 | 여행 기간 밖의 날짜입니다 | Task 6 |
| | `ITINERARY_INVALID_PASSENGERS` | 400 | 인원을 1명 이상 입력하세요 | Task 6 |
| | `ITINERARY_PRODUCT_NOT_FOUND` | 404 | 승차권 상품을 찾을 수 없습니다 | Task 8 |
| | `ITINERARY_EXCHANGE_RATE_REQUIRED` | 400 | 환율을 먼저 입력하세요 | Task 8 |
| `DirectionsErrorCode` | `DIRECTIONS_FAKE_PLACES_UNAVAILABLE` | 404 | 가짜 경로 제공자에서만 사용할 수 있습니다 | Task 10-B |
| | `DIRECTIONS_PROVIDER_FAILED` | 502 | 경로 검색 서비스에 연결할 수 없습니다 | Task 12-A |
| | `DIRECTIONS_RECORD_NOT_FOUND` | 404 | 기록을 찾을 수 없습니다 | Task 15 |

새 에러가 필요하면 자기 모듈 enum에 추가하고 이 표 형식을 `docs/modules/{module}.md`에 남긴다.

## 모듈과 파일 구조

모든 모듈은 같은 내부 구조를 따른다(관리자 CRUD도 `admin` 컨트롤러 → `XxxAdminFacade` → `usecase/admin/` 유스케이스 → 리포지토리).

```
backend/src/main/java/com/transitfare/
├─ TransitFareApplication.java
├─ common/                 GlobalExceptionHandler, HealthController, OpenApiConfig, ModuleFlywayConfig
│  └─ error/               ErrorCode, BusinessException, ApiError, CommonErrorCode
├─ transit/
│  ├─ TransitFacade.java      ← 모듈 공개 진입점
│  ├─ contract/            공개 DTO: Passengers, LegInput, LegFare, FareStatus, LegMode, TrainClass, SeatType,
│  │                       PricingResult, StationView, OperatorView, RegionView, MatchedLeg, StepToMatch
│  ├─ usecase/             PriceLegsUseCase, SearchStationsUseCase, ListOperatorsUseCase, GetRegionUseCase,
│  │                       MatchStepUseCase, ImportGtfsFaresUseCase
│  ├─ domain/              FareEngine, FareData(인터페이스), FareRuleType, StationNameNormalizer
│  ├─ entity/              Region, Operator, OperatorAlias, Line, Station, LineStation, StationAlias,
│  │                       DistanceFareBand, PairFare, FlatZone, FlatZoneStation, ReservedSeatFee, TransferDiscount
│  ├─ repository/          엔티티별 리포지토리, JpaFareData(FareData 구현)
│  ├─ controller/          StationController, PricingController
│  └─ admin/               TransitAdminController, GtfsImportController
├─ pass/
│  ├─ PassFacade.java
│  ├─ contract/            TicketLeg, TicketParty, ProductView, ProductOverride, OptimizationResult, Purchase
│  ├─ usecase/             ListProductsUseCase, OptimizeTicketsUseCase
│  ├─ domain/              TicketOptimizer, ProductSpec
│  ├─ entity/, repository/ TicketProduct, TicketCoverage
│  ├─ controller/          ProductController
│  └─ admin/               PassAdminController
├─ itinerary/
│  ├─ ItineraryFacade.java (자기 컨트롤러 전용 — 다른 모듈은 쓰지 않음)
│  ├─ contract/            TripDto, SegmentDto, SummaryDto, ProductSettingDto
│  ├─ usecase/             CreateTripUseCase, UpdateTripUseCase, DeleteTripUseCase, ListTripsUseCase, GetTripUseCase,
│  │                       AddSegmentUseCase, UpdateSegmentUseCase, DeleteSegmentUseCase, MoveSegmentUseCase,
│  │                       UpdateProductSettingUseCase, ListProductSettingsUseCase, GetTripSummaryUseCase
│  ├─ entity/, repository/ Trip, Segment, Leg, TripProductSetting
│  └─ controller/          TripController
├─ directions/
│  ├─ DirectionsFacade.java
│  ├─ contract/            PlaceRef, RouteSearchRequest, CandidateView
│  ├─ usecase/             SearchRoutesUseCase
│  ├─ domain/              RouteProvider(인터페이스), RouteCandidate, CandidateStep, ThroughServiceSplitter
│  ├─ provider/            FakeRouteProvider, GoogleRoutesProvider
│  ├─ entity/, repository/ MatchFailure, FareMismatch
│  ├─ controller/          RouteController
│  └─ admin/               DirectionsAdminController
backend/src/main/resources/
├─ application.yml, application-dev.yml, application-prod.yml
├─ db/migration/{transit,pass,itinerary,directions}/   V<n>__<module>_<설명>.sql (모듈별 독립 번호, 아래 번호표)
└─ fake-routes/*.json
backend/src/test/java/com/transitfare/  (모듈별 테스트, ArchitectureTest, seed/GoldenFareTest)
docker-compose.yml          (prod 프로필 로컬 확인용 PostgreSQL)

frontend/src/
├─ main.tsx, App.tsx, theme.ts(Mantine 테마), api/{client,types}.ts
├─ pages/  TripListPage, TripPage, PrintPage, admin/AdminPage
└─ components/ TripSettingsForm, DayList, SegmentEditor, PlaceInput, CandidateList, ManualLegForm,
               SummaryPanel, ProductSettings, Money(현지통화+원 병기), RouteMap, admin/ResourceTable
```

**마이그레이션 번호표** (모듈별 독립 번호. 범위 = 담당 세션):

| 모듈 | 번호 | 파일 | 담당 |
|---|---|---|---|
| transit | V1, V2 | `V1__transit_schema`, `V2__transit_seed_region_operators` | transit 세션 |
| transit | V100~ | `V100__transit_seed_region1`, `V101~V103__transit_seed_region2~4` | 데이터 세션 |
| transit | V200~ | `V200__transit_google_aliases`, `V201__transit_seed_showa_bus` 등 연결 단계 보강 | 2단계 |
| pass | V1 | `V1__pass_schema` | pass 세션 |
| pass | V100~ | `V100__pass_seed_products`, `V101~__pass_seed_region_passes` | 데이터 세션 |
| itinerary | V1, V2 | `V1__itinerary_schema`, `V2__itinerary_product_setting` | itinerary 세션 |
| directions | V1 | `V1__directions_schema` | directions 세션 |

## 공개 타입 (모듈 간 계약)

**`docs/contracts.md`가 원본이다** — 모든 `contract` record·enum과 퍼사드 시그니처, 모듈 의존 관계, trip의 `LegInput`+`LegFare` → `TicketLeg` 변환 표가 있다. 각 태스크는 자기 모듈 부분을 그대로 구현한다. 이름을 바꾸지 말 것.

---

### Task 1 (0단계): 프로젝트 골격, 모듈 간 계약, 모듈별 Flyway

**Files:**
- Create: `.gitignore`, `backend/`(start.spring.io), `application.yml`(공통), `application-dev.yml`, `application-prod.yml`, `docker-compose.yml`, `common/{GlobalExceptionHandler,HealthController,OpenApiConfig,ModuleFlywayConfig}.java`, `common/error/{ErrorCode,BusinessException,ApiError,CommonErrorCode}.java`, `docs/contracts.md`(계약 문서, 코드 없음), 모듈별 빈 `db/migration/{module}/`(`.gitkeep`), `frontend/`(Vite react-ts), `frontend/.env.example`, `frontend/src/theme.ts`, `frontend/src/api/{types,client}.ts`(모든 API 타입), `frontend/src/App.tsx`(모든 라우트 + 빈 페이지), `README.md`(뼈대)
- Test: `TransitFareApplicationTests.java`, `common/GlobalExceptionHandlerTest.java`, `ArchitectureTest.java`, `frontend/src/App.test.tsx`, `frontend/src/api/client.test.ts`

**Interfaces:**
- Produces: `GET /api/v1/health` → `{"status":"ok"}`. 에러:
  ```java
  public interface ErrorCode { HttpStatus status(); String message(); String name(); }
  public class BusinessException extends RuntimeException { ErrorCode errorCode; String field; } // @Getter, 생성자 (ErrorCode), (ErrorCode, String field)
  public record ApiError(int status, String code, String message, String field) {}
  public enum CommonErrorCode implements ErrorCode {
    INVALID_INPUT(BAD_REQUEST, "입력값을 확인하세요"), NOT_FOUND(NOT_FOUND, "대상을 찾을 수 없습니다"), INTERNAL_ERROR(INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다") }
  ```
  `GlobalExceptionHandler`: `BusinessException` → `errorCode.status()` + ApiError, `MethodArgumentNotValidException` → `INVALID_INPUT`(field = 첫 오류 필드), 그 외 예외 → `INTERNAL_ERROR`(로그 남김, 내부 메시지 노출 금지). 프론트 `api/types.ts`의 `ApiError` 타입, `api/client.ts`의 `apiGet<T>(path)`, `apiSend<T>(method, path, body)` — 실패 시 `ApiRequestError`(`status`, `code`, `message`, `field`) throw, 경로는 `/api/v1` 기준.

- [ ] **Step 1:** `git init`. `.gitignore`: `backend/build/`, `backend/.gradle/`, `backend/data/`, `frontend/node_modules/`, `frontend/dist/`, `frontend/.env.local`, `.DS_Store`.
- [ ] **Step 2:** start.spring.io에서 생성(Gradle Kotlin DSL, Java 25, Spring Boot 4.x 최신 안정판, group `com.transitfare`, artifact `backend`, package name `com.transitfare`, 메인 클래스 `TransitFareApplication`, 의존성 web·data-jpa·flyway·h2·postgresql·validation·lombok). 추가 의존성: `org.springdoc:springdoc-openapi-starter-webmvc-ui`(Boot 4 호환 버전), `com.tngtech.archunit:archunit-junit5`(test).
- [ ] **Step 3:** 설정 파일을 프로필별로 나눈다. `application.yml`(공통): `spring.profiles.default=dev`, `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.open-in-view=false`, `spring.flyway.enabled=false`(Boot 자동 Flyway 끔), `app.modules=transit,pass,itinerary,directions`, `springdoc.swagger-ui.path=/swagger-ui`, `directions.provider=${ROUTE_PROVIDER:fake}`, `google.routes.api-key=${GOOGLE_ROUTES_API_KEY:}`. `application-dev.yml`: `jdbc:h2:file:./data/transitfare;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`, H2 콘솔 켬. `application-prod.yml`: `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`(기본값 없음 — 빠지면 기동 실패). 테스트 `src/test/resources/application.yml`: `jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1`. `docker-compose.yml`에 로컬 확인용 postgres 서비스(DB·사용자·비밀번호 `transitfare`).
- [ ] **Step 3-1: 모듈별 Flyway** — `ModuleFlywayConfig`: `app.modules`의 모듈마다 `Flyway.configure().dataSource(ds).schemas(module).createSchemas(true).locations("classpath:db/migration/"+module).table("flyway_schema_history").load().migrate()`. `EntityManagerFactoryDependsOnPostProcessor`로 JPA가 이 빈 이후에 뜨게 한다. 폴더가 비어 있어도 기동.
- [ ] **Step 3-2: 계약 문서** — `docs/contracts.md`(계획 작성 때 이미 만들어 둠)를 커밋에 포함. 계약 코드(`contract` 타입, 퍼사드 인터페이스)는 만들지 않는다 — 각 모듈 세션 몫.
- [ ] **Step 3-3: 골든 테스트 분리** — `build.gradle.kts`: `tasks.test { useJUnitPlatform { excludeTags("golden") } }`, 별도 `goldenTest` 태스크(`includeTags("golden")`).
- [ ] **Step 4: 실패하는 테스트**
  - `ModuleFlywayMigratorTest.createsSchemaAndHistoryPerModule()` — 별도 메모리 DB와 테스트 전용 모듈(`flywaytest`, test 리소스 `db/migration/flywaytest/V1__sample.sql`, 마이그레이션 없는 `emptymodule`)로 모듈별 스키마·이력 테이블 생성 확인(실제 모듈 번호와 겹치지 않게)
  - `TransitFareApplicationTests.healthReturnsOk()` — `GET /api/v1/health` → 200, `$.status=="ok"`
  - `TransitFareApplicationTests.swaggerUiAvailable()` — `GET /v3/api-docs` → 200
  - `GlobalExceptionHandlerTest`(테스트 전용 컨트롤러 사용): `businessExceptionMapsStatusAndBody`(테스트용 enum 코드 → 409, `$.status==409`, `$.code`, `$.message`, `$.field`), `validationErrorMapsToInvalidInput`(400, `INVALID_INPUT`, field), `unexpectedErrorMapsToInternalError`(500, 내부 메시지 미노출)
  - 프론트 `client.test.ts`: `throwsApiRequestErrorWithCode`
  - `ArchitectureTest` (ArchUnit): `modulesOnlyUseOtherModulesFacadeAndApi()` — 모듈 `transit|pass|itinerary|directions` 각각에 대해, 다른 모듈의 클래스는 그 모듈의 루트 `*Facade`와 `..contract..`만 의존 가능. `domainHasNoSpringDependency()` — `..domain..`은 `org.springframework..`와 `jakarta.persistence..`에 의존하지 않음. `commonDoesNotDependOnModules()` — `common`은 기능 모듈에 의존하지 않음. `ticketDoesNotDependOnFare()` — `com.transitfare.pass..`은 `com.transitfare.transit..`에 전혀 의존하지 않음. 모든 규칙 `allowEmptyShould(true)`(0단계엔 대상 클래스 없음).
- [ ] **Step 5:** `cd backend && ./gradlew test` → FAIL(health 404).
- [ ] **Step 6:** `HealthController`, `GlobalExceptionHandler`, `OpenApiConfig`(제목 "여행 교통비 계산기 API"), `ModuleFlywayConfig` 구현. 퍼사드가 아직 없으므로 가짜 빈도 필요 없다.
- [ ] **Step 7:** `./gradlew test` → PASS.
- [ ] **Step 8:** `npm create vite@latest frontend -- --template react-ts`, `npm i @mantine/core @mantine/hooks @mantine/dates dayjs @tanstack/react-query react-router-dom @vis.gl/react-google-maps`, `npm i -D vitest @testing-library/react @testing-library/jest-dom jsdom`. `vite.config.ts`: `/api` → `http://localhost:8080` 프록시, vitest `environment:'jsdom'`. `App`을 `MantineProvider theme={theme}`로 감싼다(테마 세부는 Task 9에서 frontend-design 스킬로).
- [ ] **Step 9:** `App.test.tsx` — 제목 "여행 교통비 계산기" 표시. `npx vitest run` FAIL → 구현 → PASS. `App.tsx`에 라우트 `/`, `/trips/:id`, `/trips/:id/print`, `/admin` + 빈 페이지, `api/types.ts`에 Task 2~15의 모든 요청·응답 타입(백엔드 DTO와 같은 이름), `api/client.ts`.
- [ ] **Step 9-1: README 뼈대** — 섹션: 소개(한 문단), 구조(모듈 4개와 퍼사드 규칙), 실행 방법(`cd backend && ./gradlew bootRun`, `cd frontend && npm run dev`, `./gradlew test`, `./gradlew goldenTest`, `npx vitest run`), 작업 규칙(아래 그대로), 문서 목록(`docs/modules/*.md`, `docs/data-sources.md`, `docs/google-setup.md` — 아직 없음 표시).
  작업 규칙 내용: ① 다른 모듈은 `XxxFacade`와 `contract` 패키지만 사용, ② 모듈 간 FK·JOIN 금지, ID만 저장, ③ 브랜치 구조 `main` ← `dev` ← `feat/{transit|pass|itinerary|directions|frontend|data}`(dev에서 분기, dev에 `--no-ff` 머지), 커밋 4단계(도메인(테이블) → 계산 로직 → 저장소·유스케이스·퍼사드 → 컨트롤러)와 메시지 형식 `type(module): 대상 + 한 일`(예: `요금 계산 엔진 도메인 생성`, `구간 요금 계산 API 구현`)(이 계획의 "커밋·브랜치 규칙" 그대로), ④ 마이그레이션 번호표(이 계획의 표 그대로), 커밋된 마이그레이션 수정 금지, ⑤ 수정 금지 공용 파일 목록(`build.gradle.kts`, `application*.yml`, `common/*`, `frontend/package.json`, `frontend/src/App.tsx`, `README.md`, `CLAUDE.md`, `docs/contracts.md`) — 바꿔야 하면 커밋 메시지에 `[contract]`를 붙이고 합칠 때 조정. 모듈 계약(`contract` 타입·퍼사드)은 `docs/contracts.md`대로 각 모듈이 만들고, 바꿀 때는 문서를 먼저 고친다, ⑥ 요금·승차권 값은 `source_url`·`verified_on` 필수, 확인 못 한 값은 넣지 않음, ⑦ API 키 커밋 금지.
- [ ] **Step 9-2: CLAUDE.md** — 루트 `CLAUDE.md`(계획 작성 때 이미 만들어 둠: 브랜치·커밋 나누기·커밋 메시지 규칙)를 그대로 포함. "커밋·브랜치 규칙"이 바뀌면 이 파일도 같이 고친다.
- [ ] **Step 10: 커밋** (`main`) `chore: 골격, 모듈 계약, 모듈별 Flyway` → `git branch dev` — `dev`가 1단계 모든 세션의 출발점.
- [ ] **Step 11: GitHub 비공개 저장소** — `gh auth status`로 로그인 확인(안 돼 있으면 사용자가 `gh auth login` 직접 실행). `gh repo create transitfare --private --source . --remote origin` → `git push -u origin main dev`. 커밋 전 `git status`로 `.env.local`·API 키·`backend/data/`가 올라가지 않는지 확인. 이후 푸시는 사용자가 요청할 때만.

---

### Task 2: transit 모듈 — 지역·운영사·역·요금 데이터 스키마와 조회

**Files:**
- Create: `db/migration/transit/V1__transit_schema.sql`, `db/migration/transit/V2__transit_seed_region_operators.sql`, `transit/entity/*`, `transit/repository/*`(+`JpaFareData`), `transit/domain/{FareData,FareRuleType,StationNameNormalizer}.java`, `transit/contract/{StationView,OperatorView,RegionView}.java`, `transit/usecase/{SearchStationsUseCase,ListOperatorsUseCase,GetRegionUseCase}.java`, `transit/TransitFacadeImpl.java`, `transit/controller/StationController.java`
- Test: `transit/repository/JpaFareDataTest.java`, `transit/controller/StationControllerTest.java`

**Interfaces:**
- 테이블(모두 `transit.` 스키마, 컬럼명 고정):
  - `region(code PK, name_ko, name_ja, currency, timezone)`
  - `operator(code PK, region_code, name_ja, name_ko, fare_rule_type, fare_basis_note)`
  - `operator_alias(id, operator_code, alias)`
  - `line(id, operator_code, code, name_ja, name_ko)`
  - `station(id, region_code, operator_code, name_ja, name_ko, source_url, verified_on)` — 같은 실제 역도 운영사별 별도 행
  - `line_station(line_id, station_id, cumulative_km numeric(6,1))`
  - `station_alias(id, station_id, alias)` (unique station_id+alias)
  - `distance_fare_band(id, operator_code, kind 'BASE'|'EXPRESS', max_km numeric(6,1), amount, source_url, verified_on)`
  - `pair_fare(id, operator_code, station_a_id, station_b_id, kind 'BASE'|'EXPRESS', amount, source_url, verified_on)` — 양방향 동일
  - `flat_zone(id, operator_code, amount, source_url, verified_on)`, `flat_zone_station(zone_id, station_id)`
  - `reserved_seat_fee(id, operator_code, amount, source_url, verified_on)`
  - `transfer_discount(id, from_operator_code, to_operator_code, discount, note, source_url, verified_on)`
- `FareData`(domain 인터페이스 — JPA 타입 노출 금지):
  ```java
  Optional<OperatorRule> operator(String code);     // record OperatorRule(String code, FareRuleType ruleType)
  Optional<BigDecimal> cumulativeKm(long lineId, long stationId);
  Optional<Integer> distanceBandFare(String operatorCode, String kind, BigDecimal km); // km 이상인 가장 작은 max_km
  Optional<Integer> pairFare(String operatorCode, String kind, long a, long b);
  Optional<Integer> flatZoneFare(String operatorCode, long a, long b);
  Optional<Integer> reservedSeatFee(String operatorCode);
  Optional<Integer> transferDiscount(String fromOperator, String toOperator);
  ```
- `GET /api/v1/regions/{code}` → RegionView, `GET /api/v1/operators?region=JP-FUKUOKA` → `[{code,nameJa,nameKo,fareBasisNote}]`, `GET /api/v1/stations?region=&operator=&q=` → `[{id,operatorCode,nameJa,nameKo,lines:[{lineId,nameKo}]}]`(name_ja/name_ko/alias 부분일치, 최대 30)
- `StationNameNormalizer.normalize(String) -> String`: 공백 제거, 전각 영숫자→반각, 끝의 "駅" 제거.

- [ ] **Step 1:** `V1`에 스키마. `V2`에 `region('JP-FUKUOKA','후쿠오카','福岡','JPY','Asia/Tokyo')`와 운영사: `FUKUOKA_SUBWAY`(DISTANCE_BAND), `JR_KYUSHU`(DISTANCE_BAND), `NISHITETSU_RAIL`(STATION_PAIR), `NISHITETSU_BUS`(FLAT_ZONE_PLUS_PAIR), `NISHITETSU_HIGHWAY_BUS`(STATION_PAIR), `SHOWA_BUS`(STATION_PAIR), `KITAKYUSHU_MONORAIL`(STATION_PAIR), `OTHER`(MANUAL_ONLY). `fare_basis_note`는 공식 규정 확인 후(출처는 SQL 주석).
- [ ] **Step 2: 실패하는 테스트** — `JpaFareDataTest`(@DataJpaTest + 테스트 SQL): `distanceBandFareReturnsSmallestBandCoveringKm()`(3.0→210, 7.0→260, km 3.1 → 260), `pairFareIsSymmetric()`, `flatZoneFareOnlyWhenBothStationsInZone()`; `StationControllerTest.searchMatchesAliasAndKoreanName()`; `StationNameNormalizerTest.stripsSuffixAndWidth()`("博多駅 "→"博多", "ＪＲ"→"JR").
- [ ] **Step 3:** FAIL → **Step 4:** 구현 → **Step 5:** PASS (ArchitectureTest 포함)
- [ ] **Step 6: 커밋**
  - ① `feat(transit): 요금 기준 데이터 도메인 생성` — `V1`, `V2`, entity
  - ② `feat(transit): 역 이름 정규화·요금 조회 포트 로직 구현` — `FareData`, `FareRuleType`, `StationNameNormalizer` + `StationNameNormalizerTest`
  - ③ `feat(transit): 지역·운영사·역 조회 유스케이스 구현` — repository(`JpaFareData`), 조회 유스케이스 3개, `TransitFacadeImpl` + `JpaFareDataTest`
  - ④ `feat(transit): 지역·운영사·역 조회 API 구현` — `StationController` + `StationControllerTest`

---

### Task 3: transit 모듈 — 요금 계산 엔진 (domain, TDD)

**Files:**
- Create: `transit/domain/FareEngine.java`, `transit/contract/{Passengers,LegInput,LegFare,FareStatus,LegMode,TrainClass,SeatType,PricingResult}.java`
- Test: `transit/domain/FareEngineTest.java`, `transit/domain/InMemoryFareData.java`(test 소스)

**Interfaces:**
- Consumes: `FareData`
- Produces: `new FareEngine(FareData).price(List<LegInput>, Passengers) -> List<LegFare>` (입력과 같은 길이)

계산 규칙(설계 문서 7-1):
- `WALK` → OK, 0. `MANUAL` → MANUAL, `manualAmount` 필수.
- `DISTANCE_BAND`: 같은 운영사 TRANSIT 구간이 연속(사이 WALK 무시)하면 각 구간 km(|누적 km 차|) 합으로 BASE 밴드 1회. 금액은 묶음 첫 구간, 나머지 0 + note "앞 구간과 합산". EXPRESS 구간들의 km 합으로 EXPRESS 밴드 → `express`. RESERVED면 `reservedSeatFee` 가산.
  - **예외 구간 우선 (사용자 결정 2026-11-18):** 묶음의 첫 승차역 → 마지막 하차역으로 `pair_fare`(BASE, 특급이면 EXPRESS)를 먼저 찾고, 있으면 그 금액을 쓰고 없을 때만 km 합 밴드로 계산. 테스트 `distanceBandOperatorUsesPairFareWhenDefined`.
- `STATION_PAIR`: `pairFare(BASE)` (+EXPRESS면 `pairFare(EXPRESS)`). `FLAT_ZONE_PLUS_PAIR`: 같은 zone이면 zone 요금, 아니면 pair.
- 조회 실패 → 그 구간(묶음 전체) UNKNOWN, 금액 0, note에 빠진 항목.
- 환승 할인: 직전 TRANSIT 운영사 → 다음 TRANSIT 운영사 쌍에 할인이 있으면 다음 구간 base에서 차감.
- `partyTotal = (base + express + reserved) * adults`.

- [ ] **Step 1: 실패하는 테스트**(가상 데이터, 실제 요금 아님):
  `walkLegIsFree`, `subwayTransferWithinOperatorChargedOnceByTotalDistance`(2.0km+2.0km, 사이 WALK, 밴드 3.0→210/7.0→260 → 260, 0+note), `differentOperatorBreaksDistanceGroup`(지하철2km+버스+지하철2km → 210 두 번), `jrExpressAddsExpressBandAndReservedFee`(30km, BASE 30→570, EXPRESS 50→760, 지정석 530), `jrLineChangeSumsKm`(35.7+20.0 → 55.7km 1회), `missingFareMarksUnknownNotZeroCharge`, `flatZoneUsedWhenBothInZone`, `pairFareUsedOutsideZone`, `transferDiscountSubtractedFromSecondLeg`, `partyTotalMultipliesByAdults`(Passengers(3)), `manualLegUsesGivenFare`
- [ ] **Step 2:** `./gradlew test --tests '*FareEngineTest'` → FAIL → **Step 3:** 구현 → **Step 4:** PASS
- [ ] **Step 5: 커밋** ② `feat(transit): 요금 계산 로직 구현` — `FareEngine` + `FareEngineTest` (①③④ 없음)

---

### Task 4 (데이터 세션): 1단계 지역 요금 데이터 + 골든 테스트 (조사 포함)

값은 계획에 없다. 실행자가 공식 자료에서 확인해 넣고, 테스트 기대값도 같은 공식 자료에서 가져온다.

**Files:**
- Create: `db/migration/transit/V100__transit_seed_region1.sql`
- Test: `seed/GoldenFareTest.java`(@SpringBootTest, `@Tag("golden")`, `TransitFacade.priceLegs` 사용)

**대상(이번 여행 지역)**
- 후쿠오카 지하철 3개 노선 전 역 + 누적 km + 거리 밴드 — 후쿠오카시 교통국
- JR 큐슈: 가고시마 본선 하카타~구루메, 규다이 본선 구루메~히타(우키하 포함), 지쿠히선 메이노하마~지쿠젠마에바루 — 역별 영업 km, 운임 밴드, 특급요금 밴드, 지정석 요금, 특정 구간 예외는 `pair_fare` — JR 큐슈 공식
- 니시테츠 버스: 도심 균일 구간(flat_zone) + 하카타역·텐진·후쿠오카 공항 주요 정류장 간 요금
- 니시테츠 고속버스 히타호: 하카타 버스터미널·텐진·우키하 방면·히타 정류장 간 요금
- 쇼와 버스는 Task 13(GTFS 가져오기)에서
- `station_alias`(일본어 표기 변형), `operator_alias`(Google 운영사명 후보: "福岡市地下鉄", "JR九州", "西鉄バス" 등 — Task 12 실제 응답으로 보강)

- [ ] **Step 1:** 조사 후 `V100` 작성(모든 행 `source_url`, `verified_on`).
- [ ] **Step 2: 골든 테스트**(각 메서드 위 주석에 출처 URL): `subwayHakataToTenjin`, `subwayHakataToFukuokaAirport`, `subwayTransferAirportLineToNanakumaLine`(하카타→야쿠인), `subwayPlusJrHakataToChikuzenMaebaru`, `jrHakataToHitaLocal`, `jrHakataToHitaExpressReserved`, `jrHakataToUkiha`, `highwayBusHitaGoHakataToHita`, `nishitetsuBusInsideFlatZone`
- [ ] **Step 3:** 1단계에서는 `./gradlew test`로 마이그레이션이 H2에 적용되는지만 확인(엔진이 아직 없으므로 골든은 제외됨). **Step 4:** `./gradlew goldenTest` 실행과 불일치 수정은 1차 합치기(Task M1)에서.
- [ ] **Step 5: 커밋** `data(transit): 1단계 지역 요금 데이터 추가` → `test(transit): 1단계 지역 골든 테스트 추가`

---

### Task 5: transit 모듈 — 요금 계산 유스케이스·퍼사드·API

**Files:**
- Create: `transit/usecase/PriceLegsUseCase.java`, `transit/controller/PricingController.java`
- Modify: `transit/TransitFacadeImpl.java`
- Test: `transit/usecase/PriceLegsUseCaseTest.java`, `transit/controller/PricingControllerTest.java`

**Interfaces:**
- `TransitFacade.priceLegs(regionCode, legs, passengers) -> PricingResult`
- `POST /api/v1/pricing` `{"regionCode":"JP-FUKUOKA","passengers":{"adults":2},"legs":[LegInput...]}` → PricingResult
- 검증: 음수 인원, 합계 0 → 400 "인원을 1명 이상 입력하세요". 다른 지역 운영사가 섞이면 400.

- [ ] **Step 1: 실패하는 테스트** — `PriceLegsUseCaseTest`: `pricesLegsAndSumsParty`, `unknownLegSetsHasUnknown`, `operatorFromOtherRegionRejected`; `PricingControllerTest`: `zeroPassengersRejected`(400 메시지), `priceEndpointReturnsPricingResult`
- [ ] **Step 2:** FAIL → **Step 3:** 구현 → **Step 4:** PASS
- [ ] **Step 5: 커밋**
  - ③ `feat(transit): 구간 요금 계산 유스케이스 구현` — `PriceLegsUseCase`, `TransitFacadeImpl.priceLegs` + `PriceLegsUseCaseTest`
  - ④ `feat(transit): 구간 요금 계산 API 구현` — `PricingController` + `PricingControllerTest`

---

### Task 6: itinerary 모듈 — 여행·일정 저장

**Files:**
- Create: `db/migration/itinerary/V1__itinerary_schema.sql`, `itinerary/entity/*`, `itinerary/repository/*`, `itinerary/usecase/{Create,Update,Delete,List,Get}TripUseCase.java`, `itinerary/usecase/{Add,Update,Delete,Move}SegmentUseCase.java`, `itinerary/contract/{TripDto,SegmentDto}.java`, `itinerary/controller/TripController.java`
- Test: `itinerary/usecase/TripUseCasesTest.java`, `itinerary/controller/TripControllerTest.java`

**Interfaces:**
- 테이블(`itinerary.`): `trip(id, region_code, name, start_date, end_date, lodging_place_id null, lodging_name null, adults, local_to_krw numeric(10,4) null, created_at)`, `segment(id, trip_id, travel_date, seq, origin_place_id null, origin_name, dest_place_id null, dest_name)`, `leg(id, segment_id, seq, mode, operator_code null, line_id null, from_station_id null, to_station_id null, train_class, seat_type, manual_amount null, memo null, google_line_name null, google_from_name null, google_to_name null)` — operator/line/station은 transit 모듈의 ID 값만(FK 없음).
- API: `GET/POST /api/v1/trips`, `GET/PUT/DELETE /api/v1/trips/{id}`, `POST /api/v1/trips/{id}/segments` `{travelDate, originPlaceId?, originName, destPlaceId?, destName, legs:[LegInput...]}`, `PUT/DELETE /api/v1/trips/{id}/segments/{segmentId}`, `POST .../segments/{segmentId}/move` `{direction:"UP"|"DOWN"}`.
- 생성 시 `regionCode`는 `TransitFacade.getRegion`으로 존재 확인(없으면 400). 2차에서 시작하므로 실제 `TransitFacade`(시드 지역 `JP-FUKUOKA`) 사용. 검증: endDate<startDate 400, 구간 날짜가 기간 밖 400 "여행 기간 밖의 날짜입니다"(리뷰 포커스 4), 인원 검증은 Task 5와 같은 메시지. 좌표 저장 안 함.

- [ ] **Step 1: 실패하는 테스트** — `TripUseCasesTest`: `createTripAndList`, `unknownRegionRejected`, `segmentOutsideTripDatesRejected`, `segmentKeepsLegOrder`, `moveSegmentSwapsSeq`, `deleteTripCascadesSegments`; `TripControllerTest`: `createTripReturnsCreatedTrip`, `validationErrorReturns400WithMessage`
- [ ] **Step 2:** FAIL → **Step 3:** 구현 → **Step 4:** PASS
- [ ] **Step 5: 커밋** (계산 로직 없음)
  - ① `feat(itinerary): 여행·일정 도메인 생성` — `V1`, entity
  - ③ `feat(itinerary): 여행·일정 저장 유스케이스 구현` — repository, 유스케이스 9개, `ItineraryFacade`·`ItineraryFacadeImpl` + `TripUseCasesTest`
  - ④ `feat(itinerary): 여행·일정 API 구현` — `TripController`(→ `ItineraryFacade`) + `TripControllerTest`

---

### Task 7: pass 모듈 — 승차권 상품과 최적화

**Files:**
- Create: `db/migration/pass/V1__pass_schema.sql`, (데이터 세션) `db/migration/pass/V100__pass_seed_products.sql`, `pass/domain/{TicketOptimizer,ProductSpec}.java`, `pass/contract/*`, `pass/entity/*`, `pass/repository/*`, `pass/usecase/{ListProductsUseCase,OptimizeTicketsUseCase}.java`, `pass/PassFacadeImpl.java`, `pass/controller/ProductController.java`
- Test: `pass/domain/TicketOptimizerTest.java`, `pass/usecase/OptimizeTicketsUseCaseTest.java`, `pass/controller/ProductControllerTest.java`

**Interfaces:**
- 테이블(`pass.`): `ticket_product(code PK, region_code, type 'PASS'|'FIXED_ROUTE', name_ja, name_ko, price, valid_days, consecutive_days, uses, covers_express, reserved_seat_limit, eligibility_note null, source_url, verified_on)`, `ticket_coverage(id, product_code, operator_code, line_id null, station_a_id null, station_b_id null)`
- `ProductSpec(String code, ProductType type, int price, int validDays, int uses, boolean coversExpress, int reservedSeatLimit, List<Coverage> coverage)`, `Coverage(String operatorCode, Long lineId, Long stationAId, Long stationBId)`
- `TicketOptimizer.optimize(List<TicketLeg>, List<ProductSpec>, TicketParty) -> OptimizationResult` — 입력 타입은 `docs/contracts.md`의 pass 부분(transit 타입 사용 금지)
- `GET /api/v1/products?region=` → `[{code,type,nameKo,nameJa,price,validDays,eligibilityNote}]`(type은 `{code,label,description}`)
- `PassFacade.optimize`는 overrides를 적용(`included=false` 제외, `priceAmount`로 가격 대체)한 ProductSpec으로 최적화.

알고리즘(계획이 정함):
- 어른 1명 기준으로 최적화하고, 구매 수량 = 인원(`TicketParty.adults`). 총액 = 1명 기준 결과 × 인원.
- 커버 판정: 구간 operator가 coverage와 일치하고, line/station 지정 시 그것도 일치(FIXED_ROUTE는 양 끝이 stationA/B 쌍, 방향 무관). `fareKnown=false` 구간은 커버 불가.
- 절감: base 전액 + (`coversExpress`면 express 전액, reserved는 `reservedSeatLimit` 횟수까지).
- 연속 거리제 묶음은 묶음 전 구간이 커버될 때만 절감.
- 후보 구매 = PASS × 여행 각 날짜 시작 + FIXED_ROUTE × 1. **최대 3개 구매 조합 전수 탐색**(후보 > 60이면 단독 절감액 상위 60개). 구간마다 가장 큰 절감을 주는 구매에 배정(FIXED_ROUTE는 uses 한도, 첫 사용일부터 validDays). 총액 = 미커버 요금 + 구매가. 최소 선택, 동률이면 구매 수 적은 쪽. 다일 패스는 여행 끝을 넘어도 구매 가능.

- [ ] **Step 1: 실패하는 테스트**(가상 데이터): `noProductCheaperKeepsBaseline`, `oneDayPassChosenWhenDaySpendExceedsPrice`, `threeDayPassCoversConsecutiveDaysOnly`, `passWithoutExpressDoesNotCoverExpressSurcharge`, `reservedSeatLimitRespected`(한도 2, 지정석 3개), `unknownLegNeverCovered`, `fixedRouteRoundTripCoversBothDirections`, `purchaseCountEqualsAdults`(TicketParty(3)), `multiDayPassMayExtendPastTripEnd`, `distanceGroupCoveredOnlyWhenAllLegsCovered`; `OptimizeTicketsUseCaseTest`: `overrideExcludedProductIgnored`, `overridePriceReplacesOfficialPrice`; `ProductControllerTest`: `listsProductsByRegion`
- [ ] **Step 2:** FAIL → **Step 3:** 구현 → **Step 4:** PASS
- [ ] **Step 5 (데이터 세션):** `V100` 승차권 시드: SUNQ 패스 북큐슈(기간별), JR 큐슈 레일패스 북큐슈(기간별, eligibility_note "단기체류 외국인 전용"), 후쿠오카 지하철 1일 승차권, 니시테츠 버스 1일 승차권, 후쿠오카 투어리스트 시티 패스, 확인된 왕복권·회수권(히타호 등). coverage·가격(어른)·특급/지정석 조건은 공식 자료로.
- [ ] **Step 6 (데이터 세션):** `GoldenFareTest`에 `sunqPassCoversHitaGo`, `jrPassCoversHakataHitaExpress` 작성 — `TransitFacade.priceLegs` 결과를 `docs/contracts.md`의 변환 표대로 `TicketLeg`로 바꿔 `PassFacade.optimize`에 넣는다(실행은 Task M1)
- [ ] **Step 7: 커밋**
  - pass 세션: ① `feat(pass): 승차권 상품 도메인 생성` — `V1`, entity → ② `feat(pass): 승차권 최적화 로직 구현` — `contract`(`TicketLeg`, `TicketParty`, `ProductType`, `OptimizationResult`, `Purchase`), `TicketOptimizer`, `ProductSpec`, `ProductType` + `TicketOptimizerTest` → ③ `feat(pass): 승차권 상품·최적화 유스케이스 구현` — repository, 유스케이스 2개, `PassFacade`(인터페이스)·`PassFacadeImpl`, 나머지 `contract`(`ProductView`, `ProductOverride`) + `OptimizeTicketsUseCaseTest` → ④ `feat(pass): 승차권 상품 조회 API 구현` + `ProductControllerTest`
  - 데이터 세션: `data(pass): 승차권 상품 데이터 추가` → `test(pass): 승차권 골든 테스트 추가`

---

### Task 8: itinerary 모듈 — 승차권 설정과 여행 요약

**Files:**
- Create: `db/migration/itinerary/V2__itinerary_product_setting.sql`, `itinerary/usecase/{ListProductSettingsUseCase,UpdateProductSettingUseCase,GetTripSummaryUseCase,TicketLegMapper}.java`, `itinerary/contract/{SummaryDto,ProductSettingDto}.java`
- Modify: `itinerary/controller/TripController.java`
- Test: `itinerary/usecase/GetTripSummaryUseCaseTest.java`, `itinerary/usecase/UpdateProductSettingUseCaseTest.java`, `itinerary/usecase/TicketLegMapperTest.java`, `itinerary/controller/TripControllerTest.java`

**Interfaces:**
- Consumes: `TransitFacade.priceLegs`, `TransitFacade.getRegion`, `PassFacade.listProducts`, `PassFacade.optimize` — 2차에서 시작하므로 실제 빈과 시드 데이터 사용. 금액 기대값은 골든 테스트 값 또는 `TransitFacade` 결과로 계산(하드코딩 금지).
- `TicketLegMapper`: transit의 `LegInput`+`LegFare`+날짜 → ticket의 `TicketLeg`(`docs/contracts.md` 변환 표 그대로, WALK 제외). 인원은 `Passengers` → `TicketParty`.
- 테이블: `itinerary.trip_product_setting(trip_id, product_code, included default true, purchase_price null, purchase_currency null)` — `purchase_currency`는 지역 통화 코드 또는 'KRW'
- `GET /api/v1/trips/{id}/products` → `[{code,nameKo,officialPrice,included,purchasePrice,purchaseCurrency,effectivePrice,eligibilityNote}]`
- `PUT /api/v1/trips/{id}/products/{code}` `{included, purchasePrice?, purchaseCurrency?}` — KRW인데 `local_to_krw` 없음 → 400 "환율을 먼저 입력하세요"(리뷰 포커스 3)
- `GET /api/v1/trips/{id}/summary` →
  ```json
  {"currency":"JPY","localToKrw":9.2,
   "days":[{"date":"2026-01-15","segments":[{"segmentId":1,"legs":[LegFare...],"partyTotal":0,"hasUnknown":false}],"dayTotal":0}],
   "baselineTotal":0,"optimizedTotal":0,"savings":0,
   "purchases":[{"productCode":"","nameKo":"","startDate":"","count":2,"unitPrice":0}],
   "hasUnknown":false,"fareBasisNotes":["후쿠오카 지하철: IC카드 요금 기준"]}
  ```
  원화 환산은 프론트가 `localToKrw`로(서버는 환율만 전달).
- 유효 구매가: KRW면 `round(price / localToKrw)` 현지 통화, 아니면 그대로, 없으면 공식가.

- [ ] **Step 1: 실패하는 테스트** — `TicketLegMapperTest`: `mapsExpressReservedAndAmounts`, `walkLegsExcluded`, `unknownAndManualLegsMarkedFareUnknown`, `keepsGroupIdForDistanceGroups`; `summaryGroupsByDayAndSums`, `excludedProductNotRecommended`, `purchasePriceOverridesOfficial`, `krwPurchasePriceConvertedWithTripRate`, `krwPurchaseWithoutRateRejected`, `unknownLegPropagatesHasUnknown`; `TripControllerTest`: `summaryEndpointReturnsSummary`, `updateProductSettingEndpoint`
- [ ] **Step 2:** FAIL → **Step 3:** 구현 → **Step 4:** PASS
- [ ] **Step 5: 커밋**
  - ① `feat(itinerary): 승차권 설정 도메인 생성` — `V2`, entity `TripProductSetting`
  - ③ `feat(itinerary): 승차권 설정·여행 요약 유스케이스 구현` — repository, 유스케이스 3개, `TicketLegMapper` + 유스케이스 테스트 2개, `TicketLegMapperTest`
  - ④ `feat(itinerary): 승차권 설정·여행 요약 API 구현` — `TripController` 추가분 + `TripControllerTest` 추가분

---

### Task 9: 프론트 — 디자인 방향, 여행 목록·설정·일정 입력(역 직접 선택)·요약

**Files:**
- Create: `frontend/src/api/mock.ts`, `theme.ts`(0단계 파일 채움),
- Modify: `frontend/src/api/client.ts`(mock 전환), `pages/{TripListPage,TripPage}.tsx`, `components/{TripSettingsForm,DayList,SegmentEditor,ManualLegForm,SummaryPanel,ProductSettings,Money}.tsx`
- Test: `components/Money.test.tsx`, `components/SummaryPanel.test.tsx`, `components/ManualLegForm.test.tsx`

**Interfaces:**
- Consumes: Task 2, 5, 6, 7, 8 API(0단계 `api/types.ts`). 1단계에서는 `VITE_USE_MOCK=true`면 `api/client.ts`가 `api/mock.ts`(메모리에 여행·구간을 저장하고 계약 형태의 요금·요약·후보를 돌려주는 가짜 API)를 쓴다. 테스트도 mock.
- `<Money amount currency localToKrw />` → JPY면 `"1,230엔"`, localToKrw 있으면 `"1,230엔 (약 11,316원)"`(원 반올림, 콤마).
- 라우트: `/`, `/trips/:id`, `/trips/:id/print`(Task 14), `/admin`(Task 15).

- [ ] **Step 1: 디자인 방향** — `frontend-design` 스킬을 실행해 이 앱의 디자인 방향(색, 글꼴, 레이아웃)을 정하고 `theme.ts`(Mantine `createTheme`)에 반영. 조건: 한국어 UI, PC 화면, 부가 정보는 배지 대신 보조 텍스트, "미확인"은 경고색 텍스트, 금액이 가장 눈에 띄어야 함.
- [ ] **Step 2: 실패하는 테스트** — `Money`: `formatsLocalOnly`, `appendsKrwWhenRateGiven`; `SummaryPanel`: `showsUnknownWarning`, `showsSavingsAndPurchases`; `ManualLegForm`: `requiresAdultFare`
- [ ] **Step 3:** `npx vitest run` FAIL
- [ ] **Step 4: 구현** — 화면 동작:
  - 여행 설정: 이름, 지역(현재 후쿠오카만), 기간(Mantine DatePicker), 숙소 이름, 인원, 환율(선택).
  - 날짜별 일정과 `SegmentEditor`: 이 태스크에서는 "역 직접 선택"(운영사 → 역 검색 → 특급/보통·지정석/자유석 → `/api/v1/pricing` 미리보기, 탈것 여러 개)과 "요금 직접 입력"(운영사 '기타', 메모, 1인 금액) 두 모드.
  - 구간 요금 내역 펼치기(운임/특급/지정석). UNKNOWN은 "미확인" 경고 텍스트 + "역 다시 선택 / 요금 직접 입력".
  - `SummaryPanel`: 날짜별 합계, 개별 요금 합계, 추천 구매와 절약액, 미확인 경고, `fareBasisNotes`.
  - `ProductSettings`: "비교에 포함", "내 구매가"(현지 통화/원), 이용 조건. 원 선택 시 환율 없으면 안내 문구.
- [ ] **Step 5:** PASS
- [ ] **Step 6: 수동 확인** — `VITE_USE_MOCK=true npm run dev`로 여행 생성 → 구간 입력 → 요약 흐름(실제 백엔드 확인은 Task I1).
- [ ] **Step 7: 커밋** (각 커밋마다 해당 컴포넌트 테스트 통과) `feat(frontend): Mantine 테마 생성` → `feat(frontend): 가짜 API·금액 표시 구현` → `feat(frontend): 여행 목록·설정 화면 구현` → `feat(frontend): 일정 입력 화면 구현` → `feat(frontend): 여행 요약·승차권 설정 화면 구현`

---

### Task 10: 경로 후보, 역 매칭, 요금 검증

**10-A는 transit 세션**(`transit/usecase/MatchStepUseCase`, `TransitFacadeImpl.matchStep`, `MatchStepUseCaseTest`), **10-B는 directions 세션**(나머지 전부, 2차에서 시작하므로 실제 `TransitFacade` 사용 — 매칭 실패 케이스는 픽스처에 시드에 없는 역 이름을 넣어 재현).

**Files:**
- Create: `db/migration/directions/V1__directions_schema.sql`, `directions/domain/{RouteProvider,RouteCandidate,CandidateStep,ThroughServiceSplitter}.java`, `directions/provider/FakeRouteProvider.java`, `directions/entity/{MatchFailure,FareMismatch}.java`, `directions/repository/*`, `directions/usecase/SearchRoutesUseCase.java`, `directions/contract/{PlaceRef,RouteSearchRequest,CandidateView}.java`, `directions/DirectionsFacadeImpl.java`, `directions/controller/RouteController.java`, `transit/usecase/MatchStepUseCase.java`, `transit/contract/{StepToMatch,MatchedLeg}.java`, `resources/fake-routes/*.json`
- Modify: `transit/TransitFacadeImpl.java`(matchStep)
- Test: `transit/usecase/MatchStepUseCaseTest.java`, `directions/domain/ThroughServiceSplitterTest.java`, `directions/usecase/SearchRoutesUseCaseTest.java`, `directions/controller/RouteControllerTest.java`

**Interfaces:**
```java
record CandidateStep(String mode /*TRANSIT|WALK*/, String agencyName, String lineName, String vehicleType,
                     String trainName, String departureStopName, String arrivalStopName) {}
record RouteCandidate(List<CandidateStep> steps, Integer googleFareAmount /*없으면 null*/, String encodedPolyline /*가짜는 null*/) {}
interface RouteProvider { List<RouteCandidate> findRoutes(PlaceRef origin, PlaceRef destination, LocalDate date); }
```
- `directions.provider=fake` → `FakeRouteProvider`(픽스처를 origin/destination name 쌍으로 조회, 없으면 빈 목록), `google` → Task 12 (`@ConditionalOnProperty`).
- `GET /api/v1/routes/fake-places` → 픽스처 장소 이름 목록(가짜일 때만, 아니면 404)
- `POST /api/v1/routes/search` `{regionCode, origin:{placeId,name}, destination:{placeId,name}, date, passengers}` → `[{candidateIndex, legs:[{input:LegInput, transit:LegFare, googleLineName, googleFromName, googleToName}], partyTotal, hasUnknown, summary:"지하철 → 니시테츠 전철", encodedPolyline}]`(파티 총액 오름차순, Google 요금 필드는 응답에 없음)
- 테이블(`directions.`): `match_failure(id, region_code, agency_name, stop_name, line_name, count, last_seen_at)`(region+agency+stop+line unique), `fare_mismatch(id, region_code, route_desc, our_adult_amount, google_adult_amount, created_at)`

규칙(계획이 정함):
- **매칭(transit 모듈 `MatchStepUseCase`)**: 운영사 = `operator_alias.alias == agencyName`(정확히), 없으면 실패. 역 = 그 운영사 역의 alias·name_ja 정확 일치 → 없으면 `StationNameNormalizer` 후 일치. 노선 = 두 역이 모두 속한 line(여럿이면 `name_ja`가 lineName 포함, 그래도 여럿이면 첫 번째). `trainName` 또는 `lineName`에 "特急" → EXPRESS, 아니면 LOCAL. 좌석 기본 UNRESERVED(사용자가 바꿈).
  - **사용자 결정 (2026-11-20, transit 구현 `c0d72c2`):** ① 같은 별칭의 운영사가 여럿이면(예: Google "西日本鉄道" = 니시테츠 전철·버스) 승차·하차역이 둘 다 있는 첫 운영사(코드 순). Task 12 실제 응답에서 문제가 되면 `StepToMatch`에 `vehicleType` 추가를 재검토. ② `matchStep`은 에러를 던지지 않는다(없는 지역·빈 정류장 이름 포함) — 결과가 비면 directions가 UNKNOWN + `match_failure`. ③ 두 역의 공통 노선이 없으면 `MatchedLeg.lineId = null` → 거리제(JR·지하철)는 거리 미확인(UNKNOWN).
- **직통 분할(directions `ThroughServiceSplitter`)**: 한 단계의 승차역은 지하철, 하차역은 JR(또는 반대)이면 경계역 `姪浜`에서 두 단계로 분할. 상수 `BOUNDARIES = Map.of("福岡市地下鉄|JR九州", "姪浜")`(agency 이름 쌍 → 경계역 일본어명, Task 12 실제 응답 보고 보정).
  - **노선을 넘어 달리는 JR 직통 열차도 분할 대상**(사용자 결정 2026-11-20): 예) 특급 유후 博多→日田는 久留米에서 鹿児島本線→久大本線으로 바뀌므로 `"JR九州|JR九州" → "久留米"` 같은 항목을 경계역 표에 둔다. 분할하지 않으면 공통 노선이 없어 `lineId = null` → 요금 미확인. Task 12 실제 응답으로 보정. 테스트 `jrThroughTrainSplitAtKurume`.
- 매칭 실패 → `LegInput(TRANSIT, operatorCode=null…)`로 요금 UNKNOWN, `match_failure` upsert(count+1). 후보 유지(리뷰 포커스 5).
- **요금 검증**: `googleFareAmount != null`이고 모든 구간 OK면 어른 1명 기준 합계와 비교, 다르면 `fare_mismatch` 저장(같은 route_desc가 같은 날 있으면 생략).

- [ ] **Step 1: 실패하는 테스트**
  - `MatchStepUseCaseTest`: `matchesByAlias`, `matchesAfterNormalizingStationSuffix`, `unknownAgencyFails`, `detectsExpressFromTrainName`
  - `ThroughServiceSplitterTest`: `throughServiceSplitAtMeinohama`
  - `SearchRoutesUseCaseTest`: `candidatesSortedByPartyTotal`, `unmatchedStepBecomesUnknownAndLogsFailureOnce`(두 번 호출 → count 2, 행 1), `googleFareMismatchRecorded`, `googleFareIgnoredWhenAnyLegUnknown`
  - `RouteControllerTest`: `googleFareNeverReturnedToClient`, `fakePlacesOnlyWithFakeProvider`
- [ ] **Step 2:** FAIL → **Step 3:** 구현 + 픽스처(하카타역→다자이후, 하카타역→히타, 하카타역→지쿠젠마에바루, 하카타역→캐널시티, 매칭 실패가 섞인 후보 1개) → **Step 4:** PASS
- [ ] **Step 5: 커밋**
  - transit 세션(10-A): ③ `feat(transit): 경로 단계 역 매칭 유스케이스 구현` — `MatchStepUseCase`, `TransitFacadeImpl.matchStep` + `MatchStepUseCaseTest`
  - directions 세션(10-B): ① `feat(directions): 매칭 실패·요금 불일치 도메인 생성` — `V1`, entity → ② `feat(directions): 경로 후보·직통 운전 분할 로직 구현` — `RouteProvider`, `RouteCandidate`, `CandidateStep`, `ThroughServiceSplitter` + `ThroughServiceSplitterTest` → ③ `feat(directions): 경로 후보 검색 유스케이스 구현` — repository, `FakeRouteProvider`+픽스처, `SearchRoutesUseCase`, `DirectionsFacadeImpl` + `SearchRoutesUseCaseTest` → ④ `feat(directions): 경로 검색 API 구현` — `RouteController` + `RouteControllerTest`

---

### Task 11: 프론트 — 장소 입력과 경로 후보 선택

**Files:**
- Create: `components/{PlaceInput,CandidateList}.tsx`
- Modify: `components/SegmentEditor.tsx`
- Test: `components/CandidateList.test.tsx`

**Interfaces:**
- 1단계에서는 `api/mock.ts`에 후보 검색·fake-places 응답 추가.
- `<PlaceInput value onChange />` — `VITE_GOOGLE_MAPS_API_KEY` 없으면 `/api/v1/routes/fake-places` select, 있으면 Task 12에서 Google 자동완성. 숙소가 출발지 기본값, 도착지에도 "숙소" 빠른 선택.
- `<CandidateList candidates onSelect />` — 후보 요약, 파티 총액, 미확인 경고, 펼치면 구간 내역과 특급/보통·지정석/자유석 토글(토글 시 `/api/v1/pricing` 재계산). 선택 → Task 6 API로 구간 저장.
- `SegmentEditor` 탭: "경로 검색"(기본) / "역 직접 선택" / "요금 직접 입력".

- [ ] **Step 1: 실패하는 테스트** — `rendersCandidatesCheapestFirst`, `showsUnknownWarningText`, `togglingSeatTypeRequestsReprice`(fetch mock)
- [ ] **Step 2:** FAIL → **Step 3:** 구현 → **Step 4:** PASS
- [ ] **Step 5: 수동 확인** — mock으로 하카타역→히타 검색 → 후보 선택 → 요약 갱신
- [ ] **Step 6: 커밋** `feat(frontend): 장소 입력 화면 구현` → `feat(frontend): 경로 후보 선택 화면 구현`

---

### Task 12: Google 연동 — Routes 제공자, Places 자동완성, 지도

**12-A는 directions 세션**(`GoogleRoutesProvider`, 테스트, `docs/google-setup.md`), **12-B는 frontend 세션**(`PlaceInput` Google 자동완성, `RouteMap`). Step 5 수동 확인은 Task I1.

**사전 조건:** 사용자가 Google Cloud 프로젝트 생성, 결제카드 등록, Routes API·Places API(New)·Maps JavaScript API 사용 설정, 키 2개(서버 키: Routes API 제한 / 브라우저 키: referrer `http://localhost:5173/*`), 일일 사용량 상한 설정. 실행자는 절차를 `docs/google-setup.md`로 안내. 키가 없으면 코드·테스트만 진행하고 수동 확인은 미룸.

**Files:**
- Create: `directions/provider/GoogleRoutesProvider.java`, `docs/google-setup.md`, `frontend/src/components/RouteMap.tsx`
- Modify: `components/{PlaceInput,CandidateList}.tsx`
- Test: `directions/provider/GoogleRoutesProviderTest.java`(MockRestServiceServer + 응답 JSON 픽스처)

**Interfaces:**
- `POST https://routes.googleapis.com/directions/v2:computeRoutes`, 헤더 `X-Goog-Api-Key`, `X-Goog-FieldMask: routes.legs.steps.travelMode,routes.legs.steps.transitDetails,routes.travelAdvisory.transitFare,routes.polyline.encodedPolyline`, body `{origin:{placeId}, destination:{placeId}, travelMode:"TRANSIT", computeAlternativeRoutes:true, departureTime:<date 09:00 지역 timezone>, languageCode:"ja"}`. 매핑: agency=`transitDetails.transitLine.agencies[0].name`, lineName=`transitLine.name`, vehicleType=`transitLine.vehicle.type`, trainName=`transitLine.nameShort`+" "+`transitDetails.headsign`, 정류장=`stopDetails.departureStop.name`/`arrivalStop.name`, transit=`travelAdvisory.transitFare`(지역 통화일 때 units).
- `PlaceInput`: `@vis.gl/react-google-maps` Places 자동완성(`region:'jp'`), `{placeId, name}`만 상위로.
- `RouteMap`: 선택 후보 폴리라인, Google 표기 유지(약관).
- 구현 전 대중교통 경로 SKU 등급과 무료 한도를 공식 가격 페이지에서 확인해 `docs/google-setup.md`에 기록.

- [ ] **Step 1: 실패하는 테스트** — `parsesTransitStepsAndAgency`, `parsesFareWhenPresent`, `fareNullWhenAbsent`, `sendsApiKeyAndFieldMaskHeaders`
- [ ] **Step 2:** FAIL → **Step 3:** 구현 → **Step 4:** PASS
- [ ] **Step 5: 수동 확인(키 있을 때)** — `ROUTE_PROVIDER=google GOOGLE_ROUTES_API_KEY=... ./gradlew bootRun`, 하카타역→히타·지쿠젠마에바루·다자이후 텐만구. `directions.match_failure`를 보고 별칭을 `db/migration/transit/V200__transit_google_aliases.sql`로 보강, `ThroughServiceSplitter.BOUNDARIES` 보정.
- [ ] **Step 6: 커밋**
  - directions 세션(12-A): ③ `feat(directions): Google Routes 제공자 구현` + `GoogleRoutesProviderTest` → `docs(directions): Google 설정 안내 작성`
  - frontend 세션(12-B): `feat(frontend): Google 장소 자동완성·지도 구현`

---

### Task 13: transit 모듈 — 쇼와 버스 GTFS 요금 가져오기

**Files:**
- Create: `transit/usecase/ImportGtfsFaresUseCase.java`, `transit/TransitAdminFacade.java`·`TransitAdminFacadeImpl`(`importGtfsFares` — 관리 퍼사드의 첫 메서드), `transit/admin/GtfsImportController.java`(→ `TransitAdminFacade`)
- Test: `transit/usecase/ImportGtfsFaresUseCaseTest.java`(작은 GTFS zip 픽스처), `transit/admin/GtfsImportControllerTest.java`

**Interfaces:**
- `POST /api/v1/admin/transit/import/gtfs?operator=SHOWA_BUS` (multipart `file`, `sourceUrl` 파라미터) → `{stationsUpserted, faresUpserted, skipped:[사유]}`
- `stops.txt` → `station`(name_ja=stop_name, name_ko 비움 — 화면은 name_ko 없으면 name_ja), `fare_attributes.txt`+`fare_rules.txt`(origin_id/destination_id 방식) → `pair_fare`(BASE), source_url=파라미터, verified_on=오늘. zone 방식이거나 요금 파일이 없으면 skipped에 사유. 재실행은 멱등.
- 쇼와 버스 GTFS(CC BY 4.0)는 `backend/data/import/`(gitignore)에 두고 사용.

- [ ] **Step 1: 실패하는 테스트** — `ImportGtfsFaresUseCaseTest`: `importsStopsAndOdFares`, `reimportIsIdempotent`, `zoneBasedFeedReportedAsSkipped`; `GtfsImportControllerTest`: `uploadReturnsCounts`
- [ ] **Step 2:** FAIL → **Step 3:** 구현 → **Step 4:** PASS
- [ ] **Step 5:** (Task I1 Step 5로 이동)
- [ ] **Step 6: 커밋** ③ `feat(transit): GTFS 요금 가져오기 유스케이스 구현` + `ImportGtfsFaresUseCaseTest` → ④ `feat(transit): GTFS 요금 가져오기 API 구현` + `GtfsImportControllerTest`

---

### Task 14: 프론트 — 인쇄용 요약

**Files:**
- Create: `pages/PrintPage.tsx`, `PrintPage.css`(`@media print`)
- Test: `pages/PrintPage.test.tsx`

**Interfaces:**
- Consumes: `GET /api/v1/trips/{id}`, `GET /api/v1/trips/{id}/summary`
- 내용: 여행 이름·기간·인원, 날짜별 구간(출발→도착, 탈것별 운영사·역·특급/지정석, 요금 내역), 날짜별 합계, 총액, 추천 구매(상품·시작일·수량·가격), 절약액, 원화 병기, 미확인 경고, 요금 기준 메모. 지도·버튼은 인쇄에서 숨김, "인쇄 / PDF 저장" = `window.print()`.

- [ ] **Step 1: 실패하는 테스트** — `rendersAllDaysAndTotals`, `showsKrwWhenRateSet`
- [ ] **Step 2:** FAIL → **Step 3:** 구현 → **Step 4:** PASS
- [ ] **Step 5: 커밋** `feat(frontend): 인쇄용 요약 화면 구현`

---

### Task I1 (2단계): 합치기와 연결

**Files:**
- Create: `backend/src/test/java/com/transitfare/integration/TripFlowIntegrationTest.java`, `db/migration/transit/V201__transit_seed_showa_bus.sql`, (필요 시) `db/migration/transit/V200__transit_google_aliases.sql`
- Modify: 충돌 난 공용 파일, `TransitFareApplicationTests.java`

**Interfaces:**
- Consumes: 1단계 2차 브랜치(1차는 Task M1에서 이미 합쳐짐)

- [ ] **Step 1:** `dev`에서 순서대로 머지 커밋(`git merge --no-ff`, 메시지 `merge: feat/xxx → dev`)으로 합치기. 충돌 해결은 그 머지 커밋 안에서: `feat/itinerary` → `./gradlew test` → `feat/directions` → test → `feat/frontend` → `npx vitest run`. 테스트가 실패하면 머지 직후 `dev`에서 딴 `feat/integration`에서 고친다. 이후 Step 2~7은 모두 `feat/integration`에서.
- [ ] **Step 2:** 2차 모듈의 `contract`·퍼사드가 `docs/contracts.md`와 일치하는지, 프론트 `api/types.ts`가 문서와 일치하는지 확인.
- [ ] **Step 3: 실패하는 통합 테스트** — `TripFlowIntegrationTest`(@SpringBootTest, 실제 시드): `createTripAddHitaExpressSegmentAndSummarize()` — 여행(JP-FUKUOKA, 어른 2) → 여행 기간 안 날짜의 하카타→히타 특급 지정석 구간 → summary `baselineTotal` == `GoldenFareTest.jrHakataToHitaExpressReserved` 기대값 × 2, `optimizedTotal <= baselineTotal`; `routeSearchWithFakeProviderPricesCandidates()` — 가짜 제공자 하카타역→히타 후보 중 UNKNOWN 없는 후보가 1개 이상.
- [ ] **Step 4:** `./gradlew test goldenTest` — 2차 합류 후 다시 확인. 불일치는 데이터 수정(새 번호 마이그레이션) 또는 엔진 수정(Task 3 테스트에 재현 추가).
- [ ] **Step 5:** 쇼와 버스 GTFS 가져오기(Task 13 기능) → 이토시마·가라쓰 주요 구간을 `V201`로 고정 → `GoldenFareTest.showaBusChikuzenMaebaruToKeyaNoOto`(공식 요금) 추가·통과.
- [ ] **Step 6:** 프론트 실제 연결: `VITE_USE_MOCK=false npm run dev` + `./gradlew bootRun`, Task 9·11의 수동 확인을 실제 데이터로 반복. mock과 실제 응답이 다르면 계약(`api/types.ts`, 백엔드 DTO)에 맞춘다.
- [ ] **Step 7 (키 있을 때):** Task 12 Step 5 수동 확인, 별칭 보강 `V200`.
- [ ] **Step 7-1:** `docs/modules/*.md`의 퍼사드·API 설명이 합친 코드와 맞는지 확인하고 고친다.
- [ ] **Step 8: 커밋** (Step마다 따로) `test(integration): 여행 흐름 통합 테스트 추가` → 고친 것마다 `fix(모듈): …` / `data(transit): …` → `data(transit): 쇼와 버스 요금 데이터 추가` → `test(transit): 쇼와 버스 골든 테스트 추가` → `docs: 모듈 문서 갱신` → `feat/integration`을 `dev`에 머지

---

### Task 15 (2단계): 관리자 — 모듈별 관리 API와 관리자 화면

**Files:**
- Create: `transit/admin/TransitAdminController.java`, `pass/admin/PassAdminController.java`, `directions/admin/DirectionsAdminController.java`, `frontend/src/pages/admin/AdminPage.tsx`, `components/admin/ResourceTable.tsx`
- Test: `transit/admin/TransitAdminControllerTest.java`, `directions/admin/DirectionsAdminControllerTest.java`, `components/admin/ResourceTable.test.tsx`

**Interfaces:**
- 각 모듈 admin 컨트롤러는 **관리 전용 퍼사드** `TransitAdminFacade`·`PassAdminFacade`·`DirectionsAdminFacade`만 부르고, 퍼사드 뒤에 `usecase/admin/` 유스케이스(리소스마다 목록·추가·수정·삭제, 공통 클래스 없이 직접 작성)를 둔다. ArchitectureTest에 `adminFacadeUsedOnlyByOwnAdminControllers`(다른 모듈·일반 컨트롤러가 `*AdminFacade`를 쓰면 실패)와 `controllersDoNotUseRepositoriesOrUseCases` 추가. 커밋은 리소스 묶음마다 ③ 유스케이스·관리 퍼사드 → ④ 관리 API.
  - `GET|POST /api/v1/admin/transit/{resource}`, `PUT|DELETE /api/v1/admin/transit/{resource}/{id}` — resource ∈ `stations, station-aliases, operator-aliases, line-stations, distance-bands, pair-fares, flat-zones, reserved-seat-fees, transfer-discounts`. 요금 계열은 `sourceUrl`, `verifiedOn` 필수(400 "출처와 확인 날짜를 입력하세요").
  - `GET|POST|PUT|DELETE /api/v1/admin/pass/{products|coverage}[/{id}]` — 같은 출처 규칙.
  - `GET /api/v1/admin/directions/match-failures`(count 내림차순), `DELETE .../match-failures/{id}`, `GET /api/v1/admin/directions/fare-mismatches`(최신순), `DELETE .../fare-mismatches/{id}`.
  - 매칭 실패 해결: 프론트가 `POST /api/v1/admin/transit/station-aliases` `{stationId, alias}` 후 `DELETE /api/v1/admin/directions/match-failures/{id}`를 차례로 호출(모듈 간 직접 호출 없음 — 설계자 결정).
  - `GET /api/v1/admin/transit/export/sql`, `GET /api/v1/admin/pass/export/sql` → 해당 모듈 요금 계열 테이블 INSERT SQL(런타임 DB와 시드 파일이 갈라지는 것 방지).
- `ResourceTable`: 컬럼 정의를 받아 목록·추가·수정·삭제하는 범용 표. `AdminPage` 탭: 역/별칭/요금/승차권/매칭 실패/요금 불일치.

- [ ] **Step 1: 실패하는 테스트** — `fareCreateRequiresSource`, `exportSqlContainsPairFares`, `listMismatchesNewestFirst`, `listFailuresByCountDesc`; 프론트 `ResourceTable`: `addsRow`, `editsRow`
- [ ] **Step 2:** FAIL → **Step 3:** 구현 → **Step 4:** PASS
- [ ] **Step 5: 커밋** (`feat/admin` 브랜치, 관리 컨트롤러는 ④단계) `feat(transit): 요금 관리 API 구현` → `feat(pass): 승차권 관리 API 구현` → `feat(directions): 매칭 실패·요금 불일치 관리 API 구현` → `feat(frontend): 관리자 화면 구현` → `dev`에 머지

---

### Task 16~18 (데이터 세션): 2·3·4단계 지역 요금 데이터

각각 Task 4와 같은 절차(조사 → 마이그레이션 → 골든 테스트 작성 → 커밋 → `./gradlew goldenTest` 통과 → `dev`에 머지. 1차가 합쳐진 뒤라 엔진이 있으므로 바로 실행 가능). 11/23까지 가능한 만큼, 순서대로. 지역 패스는 `db/migration/pass/V101~`.

| 태스크 | 마이그레이션 | 대상 | 골든 테스트(최소) |
|---|---|---|---|
| 16 | `transit/V101__transit_seed_region2.sql` | 니시테츠 전철 텐진오무타선·다자이후선(다자이후·구루메·야나가와), JR 가고시마 본선 무나카타 방면, JR 지쿠히선 지쿠젠마에바루~가라쓰, 쇼와 버스 가라쓰 방면(GTFS), 다자이후·야나가와 관광 패스(ticket) | `nishitetsuTenjinToDazaifu`, `nishitetsuTenjinToYanagawa`, `jrHakataToAkama`, `subwayPlusJrHakataToKaratsu`, `dazaifuYanagawaPassCoverage` |
| 17 | `transit/V102__transit_seed_region3.sql` | JR 가고시마 본선 하카타~고쿠라·모지코, 특급 소닉 특급요금, 고속버스 후쿠오카~기타큐슈, 기타큐슈 모노레일(필요 시) | `jrHakataToKokuraExpress`, `jrHakataToMojiko`, `highwayBusTenjinToKokura` |
| 18 | `transit/V103__transit_seed_region4.sql` | JR 규다이 본선 히타~유후인, 닛포 본선 고쿠라~벳푸, 유후인노모리·소닉 특급요금, 고속버스 후쿠오카~유후인·벳푸 | `jrHakataToYufuinExpressReserved`, `jrHakataToBeppuExpress`, `highwayBusHakataToYufuin` |

- [ ] **각 태스크:** 조사 → 골든 테스트 작성 → 마이그레이션 작성 → `./gradlew test`(마이그레이션 적용 확인) → 커밋 `data(transit): N단계 지역 요금 데이터 추가` → `test(transit): N단계 지역 골든 테스트 추가`(지역 패스는 `data(pass): …`). 2단계 합치기 이후에도 계속되면 `dev`에서 새 브랜치(`feat/data-region-N`)로 이어서 `./gradlew goldenTest`까지 통과 후 `dev`에 머지.

---

### Task 19 (2단계): 최종 확인

모든 작업은 `dev`에서 딴 `feat/final`에서.

- [ ] **Step 1:** `cd backend && ./gradlew test`, `cd frontend && npx vitest run && npm run build` — 모두 통과.
- [ ] **Step 2:** PostgreSQL 호환 확인 — `docker compose up -d`, `SPRING_PROFILES_ACTIVE=prod DB_URL=jdbc:postgresql://localhost:5432/transitfare DB_USERNAME=transitfare DB_PASSWORD=transitfare ./gradlew bootRun` → Flyway 전체 적용 성공, `/api/v1/health` 200. 실패하면 해당 SQL을 양쪽 호환 문법으로 수정(새 번호 마이그레이션이 아니라, 아직 배포 전이므로 해당 파일 수정 + 로컬 H2 `backend/data/` 삭제 후 재생성 — 설계자 결정).
- [ ] **Step 3:** 실제 여행 시나리오 수동 확인: 로컬 파일 `docs/my-trip.local.md`(git 제외)의 실제 여행 일정을 입력. 총액·추천이 골든 값과 일치, 미확인 경고, 원화 병기, 인쇄 PDF 저장. 실제 일정은 저장소에 기록하지 않는다.
- [ ] **Step 4: README 마무리** — 실행 방법을 실제 동작 기준으로 갱신(환경변수 `ROUTE_PROVIDER`, `GOOGLE_ROUTES_API_KEY`, `VITE_GOOGLE_MAPS_API_KEY`, `VITE_USE_MOCK`, prod 프로필 `docker compose up -d` + `SPRING_PROFILES_ACTIVE=prod` + `DB_URL`·`DB_USERNAME`·`DB_PASSWORD`), 1단계 전용 내용(세션 표, 수정 금지 파일 목록)은 "작업 이력"으로 축소하고 브랜치·커밋 규칙은 유지, `docs/modules/*.md`·`docs/data-sources.md`·`docs/google-setup.md` 링크 연결. 커밋 `docs: README 마무리`.
- [ ] **Step 5: 릴리스** — `feat/final`을 `dev`에 머지 → `dev`에서 `./gradlew test goldenTest`, `npx vitest run` 통과 확인 → `main`에서 `git merge --no-ff dev -m "merge: dev → main (v1)"`.
