package com.example.flood_aid.services;

import com.example.flood_aid.models.ReportStatus;
import com.example.flood_aid.repositories.ReportStatusRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ReportStatusService {

    ReportStatusRepository reportStatusRepository;
    public List<ReportStatus> getReportStatuses(Boolean isUser) {
        if(isUser) {
            return reportStatusRepository.findAllByOrderByUserOrderingNumber();
        }
        return reportStatusRepository.findAllByOrderByGovernmentOrderingNumber();
    }
}
