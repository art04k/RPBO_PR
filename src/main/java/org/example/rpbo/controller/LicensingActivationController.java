package org.example.rpbo.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.rpbo.configuration.JwtTokenProvider;
import org.example.rpbo.model.*;
import org.example.rpbo.repository.*;
import org.example.rpbo.service.impl.LicensingActivationService;
import org.example.rpbo.service.impl.LicenseHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping("/licensing")
@RequiredArgsConstructor
public class LicensingActivationController {

    private final JwtTokenProvider jwtTokenProvider;
    private final LicensingActivationService licensingActivationService;
    private final LicenseRepository licenseRepository;
    private final ApplicationUserRepository applicationUserRepository;
    private final LicenseHistoryService licenseHistoryService;
    private static final Logger log = LoggerFactory.getLogger(LicensingActivationController.class);

    private static final Logger logger = LoggerFactory.getLogger(LicensingActivationController.class);

    @PostMapping("/activation")
    public ResponseEntity<?> activateLicense(HttpServletRequest request, @RequestBody LicenseActivationRequest activationRequest) {
        try {
            // Извлекаем email пользователя из токена
            String email = jwtTokenProvider.getEmailFromRequest(request);
            logger.info("Пользователь с email {} инициировал активацию лицензии", email);

            License license = getLicenseByCodeOrThrow(activationRequest.getCode());
            ApplicationUser user = getUserByEmailOrThrow(email);

            // Привязываем пользователя к лицензии, если требуется
            bindUserToLicenseIfNeeded(license, user);

            // Регистрация устройства
            Device device = licensingActivationService.registerOrUpdateDevice(activationRequest, license);

            // Проверка доступных мест на лицензии
            if (license.getDeviceCount() <= 0) {
                throw new IllegalArgumentException("Нет доступных мест для активации");
            }

            // Проверка активации лицензии на устройстве
            if (licensingActivationService.isLicenseAlreadyActivated(activationRequest, device, license)) {
                throw new IllegalArgumentException("Лицензия уже активирована на данном устройстве");
            }

            // Активация лицензии
            licensingActivationService.activateLicenseOnDevice(activationRequest, device, license);

            // Обновление доступных мест
            licensingActivationService.updateAvailableDeviceCount(license);

            // Запись в историю
            licenseHistoryService.recordLicenseChange(license.getId(), license.getUser().getId(), "Активирована", new Date(), "Лицензия активирована");

            logger.info("Лицензия с кодом {} успешно активирована на устройстве с ID {}", activationRequest.getCode(), device.getId());

            Ticket ticket = Ticket.createTicket(null, false, license.getEndingDate(), null, jwtTokenProvider.getSigningKey().toString());
            log.info("Активация завершена успешно, создан тикет: {}", ticket);

            return ResponseEntity.ok(ticket);
        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка активации лицензии: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Внутренняя ошибка при активации лицензии: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Произошла ошибка при активации лицензии");
        }
    }

    private License getLicenseByCodeOrThrow(String code) {
        return licenseRepository.findByCode(code).orElseThrow(() -> {
            logger.error("Лицензия с кодом {} не найдена", code);
            return new IllegalArgumentException("Лицензия не найдена");
        });
    }

    private ApplicationUser getUserByEmailOrThrow(String email) {
        return applicationUserRepository.findByEmail(email).orElseThrow(() -> {
            logger.error("Пользователь с email {} не найден", email);
            return new IllegalArgumentException("Пользователь не найден");
        });
    }

    private void bindUserToLicenseIfNeeded(License license, ApplicationUser user) {
        if (license.getUser() == null) {
            license.setUser(user);
            licenseRepository.save(license);
            logger.info("Лицензия с кодом {} привязана к пользователю {}", license.getCode(), user.getEmail());
        } else if (!license.getUser().getEmail().equals(user.getEmail())) {
            throw new IllegalArgumentException("Пользователь с email " + user.getEmail() + " не имеет доступа к лицензии " + license.getCode());
        }
    }
}


