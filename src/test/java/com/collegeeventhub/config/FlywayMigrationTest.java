package com.collegeeventhub.config;

import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayMigrationTest {
    @Test
    void baselineMigrationContainsApplicationSchema() throws IOException {
        String baseline = new ClassPathResource(
                "db/migration/V1__baseline_schema.sql"
        ).getContentAsString(StandardCharsets.UTF_8);

        assertTrue(baseline.contains("create table users"));
        assertTrue(baseline.contains("create table organizations"));
        assertTrue(baseline.contains("create table clubs"));
        assertTrue(baseline.contains("create table events"));
        assertTrue(baseline.contains("create table registrations"));
        assertTrue(baseline.contains("create table placement_applications"));
        assertTrue(baseline.contains("create table admin_audit_logs"));
    }

    @Test
    void auditMigrationPreservesPlacementConstraintValues() throws IOException {
        String auditMigration = new ClassPathResource(
                "db/migration/V2__audit_action_constraints.sql"
        ).getContentAsString(StandardCharsets.UTF_8);

        assertTrue(auditMigration.contains("'PLACEMENT_CREATED'"));
        assertTrue(auditMigration.contains("'PLACEMENT_UPDATED'"));
        assertTrue(auditMigration.contains("'PLACEMENT_PUBLISHED'"));
        assertTrue(auditMigration.contains("'PLACEMENT_CLOSED'"));
        assertTrue(auditMigration.contains("'PLACEMENT'"));
    }
}
