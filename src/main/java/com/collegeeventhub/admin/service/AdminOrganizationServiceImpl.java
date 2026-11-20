package com.collegeeventhub.admin.service;

import com.collegeeventhub.admin.dto.AdminOrganizationResponse;
import com.collegeeventhub.admin.audit.AdminAuditAction;
import com.collegeeventhub.admin.audit.AdminAuditLogService;
import com.collegeeventhub.admin.audit.AdminAuditTargetType;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.util.Pagination;
import com.collegeeventhub.organization.entity.OrganizationEntity;
import com.collegeeventhub.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminOrganizationServiceImpl implements AdminOrganizationService {

    private final OrganizationRepository organizationRepository;
    private final AdminAuditLogService adminAuditLogService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminOrganizationResponse> getAllOrganizations(
            int page,
            int size,
            Boolean active) {
        Page<OrganizationEntity> organizations =
                organizationRepository.findAllByFilters(
                        active != null,
                        Boolean.TRUE.equals(active),
                        Pagination.pageRequest(
                                page,
                                size,
                                Sort.by(Sort.Order.asc("id"))
                        )
                );
        return PageResponse.from(organizations, this::toResponse);
    }

    @Override
    @Transactional
    public void activateOrganization(
            Long organizationId,
            Long adminUserId) {
        OrganizationEntity organization =
                organizationRepository.findById(organizationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Organization not found"));

        boolean wasActive = Boolean.TRUE.equals(organization.getActive());
        organization.setActive(true);
        organizationRepository.save(organization);
        adminAuditLogService.record(
                adminUserId,
                AdminAuditAction.ORGANIZATION_ACTIVATED,
                AdminAuditTargetType.ORGANIZATION,
                organizationId,
                "active changed from " + wasActive + " to true"
        );
    }

    @Override
    @Transactional
    public void deactivateOrganization(
            Long organizationId,
            Long adminUserId) {
        OrganizationEntity organization =
                organizationRepository.findById(organizationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Organization not found"));

        boolean wasActive = Boolean.TRUE.equals(organization.getActive());
        organization.setActive(false);
        organizationRepository.save(organization);
        adminAuditLogService.record(
                adminUserId,
                AdminAuditAction.ORGANIZATION_DEACTIVATED,
                AdminAuditTargetType.ORGANIZATION,
                organizationId,
                "active changed from " + wasActive + " to false"
        );
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
