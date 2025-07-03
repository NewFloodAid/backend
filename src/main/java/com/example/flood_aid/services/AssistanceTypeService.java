package com.example.flood_aid.services;

import com.example.flood_aid.models.AssistanceType;
import com.example.flood_aid.repositories.AssistanceTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssistanceTypeService {

    @Autowired
    private AssistanceTypeRepository assistanceTypeRepository;
    public List<AssistanceType> getAssistanceTypes(){
        return assistanceTypeRepository.findAll();
    }
}
