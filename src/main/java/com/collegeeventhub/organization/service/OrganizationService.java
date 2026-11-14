package com.collegeeventhub.organization.service;

import com.collegeeventhub.organization.dto.OrganizationRequest;
import com.collegeeventhub.organization.dto.OrganizationResponse;

import java.util.List;

public interface OrganizationService {

    OrganizationResponse createOrganization(String username, OrganizationRequest request);

    List<OrganizationResponse> getAllOrganizations();

    OrganizationResponse getOrganizationById(Long id);

    OrganizationResponse updateOrganization(Long id, String username, OrganizationRequest request);

    void deleteOrganization(Long id, String username);
}
