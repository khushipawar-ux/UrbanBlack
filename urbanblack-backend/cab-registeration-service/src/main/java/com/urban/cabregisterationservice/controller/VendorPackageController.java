package com.urban.cabregisterationservice.controller;

import com.urban.cabregisterationservice.dto.VendorPackageRequest;
import com.urban.cabregisterationservice.entity.VendorPackage;
import com.urban.cabregisterationservice.service.VendorPackageService;
import com.urbanblack.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vendor-packages")
@RequiredArgsConstructor
public class VendorPackageController {

    private final VendorPackageService vendorPackageService;

    @PostMapping
    public ResponseEntity<VendorPackage> createVendorPackage(@RequestBody VendorPackageRequest request) {
        VendorPackage created = vendorPackageService.createVendorPackage(request);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VendorPackage> updateVendorPackage(@PathVariable Long id, @RequestBody VendorPackageRequest request) {
        VendorPackage updated = vendorPackageService.updateVendorPackage(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteVendorPackage(@PathVariable Long id) {
        vendorPackageService.deleteVendorPackage(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .message("Vendor Package Deleted Successfully")
                .success(true)
                .build());
    }

    @GetMapping
    public ResponseEntity<List<VendorPackage>> getAllVendorPackages() {
        return ResponseEntity.ok(vendorPackageService.getAllVendorPackages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendorPackage> getVendorPackageById(@PathVariable Long id) {
        return ResponseEntity.ok(vendorPackageService.getVendorPackageById(id));
    }
}
