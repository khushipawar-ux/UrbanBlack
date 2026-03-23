package com.traffic.management.service;

import com.traffic.management.entity.Depot;
import com.traffic.management.repository.DepotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final DepotRepository depotRepository;

    public String exportDepotsAsCSV() {
        List<Depot> depots = depotRepository.findAll();
        StringBuilder csv = new StringBuilder("ID,Name,City,Code,Capacity\n");
        for (Depot d : depots) {
            csv.append(d.getId()).append(",")
               .append(quote(d.getDepotName())).append(",")
               .append(quote(d.getCity())).append(",")
               .append(quote(d.getDepotCode())).append(",")
               .append(d.getCapacity()).append("\n");
        }
        return csv.toString();
    }

    // Vehicle and Employee exports are now handled by their respective services.
    // Use the AllocationService.getAllVehicles() and getAllEmployees() Feign-based methods if needed.

    private String quote(String val) {
        if (val == null) return "";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }
}
