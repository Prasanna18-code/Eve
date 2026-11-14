package com.collegeeventhub.admin.service;

import com.collegeeventhub.admin.dto.AdminOrganizationResponse;

import java.util.List;

public interface AdminOrganizationService {

    List<AdminOrganizationResponse> getAllOrganizations();

    void activateOrganization(Long organizationId);

    void deactivateOrganization(Long organizationId);
}
