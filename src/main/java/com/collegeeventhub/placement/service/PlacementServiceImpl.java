package com.collegeeventhub.placement.service;

import com.collegeeventhub.admin.audit.AdminAuditAction;
import com.collegeeventhub.admin.audit.AdminAuditLogService;
import com.collegeeventhub.admin.audit.AdminAuditTargetType;
import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.common.exception.ConflictException;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.security.StudentProfileVerificationGuard;
import com.collegeeventhub.common.util.Pagination;
import com.collegeeventhub.placement.dto.PlacementApplicantResponse;
import com.collegeeventhub.placement.dto.PlacementApplicationResponse;
import com.collegeeventhub.placement.dto.PlacementRequest;
import com.collegeeventhub.placement.dto.PlacementResponse;
import com.collegeeventhub.placement.entity.PlacementApplicationEntity;
import com.collegeeventhub.placement.entity.PlacementEntity;
import com.collegeeventhub.placement.entity.PlacementStatus;
import com.collegeeventhub.placement.entity.PlacementType;
import com.collegeeventhub.placement.repository.PlacementApplicationRepository;
import com.collegeeventhub.placement.repository.PlacementRepository;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PlacementServiceImpl implements PlacementService {

    private final PlacementRepository placementRepository;
    private final PlacementApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final StudentProfileVerificationGuard profileVerificationGuard;
    private final AdminAuditLogService adminAuditLogService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PlacementResponse> getPublishedPlacements(
            int page,
            int size,
            PlacementType type) {
        Page<PlacementEntity> placements =
                placementRepository.findPublishedByType(
                        PlacementStatus.PUBLISHED,
                        LocalDateTime.now(),
                        type != null,
                        type == null ? PlacementType.OTHER : type,
                        pageable(
                                page,
                                size,
                                Sort.Order.desc("createdAt"),
                                Sort.Order.desc("id")
                        )
                );
        return PageResponse.from(placements, this::toPlacementResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PlacementResponse getPublishedPlacement(Long placementId) {
        PlacementEntity placement = placementRepository.findById(placementId)
                .filter(item -> item.getStatus() == PlacementStatus.PUBLISHED
                        && item.getApplicationDeadline()
                        .isAfter(LocalDateTime.now()))
                .orElseThrow(() ->
                        new ResourceNotFoundException("Placement not found"));
        return toPlacementResponse(placement);
    }

    @Override
    @Transactional
    public PlacementApplicationResponse apply(
            Long placementId,
            String username) {
        UserEntity user = findUser(username);
        if (user.getRole() != Role.STUDENT) {
            throw new AccessDeniedException(
                    "Only students can apply for placement opportunities");
        }
        profileVerificationGuard.checkUser(user);

        PlacementEntity placement = placementRepository.findById(placementId)
                .filter(item -> item.getStatus() == PlacementStatus.PUBLISHED)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Placement not found"));
        if (!placement.getApplicationDeadline().isAfter(LocalDateTime.now())) {
            throw new ConflictException("The application deadline has passed");
        }
        if (applicationRepository.existsByPlacementIdAndUserId(
                placementId,
                user.getId())) {
            throw new ConflictException(
                    "You have already applied for this placement");
        }

        PlacementApplicationEntity application =
                PlacementApplicationEntity.builder()
                        .placement(placement)
                        .user(user)
                        .build();
        try {
            return toApplicationResponse(
                    applicationRepository.saveAndFlush(application));
        } catch (DataIntegrityViolationException exception) {
            if (isUniqueConstraintViolation(exception)) {
                throw new ConflictException(
                        "You have already applied for this placement");
            }
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PlacementApplicationResponse> getMyApplications(
            int page,
            int size,
            String username) {
        UserEntity user = findUser(username);
        if (user.getRole() != Role.STUDENT) {
            throw new AccessDeniedException(
                    "Only students can view placement applications");
        }
        profileVerificationGuard.checkUser(user);
        Page<PlacementApplicationEntity> applications =
                applicationRepository.findByUserId(
                        user.getId(),
                        pageable(
                                page,
                                size,
                                Sort.Order.desc("appliedAt"),
                                Sort.Order.desc("id")
                        )
                );
        return PageResponse.from(applications, this::toApplicationResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PlacementResponse> getAdminPlacements(
            int page,
            int size,
            PlacementStatus status) {
        Page<PlacementEntity> placements = placementRepository.findAllByFilters(
                status != null,
                status == null ? PlacementStatus.DRAFT : status,
                false,
                PlacementType.OTHER,
                pageable(
                        page,
                        size,
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );
        return PageResponse.from(placements, this::toPlacementResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PlacementResponse getAdminPlacement(Long placementId) {
        return toPlacementResponse(findPlacement(placementId));
    }

    @Override
    @Transactional
    public PlacementResponse createPlacement(
            PlacementRequest request,
            Long adminUserId) {
        UserEntity admin = userRepository.findById(adminUserId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Admin user not found"));
        PlacementEntity placement = newPlacement(request, admin);
        PlacementEntity saved = placementRepository.save(placement);
        adminAuditLogService.record(
                adminUserId,
                AdminAuditAction.PLACEMENT_CREATED,
                AdminAuditTargetType.PLACEMENT,
                saved.getId(),
                "placement created as DRAFT"
        );
        return toPlacementResponse(saved);
    }

    @Override
    @Transactional
    public PlacementResponse updatePlacement(
            Long placementId,
            PlacementRequest request,
            Long adminUserId) {
        PlacementEntity placement = findPlacement(placementId);
        if (placement.getStatus() == PlacementStatus.CLOSED) {
            throw new ConflictException("Closed placements cannot be edited");
        }
        applyRequest(placement, request);
        PlacementEntity saved = placementRepository.save(placement);
        adminAuditLogService.record(
                adminUserId,
                AdminAuditAction.PLACEMENT_UPDATED,
                AdminAuditTargetType.PLACEMENT,
                placementId,
                "placement details updated"
        );
        return toPlacementResponse(saved);
    }

    @Override
    @Transactional
    public PlacementResponse publishPlacement(
            Long placementId,
            Long adminUserId) {
        PlacementEntity placement = findPlacement(placementId);
        if (placement.getStatus() == PlacementStatus.CLOSED) {
            throw new ConflictException("Closed placements cannot be published");
        }
        if (!placement.getApplicationDeadline().isAfter(LocalDateTime.now())) {
            throw new ConflictException(
                    "The application deadline must be in the future");
        }
        if (placement.getStatus() != PlacementStatus.PUBLISHED) {
            placement.setStatus(PlacementStatus.PUBLISHED);
            placementRepository.save(placement);
            adminAuditLogService.record(
                    adminUserId,
                    AdminAuditAction.PLACEMENT_PUBLISHED,
                    AdminAuditTargetType.PLACEMENT,
                    placementId,
                    "status changed from DRAFT to PUBLISHED"
            );
        }
        return toPlacementResponse(placement);
    }

    @Override
    @Transactional
    public PlacementResponse closePlacement(
            Long placementId,
            Long adminUserId) {
        PlacementEntity placement = findPlacement(placementId);
        if (placement.getStatus() != PlacementStatus.CLOSED) {
            PlacementStatus previousStatus = placement.getStatus();
            placement.setStatus(PlacementStatus.CLOSED);
            placementRepository.save(placement);
            adminAuditLogService.record(
                    adminUserId,
                    AdminAuditAction.PLACEMENT_CLOSED,
                    AdminAuditTargetType.PLACEMENT,
                    placementId,
                    "status changed from " + previousStatus + " to CLOSED"
            );
        }
        return toPlacementResponse(placement);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PlacementApplicantResponse> getApplicants(
            Long placementId,
            int page,
            int size) {
        findPlacement(placementId);
        Page<PlacementApplicationEntity> applications =
                applicationRepository.findByPlacementId(
                        placementId,
                        pageable(
                                page,
                                size,
                                Sort.Order.desc("appliedAt"),
                                Sort.Order.desc("id")
                        )
                );
        return PageResponse.from(applications, this::toApplicantResponse);
    }

    private UserEntity findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }

    private PlacementEntity findPlacement(Long placementId) {
        return placementRepository.findById(placementId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Placement not found with id: " + placementId));
    }

    private PlacementEntity newPlacement(
            PlacementRequest request,
            UserEntity admin) {
        PlacementEntity placement = new PlacementEntity();
        placement.setCreatedBy(admin);
        applyRequest(placement, request);
        placement.setStatus(PlacementStatus.DRAFT);
        return placement;
    }

    private void applyRequest(
            PlacementEntity placement,
            PlacementRequest request) {
        if (request.getApplicationDeadline() == null
                || !request.getApplicationDeadline().isAfter(LocalDateTime.now())) {
            throw new ConflictException(
                    "The application deadline must be in the future");
        }
        placement.setCompanyName(request.getCompanyName().trim());
        placement.setTitle(request.getTitle().trim());
        placement.setDescription(request.getDescription().trim());
        placement.setLocation(request.getLocation().trim());
        placement.setType(request.getType());
        placement.setCompensation(request.getCompensation());
        placement.setEligibilityCriteria(request.getEligibilityCriteria());
        placement.setApplicationDeadline(request.getApplicationDeadline());
    }

    private PlacementResponse toPlacementResponse(PlacementEntity placement) {
        return PlacementResponse.builder()
                .id(placement.getId())
                .companyName(placement.getCompanyName())
                .title(placement.getTitle())
                .description(placement.getDescription())
                .location(placement.getLocation())
                .type(placement.getType())
                .compensation(placement.getCompensation())
                .eligibilityCriteria(placement.getEligibilityCriteria())
                .applicationDeadline(placement.getApplicationDeadline())
                .status(placement.getStatus())
                .createdAt(placement.getCreatedAt())
                .updatedAt(placement.getUpdatedAt())
                .build();
    }

    private PlacementApplicationResponse toApplicationResponse(
            PlacementApplicationEntity application) {
        return PlacementApplicationResponse.builder()
                .id(application.getId())
                .placementId(application.getPlacement().getId())
                .companyName(application.getPlacement().getCompanyName())
                .placementTitle(application.getPlacement().getTitle())
                .status(application.getStatus())
                .appliedAt(application.getAppliedAt())
                .build();
    }

    private PlacementApplicantResponse toApplicantResponse(
            PlacementApplicationEntity application) {
        UserEntity user = application.getUser();
        return PlacementApplicantResponse.builder()
                .applicationId(application.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .collegeEmail(user.getCollegeEmail())
                .rollNumber(user.getRollNumber())
                .branch(user.getBranch())
                .academicYear(user.getAcademicYear())
                .status(application.getStatus())
                .appliedAt(application.getAppliedAt())
                .build();
    }

    private boolean isUniqueConstraintViolation(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof SQLException sqlException
                    && "23505".equals(sqlException.getSQLState())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private org.springframework.data.domain.Pageable pageable(
            int page,
            int size,
            Sort.Order primaryOrder,
            Sort.Order secondaryOrder) {
        return Pagination.pageRequest(
                page,
                size,
                Sort.by(primaryOrder, secondaryOrder)
        );
    }
}
