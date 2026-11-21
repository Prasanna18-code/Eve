package com.collegeeventhub.resource.service;

import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.util.Pagination;
import com.collegeeventhub.resource.dto.ResourceResponse;
import com.collegeeventhub.resource.entity.ResourceEntity;
import com.collegeeventhub.resource.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ResourceCacheService {
    private final ResourceRepository resourceRepository;

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "resources",
            key = "#page + ':' + #size + ':' + #query + ':' + #category + ':' + #tag"
    )
    public PageResponse<ResourceResponse> getCatalog(
            int page,
            int size,
            String query,
            String category,
            String tag) {
        Page<ResourceEntity> resources = resourceRepository.findActiveCatalog(
                normalizeLowercase(query),
                normalizeLowercase(category),
                normalizeTag(tag),
                Pagination.pageRequest(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.desc("createdAt"),
                                Sort.Order.desc("id")
                        )
                )
        );
        return PageResponse.from(resources, this::toResponse);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "resources", key = "'detail:' + #resourceId")
    public ResourceResponse getResource(Long resourceId) {
        ResourceEntity resource = resourceRepository
                .findByIdAndActiveTrue(resourceId)
                .filter(item -> Boolean.TRUE.equals(
                        item.getOrganization().getActive()))
                .orElseThrow(() ->
                        new ResourceNotFoundException("Resource not found"));
        return toResponse(resource);
    }

    private String normalizeTag(String tag) {
        return normalizeLowercase(tag);
    }

    private String normalizeLowercase(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private ResourceResponse toResponse(ResourceEntity resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .organizationId(resource.getOrganization().getId())
                .organizationName(resource.getOrganization().getName())
                .title(resource.getTitle())
                .description(resource.getDescription())
                .url(resource.getUrl())
                .category(resource.getCategory())
                .tags(Set.copyOf(resource.getTags()))
                .active(resource.getActive())
                .createdAt(resource.getCreatedAt())
                .updatedAt(resource.getUpdatedAt())
                .build();
    }
}
