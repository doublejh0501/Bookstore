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
public class SearchKeywordInitializer implements ApplicationRunner {
    private final DataSource dataSource;

    @Value("classpath:db.seed/searchKeywordStats_seed_insert.sql")
    private Resource searchKeywordSeed;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long existing = countRows();
        if (existing > 0) {
            log.info("[SearchKeywordInitializer] skip seeding – existing rows: {}", existing);
            return;
        }

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(searchKeywordSeed);
        populator.setSeparator(";");
        populator.setContinueOnError(false);
        populator.execute(dataSource);

        long after = countRows();
        log.info("[SearchKeywordInitializer] seeded search keywords. rows now: {}", after);
    }

    private long countRows() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM search_keyword_stats")) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (Exception e) {
            log.warn("[SearchKeywordInitializer] counting search keywords failed", e);
            return 0L;
        }
    }
}
