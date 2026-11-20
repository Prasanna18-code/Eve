package com.collegeeventhub.placement.dto;

import com.collegeeventhub.placement.entity.PlacementType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlacementRequest {

    @NotBlank
    @Size(max = 150)
    private String companyName;

    @NotBlank
    @Size(max = 150)
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    @Size(max = 150)
    private String location;

    @NotNull
    private PlacementType type;

    @Size(max = 100)
    private String compensation;

    private String eligibilityCriteria;

    @NotNull
    @Future
    private LocalDateTime applicationDeadline;
}
