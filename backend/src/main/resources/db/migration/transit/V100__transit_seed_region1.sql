-- 1단계 지역(후쿠오카 시내·공항, 이토시마, 우키하, 히타) 요금 데이터. 데이터 세션 작성.
-- 확인 못 한 값은 넣지 않는다(README 작업 규칙 4). 조사 자료 목록: docs/data-sources.md

-- =====================================================================
-- 후쿠오카 시영 지하철 (FUKUOKA_SUBWAY) — 거리 구간제
-- 요금표: https://subway.city.fukuoka.lg.jp/fare/futuken/  (2026-04-01 요금표 renrakuA4Side.pdf 와 일치)
-- 역간 영업거리: https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf (令和元年10月1日現在)
-- 나나쿠마선 하카타 연장(2023-03): 하카타~덴진미나미 1.6km (교통국 사업개요 subway_01.pdf)
--   구시다진자마에 0.6km는 공식 요금표(2026-04-01)의 구간 경계로 역산한 값:
--   덴진↔구시다진자마에 2구간(2.5+x>3), 니시진↔구시다진자마에 2구간(6.4+x<=7) → 0.1km 단위로 0.6
-- 누적 km 검증: 같은 km로 계산한 전 역 쌍 구간이 2026-04-01 공식 요금표와 일치(판독 606칸, 불일치 0)
-- 공항선↔나나쿠마선 환승 거리는 하카타 경유로 계산된다(덴진↔덴진미나미는 환승역이 아님).
-- 요금 기준: 승차권(きっぷ) 普通料金. 터치 결제도 "普通料金と同じ"(요금표 PDF).
-- =====================================================================

INSERT INTO transit.operator_alias (operator_code, alias) VALUES
  ('FUKUOKA_SUBWAY', '福岡市地下鉄'),
  ('FUKUOKA_SUBWAY', '福岡市交通局'),
  ('FUKUOKA_SUBWAY', 'Fukuoka City Subway');

INSERT INTO transit.line (operator_code, code, name_ja, name_ko) VALUES
  ('FUKUOKA_SUBWAY', 'K', '空港線', '공항선'),
  ('FUKUOKA_SUBWAY', 'H', '箱崎線', '하코자키선'),
  ('FUKUOKA_SUBWAY', 'N', '七隈線', '나나쿠마선');

-- 환승역(中洲川端, 博多)은 운영사 안에서 한 행, 노선 소속은 line_station으로
INSERT INTO transit.station (region_code, operator_code, name_ja, name_ko, source_url, verified_on) VALUES
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '姪浜',           '메이노하마',           'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '室見',           '무로미',               'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '藤崎',           '후지사키',             'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '西新',           '니시진',               'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '唐人町',         '도진마치',             'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '大濠公園',       '오호리코엔',           'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '赤坂',           '아카사카',             'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '天神',           '덴진',                 'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '中洲川端',       '나카스카와바타',       'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '祇園',           '기온',                 'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '博多',           '하카타',               'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '東比恵',         '히가시히에',           'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '福岡空港',       '후쿠오카공항',         'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '呉服町',         '고후쿠마치',           'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '千代県庁口',     '지요켄초구치',         'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '馬出九大病院前', '마이다시큐다이뵤인마에', 'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '箱崎宮前',       '하코자키미야마에',     'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '箱崎九大前',     '하코자키큐다이마에',   'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '貝塚',           '가이즈카',             'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '櫛田神社前',     '구시다진자마에',       'https://subway.city.fukuoka.lg.jp/fare/pdf/renrakuA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '天神南',         '덴진미나미',           'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '渡辺通',         '와타나베도리',         'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '薬院',           '야쿠인',               'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '薬院大通',       '야쿠인오도리',         'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '桜坂',           '사쿠라자카',           'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '六本松',         '롯폰마쓰',             'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '別府',           '베후',                 'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '茶山',           '차야마',               'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '金山',           '가나야마',             'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '七隈',           '나나쿠마',             'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '福大前',         '후쿠다이마에',         'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '梅林',           '우메바야시',           'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '野芥',           '노케',                 'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '賀茂',           '가모',                 'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '次郎丸',         '지로마루',             'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'FUKUOKA_SUBWAY', '橋本',           '하시모토',             'https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf', DATE '2026-11-18');

-- 누적 km: 공항선은 姪浜 0, 하코자키선은 中洲川端 0, 나나쿠마선은 博多 0
INSERT INTO transit.line_station (line_id, station_id, cumulative_km)
SELECT l.id, s.id, v.km
FROM (VALUES
  ('K', '姪浜', 0.0), ('K', '室見', 1.5), ('K', '藤崎', 2.3), ('K', '西新', 3.4), ('K', '唐人町', 4.6),
  ('K', '大濠公園', 5.4), ('K', '赤坂', 6.5), ('K', '天神', 7.3), ('K', '中洲川端', 8.1), ('K', '祇園', 9.1),
  ('K', '博多', 9.8), ('K', '東比恵', 11.0), ('K', '福岡空港', 13.1),
  ('H', '中洲川端', 0.0), ('H', '呉服町', 0.5), ('H', '千代県庁口', 1.2), ('H', '馬出九大病院前', 2.1),
  ('H', '箱崎宮前', 2.9), ('H', '箱崎九大前', 3.7), ('H', '貝塚', 4.7),
  ('N', '博多', 0.0), ('N', '櫛田神社前', 0.6), ('N', '天神南', 1.6), ('N', '渡辺通', 2.3), ('N', '薬院', 2.8),
  ('N', '薬院大通', 3.4), ('N', '桜坂', 4.4), ('N', '六本松', 5.3), ('N', '別府', 6.1), ('N', '茶山', 7.1),
  ('N', '金山', 7.9), ('N', '七隈', 8.7), ('N', '福大前', 9.3), ('N', '梅林', 10.2), ('N', '野芥', 11.0),
  ('N', '賀茂', 11.9), ('N', '次郎丸', 12.6), ('N', '橋本', 13.6)
) AS v(line_code, name_ja, km)
JOIN transit.line l ON l.operator_code = 'FUKUOKA_SUBWAY' AND l.code = v.line_code
JOIN transit.station s ON s.operator_code = 'FUKUOKA_SUBWAY' AND s.name_ja = v.name_ja;

-- 6구간 "19km를 넘는 경우"는 max_km 999.9로 표현
INSERT INTO transit.distance_fare_band (operator_code, kind, max_km, amount, source_url, verified_on) VALUES
  ('FUKUOKA_SUBWAY', 'BASE',   3.0, 210, 'https://subway.city.fukuoka.lg.jp/fare/futuken/', DATE '2026-11-18'),
  ('FUKUOKA_SUBWAY', 'BASE',   7.0, 260, 'https://subway.city.fukuoka.lg.jp/fare/futuken/', DATE '2026-11-18'),
  ('FUKUOKA_SUBWAY', 'BASE',  11.0, 300, 'https://subway.city.fukuoka.lg.jp/fare/futuken/', DATE '2026-11-18'),
  ('FUKUOKA_SUBWAY', 'BASE',  15.0, 340, 'https://subway.city.fukuoka.lg.jp/fare/futuken/', DATE '2026-11-18'),
  ('FUKUOKA_SUBWAY', 'BASE',  19.0, 360, 'https://subway.city.fukuoka.lg.jp/fare/futuken/', DATE '2026-11-18'),
  ('FUKUOKA_SUBWAY', 'BASE', 999.9, 380, 'https://subway.city.fukuoka.lg.jp/fare/futuken/', DATE '2026-11-18');

