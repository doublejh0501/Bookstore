package com.example.Bookstore.config.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
public class AuthorInitializer implements ApplicationRunner {

    private final DataSource dataSource;

    @Value("classpath:db.seed/author_seed_insert.sql")
    private Resource authorSeed;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(authorSeed);
        populator.setSeparator(";");
        populator.setContinueOnError(true);
        populator.execute(dataSource);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM author")) {
            if (rs.next()) {
                log.info("[AuthorInitializer] author rows now: {}", rs.getInt(1));
            }
        } catch (Exception e) {
            log.warn("[AuthorInitializer] counting authors failed", e);
        }
    }
}
