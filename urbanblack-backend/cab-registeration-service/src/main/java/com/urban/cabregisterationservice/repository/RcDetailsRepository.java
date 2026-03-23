package com.urban.cabregisterationservice.repository;

import com.urban.cabregisterationservice.entity.RcDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RcDetailsRepository extends JpaRepository<RcDetails,Long> {
}
//