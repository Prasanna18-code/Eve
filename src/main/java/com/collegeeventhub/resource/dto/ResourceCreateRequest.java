package com.collegeeventhub.resource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceCreateRequest {

    @NotNull
    private Long organizationId;

    @NotBlank
    @Size(max = 150)
    private String title;

    @NotBlank
    @Size(max = 5000)
    private String description;

    @NotBlank
    @Size(max = 2048)
    @Pattern(regexp = "(?i)^https?://[^\\s]+$")
    private String url;

    @NotBlank
    @Size(max = 80)
    private String category;

    @Size(max = 20)
    private Set<@NotBlank @Size(max = 50) String> tags;
}
