package org.example.rpbo.controller;

import lombok.RequiredArgsConstructor;
import org.example.rpbo.model.LicenseCreateRequest;
import org.example.rpbo.service.impl.LicensingCreateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/licensing")
@RequiredArgsConstructor
public class LicensingCreateController {

    private final LicensingCreateService licensingCreateService;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('modification')")
    public ResponseEntity<?> createLicense(@RequestBody LicenseCreateRequest requestData,
                                           @RequestHeader("Authorization") String authHeader) {
        try {
            // Извлечение email пользователя из токена
            String email = licensingCreateService.extractEmailFromToken(authHeader);

            // Создание лицензии через сервис
            licensingCreateService.createLicense(requestData, email);

            // Возвращаем успешный ответ
            return ResponseEntity.ok("Лицензия успешно создана");
        } catch (IllegalArgumentException e) {
            // Обработка ошибок проверки
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Обработка любых других ошибок
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Произошла ошибка при создании лицензии");
        }
    }
}
