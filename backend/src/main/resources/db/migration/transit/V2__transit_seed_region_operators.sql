-- 지역과 운영사. fare_basis_note 근거(2026-11-18 확인):
--  지하철: IC는 대인 보통요금을 차감 — https://www.city.fukuoka.lg.jp/d1w_reiki/reiki_honbun/q003RG00000934.html
--  JR큐슈: SUGOCA는 보통여객운임 상당액 차감 — https://www.jrkyushu.co.jp/sugoca/use/fare/case_2/
--  모노레일: IC는 대인 편도 보통운임 차감 — https://www.kitakyushu-monorail.co.jp/pdf/iccard_kisoku.pdf
--  니시테츠 버스: 캐시리스 할인은 공항버스·다자이후 라이너만 — https://www.nishitetsu.jp/bus/norikata/cashlessbus/
--                 nimoca 환승 할인 — https://www.nishitetsu.jp/bus/norikata/noriwari/
--  니시테츠 전철·고속버스·쇼와 버스: IC와 현금 요금이 같은지 공식 자료로 확인 못 함 → 현금 기준이라고만 적음
--  쇼와 버스: 2026-10-01 요금 개정 — https://www.showa-bus.jp/2026/09/10/kaitei2026/

insert into region (code, name_ko, name_ja, currency, timezone)
values ('JP-FUKUOKA', '후쿠오카', '福岡', 'JPY', 'Asia/Tokyo');

insert into operator (code, region_code, name_ja, name_ko, fare_rule_type, fare_basis_note) values
('FUKUOKA_SUBWAY',         'JP-FUKUOKA', '福岡市地下鉄',     '후쿠오카 시영 지하철', 'DISTANCE_BAND',       '현금(종이 승차권) 요금 기준. IC카드도 같은 요금'),
('JR_KYUSHU',              'JP-FUKUOKA', 'JR九州',           'JR 큐슈',              'DISTANCE_BAND',       '현금(종이 승차권) 요금 기준. SUGOCA도 같은 운임'),
('NISHITETSU_RAIL',        'JP-FUKUOKA', '西鉄電車',         '니시테츠 전철',        'STATION_PAIR',        '현금(종이 승차권) 요금 기준'),
('NISHITETSU_BUS',         'JP-FUKUOKA', '西鉄バス',         '니시테츠 버스',        'FLAT_ZONE_PLUS_PAIR', '현금 요금 기준. 일부 노선의 IC·카드 할인과 nimoca 환승 할인은 반영하지 않음'),
('NISHITETSU_HIGHWAY_BUS', 'JP-FUKUOKA', '西鉄高速バス',     '니시테츠 고속버스',    'STATION_PAIR',        '현금 요금 기준'),
('SHOWA_BUS',              'JP-FUKUOKA', '昭和バス',         '쇼와 버스',            'STATION_PAIR',        '현금 요금 기준(2026-10-01 개정 요금)'),
('KITAKYUSHU_MONORAIL',    'JP-FUKUOKA', '北九州モノレール', '기타큐슈 모노레일',    'STATION_PAIR',        '현금(종이 승차권) 요금 기준. IC카드도 같은 요금'),
('OTHER',                  'JP-FUKUOKA', 'その他',           '기타',                 'MANUAL_ONLY',         '사용자가 직접 입력한 요금');
