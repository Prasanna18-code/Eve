package com.collegeeventhub.placement.dto;

import com.collegeeventhub.placement.entity.PlacementStatus;
import com.collegeeventhub.placement.entity.PlacementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlacementResponse {

    private Long id;
    private String companyName;
    private String title;
    private String description;
    private String location;
    private PlacementType type;
    private String compensation;
    private String eligibilityCriteria;
    private LocalDateTime applicationDeadline;
    private PlacementStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
