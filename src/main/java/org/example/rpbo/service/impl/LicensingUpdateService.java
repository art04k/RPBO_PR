package org.example.rpbo.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.rpbo.model.DeviceLicense;
import org.example.rpbo.model.License;
import org.example.rpbo.model.LicenseUpdateRequest;
import org.example.rpbo.model.Ticket;
import org.example.rpbo.repository.DeviceLicenseRepository;
import org.example.rpbo.repository.LicenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LicensingUpdateService {

    private final LicenseRepository licenseRepository;
    private final DeviceLicenseRepository deviceLicenseRepository;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public License validateLicense(LicenseUpdateRequest requestData) {
        return licenseRepository.findByCode(requestData.getCode())
                .orElseThrow(() -> new IllegalArgumentException("Недействительный ключ лицензии"));
    }

    public LocalDateTime parseExpirationDate(String newExpirationDateStr) {
        try {
            // Парсим строку в формат "yyyy-MM-dd'T'HH:mm:ss"
            return LocalDateTime.parse(newExpirationDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            throw new IllegalArgumentException("Неверный формат даты и времени. Ожидаемый формат: yyyy-MM-dd'T'HH:mm:ss");
        }
    }

    public void verifyExpirationDate(License license, LocalDateTime newExpirationDate) {
        if (license.getBlocked()) {
            throw new IllegalArgumentException("Лицензия заблокирована, продление невозможно.");
        }

        if (license.getEndingDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Лицензия истекла, продление невозможно.");
        }

        if (!newExpirationDate.isAfter(license.getEndingDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())) {
            throw new IllegalArgumentException("Новая дата окончания не может быть меньше или равна текущей.");
        }
    }

    public void extendLicense(License license, LocalDateTime newExpirationDate, int newDuration) {
        license.setEndingDate(java.sql.Timestamp.valueOf(newExpirationDate));
        license.setDuration(newDuration);
        licenseRepository.save(license);
        logger.info("Лицензия с кодом {} продлена до: {}", license.getCode(), newExpirationDate);
    }

    public Ticket createConfirmationTicket(License license, LocalDateTime newExpirationDate, int newDuration, String secretKey) {
        Optional<DeviceLicense> deviceLicenseOpt = deviceLicenseRepository.findByLicenseId(license.getId());
        Long deviceId = deviceLicenseOpt.map(DeviceLicense::getDeviceId).orElse(null);

        // Создаем тикет с использованием обновленного метода createTicket
        Ticket ticket = Ticket.createTicket(
                license.getOwner().getId(), // User ID
                false,                      // isBlocked
                java.sql.Timestamp.valueOf(newExpirationDate), // Expiration date
                deviceId,                   // Device ID
                secretKey                   // Secret key for signature
        );

        // Устанавливаем дополнительные параметры
        ticket.setTicketLifetime(newDuration);

        deviceLicenseOpt.ifPresent(deviceLicense -> {
            ticket.setActivationDate(deviceLicense.getActivationDate());
        });

        logger.info("Создан тикет: {}", ticket);
        return ticket;
    }


    public String prepareDeviceMessage(License license, Ticket ticket) {
        Optional<DeviceLicense> deviceLicenseOpt = deviceLicenseRepository.findByLicenseId(license.getId());
        if (deviceLicenseOpt.isPresent()) {
            return "Лицензия активирована на устройстве\nЛицензия продлена до: " + ticket.getExpirationDate();
        }
        return "Лицензия не активирована на устройстве\nЛицензия продлена до: " + ticket.getExpirationDate();
    }
}
