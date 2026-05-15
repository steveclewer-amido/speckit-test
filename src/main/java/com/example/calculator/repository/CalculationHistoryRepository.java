package com.example.calculator.repository;

import com.example.calculator.model.CalculationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data JPA repository for calculation history. */
public interface CalculationHistoryRepository extends JpaRepository<CalculationHistory, Long> {

    List<CalculationHistory> findTop20ByUsernameOrderByCalculatedAtDesc(String username);

    void deleteAllByUsername(String username);
}
