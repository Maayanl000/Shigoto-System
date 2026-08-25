package com.shigoto.backend.entity;

import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationUniqueConstraintTest {

    @Test
    void databaseMappingDeclaresCandidateJobUniqueConstraint() {
        Table table = Application.class.getAnnotation(Table.class);

        assertTrue(Arrays.stream(table.uniqueConstraints()).anyMatch(constraint ->
                "uk_application_candidate_job".equals(constraint.name())
                        && Set.of(constraint.columnNames()).equals(Set.of("candidate_id", "job_id"))));
    }
}
