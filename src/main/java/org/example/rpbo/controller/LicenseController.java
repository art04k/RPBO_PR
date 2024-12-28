package org.example.rpbo.controller;

import org.example.rpbo.model.License;
import org.example.rpbo.repository.LicenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/licenses")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
public class LicenseController {

    @Autowired
    private LicenseRepository licenseRepository;

    @GetMapping
    public List<License> getAllLicenses() {
        return licenseRepository.findAll();
    }

    @GetMapping("/{id}")
    public License getLicenseById(@PathVariable Long id) {
        Optional<License> license = licenseRepository.findById(id);
        return license.orElse(null);
    }

    @PostMapping
    public License createLicense(@RequestBody License license) {
        return licenseRepository.save(license);
    }

    @PutMapping("/{id}")
    public License updateLicense(@PathVariable Long id, @RequestBody License license) {
        license.setId(id);
        return licenseRepository.save(license);
    }

    @DeleteMapping("/{id}")
    public void deleteLicense(@PathVariable Long id) {
        licenseRepository.deleteById(id);
    }
}
