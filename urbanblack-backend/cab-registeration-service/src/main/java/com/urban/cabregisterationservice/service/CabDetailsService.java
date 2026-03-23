package com.urban.cabregisterationservice.service;


import com.urban.cabregisterationservice.entity.CabApplication;
import com.urban.cabregisterationservice.repository.CabApplicationRepository;
import com.urbanblack.common.enums.ApplicationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CabDetailsService {
    private final CabApplicationRepository cabApplicationRepository;

    public List<CabApplication> getApplicationByusername(String username, ApplicationStatus status){

        return cabApplicationRepository.findByUsernameAndStatus(username, status);
    }

}
//