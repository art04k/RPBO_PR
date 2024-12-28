package org.example.rpbo.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.rpbo.configuration.JwtTokenProvider;
import org.example.rpbo.model.*;
import org.example.rpbo.repository.*;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LicensingCreateService {

    private static final Logger logger = LoggerFactory.getLogger(LicensingCreateService.class);

    private final ProductRepository productRepository;
    private final ApplicationUserRepository applicationUserRepository;
    private final LicenseTypeRepository licenseTypeRepository;
    private final LicenseRepository licenseRepository;
    private final LicenseHistoryService licenseHistoryService;
    private final JwtTokenProvider jwtTokenProvider;

    public String extractEmailFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtTokenProvider.getUsername(token);
    }

    public void createLicense(LicenseCreateRequest requestData, String userEmail) {
        Product product = getEntityOrThrow(productRepository.findById(requestData.getProductId()), "Продукт не найден");
        ApplicationUser owner = getEntityOrThrow(applicationUserRepository.findById(requestData.getOwnerId()), "Пользователь не найден");
        LicenseType licenseType = getEntityOrThrow(licenseTypeRepository.findById(requestData.getLicenseTypeId()), "Тип лицензии не найден");

        if (product.isBlocked()) {
            logger.error("Продукт {} заблокирован. Лицензия не может быть создана.", product.getName());
            throw new IllegalArgumentException("Продукт заблокирован, лицензия не может быть создана");
        }

        License newLicense = new License();
        newLicense.setCode(generateActivationCode());
        newLicense.setOwner(owner);
        newLicense.setProduct(product);
        newLicense.setLicenseType(licenseType);
        newLicense.setBlocked(false);
        newLicense.setDeviceCount(requestData.getDeviceCount());
        newLicense.setDuration(licenseType.getDefaultDuration());
        newLicense.setDescription(requestData.getDescription() != null ? requestData.getDescription() : "Приятного пользования нашими продуктами!");

        licenseRepository.save(newLicense);
        logger.info("Лицензия для продукта {} успешно создана.", product.getName());
        recordLicenseHistory(newLicense, owner);
    }

    private <T> T getEntityOrThrow(Optional<T> entityOptional, String errorMessage) {
        return entityOptional.orElseThrow(() -> new IllegalArgumentException(errorMessage));
    }

    private Date convertLocalDateToDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private String generateActivationCode() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private void recordLicenseHistory(License newLicense, ApplicationUser owner) {
        String description = "Лицензия создана";
        Date changeDate = convertLocalDateToDate(LocalDate.now());
        licenseHistoryService.recordLicenseChange(newLicense.getId(), owner.getId(), "Создана", changeDate, description);
        logger.info("История изменений для лицензии {} записана.", newLicense.getCode());
    }
}
