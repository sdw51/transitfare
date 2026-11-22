-- ImportGtfsFaresUseCaseTest·GtfsImportControllerTest 전용 가상 데이터.
insert into transit.region (code, name_ko, name_ja, currency, timezone) values ('TEST', '테스트', 'テスト', 'JPY', 'Asia/Tokyo');
insert into transit.operator (code, region_code, name_ja, name_ko, fare_rule_type, fare_basis_note) values
('TEST_BUS', 'TEST', 'テストバス', '테스트 버스', 'STATION_PAIR', null);
