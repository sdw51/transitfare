package com.transitfare.common;

import java.util.List;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.beans.factory.InitializingBean;

/** 모듈마다 자기 스키마·자기 이력 테이블(flyway_schema_history)로 마이그레이션한다. */
@Slf4j
@RequiredArgsConstructor
public class ModuleFlywayMigrator implements InitializingBean {

    private final DataSource dataSource;
    private final List<String> modules;

    @Override
    public void afterPropertiesSet() {
        for (String module : modules) {
            MigrateResult result = Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(module)
                    .createSchemas(true)
                    .locations("classpath:db/migration/" + module)
                    .table("flyway_schema_history")
                    .load()
                    .migrate();
            log.info("Flyway [{}] 마이그레이션 {}개 적용", module, result.migrationsExecuted);
        }
    }
}
