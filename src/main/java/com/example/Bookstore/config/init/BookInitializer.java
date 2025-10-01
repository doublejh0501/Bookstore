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
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class BookInitializer implements ApplicationRunner {

    private final DataSource dataSource;

    @Value("classpath:db.seed/book_seed_insert.sql")
    private Resource bookSeed;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(bookSeed);
        populator.setSeparator(";");
        populator.setContinueOnError(true);
        populator.execute(dataSource);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM book")) {
            if (rs.next()) {
                log.info("[BookInitializer] book rows now: {}", rs.getInt(1));
            }
        } catch (Exception e) {
            log.warn("[BookInitializer] counting book rows failed", e);
        }
    }
}
