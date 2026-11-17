package com.transitfare.common;

import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 모듈별 Flyway를 실행하고, JPA가 마이그레이션 이후에 뜨게 한다. */
@Configuration
public class ModuleFlywayConfig {

    @Bean
    public ModuleFlywayMigrator moduleFlywayMigrator(DataSource dataSource,
                                                     @Value("${app.modules}") List<String> modules) {
        return new ModuleFlywayMigrator(dataSource, modules);
    }

    @Bean
    public static EntityManagerFactoryDependsOnPostProcessor entityManagerFactoryDependsOnModuleFlyway() {
        return new EntityManagerFactoryDependsOnPostProcessor(ModuleFlywayMigrator.class);
    }
}
