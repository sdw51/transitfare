# 요금 데이터 출처

시드 마이그레이션에 넣은 요금·승차권 값을 어디서 확인했는지 정리한다. 확인 날짜는 모두 **2026-11-18**(시드의 `verified_on`).
금액은 어른 1명 기준이다(어린이 요금은 v1 범위 밖).

| 마이그레이션 | 내용 |
|---|---|
| `db/migration/transit/V100__transit_seed_region1.sql` | 1단계 지역(후쿠오카 시내·공항, 이토시마, 우키하, 히타) 역·요금 |
| `db/migration/pass/V100__pass_seed_products.sql` | 승차권 상품 8개 |

골든 테스트: `backend/src/test/java/com/transitfare/seed/GoldenFareTest.java` (`./gradlew goldenTest`)

## 요금 기준

| 운영사 | 기준 | 비고 |
|---|---|---|
| 후쿠오카 시영 지하철 | 승차권(きっぷ) 보통요금 | 터치 결제도 같은 금액 |
| JR 큐슈 | 승차권 보통운임, 재래선 특급 **통상기** | 번성기 +200·최번성기 +400, 관광열차(ゆふいんの森 등) +500은 넣지 않음 |
| 니시테츠 버스 | **IC·카드** | 150엔 구역은 현금과 같음. 공항 노선만 IC가 20엔 쌈(사용자 결정) |
| 니시테츠 고속버스 히타호 | 운임표 금액 | IC·현금 차이 표기 없음 |

## 후쿠오카 시영 지하철 (`FUKUOKA_SUBWAY`)

| 자료 | URL | 쓴 곳 |
|---|---|---|
| 보통권 요금 | https://subway.city.fukuoka.lg.jp/fare/futuken/ | 거리 밴드 6개(3·7·11·15·19km, 초과) 210~380엔 |
| 요금표 (2026-04-01~) | https://subway.city.fukuoka.lg.jp/fare/pdf/renrakuA4Side.pdf | 역 쌍 구간 검증, 공항·하코자키선↔나나쿠마선 요금 306쌍, 지하철+JR 연락운임 |
| 역간 영업거리표 (2019-10-01 현재) | https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf | 역별 누적 km |
| 교통국 사업개요 | https://subway.city.fukuoka.lg.jp/subway/about/pdf/subway_01.pdf | 노선 길이(공항선 13.1, 하코자키선 4.7, 나나쿠마선 13.6km), 하카타~덴진미나미 1.6km |
| 요금 조례 | https://www.city.fukuoka.lg.jp/d1w_reiki/reiki_honbun/q003RG00000931.html | 구간 정의(営業キロ程) |

- 다룬 구간: 공항선·하코자키선·나나쿠마선 전 역(36역).
- **검증:** 누적 km로 계산한 전 역 쌍 구간을 2026-04-01 요금표의 색칸과 비교했다. 판독한 606칸 모두 일치(24칸은 글자와 겹쳐 판독 못 함).
- **역산한 값:** 구시다진자마에(2023년 개업)는 거리표에 없어 요금표의 구간 경계로 역산했다(하카타 기준 0.6km).
- **환승:** 공항선↔나나쿠마선 거리는 하카타 경유로 계산된다(덴진↔덴진미나미는 환승역이 아님). 구간을 어떻게 나눠 넣어도 공식 요금이 나오게 두 노선군 사이 306쌍을 `pair_fare`로 넣었다.
- **넣지 않음:** JR 지쿠히선·니시테츠 가이즈카선 환승 할인. 특정 역 범위에만 적용되는데 `transfer_discount`는 운영사 쌍만 표현해서 넣으면 해당 없는 구간도 할인된다.

## JR 큐슈 (`JR_KYUSHU`)

| 자료 | URL | 쓴 곳 |
|---|---|---|
| 여객영업규칙 (2026-10-01 현행) 제3장 운임·요금 | https://www.jrkyushu.co.jp/railway/ticket/rule/transport/__icsFiles/afieldfile/2026/09/30/20261001_conditions_of_carriage_01_65_146.pdf | 간선 운임표(第77条の5, 第84条の5), 지방교통선 특정 운임(第77条の10) |
| 여객영업규칙 별표 | https://www.jrkyushu.co.jp/railway/ticket/rule/transport/__icsFiles/afieldfile/2026/10/02/20261001_conditions_of_carriage_01_beppyou.pdf.pdf | 지방교통선 목록(久大線 = 지방교통선, 鹿児島本線·筑肥線 = 간선) |
| 2025 운임 개정 가이드북 | https://www.jrkyushu.co.jp/railway/kaitei/pdf/241217_unchinkaitei_guidebook.pdf | 재래선 특급요금표, 博多~久留米 35.7km, 久留米~由布院 의제키로 109.0km 예시 |
| ゆふいんの森 | https://www.jrkyushu.co.jp/trains/yufuinnomori/ | 博多~日田 운임 1,930엔 대조 |
| 역별 영업키로 (**비공식**) | https://ja.wikipedia.org/wiki/鹿児島本線, https://ja.wikipedia.org/wiki/久大本線, https://ja.wikipedia.org/wiki/筑肥線 | 역별 누적 km (국토교통성 鉄道要覧 인용) |

