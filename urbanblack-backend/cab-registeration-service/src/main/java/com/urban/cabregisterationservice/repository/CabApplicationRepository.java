package com.urban.cabregisterationservice.repository;

import com.urban.cabregisterationservice.entity.CabApplication;
import com.urbanblack.common.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CabApplicationRepository extends JpaRepository<CabApplication,Long> {

    List<CabApplication> findByStatusOrderByCreatedDateDesc (ApplicationStatus status);

    List<CabApplication> findByUsername(String username);

    List<CabApplication> findByUsernameAndStatus(String username, ApplicationStatus status);
}
//