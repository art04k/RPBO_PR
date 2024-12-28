package org.example.rpbo.repository;

import org.example.rpbo.model.LicenseHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LicenseHistoryRepository extends JpaRepository<LicenseHistory, Long> {
    // Дополнительные методы, если нужно
}
