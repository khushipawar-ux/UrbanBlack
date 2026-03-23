package com.urban.cabregisterationservice.service;

import com.urban.cabregisterationservice.dto.VendorPackageRequest;
import com.urban.cabregisterationservice.entity.VendorPackage;
import com.urban.cabregisterationservice.repository.VendorPackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendorPackageService {

    private final VendorPackageRepository vendorPackageRepository;

    @Transactional
    public VendorPackage createVendorPackage(VendorPackageRequest request) {
        log.info("Creating new vendor package");
        VendorPackage vendorPackage = VendorPackage.builder()
                .monthlyPackage(request.getMonthlyPackage())
                .monthlyKm(request.getMonthlyKm())
                .dailyHours(request.getDailyHours())
                .monthlyDaysCover(request.getMonthlyDaysCover())
                .perDayKm(request.getPerDayKm())
                .vendorPerDayPackage(request.getVendorPerDayPackage())
                .build();
        return vendorPackageRepository.save(vendorPackage);
    }

    @Transactional
    public VendorPackage updateVendorPackage(Long id, VendorPackageRequest request) {
        log.info("Updating vendor package with id: {}", id);
        VendorPackage vendorPackage = vendorPackageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor package not found with id: " + id));

        vendorPackage.setMonthlyPackage(request.getMonthlyPackage());
        vendorPackage.setMonthlyKm(request.getMonthlyKm());
        vendorPackage.setDailyHours(request.getDailyHours());
        vendorPackage.setMonthlyDaysCover(request.getMonthlyDaysCover());
        vendorPackage.setPerDayKm(request.getPerDayKm());
        vendorPackage.setVendorPerDayPackage(request.getVendorPerDayPackage());

        return vendorPackageRepository.save(vendorPackage);
    }

    @Transactional
    public void deleteVendorPackage(Long id) {
        log.info("Deleting vendor package with id: {}", id);
        if (!vendorPackageRepository.existsById(id)) {
            throw new RuntimeException("Vendor package not found with id: " + id);
        }
        vendorPackageRepository.deleteById(id);
    }

    public List<VendorPackage> getAllVendorPackages() {
        return vendorPackageRepository.findAll();
    }

    public VendorPackage getVendorPackageById(Long id) {
        return vendorPackageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor package not found with id: " + id));
    }
}
