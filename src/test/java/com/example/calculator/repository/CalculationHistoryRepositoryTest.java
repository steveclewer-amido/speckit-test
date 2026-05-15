package com.example.calculator.repository;

import com.example.calculator.model.CalculationHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CalculationHistoryRepositoryTest {

    @Autowired
    private CalculationHistoryRepository repository;

    // Helper — explicit calculatedAt for deterministic ordering (Constitution Principle II)
    private CalculationHistory entry(String username, double a, double b,
                                     String op, double result, Instant at) {
        CalculationHistory h = new CalculationHistory();
        h.setUsername(username);
        h.setOperandA(a);
        h.setOperandB(b);
        h.setOperation(op);
        h.setResult(result);
        h.setCalculatedAt(at);
        return h;
    }

    @Test
    void savesAndRetrievesEntry() {
        Instant t = Instant.parse("2026-01-01T00:00:00Z");
        repository.save(entry("alice", 2, 3, "ADD", 5, t));

        List<CalculationHistory> results =
                repository.findTop20ByUsernameOrderByCalculatedAtDesc("alice");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getResult()).isEqualTo(5.0);
    }

    @Test
    void returnsNewestFirst() {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        repository.save(entry("alice", 1, 2, "ADD",      3,  base));
        repository.save(entry("alice", 5, 5, "MULTIPLY", 25, base.plusSeconds(1)));
        repository.save(entry("alice", 10, 2, "SUBTRACT", 8, base.plusSeconds(2)));

        List<CalculationHistory> results =
                repository.findTop20ByUsernameOrderByCalculatedAtDesc("alice");

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getResult()).isEqualTo(8.0);  // most recent
        assertThat(results.get(2).getResult()).isEqualTo(3.0);  // oldest
    }

    @Test
    void filtersEntriesByUsername() {
        Instant t = Instant.parse("2026-01-01T00:00:00Z");
        repository.save(entry("alice", 1, 1, "ADD",    2, t));
        repository.save(entry("bob",   9, 3, "DIVIDE", 3, t.plusSeconds(1)));

        assertThat(repository.findTop20ByUsernameOrderByCalculatedAtDesc("alice")).hasSize(1);
        assertThat(repository.findTop20ByUsernameOrderByCalculatedAtDesc("bob")).hasSize(1);
        assertThat(repository.findTop20ByUsernameOrderByCalculatedAtDesc("charlie")).isEmpty();
    }

    @Test
    void limitsResultsToTop20() {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        for (int i = 0; i < 25; i++) {
            repository.save(entry("alice", i, 1, "ADD", i + 1, base.plusSeconds(i)));
        }

        List<CalculationHistory> results =
                repository.findTop20ByUsernameOrderByCalculatedAtDesc("alice");

        assertThat(results).hasSize(20);
        // most recent saved: i=24 → result=25.0
        assertThat(results.get(0).getResult()).isEqualTo(25.0);
        // 20th entry back: i=5 → result=6.0
        assertThat(results.get(19).getResult()).isEqualTo(6.0);
    }

    @Test
    void deleteAllByUsernameRemovesOnlyThatUsersEntries() {
        Instant t = Instant.parse("2026-01-01T00:00:00Z");
        repository.save(entry("alice", 1, 1, "ADD", 2, t));
        repository.save(entry("alice", 2, 2, "ADD", 4, t.plusSeconds(1)));
        repository.save(entry("bob",   9, 3, "DIVIDE", 3, t));

        repository.deleteAllByUsername("alice");

        assertThat(repository.findTop20ByUsernameOrderByCalculatedAtDesc("alice")).isEmpty();
        assertThat(repository.findTop20ByUsernameOrderByCalculatedAtDesc("bob")).hasSize(1);
    }
}
