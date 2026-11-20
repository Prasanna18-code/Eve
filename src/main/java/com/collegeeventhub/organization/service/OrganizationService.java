package com.collegeeventhub.organization.service;

import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.organization.dto.OrganizationRequest;
import com.collegeeventhub.organization.dto.OrganizationResponse;

public interface OrganizationService {

    OrganizationResponse createOrganization(String username, OrganizationRequest request);

    PageResponse<OrganizationResponse> getAllOrganizations(
            int page,
            int size,
            Boolean active
    );

    OrganizationResponse getOrganizationById(Long id);

    OrganizationResponse updateOrganization(Long id, String username, OrganizationRequest request);

    void deleteOrganization(Long id, String username);
}
