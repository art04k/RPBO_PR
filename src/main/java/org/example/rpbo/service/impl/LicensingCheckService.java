package org.example.rpbo.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.rpbo.model.*;
import org.example.rpbo.repository.DeviceLicenseRepository;
import org.example.rpbo.repository.DeviceRepository;
import org.example.rpbo.repository.LicenseRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LicensingCheckService {

    private final DeviceRepository deviceRepository;
    private final DeviceLicenseRepository deviceLicenseRepository;
    private final LicenseRepository licenseRepository;
    private static final String SECRET_KEY = "YourSecretKeyHere"; // Секретный ключ для подписи

    public Ticket checkLicenseForDevice(String macAddress, String deviceName) {
        // 1. Поиск устройства по MAC-адресу и имени
        Optional<Device> deviceOptional = deviceRepository.findByMacAddressAndName(macAddress, deviceName);
        if (deviceOptional.isEmpty()) {
            throw new IllegalArgumentException("Устройство с данным MAC-адресом и именем не найдено.");
        }
        Device device = deviceOptional.get();

        // 2. Поиск лицензии для устройства
        Optional<DeviceLicense> deviceLicenseOptional = deviceLicenseRepository.findByDeviceId(device.getId());
        if (deviceLicenseOptional.isEmpty()) {
            throw new IllegalArgumentException("Активная лицензия для устройства не найдена.");
        }
        DeviceLicense deviceLicense = deviceLicenseOptional.get();

        // 3. Поиск лицензии по licenseId
        Optional<License> licenseOptional = licenseRepository.findById(deviceLicense.getLicenseId());
        if (licenseOptional.isEmpty()) {
            throw new IllegalArgumentException("Лицензия не найдена по licenseId.");
        }
        License license = licenseOptional.get();

        // 4. Проверка состояния лицензии
        if (Boolean.TRUE.equals(license.getBlocked())) {
            throw new IllegalArgumentException("Лицензия заблокирована.");
        }

        // 5. Проверка срока действия лицензии
        if (license.getEndingDate().before(new Date())) {
            throw new IllegalArgumentException("Срок действия лицензии истек.");
        }

        Ticket ticket = Ticket.createTicket(
                license.getUser().getId(),
                license.getBlocked(),
                license.getEndingDate(),
                device.getId(),
                SECRET_KEY
        );


        return ticket;
    }
}
