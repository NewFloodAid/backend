package com.example.flood_aid.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssistanceTopicStatDto {
    private Long assistanceTypeId;
    private String assistanceTypeName;
    private Long reportCount;
    private Long totalQuantity;
}
