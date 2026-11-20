package com.collegeeventhub.admin.service;

import com.collegeeventhub.admin.dto.AdminOrganizationResponse;
import com.collegeeventhub.common.response.PageResponse;

public interface AdminOrganizationService {

    PageResponse<AdminOrganizationResponse> getAllOrganizations(
            int page,
            int size,
            Boolean active
    );

    void activateOrganization(Long organizationId);

    void deactivateOrganization(Long organizationId);
}
