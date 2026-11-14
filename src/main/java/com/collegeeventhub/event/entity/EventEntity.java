package com.collegeeventhub.event.entity;

import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.common.enums.EventType;
import com.collegeeventhub.organization.entity.OrganizationEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDate eventDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private String venue;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private LocalDateTime registrationDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EventStatus status = EventStatus.UPCOMING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ElementCollection
    @CollectionTable(
            name = "event_target_branches",
            joinColumns = @JoinColumn(name = "event_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_event_target_branch",
                    columnNames = {"event_id", "branch"}
            )
    )
    @Column(name = "branch", nullable = false)
    @Builder.Default
    private Set<String> eligibleBranches = new HashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "event_target_years",
            joinColumns = @JoinColumn(name = "event_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_event_target_year",
                    columnNames = {"event_id", "academic_year"}
            )
    )
    @Column(name = "academic_year", nullable = false)
    @Builder.Default
    private Set<Integer> eligibleAcademicYears = new HashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "event_target_sections",
            joinColumns = @JoinColumn(name = "event_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_event_target_section",
                    columnNames = {"event_id", "section"}
            )
    )
    @Column(name = "section", nullable = false)
    @Builder.Default
    private Set<String> eligibleSections = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}