-- 공항선·하코자키선 역 ↔ 나나쿠마선 역: 공식 요금(하카타 경유 거리) — 2026-04-01 요금표 https://subway.city.fukuoka.lg.jp/fare/pdf/renrakuA4Side.pdf
-- 덴진↔덴진미나미를 걸어서 갈아타는 경로로 구간이 들어와도 엔진이 묶음의 첫 역→마지막 역 pair_fare를 먼저 쓰므로 공식 요금이 된다.
-- 값은 위 누적 km의 하카타 경유 최단거리 → 구간 요금(요금표 판독 606칸과 불일치 0으로 검증).
INSERT INTO transit.pair_fare (operator_code, station_a_id, station_b_id, kind, amount, source_url, verified_on)
SELECT 'FUKUOKA_SUBWAY', a.id, b.id, 'BASE', v.amount, 'https://subway.city.fukuoka.lg.jp/fare/pdf/renrakuA4Side.pdf', DATE '2026-11-18'
FROM (VALUES
  ('姪浜', '櫛田神社前', 300),
  ('姪浜', '天神南', 340),
  ('姪浜', '渡辺通', 340),
  ('姪浜', '薬院', 340),
  ('姪浜', '薬院大通', 340),
  ('姪浜', '桜坂', 340),
  ('姪浜', '六本松', 360),
  ('姪浜', '別府', 360),
  ('姪浜', '茶山', 360),
  ('姪浜', '金山', 360),
  ('姪浜', '七隈', 360),
  ('姪浜', '福大前', 380),
  ('姪浜', '梅林', 380),
  ('姪浜', '野芥', 380),
  ('姪浜', '賀茂', 380),
  ('姪浜', '次郎丸', 380),
  ('姪浜', '橋本', 380),
  ('室見', '櫛田神社前', 300),
  ('室見', '天神南', 300),
  ('室見', '渡辺通', 300),
  ('室見', '薬院', 340),
  ('室見', '薬院大通', 340),
  ('室見', '桜坂', 340),
  ('室見', '六本松', 340),
  ('室見', '別府', 340),
  ('室見', '茶山', 360),
  ('室見', '金山', 360),
  ('室見', '七隈', 360),
  ('室見', '福大前', 360),
  ('室見', '梅林', 360),
  ('室見', '野芥', 380),
  ('室見', '賀茂', 380),
  ('室見', '次郎丸', 380),
  ('室見', '橋本', 380),
  ('藤崎', '櫛田神社前', 300),
  ('藤崎', '天神南', 300),
  ('藤崎', '渡辺通', 300),
  ('藤崎', '薬院', 300),
  ('藤崎', '薬院大通', 300),
  ('藤崎', '桜坂', 340),
  ('藤崎', '六本松', 340),
  ('藤崎', '別府', 340),
  ('藤崎', '茶山', 340),
  ('藤崎', '金山', 360),
  ('藤崎', '七隈', 360),
  ('藤崎', '福大前', 360),
  ('藤崎', '梅林', 360),
  ('藤崎', '野芥', 360),
  ('藤崎', '賀茂', 380),
  ('藤崎', '次郎丸', 380),
  ('藤崎', '橋本', 380),
  ('西新', '櫛田神社前', 260),
  ('西新', '天神南', 300),
  ('西新', '渡辺通', 300),
  ('西新', '薬院', 300),
  ('西新', '薬院大通', 300),
  ('西新', '桜坂', 300),
  ('西新', '六本松', 340),
  ('西新', '別府', 340),
  ('西新', '茶山', 340),
  ('西新', '金山', 340),
  ('西新', '七隈', 360),
  ('西新', '福大前', 360),
  ('西新', '梅林', 360),
  ('西新', '野芥', 360),
  ('西新', '賀茂', 360),
  ('西新', '次郎丸', 360),
  ('西新', '橋本', 380),
  ('唐人町', '櫛田神社前', 260),
  ('唐人町', '天神南', 260),
  ('唐人町', '渡辺通', 300),
  ('唐人町', '薬院', 300),
  ('唐人町', '薬院大通', 300),
  ('唐人町', '桜坂', 300),
  ('唐人町', '六本松', 300),
  ('唐人町', '別府', 340),
  ('唐人町', '茶山', 340),
  ('唐人町', '金山', 340),
  ('唐人町', '七隈', 340),
  ('唐人町', '福大前', 340),
  ('唐人町', '梅林', 360),
  ('唐人町', '野芥', 360),
  ('唐人町', '賀茂', 360),
  ('唐人町', '次郎丸', 360),
  ('唐人町', '橋本', 360),
  ('大濠公園', '櫛田神社前', 260),
  ('大濠公園', '天神南', 260),
  ('大濠公園', '渡辺通', 260),
  ('大濠公園', '薬院', 300),
  ('大濠公園', '薬院大通', 300),
  ('大濠公園', '桜坂', 300),
  ('大濠公園', '六本松', 300),
  ('大濠公園', '別府', 300),
  ('大濠公園', '茶山', 340),
  ('大濠公園', '金山', 340),
  ('大濠公園', '七隈', 340),
  ('大濠公園', '福大前', 340),
  ('大濠公園', '梅林', 340),
  ('大濠公園', '野芥', 360),
  ('大濠公園', '賀茂', 360),
  ('大濠公園', '次郎丸', 360),
  ('大濠公園', '橋本', 360),
  ('赤坂', '櫛田神社前', 260),
  ('赤坂', '天神南', 260),
  ('赤坂', '渡辺通', 260),
  ('赤坂', '薬院', 260),
  ('赤坂', '薬院大通', 260),
  ('赤坂', '桜坂', 300),
  ('赤坂', '六本松', 300),
  ('赤坂', '別府', 300),
  ('赤坂', '茶山', 300),
  ('赤坂', '金山', 340),
  ('赤坂', '七隈', 340),
  ('赤坂', '福大前', 340),
  ('赤坂', '梅林', 340),
  ('赤坂', '野芥', 340),
  ('赤坂', '賀茂', 360),
  ('赤坂', '次郎丸', 360),
  ('赤坂', '橋本', 360),
  ('天神', '櫛田神社前', 260),
  ('天神', '天神南', 260),
  ('天神', '渡辺通', 260),
  ('天神', '薬院', 260),
  ('天神', '薬院大通', 260),
  ('天神', '桜坂', 260),
  ('天神', '六本松', 300),
  ('天神', '別府', 300),
  ('天神', '茶山', 300),
  ('天神', '金山', 300),
  ('天神', '七隈', 340),
  ('天神', '福大前', 340),
  ('天神', '梅林', 340),
  ('天神', '野芥', 340),
  ('天神', '賀茂', 340),
  ('天神', '次郎丸', 360),
  ('天神', '橋本', 360),
  ('中洲川端', '櫛田神社前', 210),
  ('中洲川端', '天神南', 260),
  ('中洲川端', '渡辺通', 260),
  ('中洲川端', '薬院', 260),
  ('中洲川端', '薬院大通', 260),
  ('中洲川端', '桜坂', 260),
  ('中洲川端', '六本松', 260),
  ('中洲川端', '別府', 300),
  ('中洲川端', '茶山', 300),
  ('中洲川端', '金山', 300),
  ('中洲川端', '七隈', 300),
  ('中洲川端', '福大前', 300),
  ('中洲川端', '梅林', 340),
  ('中洲川端', '野芥', 340),
  ('中洲川端', '賀茂', 340),
  ('中洲川端', '次郎丸', 340),
  ('中洲川端', '橋本', 360),
  ('祇園', '櫛田神社前', 210),
  ('祇園', '天神南', 210),
  ('祇園', '渡辺通', 210),
  ('祇園', '薬院', 260),
  ('祇園', '薬院大通', 260),
  ('祇園', '桜坂', 260),
  ('祇園', '六本松', 260),
  ('祇園', '別府', 260),
  ('祇園', '茶山', 300),
  ('祇園', '金山', 300),
  ('祇園', '七隈', 300),
  ('祇園', '福大前', 300),
  ('祇園', '梅林', 300),
  ('祇園', '野芥', 340),
  ('祇園', '賀茂', 340),
  ('祇園', '次郎丸', 340),
  ('祇園', '橋本', 340),
  ('東比恵', '櫛田神社前', 210),
  ('東比恵', '天神南', 210),
  ('東比恵', '渡辺通', 260),
  ('東比恵', '薬院', 260),
  ('東比恵', '薬院大通', 260),
  ('東比恵', '桜坂', 260),
  ('東比恵', '六本松', 260),
  ('東比恵', '別府', 300),
  ('東比恵', '茶山', 300),
  ('東比恵', '金山', 300),
  ('東比恵', '七隈', 300),
  ('東比恵', '福大前', 300),
  ('東比恵', '梅林', 340),
  ('東比恵', '野芥', 340),
  ('東比恵', '賀茂', 340),
  ('東比恵', '次郎丸', 340),
  ('東比恵', '橋本', 340),
  ('福岡空港', '櫛田神社前', 260),
  ('福岡空港', '天神南', 260),
  ('福岡空港', '渡辺通', 260),
  ('福岡空港', '薬院', 260),
  ('福岡空港', '薬院大通', 260),
  ('福岡空港', '桜坂', 300),
  ('福岡空港', '六本松', 300),
  ('福岡空港', '別府', 300),
  ('福岡空港', '茶山', 300),
  ('福岡空港', '金山', 340),
  ('福岡空港', '七隈', 340),
  ('福岡空港', '福大前', 340),
  ('福岡空港', '梅林', 340),
  ('福岡空港', '野芥', 340),
  ('福岡空港', '賀茂', 360),
  ('福岡空港', '次郎丸', 360),
  ('福岡空港', '橋本', 360),
  ('呉服町', '櫛田神社前', 210),
  ('呉服町', '天神南', 260),
  ('呉服町', '渡辺通', 260),
  ('呉服町', '薬院', 260),
  ('呉服町', '薬院大通', 260),
  ('呉服町', '桜坂', 260),
  ('呉服町', '六本松', 300),
  ('呉服町', '別府', 300),
  ('呉服町', '茶山', 300),
  ('呉服町', '金山', 300),
  ('呉服町', '七隈', 300),
  ('呉服町', '福大前', 340),
  ('呉服町', '梅林', 340),
  ('呉服町', '野芥', 340),
  ('呉服町', '賀茂', 340),
  ('呉服町', '次郎丸', 340),
  ('呉服町', '橋本', 360),
  ('千代県庁口', '櫛田神社前', 260),
  ('千代県庁口', '天神南', 260),
  ('千代県庁口', '渡辺通', 260),
  ('千代県庁口', '薬院', 260),
  ('千代県庁口', '薬院大通', 260),
  ('千代県庁口', '桜坂', 300),
  ('千代県庁口', '六本松', 300),
  ('千代県庁口', '別府', 300),
  ('千代県庁口', '茶山', 300),
  ('千代県庁口', '金山', 300),
  ('千代県庁口', '七隈', 340),
  ('千代県庁口', '福大前', 340),
  ('千代県庁口', '梅林', 340),
  ('千代県庁口', '野芥', 340),
  ('千代県庁口', '賀茂', 340),
  ('千代県庁口', '次郎丸', 360),
  ('千代県庁口', '橋本', 360),
  ('馬出九大病院前', '櫛田神社前', 260),
  ('馬出九大病院前', '天神南', 260),
  ('馬出九大病院前', '渡辺通', 260),
  ('馬出九大病院前', '薬院', 260),
  ('馬出九大病院前', '薬院大通', 300),
  ('馬出九大病院前', '桜坂', 300),
  ('馬出九大病院前', '六本松', 300),
  ('馬出九大病院前', '別府', 300),
  ('馬出九大病院前', '茶山', 300),
  ('馬出九大病院前', '金山', 340),
  ('馬出九大病院前', '七隈', 340),
  ('馬出九大病院前', '福大前', 340),
  ('馬出九大病院前', '梅林', 340),
  ('馬出九大病院前', '野芥', 340),
  ('馬出九大病院前', '賀茂', 360),
  ('馬出九大病院前', '次郎丸', 360),
  ('馬出九大病院前', '橋本', 360),
  ('箱崎宮前', '櫛田神社前', 260),
  ('箱崎宮前', '天神南', 260),
  ('箱崎宮前', '渡辺通', 260),
  ('箱崎宮前', '薬院', 300),
  ('箱崎宮前', '薬院大通', 300),
  ('箱崎宮前', '桜坂', 300),
  ('箱崎宮前', '六本松', 300),
  ('箱崎宮前', '別府', 300),
  ('箱崎宮前', '茶山', 340),
  ('箱崎宮前', '金山', 340),
  ('箱崎宮前', '七隈', 340),
  ('箱崎宮前', '福大前', 340),
  ('箱崎宮前', '梅林', 340),
  ('箱崎宮前', '野芥', 360),
  ('箱崎宮前', '賀茂', 360),
  ('箱崎宮前', '次郎丸', 360),
  ('箱崎宮前', '橋本', 360),
  ('箱崎九大前', '櫛田神社前', 260),
  ('箱崎九大前', '天神南', 260),
  ('箱崎九大前', '渡辺通', 300),
  ('箱崎九大前', '薬院', 300),
  ('箱崎九大前', '薬院大通', 300),
  ('箱崎九大前', '桜坂', 300),
  ('箱崎九大前', '六本松', 300),
  ('箱崎九大前', '別府', 340),
  ('箱崎九大前', '茶山', 340),
  ('箱崎九大前', '金山', 340),
  ('箱崎九大前', '七隈', 340),
  ('箱崎九大前', '福大前', 340),
  ('箱崎九大前', '梅林', 360),
  ('箱崎九大前', '野芥', 360),
  ('箱崎九大前', '賀茂', 360),
  ('箱崎九大前', '次郎丸', 360),
  ('箱崎九大前', '橋本', 360),
  ('貝塚', '櫛田神社前', 260),
  ('貝塚', '天神南', 300),
  ('貝塚', '渡辺通', 300),
  ('貝塚', '薬院', 300),
  ('貝塚', '薬院大通', 300),
  ('貝塚', '桜坂', 300),
  ('貝塚', '六本松', 340),
  ('貝塚', '別府', 340),
  ('貝塚', '茶山', 340),
  ('貝塚', '金山', 340),
  ('貝塚', '七隈', 360),
  ('貝塚', '福大前', 360),
  ('貝塚', '梅林', 360),
  ('貝塚', '野芥', 360),
  ('貝塚', '賀茂', 360),
  ('貝塚', '次郎丸', 360),
  ('貝塚', '橋本', 380)
) AS v(a_name, b_name, amount)
JOIN transit.station a ON a.operator_code = 'FUKUOKA_SUBWAY' AND a.name_ja = v.a_name
JOIN transit.station b ON b.operator_code = 'FUKUOKA_SUBWAY' AND b.name_ja = v.b_name;

