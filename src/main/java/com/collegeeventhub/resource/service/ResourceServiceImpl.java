package com.collegeeventhub.resource.service;

import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.common.exception.ConflictException;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.security.StudentProfileVerificationGuard;
import com.collegeeventhub.common.util.Pagination;
import com.collegeeventhub.organization.entity.OrganizationEntity;
import com.collegeeventhub.organization.repository.OrganizationRepository;
import com.collegeeventhub.resource.dto.ResourceCreateRequest;
import com.collegeeventhub.resource.dto.ResourceResponse;
import com.collegeeventhub.resource.dto.ResourceUpdateRequest;
import com.collegeeventhub.resource.entity.ResourceEntity;
import com.collegeeventhub.resource.repository.ResourceRepository;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final StudentProfileVerificationGuard profileVerificationGuard;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> getCatalog(
            int page,
            int size,
            String query,
            String category,
            String tag,
            String username) {
        UserEntity user = findUser(username);
        requireVerifiedStudent(user);
        Page<ResourceEntity> resources = resourceRepository.findActiveCatalog(
                normalizeLowercase(query),
                normalizeLowercase(category),
                normalizeTag(tag),
                pageable(page, size)
        );
        return PageResponse.from(resources, this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceResponse getResource(Long resourceId, String username) {
        UserEntity user = findUser(username);
        requireVerifiedStudent(user);
        ResourceEntity resource = resourceRepository
                .findByIdAndActiveTrue(resourceId)
                .filter(item -> Boolean.TRUE.equals(
                        item.getOrganization().getActive()))
                .orElseThrow(() ->
                        new ResourceNotFoundException("Resource not found"));
        return toResponse(resource);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> getManagedResources(
            int page,
            int size,
            Long organizationId,
            String username) {
        UserEntity user = findUser(username);
        Page<ResourceEntity> resources;
        if (user.getRole() == Role.ADMIN) {
            if (organizationId == null) {
                resources = resourceRepository.findAll(pageable(page, size));
            } else {
                requireOrganization(organizationId);
                resources = resourceRepository.findAllByOrganizationId(
                        organizationId,
                        pageable(page, size)
                );
            }
        } else {
            Collection<Long> organizationIds = organizationRepository
                    .findByLeaderId(user.getId())
                    .stream()
                    .map(OrganizationEntity::getId)
                    .toList();
            if (organizationIds.isEmpty()) {
                throw new AccessDeniedException(
                        "Only admins and organization leaders can manage resources");
            }
            if (organizationId != null) {
                if (!organizationIds.contains(organizationId)) {
                    throw new AccessDeniedException(
                            "You are not the leader of this organization");
                }
                resources = resourceRepository.findAllByOrganizationId(
                        organizationId,
                        pageable(page, size)
                );
            } else {
                resources = resourceRepository.findAllByOrganizationIdIn(
                        organizationIds,
                        pageable(page, size)
                );
            }
        }
        return PageResponse.from(resources, this::toResponse);
    }

    @Override
    @Transactional
    public ResourceResponse createResource(
            ResourceCreateRequest request,
            String username) {
        UserEntity user = findUser(username);
        OrganizationEntity organization =
                requireOrganization(request.getOrganizationId());
        requireCanManage(user, organization);
        if (!Boolean.TRUE.equals(organization.getActive())) {
            throw new ConflictException(
                    "Resources cannot be added to an inactive organization");
        }

        ResourceEntity resource = ResourceEntity.builder()
                .organization(organization)
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .url(request.getUrl().trim())
                .category(request.getCategory().trim())
                .tags(normalizeTags(request.getTags()))
                .build();
        return toResponse(resourceRepository.save(resource));
    }

    @Override
    @Transactional
    public ResourceResponse updateResource(
            Long resourceId,
            ResourceUpdateRequest request,
            String username) {
        UserEntity user = findUser(username);
        ResourceEntity resource = findManagedResource(resourceId, user);
        resource.setTitle(request.getTitle().trim());
        resource.setDescription(request.getDescription().trim());
        resource.setUrl(request.getUrl().trim());
        resource.setCategory(request.getCategory().trim());
        resource.setTags(normalizeTags(request.getTags()));
        return toResponse(resourceRepository.save(resource));
    }

    @Override
    @Transactional
    public ResourceResponse deactivateResource(
            Long resourceId,
            String username) {
        UserEntity user = findUser(username);
        ResourceEntity resource = findManagedResource(resourceId, user);
        resource.setActive(false);
        return toResponse(resourceRepository.save(resource));
    }

    private ResourceEntity findManagedResource(
            Long resourceId,
            UserEntity user) {
        ResourceEntity resource = resourceRepository.findById(resourceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Resource not found"));
        requireCanManage(user, resource.getOrganization());
        if (!Boolean.TRUE.equals(resource.getActive())) {
            throw new ResourceNotFoundException("Resource not found");
        }
        return resource;
    }

    private void requireCanManage(
            UserEntity user,
            OrganizationEntity organization) {
        if (user.getRole() != Role.ADMIN
                && !organization.getLeader().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                    "Only admins and the organization leader can manage this resource");
        }
    }

    private void requireVerifiedStudent(UserEntity user) {
        if (user.getRole() != Role.STUDENT) {
            throw new AccessDeniedException(
                    "Only verified students can access the resource library");
        }
        profileVerificationGuard.checkUser(user);
    }

    private UserEntity findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }

    private OrganizationEntity requireOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Organization not found with id: "
                                        + organizationId));
    }

    private Set<String> normalizeTags(Set<String> tags) {
        if (tags == null) {
            return Set.of();
        }
        return tags.stream()
                .map(this::normalizeTag)
                .collect(Collectors.toSet());
    }

    private String normalizeTag(String tag) {
        return normalizeLowercase(tag);
    }

    private String normalizeLowercase(String value) {
        String normalized = normalizeFilter(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private org.springframework.data.domain.Pageable pageable(
            int page,
            int size) {
        return Pagination.pageRequest(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );
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
