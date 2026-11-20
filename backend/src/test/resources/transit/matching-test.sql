-- MatchStepUseCaseTest 전용 가상 데이터. id 1~99.
insert into transit.region (code, name_ko, name_ja, currency, timezone) values ('TEST', '테스트', 'テスト', 'JPY', 'Asia/Tokyo');
insert into transit.operator (code, region_code, name_ja, name_ko, fare_rule_type, fare_basis_note) values
('TEST_JR',   'TEST', 'JR九州',   'JR',   'DISTANCE_BAND', null),
('TEST_RAIL', 'TEST', '西鉄電車', '전철', 'STATION_PAIR',  null),
('TEST_BUS',  'TEST', '西鉄バス', '버스', 'STATION_PAIR',  null);
insert into transit.operator_alias (operator_code, alias) values
('TEST_JR', 'JR九州'), ('TEST_RAIL', '西日本鉄道'), ('TEST_BUS', '西日本鉄道');
insert into transit.line (id, operator_code, code, name_ja, name_ko) values (1, 'TEST_JR', 'KAGOSHIMA', '鹿児島本線', '가고시마 본선');
insert into transit.station (id, region_code, operator_code, name_ja, name_ko, source_url, verified_on) values
(1,  'TEST', 'TEST_JR',   '博多',           '하카타',     'https://example.test', date '2026-11-19'),
(2,  'TEST', 'TEST_JR',   '二日市',         '후쓰카이치', 'https://example.test', date '2026-11-19'),
(11, 'TEST', 'TEST_RAIL', '天神',           '텐진',       'https://example.test', date '2026-11-19'),
(12, 'TEST', 'TEST_RAIL', '大橋',           '오하시',     'https://example.test', date '2026-11-19'),
(21, 'TEST', 'TEST_BUS',  '天神',           '텐진',       'https://example.test', date '2026-11-19'),
(22, 'TEST', 'TEST_BUS',  'キャナルシティ', '캐널시티',   'https://example.test', date '2026-11-19');
insert into transit.line_station (line_id, station_id, cumulative_km) values (1, 1, 0.0), (1, 2, 16.6);
insert into transit.station_alias (station_id, alias) values (2, 'ふつかいち');
