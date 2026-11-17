# 여행 교통비 계산기

해외여행 일정을 입력하면 자체 요금 데이터로 구간별 대중교통 요금과 총액을 계산하고,
패스·할인 승차권 중 가장 싼 조합을 추천하는 웹사이트. 첫 지역은 후쿠오카(`JP-FUKUOKA`).

## 구조

모듈러 모놀리스. Spring Boot 서버 하나 안에 기능 모듈 4개.

| 모듈 | 하는 일 | 다른 모듈에서 부를 때 |
|---|---|---|
| `transit` | 지역·운영사·역·노선 데이터, 구간 요금 계산 | `TransitFacade` |
| `pass` | 패스·할인 승차권 상품, 가장 싼 조합 계산 (transit을 모름) | `PassFacade` |
| `itinerary` | 여행·일정 저장, 여행 요약 (transit·pass를 부르는 오케스트레이터) | (없음) |
| `directions` | 출발·도착 장소로 경로 후보 검색, 역 매칭 | `DirectionsFacade` |

- 모듈 안 패키지: `contract`(밖에 공개하는 데이터 모양), `controller`, `usecase`, `domain`(순수 Java 계산 규칙), `entity`, `repository`, `admin`, `error`
- 모듈 간 계약과 의존 관계 다이어그램: [docs/contracts.md](docs/contracts.md)
- 프론트: React + TypeScript + Vite + Mantine (`frontend/`)

## 실행 방법

필요: Java 25, Node 24 (PostgreSQL을 확인할 때만 Docker)

```bash
# 백엔드 (dev 프로필, H2 파일 DB: backend/data/)
cd backend && ./gradlew bootRun
```

```bash
# 프론트 (http://localhost:5173, /api는 8080으로 프록시)
cd frontend && cp .env.example .env.local && npm install && npm run dev
```

```bash
# 테스트
cd backend && ./gradlew test          # 단위·통합·아키텍처 테스트
cd backend && ./gradlew goldenTest    # 공식 요금으로 검증하는 골든 테스트
cd frontend && npx vitest run
```

- Swagger UI: http://localhost:8080/swagger-ui
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/transitfare`)

## 작업 규칙

1. 다른 모듈은 그 모듈의 퍼사드와 `contract` 패키지만 사용한다. (`ArchitectureTest`가 검사)
2. 모듈마다 DB 스키마가 따로다(`transit`, `pass`, `itinerary`, `directions`). 모듈 간 FK·JOIN 금지, 다른 모듈 데이터는 ID 값만 저장.
3. 마이그레이션은 모듈별 폴더 `db/migration/{module}`에 아래 번호 범위로. 커밋된 마이그레이션은 고치지 않고 새 번호로 추가.

   | 모듈 | 번호 | 용도 |
   |---|---|---|
   | transit | V1~V99 | 스키마, 지역·운영사 |
   | transit | V100~V199 | 지역별 요금 데이터 |
   | transit | V200~ | 연결 단계 보강 (Google 별칭, 쇼와 버스) |
   | pass | V1~V99 / V100~ | 스키마 / 승차권 상품 데이터 |
   | itinerary | V1~ | 스키마 |
   | directions | V1~ | 스키마 |

4. 요금·승차권 값은 공식 자료의 `source_url`과 확인 날짜 `verified_on`을 반드시 기록한다. 확인 못 한 값은 넣지 않는다(0원으로 넣지 않음).
5. 함부로 고치지 않는 공용 파일: `build.gradle.kts`, `application*.yml`, `common/`, `frontend/package.json`, `frontend/src/App.tsx`, `README.md`, `CLAUDE.md`, `docs/contracts.md`. 바꿔야 하면 커밋 메시지 끝에 `[contract]`.
6. 모듈 계약을 바꿀 때는 `docs/contracts.md`를 먼저 고친다.
7. API 키는 커밋하지 않는다. 백엔드는 환경변수 `GOOGLE_ROUTES_API_KEY`, 프론트는 `frontend/.env.local`.
8. 브랜치·커밋·코드 스타일 규칙은 [CLAUDE.md](CLAUDE.md).

## 문서

| 문서 | 내용 |
|---|---|
| [설계 문서](docs/superpowers/specs/2026-11-17-fukuoka-transit-fare-design.md) | 왜, 무엇을 만드는지 |
| [구현 계획](docs/superpowers/plans/2026-11-17-fukuoka-transit-fare.md) | 태스크, 일정, 진행 방식 |
| [모듈 간 계약](docs/contracts.md) | contract 타입, 퍼사드 시그니처, 의존 관계 다이어그램 |
| `docs/modules/*.md` | 모듈별 설명 (1단계에서 작성 예정) |
| `docs/data-sources.md` | 조사한 공식 요금 자료 목록 (데이터 세션에서 작성 예정) |
| `docs/google-setup.md` | Google Maps Platform 설정 (Task 12에서 작성 예정) |
