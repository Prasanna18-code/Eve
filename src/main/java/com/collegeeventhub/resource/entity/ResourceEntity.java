package com.collegeeventhub.resource.entity;

import com.collegeeventhub.organization.entity.OrganizationEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "learning_resources",
        indexes = {
                @Index(
                        name = "idx_learning_resource_active_category",
                        columnList = "active, category"
                ),
                @Index(
                        name = "idx_learning_resource_organization",
                        columnList = "organization_id, active"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false, length = 80)
    private String category;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "learning_resource_tags",
            joinColumns = @JoinColumn(name = "resource_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_learning_resource_tag",
                    columnNames = {"resource_id", "tag"}
            ),
            indexes = @Index(
                    name = "idx_learning_resource_tag",
                    columnList = "tag"
            )
    )
    @Column(name = "tag", nullable = false, length = 50)
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

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
