# pass 모듈 — 승차권 상품과 최적화

패키지 `com.transitfare.pass`, DB 스키마 `pass`. 패스·왕복권·회수권 상품을 보관하고, 여행 구간에 대해 가장 싼 승차권 구매 조합을 계산한다. **transit을 모른다**(`ArchitectureTest.passDoesNotDependOnTransit`): 입력은 itinerary가 transit 결과를 바꿔 넘기는 `TicketLeg`. 금액은 모두 어른 1명 기준(어린이 요금 없음).

## 퍼사드 `PassFacade`

| 메서드 | 하는 일 | 유스케이스 |
|---|---|---|
| `listProducts(regionCode)` | 지역 상품 목록(코드 순). 없으면 빈 목록 | `ListProductsUseCase` |
| `optimize(regionCode, legs, party, overrides)` | 사용자 설정(`included=false` 제외, `priceAmount`로 가격 대체)을 적용한 상품으로 가장 싼 조합 계산 | `OptimizeTicketsUseCase` → `TicketOptimizer` |

부르는 곳: itinerary(여행 요약), pass `ProductController`.

## API

- `GET /api/v1/products?region=JP-FUKUOKA` → `[{code, type:{code,label,description}, nameKo, nameJa, price, validDays, eligibilityNote}]`. `region`이 없으면 400 `INVALID_INPUT`.

## 최적화 규칙 (`TicketOptimizer`)

1. 1명 기준으로 계산하고, 금액은 × 인원, 구매 수량 = 인원.
2. 후보 구매: PASS × (구간이 있는 각 날짜 시작) + FIXED_ROUTE × 1. 커버하는 구간이 없는 후보는 버리고, 60개가 넘으면 단독 절감액(절감 − 가격) 상위 60개.
3. 후보 0~3개의 모든 조합을 평가. 총액 = 커버 안 된 요금 + 구매가. 가장 싼 조합, 동률이면 구매 수가 적은 조합.
4. 조합 안에서는 구간 덩어리를 날짜순으로 보며 가장 크게 절감하는 구매에 배정.
5. 커버 조건: `fareKnown=true` + 적용 범위(운영사, 지정 시 노선, 역 쌍이면 양 끝이 그 쌍·방향 무관) + 유효 기간. PASS는 시작일부터 `validDays`일(여행 끝을 넘어도 됨), FIXED_ROUTE는 첫 사용일부터 `validDays`일이고 남은 `uses`가 있어야 함.
6. 절감 = 운임 전액 + (`coversExpress`면 특급요금) + (지정석은 `reservedSeatLimit` 횟수까지).
7. 연속 거리제 묶음(같은 `groupId`)은 묶음 전 구간이 같은 구매로 커버될 때만 절감.
8. `legCoveredBy`: 커버된 `TicketLeg` 순번 → 상품 코드.

**알려진 한계:** `consecutive_days=false`(기간 안에서 날짜 골라 쓰는 패스)도 연속 일수로 계산(보수적). FIXED_ROUTE는 같은 상품을 한 번만 산다고 본다.

## 테이블 (`V1__pass_schema.sql`, 시드는 데이터 세션 `V100~`)

- `ticket_product(code PK, region_code, type 'PASS'|'FIXED_ROUTE', name_ja, name_ko, price, valid_days, consecutive_days, uses, covers_express, reserved_seat_limit, eligibility_note null, source_url, verified_on)` — `uses`: FIXED_ROUTE 승차 횟수, PASS는 0 = 무제한. `reserved_seat_limit` 0 = 지정석 커버 안 함.
- `ticket_coverage(id, product_code FK, operator_code, line_id null, station_a_id null, station_b_id null)` — `line_id` null = 운영사 전 노선. transit의 노선·역 ID는 값만 저장.

## 에러 코드

아직 없음. `PASS_PRODUCT_NOT_FOUND`, `PASS_SOURCE_REQUIRED`는 관리자 기능(Task 15)에서 추가.

## 테스트

```bash
cd backend && ./gradlew test --tests 'com.transitfare.pass.*'   # pass만
```

```bash
cd backend && ./gradlew test                                     # ArchitectureTest 포함 전체
```

- `TicketOptimizerTest`(순수 로직 10개), `OptimizeTicketsUseCaseTest`(H2, 테스트 지역 `TEST-REGION`, 2개), `ProductControllerTest`(MockMvc, 2개)
- 실제 상품으로 하는 골든 테스트(`sunqPassCoversHitaGo`, `jrPassCoversHakataHitaExpress`)는 데이터 세션 몫, `./gradlew goldenTest`.