-- =====================================================================
-- JR 큐슈 (JR_KYUSHU) — 거리 구간제 (2025-04-01 운임 개정, 2026-10-01 여객영업규칙 현행)
-- 운임: 旅客営業規則 第77条の5(간선 11~100km), 第84条の5(10km까지) — https://www.jrkyushu.co.jp/railway/ticket/rule/transport/__icsFiles/afieldfile/2026/09/30/20261001_conditions_of_carriage_01_65_146.pdf
-- 특급요금(통상기, JR큐슈 재래선): 지정석 1,030/1,280/1,530/1,730/2,330/2,730/2,930/3,130,
--   자유석 = 지정석 − 530 (第57条の3, 개정 가이드북 https://www.jrkyushu.co.jp/railway/kaitei/pdf/241217_unchinkaitei_guidebook.pdf)
--   모델: EXPRESS 밴드 = 자유석 특급요금, reserved_seat_fee = 530(지정석 차액)
--   번성기(+200)·최번성기(+400)·관광열차(ゆふいんの森 등 +500)는 반영하지 않음 → 통상기 특급 기준
-- 선 구분(別表第1号): 鹿児島本線·筑肥線 = 간선, 久大線 = 지방교통선
--   간선+지방교통선 연속 승차는 운임계산키로(간선 영업키로 + 지방교통선 의제키로)로 간선 운임표 적용(第81条の5)
--   → 久大本線 line_station.cumulative_km 에는 의제키로(영업키로×1.1, 0.1km 반올림)를 저장한다.
--     공식 확인: 가이드북 久留米~由布院 영업 99.1 → 의제 109.0
--   특급요금은 영업키로 기준이지만 엔진은 같은 km를 쓴다. 이 데이터 범위(博多~日田 83.3/88.1,
--   博多~うきは 65.7/68.7)에서는 밴드가 같아 결과 동일. 범위를 넓힐 때 재검토.
-- 역별 영업키로: JR큐슈 공식 역별 키로 공개 자료 없음 → 일본어 위키백과 노선 기사(국토교통성 鉄道要覧 인용).
--   공식 대조: 博多~久留米 35.7km(가이드북), 姪浜~筑肥線 각 역 운임(지하철 연락운임표 renrakuA4Side.pdf) 일치,
--   博多~日田 운임 1,930엔(ゆふいんの森 공식 페이지 4,160엔 − 특급 2,230엔) 일치
-- =====================================================================

INSERT INTO transit.operator_alias (operator_code, alias) VALUES
  ('JR_KYUSHU', 'JR九州'),
  ('JR_KYUSHU', 'JR Kyushu'),
  ('JR_KYUSHU', '九州旅客鉄道');

INSERT INTO transit.line (operator_code, code, name_ja, name_ko) VALUES
  ('JR_KYUSHU', 'KAGOSHIMA', '鹿児島本線', '가고시마본선'),
  ('JR_KYUSHU', 'KYUDAI',    '久大本線',   '규다이본선'),
  ('JR_KYUSHU', 'CHIKUHI',   '筑肥線',     '지쿠히선');

-- 久留米는 두 노선 공통 한 행
INSERT INTO transit.station (region_code, operator_code, name_ja, name_ko, source_url, verified_on) VALUES
  ('JP-FUKUOKA', 'JR_KYUSHU', '博多', '하카타', 'https://ja.wikipedia.org/wiki/%E9%B9%BF%E5%85%90%E5%B3%B6%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '竹下', '다케시타', 'https://ja.wikipedia.org/wiki/%E9%B9%BF%E5%85%90%E5%B3%B6%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '笹原', '사사바루', 'https://ja.wikipedia.org/wiki/%E9%B9%BF%E5%85%90%E5%B3%B6%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '南福岡', '미나미후쿠오카', 'https://ja.wikipedia.org/wiki/%E9%B9%BF%E5%85%90%E5%B3%B6%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '春日', '가스가', 'https://ja.wikipedia.org/wiki/%E9%B9%BF%E5%85%90%E5%B3%B6%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '大野城', '오노조', 'https://ja.wikipedia.org/wiki/%E9%B9%BF%E5%85%90%E5%B3%B6%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '水城', '미즈키', 'https://ja.wikipedia.org/wiki/%E9%B9%BF%E5%85%90%E5%B3%B6%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '都府楼南', '도후로미나미', 'https://ja.wikipedia.org/wiki/%E9%B9%BF%E5%85%90%E5%B3%B6%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '二日市', '후쓰카이치', 'https://ja.wikipedia.org/wiki/%E9%B9%BF%E5%85%90%E5%B3%B6%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '天拝山', '덴파이잔', 'https://ja.wikipedia.org/wiki/%E9%B9%BF%E5%85%90%E5%B3%B6%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '原田', '하루다', 'https://ja.wikipedia.org/wiki/%E9%B9%BF%E5%85%90%E5%B3%B6%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', 'けやき台', '게야키다이', 'https://ja.wikipedia.org/wiki/%E9%B9%BF%E5%85%90%E5%B3%B6%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '基山', '기야마', 'https://ja.wikipedia.org/wiki/%E9%B9%BF%E5%85%90%E5%B3%B6%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '弥生が丘', '야요이가오카', 'https://ja.wikipedia.org/wiki/%E9%B9%BF%E5%85%90%E5%B3%B6%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '田代', '다시로', 'https://ja.wikipedia.org/wiki/%E9%B9%BF%E5%85%90%E5%B3%B6%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '鳥栖', '도스', 'https://ja.wikipedia.org/wiki/%E9%B9%BF%E5%85%90%E5%B3%B6%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '肥前旭', '히젠아사히', 'https://ja.wikipedia.org/wiki/%E9%B9%BF%E5%85%90%E5%B3%B6%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '久留米', '구루메', 'https://ja.wikipedia.org/wiki/%E9%B9%BF%E5%85%90%E5%B3%B6%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '久留米高校前', '구루메코코마에', 'https://ja.wikipedia.org/wiki/%E4%B9%85%E5%A4%A7%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '南久留米', '미나미쿠루메', 'https://ja.wikipedia.org/wiki/%E4%B9%85%E5%A4%A7%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '久留米大学前', '구루메다이가쿠마에', 'https://ja.wikipedia.org/wiki/%E4%B9%85%E5%A4%A7%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '御井', '미이', 'https://ja.wikipedia.org/wiki/%E4%B9%85%E5%A4%A7%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '善導寺', '젠도지', 'https://ja.wikipedia.org/wiki/%E4%B9%85%E5%A4%A7%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '筑後草野', '지쿠고쿠사노', 'https://ja.wikipedia.org/wiki/%E4%B9%85%E5%A4%A7%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '田主丸', '다누시마루', 'https://ja.wikipedia.org/wiki/%E4%B9%85%E5%A4%A7%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '筑後吉井', '지쿠고요시이', 'https://ja.wikipedia.org/wiki/%E4%B9%85%E5%A4%A7%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', 'うきは', '우키하', 'https://ja.wikipedia.org/wiki/%E4%B9%85%E5%A4%A7%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '筑後大石', '지쿠고오이시', 'https://ja.wikipedia.org/wiki/%E4%B9%85%E5%A4%A7%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '夜明', '요아케', 'https://ja.wikipedia.org/wiki/%E4%B9%85%E5%A4%A7%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '光岡', '데루오카', 'https://ja.wikipedia.org/wiki/%E4%B9%85%E5%A4%A7%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '日田', '히타', 'https://ja.wikipedia.org/wiki/%E4%B9%85%E5%A4%A7%E6%9C%AC%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '姪浜', '메이노하마', 'https://ja.wikipedia.org/wiki/%E7%AD%91%E8%82%A5%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '下山門', '시모야마토', 'https://ja.wikipedia.org/wiki/%E7%AD%91%E8%82%A5%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '今宿', '이마주쿠', 'https://ja.wikipedia.org/wiki/%E7%AD%91%E8%82%A5%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '九大学研都市', '규다이각켄토시', 'https://ja.wikipedia.org/wiki/%E7%AD%91%E8%82%A5%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '周船寺', '스센지', 'https://ja.wikipedia.org/wiki/%E7%AD%91%E8%82%A5%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '波多江', '하타에', 'https://ja.wikipedia.org/wiki/%E7%AD%91%E8%82%A5%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '糸島高校前', '이토시마코코마에', 'https://ja.wikipedia.org/wiki/%E7%AD%91%E8%82%A5%E7%B7%9A', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'JR_KYUSHU', '筑前前原', '지쿠젠마에바루', 'https://ja.wikipedia.org/wiki/%E7%AD%91%E8%82%A5%E7%B7%9A', DATE '2026-11-18');

-- 鹿児島本線: 門司港 기준 영업키로 / 久大本線: 久留米 기준 의제키로(괄호 안 영업키로) / 筑肥線: 姪浜 기준 영업키로
INSERT INTO transit.line_station (line_id, station_id, cumulative_km)
SELECT l.id, s.id, v.km
FROM (VALUES
  ('KAGOSHIMA', '博多', 78.2),
  ('KAGOSHIMA', '竹下', 80.9),
  ('KAGOSHIMA', '笹原', 83.3),
  ('KAGOSHIMA', '南福岡', 84.9),
  ('KAGOSHIMA', '春日', 86.1),
  ('KAGOSHIMA', '大野城', 87.4),
  ('KAGOSHIMA', '水城', 88.8),
  ('KAGOSHIMA', '都府楼南', 91.0),
  ('KAGOSHIMA', '二日市', 92.4),
  ('KAGOSHIMA', '天拝山', 94.3),
  ('KAGOSHIMA', '原田', 97.9),
  ('KAGOSHIMA', 'けやき台', 99.9),
  ('KAGOSHIMA', '基山', 101.4),
  ('KAGOSHIMA', '弥生が丘', 103.5),
  ('KAGOSHIMA', '田代', 105.6),
  ('KAGOSHIMA', '鳥栖', 106.8),
  ('KAGOSHIMA', '肥前旭', 110.4),
  ('KAGOSHIMA', '久留米', 113.9),
  ('KYUDAI', '久留米', 0.0),
  ('KYUDAI', '久留米高校前', 3.7),
  ('KYUDAI', '南久留米', 5.4),
  ('KYUDAI', '久留米大学前', 7.5),
  ('KYUDAI', '御井', 8.8),
  ('KYUDAI', '善導寺', 13.9),
  ('KYUDAI', '筑後草野', 17.3),
  ('KYUDAI', '田主丸', 22.9),
  ('KYUDAI', '筑後吉井', 29.0),
  ('KYUDAI', 'うきは', 33.0),
  ('KYUDAI', '筑後大石', 36.3),
  ('KYUDAI', '夜明', 43.0),
  ('KYUDAI', '光岡', 49.7),
  ('KYUDAI', '日田', 52.4),
  ('CHIKUHI', '姪浜', 0.0),
  ('CHIKUHI', '下山門', 1.6),
  ('CHIKUHI', '今宿', 5.2),
  ('CHIKUHI', '九大学研都市', 6.5),
  ('CHIKUHI', '周船寺', 8.1),
  ('CHIKUHI', '波多江', 10.1),
  ('CHIKUHI', '糸島高校前', 11.3),
  ('CHIKUHI', '筑前前原', 12.7)
) AS v(line_code, name_ja, km)
JOIN transit.line l ON l.operator_code = 'JR_KYUSHU' AND l.code = v.line_code
JOIN transit.station s ON s.operator_code = 'JR_KYUSHU' AND s.name_ja = v.name_ja;

-- 久大本線 영업키로 참고(의제키로 계산 근거): 久留米 0.0, 久留米高校前 3.4, 南久留米 4.9, 久留米大学前 6.8, 御井 8.0, 善導寺 12.6, 筑後草野 15.7, 田主丸 20.8, 筑後吉井 26.4, うきは 30.0, 筑後大石 33.0, 夜明 39.1, 光岡 45.2, 日田 47.6

-- 100km 초과는 km당 계산(19엔75전)이라 밴드로 넣지 않음 → 이 범위 밖은 미확인 처리
INSERT INTO transit.distance_fare_band (operator_code, kind, max_km, amount, source_url, verified_on) VALUES
  ('JR_KYUSHU', 'BASE', 3.0, 200, 'https://www.jrkyushu.co.jp/railway/ticket/rule/transport/__icsFiles/afieldfile/2026/09/30/20261001_conditions_of_carriage_01_65_146.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'BASE', 6.0, 240, 'https://www.jrkyushu.co.jp/railway/ticket/rule/transport/__icsFiles/afieldfile/2026/09/30/20261001_conditions_of_carriage_01_65_146.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'BASE', 10.0, 270, 'https://www.jrkyushu.co.jp/railway/ticket/rule/transport/__icsFiles/afieldfile/2026/09/30/20261001_conditions_of_carriage_01_65_146.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'BASE', 15.0, 340, 'https://www.jrkyushu.co.jp/railway/ticket/rule/transport/__icsFiles/afieldfile/2026/09/30/20261001_conditions_of_carriage_01_65_146.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'BASE', 20.0, 450, 'https://www.jrkyushu.co.jp/railway/ticket/rule/transport/__icsFiles/afieldfile/2026/09/30/20261001_conditions_of_carriage_01_65_146.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'BASE', 25.0, 560, 'https://www.jrkyushu.co.jp/railway/ticket/rule/transport/__icsFiles/afieldfile/2026/09/30/20261001_conditions_of_carriage_01_65_146.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'BASE', 30.0, 660, 'https://www.jrkyushu.co.jp/railway/ticket/rule/transport/__icsFiles/afieldfile/2026/09/30/20261001_conditions_of_carriage_01_65_146.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'BASE', 35.0, 760, 'https://www.jrkyushu.co.jp/railway/ticket/rule/transport/__icsFiles/afieldfile/2026/09/30/20261001_conditions_of_carriage_01_65_146.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'BASE', 40.0, 870, 'https://www.jrkyushu.co.jp/railway/ticket/rule/transport/__icsFiles/afieldfile/2026/09/30/20261001_conditions_of_carriage_01_65_146.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'BASE', 45.0, 990, 'https://www.jrkyushu.co.jp/railway/ticket/rule/transport/__icsFiles/afieldfile/2026/09/30/20261001_conditions_of_carriage_01_65_146.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'BASE', 50.0, 1090, 'https://www.jrkyushu.co.jp/railway/ticket/rule/transport/__icsFiles/afieldfile/2026/09/30/20261001_conditions_of_carriage_01_65_146.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'BASE', 60.0, 1300, 'https://www.jrkyushu.co.jp/railway/ticket/rule/transport/__icsFiles/afieldfile/2026/09/30/20261001_conditions_of_carriage_01_65_146.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'BASE', 70.0, 1510, 'https://www.jrkyushu.co.jp/railway/ticket/rule/transport/__icsFiles/afieldfile/2026/09/30/20261001_conditions_of_carriage_01_65_146.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'BASE', 80.0, 1730, 'https://www.jrkyushu.co.jp/railway/ticket/rule/transport/__icsFiles/afieldfile/2026/09/30/20261001_conditions_of_carriage_01_65_146.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'BASE', 90.0, 1930, 'https://www.jrkyushu.co.jp/railway/ticket/rule/transport/__icsFiles/afieldfile/2026/09/30/20261001_conditions_of_carriage_01_65_146.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'BASE', 100.0, 2130, 'https://www.jrkyushu.co.jp/railway/ticket/rule/transport/__icsFiles/afieldfile/2026/09/30/20261001_conditions_of_carriage_01_65_146.pdf', DATE '2026-11-18');

-- 자유석 특급요금 (301km 이상은 max_km 999.9)
INSERT INTO transit.distance_fare_band (operator_code, kind, max_km, amount, source_url, verified_on) VALUES
  ('JR_KYUSHU', 'EXPRESS', 25.0, 500, 'https://www.jrkyushu.co.jp/railway/kaitei/pdf/241217_unchinkaitei_guidebook.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'EXPRESS', 50.0, 750, 'https://www.jrkyushu.co.jp/railway/kaitei/pdf/241217_unchinkaitei_guidebook.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'EXPRESS', 75.0, 1000, 'https://www.jrkyushu.co.jp/railway/kaitei/pdf/241217_unchinkaitei_guidebook.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'EXPRESS', 100.0, 1200, 'https://www.jrkyushu.co.jp/railway/kaitei/pdf/241217_unchinkaitei_guidebook.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'EXPRESS', 150.0, 1800, 'https://www.jrkyushu.co.jp/railway/kaitei/pdf/241217_unchinkaitei_guidebook.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'EXPRESS', 200.0, 2200, 'https://www.jrkyushu.co.jp/railway/kaitei/pdf/241217_unchinkaitei_guidebook.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'EXPRESS', 300.0, 2400, 'https://www.jrkyushu.co.jp/railway/kaitei/pdf/241217_unchinkaitei_guidebook.pdf', DATE '2026-11-18'),
  ('JR_KYUSHU', 'EXPRESS', 999.9, 2600, 'https://www.jrkyushu.co.jp/railway/kaitei/pdf/241217_unchinkaitei_guidebook.pdf', DATE '2026-11-18');

-- 지정석 = 자유석 + 530
INSERT INTO transit.reserved_seat_fee (operator_code, amount, source_url, verified_on) VALUES
  ('JR_KYUSHU', 530, 'https://www.jrkyushu.co.jp/railway/kaitei/pdf/241217_unchinkaitei_guidebook.pdf', DATE '2026-11-18');

-- 지방교통선(久大線) 안에서만 타는 경우의 특정 운임(第77条の10 ②, 第84条の5(2)) — 간선 운임표보다 싸다
-- 엔진이 DISTANCE_BAND 운영사에서 pair_fare(BASE)를 우선 적용해야 의미가 있음 (transit 세션 확인 중)
INSERT INTO transit.pair_fare (operator_code, station_a_id, station_b_id, kind, amount, source_url, verified_on)
SELECT 'JR_KYUSHU', a.id, b.id, 'BASE', v.amount, 'https://www.jrkyushu.co.jp/railway/ticket/rule/transport/__icsFiles/afieldfile/2026/09/30/20261001_conditions_of_carriage_01_65_146.pdf', DATE '2026-11-18'
FROM (VALUES
  ('久留米高校前', '善導寺', 320),  -- 영업 9.2 / 의제 10.2
  ('久留米高校前', '筑後吉井', 580),  -- 영업 23.0 / 의제 25.3
  ('久留米大学前', '田主丸', 360),  -- 영업 14.0 / 의제 15.4
  ('久留米大学前', '筑後吉井', 470),  -- 영업 19.6 / 의제 21.5
  ('御井', '筑後吉井', 470),  -- 영업 18.4 / 의제 20.2
  ('善導寺', '筑後吉井', 360),  -- 영업 13.8 / 의제 15.1
  ('筑後草野', 'うきは', 360),  -- 영업 14.3 / 의제 15.7
  ('筑後草野', '日田', 840),  -- 영업 31.9 / 의제 35.1
  ('田主丸', 'うきは', 320),  -- 영업 9.2 / 의제 10.1
  ('田主丸', '夜明', 470),  -- 영업 18.3 / 의제 20.1
  ('筑後吉井', '光岡', 470),  -- 영업 18.8 / 의제 20.7
  ('うきは', '筑後大石', 210),  -- 영업 3.0 / 의제 3.3
  ('筑後大石', '日田', 360)  -- 영업 14.6 / 의제 16.1
) AS v(a_name, b_name, amount)
JOIN transit.station a ON a.operator_code = 'JR_KYUSHU' AND a.name_ja = v.a_name
JOIN transit.station b ON b.operator_code = 'JR_KYUSHU' AND b.name_ja = v.b_name;

-- =====================================================================
-- 니시테츠 버스 (NISHITETSU_BUS) — 균일 구간 + 정류장 쌍 요금
-- 후쿠오카 도심 150엔 균일 구간: https://www.nishitetsu.jp/bus/rosen/150/
--   "このエリア内の乗り降りならどのバスでも大人150円" — 구역 지도(d6c345ae70b2675a0ae88ba5310feff8.png)의 정류장 39곳 전부.
--   정류장 이름은 니시테츠 시각표 정류장 검색(https://jik.nishitetsu.jp/) 표기. 승차장(のりば)별로 나뉜 정류장은 기본 이름 한 행.
--   지도의 "天神ソラリアステージ前(福ビル街区)(7C)" 승차장은 현재 jik에서 "天神ワンビル前（旧天神ソラリア）" → 天神ワンビル前으로 넣음.
--   지도에 이름 없는 덴진 승차장 약 3곳은 확인 못 해 넣지 않음. 역 기준 150엔 구역(역 반경 약 1km)도 넣지 않음.
-- 요금 기준: IC·카드(nimoca·교통계 IC·신용카드 터치). 150엔 구역은 현금과 같고, 공항 노선만 IC가 20엔 싸다.
-- =====================================================================

INSERT INTO transit.operator_alias (operator_code, alias) VALUES
  ('NISHITETSU_BUS', '西鉄バス'),
  ('NISHITETSU_BUS', '西日本鉄道'),
  ('NISHITETSU_BUS', 'Nishitetsu Bus');

INSERT INTO transit.station (region_code, operator_code, name_ja, name_ko, source_url, verified_on) VALUES
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '蔵本', '구라모토', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '博多五町', '하카타고초', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '土居町', '도이마치', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '呉服町', '고후쿠마치', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '奥の堂', '오쿠노도', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '川端町・博多座前', '가와바타마치·하카타자앞', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '中洲', '나카스', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '東中洲', '히가시나카스', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '中洲・南新地', '나카스·미나미신치', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '祇園町', '기온마치', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', 'アクロス福岡・水鏡天満宮前', '아크로스후쿠오카·스이쿄텐만구앞', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', 'キャナルシティ博多前', '캐널시티하카타앞', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', 'キャナルシティ博多前（櫛田神社前駅）', '캐널시티하카타앞(구시다진자마에역)', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '駅前一丁目', '에키마에1초메', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '駅前三丁目', '에키마에3초메', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '駅前四丁目', '에키마에4초메', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '博多バスターミナル', '하카타버스터미널', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '博多駅筑紫口', '하카타역 지쿠시구치', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '博多駅センタービル前Ｅ', '하카타역 센터빌딩앞E', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '博多駅前Ａ', '하카타역앞A', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', 'テレＱ前', '테레Q앞', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '住吉', '스미요시', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '柳橋', '야나기바시', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '春吉', '하루요시', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '渡辺通一丁目', '와타나베도리1초메', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '渡辺通二丁目', '와타나베도리2초메', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '薬院駅前', '야쿠인역앞', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '天神北', '덴진키타', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '天神中央郵便局前', '덴진중앙우체국앞', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '天神日銀前', '덴진니치긴앞', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '天神四丁目', '덴진4초메', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '天神三丁目', '덴진3초메', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '天神協和ビル前', '덴진교와빌딩앞', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '天神ソラリアステージ前', '덴진솔라리아스테이지앞', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '天神ワンビル前', '덴진원빌딩앞', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '天神一丁目', '덴진1초메', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '天神大丸前', '덴진다이마루앞', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '天神南', '덴진미나미', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '天神警固神社・三越前', '덴진케고신사·미쓰코시앞', 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '西鉄天神高速バスターミナル', '니시테츠 덴진 고속버스터미널', 'https://www.nishitetsu.jp/bus/rosen/airportexpress/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_BUS', '福岡空港国際線', '후쿠오카공항 국제선', 'https://www.nishitetsu.jp/bus/rosen/akeito/', DATE '2026-11-18');

-- 표기 변형(전각·반각, 승차장 번호). 역 이름 정규화가 전각 영숫자를 반각으로 바꾸므로 반각 별칭도 둔다
INSERT INTO transit.station_alias (station_id, alias)
SELECT s.id, v.alias
FROM (VALUES
  ('博多駅前Ａ', '博多駅前A'),
  ('博多駅センタービル前Ｅ', '博多駅センタービル前E'),
  ('テレＱ前', 'テレQ前'),
  ('天神ワンビル前', '天神ワンビル前（旧天神ソラリア）'),
  ('天神大丸前', '天神大丸前（４）'),
  ('天神日銀前', '天神日銀前（１９）'),
  ('天神協和ビル前', '天神協和ビル前（１０）'),
  ('西鉄天神高速バスターミナル', '天神高速バスターミナル'),
  ('福岡空港国際線', '福岡空港国際線ターミナル')
) AS v(name_ja, alias)
JOIN transit.station s ON s.operator_code = 'NISHITETSU_BUS' AND s.name_ja = v.name_ja;

-- flat_zone에 이름 컬럼이 없어 운영사+금액으로 찾는다(현재 NISHITETSU_BUS 150엔 구역은 하나)
INSERT INTO transit.flat_zone (operator_code, amount, source_url, verified_on) VALUES
  ('NISHITETSU_BUS', 150, 'https://www.nishitetsu.jp/bus/rosen/150/', DATE '2026-11-18');

INSERT INTO transit.flat_zone_station (zone_id, station_id)
SELECT z.id, s.id
FROM transit.flat_zone z
JOIN transit.station s ON s.operator_code = 'NISHITETSU_BUS'
WHERE z.operator_code = 'NISHITETSU_BUS' AND z.amount = 150
  AND s.name_ja IN (
    '蔵本',
    '博多五町',
    '土居町',
    '呉服町',
    '奥の堂',
    '川端町・博多座前',
    '中洲',
    '東中洲',
    '中洲・南新地',
    '祇園町',
    'アクロス福岡・水鏡天満宮前',
    'キャナルシティ博多前',
    'キャナルシティ博多前（櫛田神社前駅）',
    '駅前一丁目',
    '駅前三丁目',
    '駅前四丁目',
    '博多バスターミナル',
    '博多駅筑紫口',
    '博多駅センタービル前Ｅ',
    '博多駅前Ａ',
    'テレＱ前',
    '住吉',
    '柳橋',
    '春吉',
    '渡辺通一丁目',
    '渡辺通二丁目',
    '薬院駅前',
    '天神北',
    '天神中央郵便局前',
    '天神日銀前',
    '天神四丁目',
    '天神三丁目',
    '天神協和ビル前',
    '天神ソラリアステージ前',
    '天神ワンビル前',
    '天神一丁目',
    '天神大丸前',
    '天神南',
    '天神警固神社・三越前'
  );

-- 공항 직행: 博多駅筑紫口 ⇄ 福岡空港国際線 IC 380 (현금 400) — https://www.nishitetsu.jp/bus/rosen/akeito/, https://www.nishitetsu.jp/bus/norikata/cashlessbus/
-- 특급 Airport Express: 西鉄天神高速BT ⇄ 福岡空港国際線 IC 480 (현금 500) — https://www.nishitetsu.jp/bus/rosen/airportexpress/
-- 博多駅 ⇄ 福岡空港国内線은 니시테츠 직통 노선 없음(jik 검색 "直通ルートは検索できませんでした")
INSERT INTO transit.pair_fare (operator_code, station_a_id, station_b_id, kind, amount, source_url, verified_on)
SELECT 'NISHITETSU_BUS', a.id, b.id, 'BASE', v.amount, v.url, DATE '2026-11-18'
FROM (VALUES
  ('博多駅筑紫口', '福岡空港国際線', 380, 'https://www.nishitetsu.jp/bus/rosen/akeito/'),
  ('西鉄天神高速バスターミナル', '福岡空港国際線', 480, 'https://www.nishitetsu.jp/bus/rosen/airportexpress/')
) AS v(a_name, b_name, amount, url)
JOIN transit.station a ON a.operator_code = 'NISHITETSU_BUS' AND a.name_ja = v.a_name
JOIN transit.station b ON b.operator_code = 'NISHITETSU_BUS' AND b.name_ja = v.b_name;

-- =====================================================================
-- 니시테츠 고속버스 히타호 (NISHITETSU_HIGHWAY_BUS) — 정류장 쌍 요금 (어른 편도)
-- https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/  운임표 이미지 61b1d8cf510253214ed63f65faab45d0.jpg
--   博多BT·西鉄天神高速BT·呉服町·東光二丁目은 운임표에서 한 칸(같은 요금). 이 네 곳끼리, 博多BT~福岡空港国内線은 발매 없음(===)
--   우키하(うきは) 시내 정류장은 없음 — 杷木 다음이 日田インター口
--   nimoca는 승차 시 자동 정산, 현금은 정리권. IC·현금 요금 차이 표기 없음
-- =====================================================================

INSERT INTO transit.operator_alias (operator_code, alias) VALUES
  ('NISHITETSU_HIGHWAY_BUS', '西鉄高速バス'),
  ('NISHITETSU_HIGHWAY_BUS', '西日本鉄道'),
  ('NISHITETSU_HIGHWAY_BUS', 'ひた号');

INSERT INTO transit.station (region_code, operator_code, name_ja, name_ko, source_url, verified_on) VALUES
  ('JP-FUKUOKA', 'NISHITETSU_HIGHWAY_BUS', '博多バスターミナル', '하카타버스터미널', 'https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_HIGHWAY_BUS', '西鉄天神高速バスターミナル', '니시테츠 덴진 고속버스터미널', 'https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_HIGHWAY_BUS', '呉服町', '고후쿠마치', 'https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_HIGHWAY_BUS', '東光二丁目', '도코2초메', 'https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_HIGHWAY_BUS', '福岡空港国内線', '후쿠오카공항 국내선', 'https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_HIGHWAY_BUS', '筑紫野', '지쿠시노', 'https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_HIGHWAY_BUS', '高速基山', '고속 기야마', 'https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_HIGHWAY_BUS', '高速小郡大板井', '고속 오고리오이타이', 'https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_HIGHWAY_BUS', '高速大刀洗', '고속 다치아라이', 'https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_HIGHWAY_BUS', '高速甘木', '고속 아마기', 'https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_HIGHWAY_BUS', '朝倉インター', '아사쿠라 인터', 'https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_HIGHWAY_BUS', '杷木', '하키', 'https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_HIGHWAY_BUS', '日田インター口', '히타 인터구치', 'https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_HIGHWAY_BUS', '昭和学園前', '쇼와가쿠엔마에', 'https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_HIGHWAY_BUS', '城内・豆田入口', '조나이·마메다이리구치', 'https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_HIGHWAY_BUS', '市役所前', '시야쿠쇼마에', 'https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_HIGHWAY_BUS', '田島一丁目', '다지마1초메', 'https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_HIGHWAY_BUS', '日田バスターミナル', '히타버스터미널', 'https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/', DATE '2026-11-18'),
  ('JP-FUKUOKA', 'NISHITETSU_HIGHWAY_BUS', '日田営業所', '히타영업소', 'https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/', DATE '2026-11-18');

INSERT INTO transit.station_alias (station_id, alias)
SELECT s.id, v.alias
FROM (VALUES
  ('博多バスターミナル', '博多BT'),
  ('西鉄天神高速バスターミナル', '天神高速バスターミナル'),
  ('筑紫野', '筑紫野(二日市温泉入口)'),
  ('朝倉インター', '朝倉IC'),
  ('日田バスターミナル', '日田BT')
) AS v(name_ja, alias)
JOIN transit.station s ON s.operator_code = 'NISHITETSU_HIGHWAY_BUS' AND s.name_ja = v.name_ja;

-- 161쌍
INSERT INTO transit.pair_fare (operator_code, station_a_id, station_b_id, kind, amount, source_url, verified_on)
SELECT 'NISHITETSU_HIGHWAY_BUS', a.id, b.id, 'BASE', v.amount, 'https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/', DATE '2026-11-18'
FROM (VALUES
  ('博多バスターミナル', '筑紫野', 600),
  ('博多バスターミナル', '高速基山', 770),
  ('博多バスターミナル', '高速小郡大板井', 1040),
  ('博多バスターミナル', '高速大刀洗', 1110),
  ('博多バスターミナル', '高速甘木', 1110),
  ('博多バスターミナル', '朝倉インター', 1360),
  ('博多バスターミナル', '杷木', 1470),
  ('博多バスターミナル', '日田インター口', 1980),
  ('博多バスターミナル', '昭和学園前', 1980),
  ('博多バスターミナル', '城内・豆田入口', 1980),
  ('博多バスターミナル', '市役所前', 1980),
  ('博多バスターミナル', '田島一丁目', 1980),
  ('博多バスターミナル', '日田バスターミナル', 1980),
  ('博多バスターミナル', '日田営業所', 1980),
  ('西鉄天神高速バスターミナル', '筑紫野', 600),
  ('西鉄天神高速バスターミナル', '高速基山', 770),
  ('西鉄天神高速バスターミナル', '高速小郡大板井', 1040),
  ('西鉄天神高速バスターミナル', '高速大刀洗', 1110),
  ('西鉄天神高速バスターミナル', '高速甘木', 1110),
  ('西鉄天神高速バスターミナル', '朝倉インター', 1360),
  ('西鉄天神高速バスターミナル', '杷木', 1470),
  ('西鉄天神高速バスターミナル', '日田インター口', 1980),
  ('西鉄天神高速バスターミナル', '昭和学園前', 1980),
  ('西鉄天神高速バスターミナル', '城内・豆田入口', 1980),
  ('西鉄天神高速バスターミナル', '市役所前', 1980),
  ('西鉄天神高速バスターミナル', '田島一丁目', 1980),
  ('西鉄天神高速バスターミナル', '日田バスターミナル', 1980),
  ('西鉄天神高速バスターミナル', '日田営業所', 1980),
  ('呉服町', '筑紫野', 600),
  ('呉服町', '高速基山', 770),
  ('呉服町', '高速小郡大板井', 1040),
  ('呉服町', '高速大刀洗', 1110),
  ('呉服町', '高速甘木', 1110),
  ('呉服町', '朝倉インター', 1360),
  ('呉服町', '杷木', 1470),
  ('呉服町', '日田インター口', 1980),
  ('呉服町', '昭和学園前', 1980),
  ('呉服町', '城内・豆田入口', 1980),
  ('呉服町', '市役所前', 1980),
  ('呉服町', '田島一丁目', 1980),
  ('呉服町', '日田バスターミナル', 1980),
  ('呉服町', '日田営業所', 1980),
  ('東光二丁目', '筑紫野', 600),
  ('東光二丁目', '高速基山', 770),
  ('東光二丁目', '高速小郡大板井', 1040),
  ('東光二丁目', '高速大刀洗', 1110),
  ('東光二丁目', '高速甘木', 1110),
  ('東光二丁目', '朝倉インター', 1360),
  ('東光二丁目', '杷木', 1470),
  ('東光二丁目', '日田インター口', 1980),
  ('東光二丁目', '昭和学園前', 1980),
  ('東光二丁目', '城内・豆田入口', 1980),
  ('東光二丁目', '市役所前', 1980),
  ('東光二丁目', '田島一丁目', 1980),
  ('東光二丁目', '日田バスターミナル', 1980),
  ('東光二丁目', '日田営業所', 1980),
  ('福岡空港国内線', '筑紫野', 560),
  ('福岡空港国内線', '高速基山', 770),
  ('福岡空港国内線', '高速小郡大板井', 1040),
  ('福岡空港国内線', '高速大刀洗', 1110),
  ('福岡空港国内線', '高速甘木', 1110),
  ('福岡空港国内線', '朝倉インター', 1360),
  ('福岡空港国内線', '杷木', 1470),
  ('福岡空港国内線', '日田インター口', 1980),
  ('福岡空港国内線', '昭和学園前', 1980),
  ('福岡空港国内線', '城内・豆田入口', 1980),
  ('福岡空港国内線', '市役所前', 1980),
  ('福岡空港国内線', '田島一丁目', 1980),
  ('福岡空港国内線', '日田バスターミナル', 1980),
  ('福岡空港国内線', '日田営業所', 1980),
  ('筑紫野', '高速基山', 310),
  ('筑紫野', '高速小郡大板井', 590),
  ('筑紫野', '高速大刀洗', 790),
  ('筑紫野', '高速甘木', 920),
  ('筑紫野', '朝倉インター', 1190),
  ('筑紫野', '杷木', 1410),
  ('筑紫野', '日田インター口', 1810),
  ('筑紫野', '昭和学園前', 1810),
  ('筑紫野', '城内・豆田入口', 1810),
  ('筑紫野', '市役所前', 1810),
  ('筑紫野', '田島一丁目', 1810),
  ('筑紫野', '日田バスターミナル', 1810),
  ('筑紫野', '日田営業所', 1810),
  ('高速基山', '高速小郡大板井', 370),
  ('高速基山', '高速大刀洗', 570),
  ('高速基山', '高速甘木', 700),
  ('高速基山', '朝倉インター', 980),
  ('高速基山', '杷木', 1240),
  ('高速基山', '日田インター口', 1640),
  ('高速基山', '昭和学園前', 1700),
  ('高速基山', '城内・豆田入口', 1700),
  ('高速基山', '市役所前', 1700),
  ('高速基山', '田島一丁目', 1700),
  ('高速基山', '日田バスターミナル', 1700),
  ('高速基山', '日田営業所', 1700),
  ('高速小郡大板井', '高速大刀洗', 300),
  ('高速小郡大板井', '高速甘木', 440),
  ('高速小郡大板井', '朝倉インター', 750),
  ('高速小郡大板井', '杷木', 1040),
  ('高速小郡大板井', '日田インター口', 1470),
  ('高速小郡大板井', '昭和学園前', 1470),
  ('高速小郡大板井', '城内・豆田入口', 1520),
  ('高速小郡大板井', '市役所前', 1520),
  ('高速小郡大板井', '田島一丁目', 1520),
  ('高速小郡大板井', '日田バスターミナル', 1520),
  ('高速小郡大板井', '日田営業所', 1520),
  ('高速大刀洗', '高速甘木', 230),
  ('高速大刀洗', '朝倉インター', 540),
  ('高速大刀洗', '杷木', 860),
  ('高速大刀洗', '日田インター口', 1310),
  ('高速大刀洗', '昭和学園前', 1360),
  ('高速大刀洗', '城内・豆田入口', 1360),
  ('高速大刀洗', '市役所前', 1360),
  ('高速大刀洗', '田島一丁目', 1360),
  ('高速大刀洗', '日田バスターミナル', 1360),
  ('高速大刀洗', '日田営業所', 1360),
  ('高速甘木', '朝倉インター', 410),
  ('高速甘木', '杷木', 730),
  ('高速甘木', '日田インター口', 1190),
  ('高速甘木', '昭和学園前', 1240),
  ('高速甘木', '城内・豆田入口', 1240),
  ('高速甘木', '市役所前', 1240),
  ('高速甘木', '田島一丁目', 1240),
  ('高速甘木', '日田バスターミナル', 1240),
  ('高速甘木', '日田営業所', 1240),
  ('朝倉インター', '杷木', 450),
  ('朝倉インター', '日田インター口', 940),
  ('朝倉インター', '昭和学園前', 980),
  ('朝倉インター', '城内・豆田入口', 1000),
  ('朝倉インター', '市役所前', 1000),
  ('朝倉インター', '田島一丁目', 1000),
  ('朝倉インター', '日田バスターミナル', 1000),
  ('朝倉インター', '日田営業所', 1000),
  ('杷木', '日田インター口', 650),
  ('杷木', '昭和学園前', 680),
  ('杷木', '城内・豆田入口', 680),
  ('杷木', '市役所前', 680),
  ('杷木', '田島一丁目', 680),
  ('杷木', '日田バスターミナル', 680),
  ('杷木', '日田営業所', 680),
  ('日田インター口', '昭和学園前', 150),
  ('日田インター口', '城内・豆田入口', 200),
  ('日田インター口', '市役所前', 200),
  ('日田インター口', '田島一丁目', 200),
  ('日田インター口', '日田バスターミナル', 200),
  ('日田インター口', '日田営業所', 200),
  ('昭和学園前', '城内・豆田入口', 150),
  ('昭和学園前', '市役所前', 200),
  ('昭和学園前', '田島一丁目', 200),
  ('昭和学園前', '日田バスターミナル', 200),
  ('昭和学園前', '日田営業所', 200),
  ('城内・豆田入口', '市役所前', 150),
  ('城内・豆田入口', '田島一丁目', 200),
  ('城内・豆田入口', '日田バスターミナル', 150),
  ('城内・豆田入口', '日田営業所', 200),
  ('市役所前', '田島一丁目', 150),
  ('市役所前', '日田バスターミナル', 150),
  ('市役所前', '日田営業所', 200),
  ('田島一丁目', '日田バスターミナル', 150),
  ('田島一丁目', '日田営業所', 200),
  ('日田バスターミナル', '日田営業所', 150)
) AS v(a_name, b_name, amount)
JOIN transit.station a ON a.operator_code = 'NISHITETSU_HIGHWAY_BUS' AND a.name_ja = v.a_name
JOIN transit.station b ON b.operator_code = 'NISHITETSU_HIGHWAY_BUS' AND b.name_ja = v.b_name;
