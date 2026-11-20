package com.collegeeventhub.placement.dto;

import com.collegeeventhub.placement.entity.PlacementApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlacementApplicationResponse {

    private Long id;
    private Long placementId;
    private String companyName;
    private String placementTitle;
    private PlacementApplicationStatus status;
    private LocalDateTime appliedAt;
}
