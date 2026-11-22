-- 1단계 지역(후쿠오카) 승차권 상품. 데이터 세션 작성. 가격은 어른 1명 기준(어린이 요금은 v1 범위 밖).
-- 확인 못 한 값은 넣지 않는다(README 작업 규칙 4). 조사 자료 목록: docs/data-sources.md
--
-- 적용 범위(ticket_coverage)는 운영사 단위로만 넣는다(line_id·station_a_id·station_b_id = null).
--   역·노선 id는 transit 스키마에서 자동 생성되고, 모듈 간 JOIN은 금지라 pass 시드에서 찾을 수 없다.
--   그래서 운영사 일부 구간만 되는 상품은 아래처럼 처리했다.
--   - 투어리스트 시티 패스의 JR(시내 일부 구간)은 넣지 않음 → 절약액을 적게 잡는 쪽(과대 추천 없음)
--   - 히타 마룻토 패스(후쿠오카~히타 왕복)는 히타호 운영사 전체 2회로 근사

-- SUNQ 패스 북큐슈+시모노세키 3일권(디지털): 이용 시작일부터 5일 중 3일 선택. 종이권 12,000엔(연속 3일)은
--   디지털권이 항상 같거나 유리해서 넣지 않음. 2일권은 공식 가격을 찾지 못해 넣지 않음.
--   비연속 선택을 표현할 컬럼이 없어 consecutive_days=true(연속 3일)로 둔다 → 보수적(실제보다 덜 유리)
--   커버: "高速･特急･幹線バスはもちろん、対象エリア内の一般路線バスにも乗り放題"(구역 페이지, 2026-04-01 시점),
--   히타호 노선 "福岡･福岡空港～日田" 게재. 버스 전용(철도 없음)
INSERT INTO pass.ticket_product (code, region_code, type, name_ja, name_ko, price, valid_days, consecutive_days, uses,
                                 covers_express, reserved_seat_limit, eligibility_note, source_url, verified_on) VALUES
  ('SUNQ_NORTH_3DAY', 'JP-FUKUOKA', 'PASS', 'SUNQパス 北部九州＋下関・長門 3日券（デジタル）', 'SUNQ 패스 북큐슈+시모노세키 3일권(디지털)',
   10000, 3, TRUE, 0, FALSE, 0, 'my route 앱 디지털권. 이용 시작일부터 5일 안에서 3일 선택(계산은 연속 3일로 함)',
   'https://www.sunqpass.jp/information/info/2024-09-09/JP_20241001price.pdf', DATE '2026-11-18'),

-- JR 큐슈 레일패스 북큐슈: 연속 일수, 특급 포함, 무료 지정석 6회. 단기체류 외국인 전용. 후쿠오카 지하철 불가
  ('JR_KYUSHU_RAIL_PASS_NORTH_3DAY', 'JP-FUKUOKA', 'PASS', 'JR九州レールパス 北部九州 3日間', 'JR 큐슈 레일패스 북큐슈 3일',
   15000, 3, TRUE, 0, TRUE, 6, '단기체류 외국인 관광객 전용(일본 외 여권, 단기체재)',
   'https://www.jrkyushu.co.jp/english/railpass/railpass.html', DATE '2026-11-18'),
  ('JR_KYUSHU_RAIL_PASS_NORTH_5DAY', 'JP-FUKUOKA', 'PASS', 'JR九州レールパス 北部九州 5日間', 'JR 큐슈 레일패스 북큐슈 5일',
   17000, 5, TRUE, 0, TRUE, 6, '단기체류 외국인 관광객 전용(일본 외 여권, 단기체재)',
   'https://www.jrkyushu.co.jp/english/railpass/railpass.html', DATE '2026-11-18'),

-- 후쿠오카 지하철 1일 승차권: 전 노선, 개찰 당일. (신용카드 터치 결제도 1일 상한 640엔)
  ('FUKUOKA_SUBWAY_1DAY', 'JP-FUKUOKA', 'PASS', '福岡市地下鉄 1日乗車券', '후쿠오카 지하철 1일 승차권',
   640, 1, TRUE, 0, FALSE, 0, NULL,
   'https://subway.city.fukuoka.lg.jp/fare/pdf/renrakuA4Side.pdf', DATE '2026-11-18'),

