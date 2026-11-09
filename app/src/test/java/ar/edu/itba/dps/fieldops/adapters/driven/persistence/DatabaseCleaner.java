package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import javax.sql.DataSource;
import java.sql.SQLException;

class DatabaseCleaner {
    private final DataSource dataSource;

    DatabaseCleaner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    void clean() {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("TRUNCATE zone, resource, expedition, replan_proposal, domain_event, event_delivery, notification, expedition_progress RESTART IDENTITY CASCADE");
        } catch (SQLException failure) {
            throw new IllegalStateException("the database could not be cleaned", failure);
        }
    }
}
