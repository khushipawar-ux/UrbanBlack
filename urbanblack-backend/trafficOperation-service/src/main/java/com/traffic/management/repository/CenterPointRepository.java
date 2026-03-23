package com.traffic.management.repository;

import com.traffic.management.entity.CenterPoint;
import com.traffic.management.entity.Depot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CenterPointRepository extends JpaRepository<CenterPoint, Long> {
    List<CenterPoint> findByDepot(Depot depot);
}
