package org.example.rpbo.repository;

import org.example.rpbo.model.DeviceLicense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceLicenseRepository extends JpaRepository<DeviceLicense, Long> {

    // Метод для поиска по license_id
    Optional<DeviceLicense> findByLicenseId(Long licenseId);

    Optional<DeviceLicense> findByDeviceId(Long deviceId);

    // Метод для поиска записи по deviceId и licenseId
    Optional<DeviceLicense> findByDeviceIdAndLicenseId(Long deviceId, Long licenseId);

    boolean existsByDeviceIdAndLicenseId(Long deviceId, Long licenseId);

}