- 다룬 구간: 가고시마본선 하카타~구루메(18역), 규다이본선 구루메~히타(14역), 지쿠히선 메이노하마~지쿠젠마에바루(8역).
- **역별 km는 비공식이다.** JR 큐슈가 역별 km를 공개하지 않아 위키백과 값을 썼다(사용자 결정). 공식 값과 대조:
  - 博多~久留米 35.7km: 가이드북과 일치.
  - 지쿠히선 각 역 운임: 지하철 연락운임표의 姪浜 행과 일치.
  - 博多~日田 1,930엔: ゆふいんの森 공식가와 일치.
- **지방교통선:** 규다이본선 누적 km에는 의제키로(영업키로 × 1.1, 0.1km 반올림)를 넣었다. 간선과 이어 타면 운임계산키로로 간선 운임표를 쓰기 때문이다(第81条の5). 규다이선 안에서만 타서 더 싼 특정 운임이 되는 13쌍은 `pair_fare`로 넣었다.
- **특급:** 자유석 특급요금을 `EXPRESS` 밴드로, 지정석 차액 530엔을 `reserved_seat_fee`로 넣었다. 특급요금은 원래 영업키로 기준인데, 엔진은 의제키로를 섞은 km를 쓴다. 이 범위(博多~日田, 博多~うきは)에서는 밴드가 같아 결과가 같다.
- **넣지 않음:** 100km를 넘는 운임(km당 계산이라 밴드로 표현 못 함 → 미확인).

## 니시테츠 버스 (`NISHITETSU_BUS`)

| 자료 | URL | 쓴 곳 |
|---|---|---|
| 후쿠오카 도심 150엔 균일 운임 구역 | https://www.nishitetsu.jp/bus/rosen/150/ (지도 이미지 `d6c345ae70b2675a0ae88ba5310feff8.png`) | 균일 구간 150엔, 정류장 39곳 |
| 니시테츠 시각표 정류장 검색 | https://jik.nishitetsu.jp/ | 정류장 공식 표기 대조 |
| 하카타역~공항 국제선 직행 | https://www.nishitetsu.jp/bus/rosen/akeito/ , https://www.nishitetsu.jp/bus/norikata/cashlessbus/ | 博多駅筑紫口~福岡空港国際線 IC 380엔 |
| Airport Express | https://www.nishitetsu.jp/bus/rosen/airportexpress/ | 西鉄天神高速BT~福岡空港国際線 IC 480엔 |

- 다룬 구간: 도심 균일 구간 정류장 39곳과 공항 노선 2쌍.
- 지도의 정류장 39곳을 모두 jik 표기로 대조했다. 승차장별로 나뉜 정류장은 기본 이름 한 행이다. 지도의 "天神ソラリアステージ前(7C)" 승차장은 지금 이름인 `天神ワンビル前`로 넣었다.
- **넣지 않음:**
  - 지도에 이름 없는 덴진 승차장 약 3곳.
  - 역 반경 150엔 구역.
  - 하카타역~공항 국내선(니시테츠 직통 노선 없음).

## 니시테츠 고속버스 히타호 (`NISHITETSU_HIGHWAY_BUS`)

| 자료 | URL | 쓴 곳 |
|---|---|---|
| ひた号 노선·운임표 | https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/ (운임표 이미지 `61b1d8cf510253214ed63f65faab45d0.jpg`) | 정류장 19곳, 요금 161쌍 |

- 하카타BT·덴진 고속BT·고후쿠마치·도코2초메는 운임표에서 같은 칸이다. 이 넷끼리와 하카타BT~공항 국내선은 발매하지 않아(===) 넣지 않았다.
- **우키하 시내 정류장은 없다.** 하키 다음 정류장이 히타 인터구치다.

## 승차권 상품 (`pass`)

| 상품 | 가격 | URL |
|---|---|---|
| SUNQ 패스 북큐슈+시모노세키 3일(디지털) | 10,000 | https://www.sunqpass.jp/information/info/2024-09-09/JP_20241001price.pdf , 구역 https://www.sunqpass.jp/area/ |
| JR 큐슈 레일패스 북큐슈 3일 / 5일 | 15,000 / 17,000 | https://www.jrkyushu.co.jp/english/railpass/railpass.html |
| 후쿠오카 지하철 1일 승차권 | 640 | https://subway.city.fukuoka.lg.jp/fare/pdf/renrakuA4Side.pdf |
| 니시테츠 버스 후쿠오카 시내 1일 프리 승차권(종이) | 1,200 | https://www.nishitetsu.jp/bus/jyousha/cityfree/ |
| 니시테츠 버스 후쿠오카 시내 24시간권(디지털) | 1,100 | https://www.nishitetsu.jp/bus/sumanori/ |
| 후쿠오카 투어리스트 시티 패스(시내) | 2,500 | https://gofukuoka.jp/citypass.html |
| 히타 마룻토 패스 | 4,580 | https://hitabus.com/pickups/4217 |

- **적용 범위는 운영사 단위다**(사용자 결정). 역·노선 id는 transit 모듈 소관이라 pass 시드에서 지정하지 않는다. 그래서:
  - 투어리스트 시티 패스의 JR(시내 일부 구간)은 넣지 않았다.
  - 마룻토 패스(후쿠오카~히타 왕복)는 히타호 아무 구간 2회로 근사했다.
- **실제보다 덜 유리하게 계산하는 것:** SUNQ 디지털권은 "5일 중 3일 선택"을 연속 3일로, 24시간권은 1일로 계산한다.
- **넣지 않음:**
  - SUNQ 종이권(디지털권이 항상 같거나 유리).
  - SUNQ 2일권(공식 가격을 찾지 못함).
  - 히타호 왕복권·회수권(판매 없음).
