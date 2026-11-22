-- JpaFareDataTest 전용 가상 데이터(실제 요금 아님). 지역 TEST, id 1~99.
insert into transit.region (code, name_ko, name_ja, currency, timezone) values ('TEST', '테스트', 'テスト', 'JPY', 'Asia/Tokyo');
insert into transit.operator (code, region_code, name_ja, name_ko, fare_rule_type, fare_basis_note) values
('TEST_DIST', 'TEST', '距離線', '거리제', 'DISTANCE_BAND', null),
('TEST_PAIR', 'TEST', '区間線', '쌍요금', 'STATION_PAIR', null),
('TEST_BUS',  'TEST', '均一バス', '균일버스', 'FLAT_ZONE_PLUS_PAIR', null);
insert into transit.line (id, operator_code, code, name_ja, name_ko) values (1, 'TEST_DIST', 'L1', '一号線', '1호선');
insert into transit.station (id, region_code, operator_code, name_ja, name_ko, source_url, verified_on) values
(1,  'TEST', 'TEST_DIST', 'A駅', 'A역', 'https://example.test', date '2026-11-18'),
(2,  'TEST', 'TEST_DIST', 'B駅', 'B역', 'https://example.test', date '2026-11-18'),
(11, 'TEST', 'TEST_PAIR', 'P1', 'P1', 'https://example.test', date '2026-11-18'),
(12, 'TEST', 'TEST_PAIR', 'P2', 'P2', 'https://example.test', date '2026-11-18'),
(21, 'TEST', 'TEST_BUS',  'Z1', 'Z1', 'https://example.test', date '2026-11-18'),
(22, 'TEST', 'TEST_BUS',  'Z2', 'Z2', 'https://example.test', date '2026-11-18'),
(23, 'TEST', 'TEST_BUS',  'Z3', 'Z3', 'https://example.test', date '2026-11-18');
insert into transit.line_station (line_id, station_id, cumulative_km) values (1, 1, 0.0), (1, 2, 2.0);
insert into transit.distance_fare_band (operator_code, kind, max_km, amount, source_url, verified_on) values
('TEST_DIST', 'BASE', 3.0, 210, 'https://example.test', date '2026-11-18'),
('TEST_DIST', 'BASE', 7.0, 260, 'https://example.test', date '2026-11-18');
insert into transit.pair_fare (operator_code, station_a_id, station_b_id, kind, amount, source_url, verified_on) values
('TEST_PAIR', 11, 12, 'BASE', 500, 'https://example.test', date '2026-11-18');
insert into transit.flat_zone (id, operator_code, amount, source_url, verified_on) values (1, 'TEST_BUS', 150, 'https://example.test', date '2026-11-18');
insert into transit.flat_zone_station (zone_id, station_id) values (1, 21), (1, 22);
insert into transit.reserved_seat_fee (operator_code, amount, source_url, verified_on) values ('TEST_DIST', 530, 'https://example.test', date '2026-11-18');
insert into transit.transfer_discount (from_operator_code, to_operator_code, discount, note, source_url, verified_on) values
('TEST_DIST', 'TEST_BUS', 30, null, 'https://example.test', date '2026-11-18');
