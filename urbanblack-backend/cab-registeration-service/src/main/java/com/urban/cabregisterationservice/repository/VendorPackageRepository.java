package com.urban.cabregisterationservice.repository;

import com.urban.cabregisterationservice.entity.VendorPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorPackageRepository extends JpaRepository<VendorPackage, Long> {
}
