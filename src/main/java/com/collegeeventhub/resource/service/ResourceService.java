package com.collegeeventhub.resource.service;

import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.resource.dto.ResourceCreateRequest;
import com.collegeeventhub.resource.dto.ResourceResponse;
import com.collegeeventhub.resource.dto.ResourceUpdateRequest;

public interface ResourceService {

    PageResponse<ResourceResponse> getCatalog(
            int page,
            int size,
            String query,
            String category,
            String tag,
            String username
    );

    ResourceResponse getResource(Long resourceId, String username);

    PageResponse<ResourceResponse> getManagedResources(
            int page,
            int size,
            Long organizationId,
            String username
    );

    ResourceResponse createResource(
            ResourceCreateRequest request,
            String username
    );

    ResourceResponse updateResource(
            Long resourceId,
            ResourceUpdateRequest request,
            String username
    );

    ResourceResponse deactivateResource(Long resourceId, String username);
}
