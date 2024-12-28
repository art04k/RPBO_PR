package org.example.rpbo.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.rpbo.model.*;
import org.example.rpbo.repository.DeviceRepository;
import org.example.rpbo.repository.DeviceLicenseRepository;
import org.example.rpbo.repository.LicenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class LicensingActivationService {

    private final DeviceRepository deviceRepository;
    private final LicenseRepository licenseRepository;
    private final DeviceLicenseRepository deviceLicenseRepository;

    @Transactional
    public Device registerOrUpdateDevice(LicenseActivationRequest activationRequest, License license) {
        // Ищем устройство по MAC-адресу и имени
        return deviceRepository.findByMacAddressAndName(activationRequest.getMacAddress(), activationRequest.getDeviceName())
                .orElseGet(() -> createNewDevice(activationRequest, license));
    }

    private Device createNewDevice(LicenseActivationRequest activationRequest, License license) {
        Device newDevice = new Device();
        newDevice.setMacAddress(activationRequest.getMacAddress());
        newDevice.setName(activationRequest.getDeviceName());
        newDevice.setUserId(license.getUser().getId());
        return deviceRepository.save(newDevice);
    }

    public boolean isLicenseAlreadyActivated(LicenseActivationRequest activationRequest, Device device, License license) {
        return deviceLicenseRepository.existsByDeviceIdAndLicenseId(device.getId(), license.getId());
    }

    @Transactional
    public void activateLicenseOnDevice(LicenseActivationRequest activationRequest, Device device, License license) {
        if (isLicenseAlreadyActivated(activationRequest, device, license)) {
            throw new IllegalStateException("Лицензия уже активирована на этом устройстве");
        }

        // Установить дату активации и окончания
        Date activationDate = new Date();
        Date endingDate = calculateEndDate(license.getDuration());

        license.setFirstActivationDate(activationDate);
        license.setEndingDate(endingDate);

        licenseRepository.save(license);

        DeviceLicense deviceLicense = new DeviceLicense();
        deviceLicense.setLicenseId(license.getId());
        deviceLicense.setDeviceId(device.getId());
        deviceLicense.setActivationDate(activationDate);
        deviceLicenseRepository.save(deviceLicense);
    }

    private Date calculateEndDate(int duration) {
        LocalDate endingLocalDate = LocalDate.now().plusDays(duration);
        return Date.from(endingLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public void updateAvailableDeviceCount(License license) {
        if (license != null) {
            int remainingDevices = license.getDeviceCount() - 1;
            if (remainingDevices >= 0) {
                license.setDeviceCount(remainingDevices);
                // Сохранение изменений в репозиторий
                licenseRepository.save(license);
            } else {
                throw new IllegalArgumentException("Количество доступных устройств не может быть отрицательным");
            }
        } else {
            throw new IllegalArgumentException("Лицензия не может быть null");
        }
    }


}
