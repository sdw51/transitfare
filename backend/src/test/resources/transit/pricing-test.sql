-- PriceLegsUseCaseTest·PricingControllerTest 전용 가상 데이터(실제 요금 아님). id 1~99.
insert into transit.region (code, name_ko, name_ja, currency, timezone) values
('TEST',  '테스트',  'テスト',  'JPY', 'Asia/Tokyo'),
('TEST2', '테스트2', 'テスト2', 'JPY', 'Asia/Tokyo');
insert into transit.operator (code, region_code, name_ja, name_ko, fare_rule_type, fare_basis_note) values
('TEST_SUBWAY', 'TEST',  'テスト地下鉄', '테스트 지하철', 'DISTANCE_BAND', null),
('TEST_OTHER',  'TEST',  'その他',       '기타',          'MANUAL_ONLY',   null),
('TEST2_BUS',   'TEST2', 'テスト2バス',  '테스트2 버스',  'STATION_PAIR',  null);
insert into transit.line (id, operator_code, code, name_ja, name_ko) values (1, 'TEST_SUBWAY', 'L1', '一号線', '1호선');
insert into transit.station (id, region_code, operator_code, name_ja, name_ko, source_url, verified_on) values
(1, 'TEST', 'TEST_SUBWAY', 'A', 'A', 'https://example.test', date '2026-11-19'),
(2, 'TEST', 'TEST_SUBWAY', 'B', 'B', 'https://example.test', date '2026-11-19'),
(9, 'TEST', 'TEST_SUBWAY', 'Z', 'Z', 'https://example.test', date '2026-11-19');
insert into transit.line_station (line_id, station_id, cumulative_km) values (1, 1, 0.0), (1, 2, 2.0);
insert into transit.distance_fare_band (operator_code, kind, max_km, amount, source_url, verified_on) values
('TEST_SUBWAY', 'BASE', 3.0, 210, 'https://example.test', date '2026-11-19'),
('TEST_SUBWAY', 'BASE', 7.0, 260, 'https://example.test', date '2026-11-19');
