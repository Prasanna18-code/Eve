package com.collegeeventhub.organization.service;

import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.util.Pagination;
import com.collegeeventhub.organization.dto.OrganizationRequest;
import com.collegeeventhub.organization.dto.OrganizationResponse;
import com.collegeeventhub.organization.entity.OrganizationEntity;
import com.collegeeventhub.organization.mapper.OrganizationMapper;
import com.collegeeventhub.organization.repository.OrganizationRepository;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;
    private final UserRepository userRepository;

    @Override
    @CacheEvict(cacheNames = "organizations", allEntries = true)
    public OrganizationResponse createOrganization(
            String username,
            OrganizationRequest request) {

        UserEntity leader = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with username: " + username));

        if (organizationRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException(
                    "Organization with this name already exists");
        }

        OrganizationEntity organization =
                organizationMapper.toEntity(request);

        organization.setLeader(leader);

        OrganizationEntity savedOrganization =
                organizationRepository.save(organization);

        return organizationMapper.toResponse(savedOrganization);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "organizations",
            key = "#page + ':' + #size + ':' + #active"
    )
    public PageResponse<OrganizationResponse> getAllOrganizations(
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
        return PageResponse.from(organizations, organizationMapper::toResponse);
    }

    @Override
    @Cacheable(cacheNames = "organizations", key = "'id:' + #id")
    public OrganizationResponse getOrganizationById(Long id) {

        OrganizationEntity organization =
                organizationRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Organization not found with id: " + id));

        return organizationMapper.toResponse(organization);
    }

    @Override
    @CacheEvict(cacheNames = "organizations", allEntries = true)
    public OrganizationResponse updateOrganization(
            Long id,
            String username,
            OrganizationRequest request) {

        OrganizationEntity organization =
                organizationRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Organization not found with id: " + id));

        verifyLeader(organization, username);

        organization.setName(request.getName());
        organization.setDescription(request.getDescription());
        organization.setEmail(request.getEmail());
        organization.setPhoneNumber(request.getPhoneNumber());
        organization.setLogo(request.getLogo());
        organization.setWebsite(request.getWebsite());

        OrganizationEntity updatedOrganization =
                organizationRepository.save(organization);

        return organizationMapper.toResponse(updatedOrganization);
    }

    @Override
    @CacheEvict(cacheNames = "organizations", allEntries = true)
    public void deleteOrganization(
            Long id,
            String username) {

        OrganizationEntity organization =
                organizationRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Organization not found with id: " + id));

        verifyLeader(organization, username);

        organizationRepository.delete(organization);
    }

    private void verifyLeader(
            OrganizationEntity organization,
            String username) {

        if (!organization.getLeader()
                .getUsername()
                .equals(username)) {

            throw new AccessDeniedException(
                    "You are not the leader of this organization");
        }
    }
}
