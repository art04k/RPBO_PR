package org.example.rpbo.controller;

import org.example.rpbo.model.Device;
import org.example.rpbo.service.impl.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/devices")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
public class DeviceController {

    private final DeviceService deviceService;

    @Autowired
    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    // Создание или обновление устройства
    @PostMapping
    public ResponseEntity<Device> createOrUpdateDevice(@RequestBody Device device) {
        Device savedDevice = deviceService.saveDevice(device);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDevice);
    }

    // Получение устройства по ID
    @GetMapping("/{id}")
    public ResponseEntity<Device> getDeviceById(@PathVariable Long id) {
        Optional<Device> device = deviceService.getDeviceById(id);
        return device.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // Получение устройства по MAC-адресу
    @GetMapping("/mac/{macAddress}")
    public ResponseEntity<Device> getDeviceByMacAddress(@PathVariable String macAddress) {
        Device device = deviceService.getDeviceByMacAddress(macAddress);
        return device != null ? ResponseEntity.ok(device) : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    // Получение всех устройств
    @GetMapping
    public ResponseEntity<List<Device>> getAllDevices() {
        List<Device> devices = deviceService.getAllDevices();
        return ResponseEntity.ok(devices);
    }

    // Удаление устройства по ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        Optional<Device> device = deviceService.getDeviceById(id);
        if (device.isPresent()) {
            deviceService.deleteDevice(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
