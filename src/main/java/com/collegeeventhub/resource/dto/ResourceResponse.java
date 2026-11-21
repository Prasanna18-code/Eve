package com.collegeeventhub.resource.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceResponse {

    private Long id;
    private Long organizationId;
    private String organizationName;
    private String title;
    private String description;
    private String url;
    private String category;
    private Set<String> tags;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
