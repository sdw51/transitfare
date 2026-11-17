package com.transitfare.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** 실제 모듈 마이그레이션과 섞이지 않게 별도 DB와 테스트 전용 모듈을 쓴다. */
class ModuleFlywayMigratorTest {

    @Test
    @DisplayName("모듈마다 자기 스키마와 이력 테이블을 만들고, 마이그레이션이 없는 모듈도 스키마는 만든다")
    void createsSchemaAndHistoryPerModule() {
        DataSource ds = new DriverManagerDataSource(
                "jdbc:h2:mem:flyway-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");

        new ModuleFlywayMigrator(ds, List.of("flywaytest", "emptymodule")).afterPropertiesSet();

        JdbcTemplate jdbc = new JdbcTemplate(ds);
        assertThat(jdbc.queryForObject(
                "select count(*) from flywaytest.flyway_schema_history where version = '1' and success", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from flywaytest.sample", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.schemata where schema_name = 'emptymodule'", Integer.class))
                .isEqualTo(1);
    }
}