-- 니시테츠 버스 후쿠오카 시내 1일 프리 승차권(종이): 이용일 당일, 후쿠오카 시내 노선버스. 고속버스(히타호) 불가
  ('NISHITETSU_BUS_FUKUOKA_1DAY', 'JP-FUKUOKA', 'PASS', '福岡市内1日フリー乗車券', '니시테츠 버스 후쿠오카 시내 1일 프리 승차권',
   1200, 1, TRUE, 0, FALSE, 0, '시내 구역 밖으로 나가면 구역 경계부터 운임 별도',
   'https://www.nishitetsu.jp/bus/jyousha/cityfree/', DATE '2026-11-18'),
-- 같은 구역 디지털 24시간권(my route): 24시간이지만 계산은 1일로 함 → 보수적
  ('NISHITETSU_BUS_FUKUOKA_24H', 'JP-FUKUOKA', 'PASS', '福岡市内 24時間フリー乗車券（デジタル）', '니시테츠 버스 후쿠오카 시내 24시간권(디지털)',
   1100, 1, TRUE, 0, FALSE, 0, 'my route 앱 디지털권. 24시간 유효(계산은 이용일 1일로 함)',
   'https://www.nishitetsu.jp/bus/sumanori/', DATE '2026-11-18'),

-- 후쿠오카 투어리스트 시티 패스(시내판): 1일. 지하철·니시테츠 버스·JR(시내 일부)·니시테츠 전철·쇼와 버스·시영 도선.
--   단기체류 외국인 전용. 여기서는 지하철·니시테츠 버스만 커버로 넣음(위 머리말 참고)
  ('FUKUOKA_TOURIST_CITY_PASS', 'JP-FUKUOKA', 'PASS', 'FUKUOKA TOURIST CITY PASS（福岡市内）', '후쿠오카 투어리스트 시티 패스(시내)',
   2500, 1, TRUE, 0, FALSE, 0, '단기체류 외국인 관광객 전용(구입 시 여권 제시). JR은 시내 일부 구간만 되어 계산에 넣지 않음',
   'https://gofukuoka.jp/citypass.html', DATE '2026-11-18'),

-- 히타 마룻토 패스: 히타호 후쿠오카~히타 왕복 + 히타버스 노선 1일 + 쿠폰. my route 전용. 구입 후 10일 안,
--   이용 시작부터 다음다음 날 2시까지. 히타호 왕복 = 2회
  ('HITA_MARUTTO_PASS', 'JP-FUKUOKA', 'FIXED_ROUTE', '日田まるっとPASS', '히타 마룻토 패스',
   4580, 2, TRUE, 2, FALSE, 0, 'my route 앱 전용. 히타호 후쿠오카(또는 후쿠오카공항)~히타 왕복. 계산은 히타호 아무 구간 2회로 근사',
   'https://hitabus.com/pickups/4217', DATE '2026-11-18');

INSERT INTO pass.ticket_coverage (product_code, operator_code) VALUES
  ('SUNQ_NORTH_3DAY', 'NISHITETSU_BUS'),
  ('SUNQ_NORTH_3DAY', 'NISHITETSU_HIGHWAY_BUS'),
  ('JR_KYUSHU_RAIL_PASS_NORTH_3DAY', 'JR_KYUSHU'),
  ('JR_KYUSHU_RAIL_PASS_NORTH_5DAY', 'JR_KYUSHU'),
  ('FUKUOKA_SUBWAY_1DAY', 'FUKUOKA_SUBWAY'),
  ('NISHITETSU_BUS_FUKUOKA_1DAY', 'NISHITETSU_BUS'),
  ('NISHITETSU_BUS_FUKUOKA_24H', 'NISHITETSU_BUS'),
  ('FUKUOKA_TOURIST_CITY_PASS', 'FUKUOKA_SUBWAY'),
  ('FUKUOKA_TOURIST_CITY_PASS', 'NISHITETSU_BUS'),
  ('HITA_MARUTTO_PASS', 'NISHITETSU_HIGHWAY_BUS');
