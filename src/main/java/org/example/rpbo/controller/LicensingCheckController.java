package org.example.rpbo.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.rpbo.configuration.JwtTokenProvider;
import org.example.rpbo.model.LicenseCheckRequest;
import org.example.rpbo.model.Ticket;
import org.example.rpbo.service.impl.LicensingCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/licensing")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('modification')")
public class LicensingCheckController {

    private final JwtTokenProvider jwtTokenProvider;
    private final LicensingCheckService licensingCheckService;

    @PostMapping("/check")
    public ResponseEntity<?> checkLicense(HttpServletRequest request, @RequestBody LicenseCheckRequest requestData) {
        Logger logger = LoggerFactory.getLogger(getClass());

        try {
            // 1. Извлекаем роли из токена
            Set<String> roles = jwtTokenProvider.getRolesFromRequest(request);
            logger.info("Роль извлечена из токена: {}", roles);

            // 2. Проверка аутентификации пользователя
            if (roles.isEmpty()) {
                logger.error("Ошибка аутентификации: отсутствуют роли");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Ошибка аутентификации");
            }

            // 3. Проверка лицензии для устройства
            Ticket ticket = licensingCheckService.checkLicenseForDevice(requestData.getMacAddress(), requestData.getDeviceName());

            // 4. Обработка результата
            logger.info("Тикет успешно создан: {}", ticket);
            return ResponseEntity.status(HttpStatus.OK).body(ticket);

        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка проверки лицензии: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Произошла ошибка при проверке лицензии: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Произошла ошибка при проверке лицензии.");
        }
    }
}
