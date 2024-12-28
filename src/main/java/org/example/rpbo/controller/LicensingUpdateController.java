package org.example.rpbo.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.rpbo.configuration.JwtTokenProvider;
import org.example.rpbo.model.*;
import org.example.rpbo.repository.*;
import org.example.rpbo.service.impl.LicensingUpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@RestController
@RequestMapping("/licensing")
@RequiredArgsConstructor
public class LicensingUpdateController {

    private final JwtTokenProvider jwtTokenProvider;
    private final LicenseRepository licenseRepository;
    private final DeviceLicenseRepository deviceLicenseRepository;
    private final LicensingUpdateService licensingUpdateService;

    private static final Logger logger = LoggerFactory.getLogger(LicensingUpdateController.class);

    @PostMapping("/update")
    public ResponseEntity<?> updateLicense(HttpServletRequest request, @RequestBody LicenseUpdateRequest requestData) {
        try {
            // Получение ролей пользователя из токена
            Set<String> roles = jwtTokenProvider.getRolesFromRequest(request);
            if (roles.isEmpty()) {
                logger.warn("Ошибка аутентификации: отсутствуют роли пользователя.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Ошибка аутентификации");
            }

            logger.info("Роли пользователя: {}", roles);

            // Валидация лицензии
            License license = licensingUpdateService.validateLicense(requestData);
            logger.info("Лицензия успешно найдена: {}", license.getCode());

            // Парсинг и проверка новой даты окончания лицензии
            LocalDateTime newExpirationDate = licensingUpdateService.parseExpirationDate(requestData.getNewExpirationDate());
            licensingUpdateService.verifyExpirationDate(license, newExpirationDate);
            logger.info("Новая дата окончания лицензии: {}", newExpirationDate);

            // Вычисление нового срока действия лицензии
            int newDuration = calculateDaysBetween(newExpirationDate);
            logger.info("Новый срок действия лицензии: {} дней", newDuration);

            // Продление лицензии
            licensingUpdateService.extendLicense(license, newExpirationDate, newDuration);
            logger.info("Лицензия продлена до {}", newExpirationDate);

            // Создание тикета с подтверждением
            String secretKey = "your-secret-key"; // Секретный ключ для подписи тикета (замените на ваш ключ)
            Ticket ticket = licensingUpdateService.createConfirmationTicket(license, newExpirationDate, newDuration, secretKey);

            // Подготовка сообщения для устройства
            String deviceMessage = licensingUpdateService.prepareDeviceMessage(license, ticket);
            logger.info("Сообщение для устройства: {}", deviceMessage);

            return ResponseEntity.ok(ticket);

        } catch (DateTimeParseException e) {
            logger.error("Ошибка при парсинге даты: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Неверный формат даты и времени.");
        } catch (IllegalArgumentException e) {
            logger.error("Ошибка: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Внутренняя ошибка: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Произошла ошибка при продлении лицензии.");
        }
    }

    public static int calculateDaysBetween(LocalDateTime expirationDate) {
        LocalDateTime currentDate = LocalDateTime.now();
        return (int) ChronoUnit.DAYS.between(currentDate, expirationDate);
    }
}
