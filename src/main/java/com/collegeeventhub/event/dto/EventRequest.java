package com.collegeeventhub.event.dto;

import com.collegeeventhub.common.enums.EventType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private LocalDate eventDate;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    @NotBlank
    private String venue;

    @NotNull
    @Positive
    private Integer capacity;

    @NotNull
    private LocalDateTime registrationDeadline;

    @NotNull
    private EventType type;

    private Set<String> eligibleBranches;

    private Set<Integer> eligibleAcademicYears;

    private Set<String> eligibleSections;

    @JsonIgnore
    private boolean eligibleBranchesProvided;

    @JsonIgnore
    private boolean eligibleAcademicYearsProvided;

    @JsonIgnore
    private boolean eligibleSectionsProvided;

    @JsonSetter("eligibleBranches")
    public void setEligibleBranches(Set<String> eligibleBranches) {
        this.eligibleBranches = eligibleBranches == null
                ? null
                : new HashSet<>(eligibleBranches);
        eligibleBranchesProvided = true;
    }

    @JsonSetter("eligibleAcademicYears")
    public void setEligibleAcademicYears(Set<Integer> eligibleAcademicYears) {
        this.eligibleAcademicYears = eligibleAcademicYears == null
                ? null
                : new HashSet<>(eligibleAcademicYears);
        eligibleAcademicYearsProvided = true;
    }

    @JsonSetter("eligibleSections")
    public void setEligibleSections(Set<String> eligibleSections) {
        this.eligibleSections = eligibleSections == null
                ? null
                : new HashSet<>(eligibleSections);
        eligibleSectionsProvided = true;
    }

    @JsonIgnore
    public boolean wasEligibleBranchesProvided() {
        return eligibleBranchesProvided;
    }

    @JsonIgnore
    public boolean wasEligibleAcademicYearsProvided() {
        return eligibleAcademicYearsProvided;
    }

    @JsonIgnore
    public boolean wasEligibleSectionsProvided() {
        return eligibleSectionsProvided;
    }
}
