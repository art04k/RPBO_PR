package org.example.rpbo.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.rpbo.model.LicenseHistory;
import org.example.rpbo.repository.LicenseHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class LicenseHistoryService {

    private final LicenseHistoryRepository licenseHistoryRepository;

    // Метод для записи изменений в истории лицензий
    public void recordLicenseChange(Long licenseId, Long userId, String status, Date changeDate, String description) {
        LicenseHistory history = new LicenseHistory();
        history.setLicenseId(licenseId);
        history.setUserId(userId);
        history.setStatus(status);
        history.setChangeDate(changeDate);
        history.setDescription(description);

        // Сохраняем запись в базу данных
        licenseHistoryRepository.save(history);
    }
}
