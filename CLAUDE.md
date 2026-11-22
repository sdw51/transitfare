# CLAUDE.md

여행 교통비 계산기(패키지 `com.transitfare`). 첫 지역은 후쿠오카, 이후 다른 해외 지역을 추가한다. 설계는 `docs/superpowers/specs/2026-11-17-fukuoka-transit-fare-design.md`, 구현 계획은 `docs/superpowers/plans/2026-11-17-fukuoka-transit-fare.md`. 모듈 경계·마이그레이션 번호·수정 금지 파일 같은 작업 규칙은 `README.md`의 "작업 규칙"을 따른다.

## 코드 작성 방식

- 코드를 파일에 쓰기 전에 **먼저 사용자에게 보여주고 승인받는다.** 단위는 커밋 하나(아래 "커밋 나누기"의 한 단계): 그 커밋에 들어갈 테스트와 구현 코드를 보여줌 → 승인 → 파일 작성 → 테스트 실행 → 커밋.
- 사용자가 고치라고 하면 고친 코드를 다시 보여주고 승인받는다.
- 설계·구조·규칙 결정은 혼자 정하지 말고 선택지와 추천을 보여주고 묻는다.

## 코드 스타일

- Lombok: 의존성 주입은 `@RequiredArgsConstructor` + `private final` 필드. 엔티티는 `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@Builder`. 로그는 `@Slf4j`.
- 엔티티: 클래스에 `@Table(schema = "모듈", name = "테이블")`, **모든 필드에 `@Column(name = "컬럼명")` 명시**, NOT NULL 컬럼은 `nullable = false`. id는 `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")`, 연관은 `@JoinColumn(name = …)`, enum은 `@Enumerated(EnumType.STRING)` + `@Column`.
- **컨트롤러는 자기 모듈의 퍼사드만 부른다**(유스케이스·리포지토리를 직접 부르지 않음). 화면에서만 쓰는 기능도 퍼사드에 메서드를 둔다. **예외 없음** — 관리자 화면(`admin` 컨트롤러)은 관리 전용 퍼사드 `XxxAdminFacade` → 유스케이스 → 리포지토리로 간다. `XxxAdminFacade`는 자기 모듈의 `admin` 컨트롤러만 쓰고, 다른 모듈은 쓰지 않는다. CRUD 공통 클래스는 두지 않고 유스케이스를 직접 작성한다.
- `@Setter`, `@Data`는 쓰지 않는다. 값 변경은 이름 있는 메서드로 (`itinerary.changePeriod(start, end)`).
- 공개 DTO(`contract` 패키지)와 도메인 값 객체는 Java record.
- `contract`의 enum은 한글 `label`·`description`을 가진다. 공통 인터페이스 없이 enum마다 `@JsonFormat(shape = OBJECT)`, `@JsonProperty("code") getCode()`, `@JsonCreator from(Object)`를 둔다(`docs/contracts.md`의 "코드 enum" 모양과 표). 응답은 `{code, label, description}`, 요청은 코드 문자열·객체 둘 다 허용, DB에는 코드만.
- 에러: 모듈마다 `{module}/error/XxxErrorCode` enum(`ErrorCode` 구현, `HttpStatus` + 한글 메시지)에 모은다. code 이름은 모듈 접두사(`ITINERARY_OUT_OF_PERIOD`). 유스케이스에서 `throw new BusinessException(코드, "필드명")`. 응답 `{"status","code","message","field"}`. `domain` 패키지에서는 던지지 않는다. 에러 테스트는 HTTP 상태와 `$.code`를 둘 다 확인.
- 주석(한글 Javadoc):
  - **유스케이스:** 클래스에 한 줄(어떤 사용자 행동인지), `execute` 등 public 메서드마다 무엇을 하는지·입력·결과·던지는 에러 코드.
  - **퍼사드:** 인터페이스와 구현의 메서드마다 무엇을 하는지와 **연결된 유스케이스와 그 유스케이스가 하는 일**(`{@link PriceLegsUseCase}`: 구간 목록을 받아 운영사별 요금 규칙으로 계산…).
  - **컨트롤러(REST API):** 클래스 주석에 누가 부르는지(프론트 어느 화면, 다른 모듈은 HTTP가 아니라 퍼사드로 부른다는 점)와 저장하는지·조회만 하는지. 메서드 주석에 "언제:" 단락(사용자의 어떤 행동 중에 불리는지)과 연결된 퍼사드 메서드·유스케이스가 하는 일.
  - **domain:** 계산 규칙 메서드마다 규칙 요약(설계 문서 7-1장의 어느 규칙인지). 코드만 봐서 알기 어려운 분기에는 줄 주석.
