package com.shigoto.backend.entity;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GithubDataPersistenceMappingTest {
    @Test
    void mapsOneReusableVersionedRecordPerCandidate() throws Exception {
        Table table = GithubData.class.getAnnotation(Table.class);

        assertTrue(Arrays.stream(table.uniqueConstraints()).anyMatch(constraint ->
                "uk_github_data_candidate".equals(constraint.name())
                        && Arrays.equals(new String[]{"candidate_id"}, constraint.columnNames())));
        assertNotNull(GithubData.class.getDeclaredField("version").getAnnotation(Version.class));
        assertNotNull(GithubData.class.getDeclaredField("candidate").getAnnotation(OneToOne.class));
        assertNotNull(GithubData.class.getDeclaredField("topLanguages").getAnnotation(ElementCollection.class));
    }
}
