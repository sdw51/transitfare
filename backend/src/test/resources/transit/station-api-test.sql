-- StationControllerTest 전용 가상 데이터. 지역 TEST, id 1~99.
insert into transit.region (code, name_ko, name_ja, currency, timezone) values ('TEST', '테스트', 'テスト', 'JPY', 'Asia/Tokyo');
insert into transit.operator (code, region_code, name_ja, name_ko, fare_rule_type, fare_basis_note) values
('TEST_SUBWAY', 'TEST', 'テスト地下鉄', '테스트 지하철', 'DISTANCE_BAND', '현금 요금 기준'),
('TEST_BUS',    'TEST', 'テストバス',   '테스트 버스',   'STATION_PAIR',  null);
insert into transit.line (id, operator_code, code, name_ja, name_ko) values (1, 'TEST_SUBWAY', 'AIRPORT', '空港線', '공항선');
insert into transit.station (id, region_code, operator_code, name_ja, name_ko, source_url, verified_on) values
(1, 'TEST', 'TEST_SUBWAY', '博多',   '하카타',     'https://example.test', date '2026-11-19'),
(2, 'TEST', 'TEST_SUBWAY', '天神',   '텐진',       'https://example.test', date '2026-11-19'),
(3, 'TEST', 'TEST_SUBWAY', '天神南', '텐진미나미', 'https://example.test', date '2026-11-19'),
(4, 'TEST', 'TEST_BUS',    '天神',   '텐진',       'https://example.test', date '2026-11-19');
insert into transit.line_station (line_id, station_id, cumulative_km) values (1, 1, 0.0), (1, 2, 2.6);
insert into transit.station_alias (station_id, alias) values (1, 'Hakata');