- 테스트: 메서드명은 영어(계획에 적힌 이름), 모든 테스트에 한글 `@DisplayName("도보 구간은 요금이 0원이다")`. 프론트는 `it("…")`에 한글 설명.

## 브랜치

```
main  ← 완성본. 직접 커밋 금지
 └ dev  ← 통합 브랜치. 직접 커밋 금지
    └ feat/{transit|pass|data|itinerary|directions|frontend|wave1-fix|data-region-N|integration|admin|final}  ← dev에서 분기
```

- 작업은 항상 `dev`에서 딴 `feat/*` 브랜치에서 한다. 모듈 브랜치 아래로 더 쪼개지 않는다.
- **`feat/*` → `dev`는 PR로 합친다(사용자 결정 2026-11-23).** 세션은 작업이 끝나면(테스트 통과, 커밋 완료) 브랜치를 푸시하고 `gh pr create --base dev`로 PR을 만든 뒤 사용자에게 알린다. PR 설명에는 무엇을·왜, 커밋 목록, 테스트 결과, 작업 중 사용자가 정한 결정을 적는다. **머지는 사용자가 GitHub에서 직접**("Create a merge commit") 하고, 세션은 머지하지 않는다. 머지 뒤 로컬 `dev`는 `git pull`로 맞춘다.
- PR 머지는 머지 커밋으로(스쿼시·리베이스 금지). PR이 `dev`와 충돌하면 `dev`를 그 브랜치에 머지해 풀고 다시 푸시한다.
- `dev` → `main`: 최종 확인 후 한 번, `dev` → `main` PR을 만들고 사용자가 GitHub에서 머지.
- 푸시는 자기 `feat/*` 브랜치를 PR로 올릴 때만. `dev`·`main`에는 푸시하지 않는다.

## 커밋 나누기

백엔드 기능 하나는 아래 순서로 최대 4번 커밋한다. 해당 계층이 없으면 그 커밋은 건너뛴다.

| 순서 | 들어가는 것 | 함께 넣는 테스트·확인 | 메시지 끝말 |
|---|---|---|---|
| ① 도메인(테이블) | 마이그레이션(스키마), `entity` | `./gradlew test`(마이그레이션 적용, `ddl-auto: validate`로 매핑 검증) | `… 도메인 생성` |
| ② 계산 로직 | `domain` 패키지(순수 Java 계산 규칙, 포트 인터페이스) | 로직 단위 테스트 | `… 로직 구현` |
| ③ 저장소·유스케이스·퍼사드 | `repository`, `provider`, `usecase`, `XxxFacadeImpl` | 리포지토리·유스케이스 테스트 | `… 유스케이스 구현` |
| ④ 컨트롤러 | `controller`, `admin` 컨트롤러 | MockMvc 테스트 | `… API 구현` |

- 단계마다 TDD: 테스트 작성 → 실패 확인 → 구현 → `./gradlew test` 통과 → 커밋. 테스트가 실패하는 상태로 커밋하지 않는다.
- 프론트는 화면(또는 공용 컴포넌트) 단위: `… 화면 구현`. 각 커밋에서 `npx vitest run` 통과.
- 데이터는 시드 마이그레이션과 골든 테스트를 따로: `… 데이터 추가`, `… 골든 테스트 추가`.

## 커밋 메시지

형식: `type(module): 대상 + 한 일` (한글 설명, 앞에 단계 이름을 붙이지 않는다)

```
feat(transit): 요금 기준 데이터 도메인 생성
feat(transit): 요금 계산 로직 구현
feat(transit): 구간 요금 계산 유스케이스 구현
feat(transit): 구간 요금 계산 API 구현
feat(frontend): 여행 요약 화면 구현
data(transit): 1단계 지역 요금 데이터 추가
test(transit): 1단계 지역 골든 테스트 추가
```

- type: `feat` 기능, `fix` 버그 수정, `data` 요금·승차권 데이터, `test` 테스트만, `docs` 문서, `chore` 설정·골격·통합, `refactor` 동작 변화 없는 정리
- module: `transit`, `pass`, `itinerary`, `directions`, `frontend`, `integration` (공통·골격은 생략)
- 수정 금지 공용 파일을 건드린 커밋은 메시지 끝에 `[contract]`.
- Claude가 만든 커밋은 마지막 줄에 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- API 키·`.env.local`은 커밋하지 않는다.
