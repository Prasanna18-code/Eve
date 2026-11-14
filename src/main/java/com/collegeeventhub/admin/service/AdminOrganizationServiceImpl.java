package com.collegeeventhub.admin.service;

import com.collegeeventhub.admin.dto.AdminOrganizationResponse;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.organization.entity.OrganizationEntity;
import com.collegeeventhub.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminOrganizationServiceImpl implements AdminOrganizationService {

    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminOrganizationResponse> getAllOrganizations() {

        return organizationRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

            @Override
            @Transactional
            public void activateOrganization(Long organizationId) {

            OrganizationEntity organization =
                organizationRepository.findById(organizationId)
                    .orElseThrow(() ->
                        new ResourceNotFoundException(
                            "Organization not found"));

            organization.setActive(true);
            organizationRepository.save(organization);
            }

            @Override
            @Transactional
            public void deactivateOrganization(Long organizationId) {

            OrganizationEntity organization =
                organizationRepository.findById(organizationId)
                    .orElseThrow(() ->
                        new ResourceNotFoundException(
                            "Organization not found"));

            organization.setActive(false);
            organizationRepository.save(organization);
            }

    private AdminOrganizationResponse toResponse(
            OrganizationEntity organization) {

        return AdminOrganizationResponse.builder()
                .id(organization.getId())
                .name(organization.getName())
                .description(organization.getDescription())
                .email(organization.getEmail())
                .phoneNumber(organization.getPhoneNumber())
                .logo(organization.getLogo())
                .website(organization.getWebsite())
                .active(organization.getActive())
                .leaderId(organization.getLeader().getId())
                .leaderUsername(organization.getLeader().getUsername())
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
                .build();
    }
}
