package com.collegeeventhub.resource.controller;

import com.collegeeventhub.common.response.ApiResponse;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.util.Pagination;
import com.collegeeventhub.resource.dto.ResourceCreateRequest;
import com.collegeeventhub.resource.dto.ResourceResponse;
import com.collegeeventhub.resource.dto.ResourceUpdateRequest;
import com.collegeeventhub.resource.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ResourceResponse>>>
    getCatalog(
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_SIZE) int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            Authentication authentication) {
        Pagination.validate(page, size);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Resources fetched successfully",
                resourceService.getCatalog(
                        page,
                        size,
                        q,
                        category,
                        tag,
                        authentication.getName()
                )
        ));
    }

    @GetMapping("/manage")
    public ResponseEntity<ApiResponse<PageResponse<ResourceResponse>>>
    getManagedResources(
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_SIZE) int size,
            @RequestParam(required = false) Long organizationId,
            Authentication authentication) {
        Pagination.validate(page, size);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Managed resources fetched successfully",
                resourceService.getManagedResources(
                        page,
                        size,
                        organizationId,
                        authentication.getName()
                )
        ));
    }

    @GetMapping("/{resourceId}")
    public ResponseEntity<ApiResponse<ResourceResponse>> getResource(
            @PathVariable Long resourceId,
            Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Resource fetched successfully",
                resourceService.getResource(
                        resourceId,
                        authentication.getName()
                )
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ResourceResponse>> createResource(
            @Valid @RequestBody ResourceCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                true,
                "Resource created successfully",
                resourceService.createResource(
                        request,
                        authentication.getName()
                )
        ));
    }

    @PutMapping("/{resourceId}")
    public ResponseEntity<ApiResponse<ResourceResponse>> updateResource(
            @PathVariable Long resourceId,
            @Valid @RequestBody ResourceUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Resource updated successfully",
                resourceService.updateResource(
                        resourceId,
                        request,
                        authentication.getName()
                )
        ));
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<ApiResponse<ResourceResponse>> deactivateResource(
            @PathVariable Long resourceId,
            Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Resource deactivated successfully",
                resourceService.deactivateResource(
                        resourceId,
                        authentication.getName()
                )
        ));
    }
}
