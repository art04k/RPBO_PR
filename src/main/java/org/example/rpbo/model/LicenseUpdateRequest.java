package org.example.rpbo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LicenseUpdateRequest {
    private String code;  // Код лицензии
    private String newExpirationDate;  // Новая дата окончания срока
    private Long userId;  // ID пользователя, который отправляет запрос
}
