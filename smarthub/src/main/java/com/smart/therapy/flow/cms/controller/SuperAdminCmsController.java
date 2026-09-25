package com.smart.therapy.flow.cms.controller;

import com.smart.therapy.flow.cms.dto.*;
import com.smart.therapy.flow.cms.service.CmsLearningHubService;
import com.smart.therapy.flow.cms.service.CmsService;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/super-admin/cms")
@RequiredArgsConstructor
@Tag(name = "Super Admin CMS", description = "Manage TherapyFlow marketing CMS content")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class SuperAdminCmsController {

    private final CmsService cmsService;
    private final CmsLearningHubService learningHubService;
    private final PlatformAuditService platformAuditService;

    @GetMapping("/landing-page")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get landing page draft + published", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<CmsLandingPageAdminResponse> getLandingPage() {
        return ResponseEntity.ok(cmsService.getLandingPageAdmin());
    }

    @PutMapping("/landing-page")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Save landing page draft", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<CmsLandingPageAdminResponse> saveLandingPage(
            @Valid @RequestBody CmsLandingPageUpdateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long authId = principal != null ? principal.getAuthId() : null;
        CmsLandingPageAdminResponse saved = cmsService.saveLandingPageDraft(request.getDraftContent(), authId);
        platformAuditService.log(authId, "CMS_LANDING_DRAFT_SAVED", "CmsLandingPage",
                String.valueOf(saved.getId()), "draft saved");
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/landing-page/publish")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Publish landing page draft", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<CmsLandingPageAdminResponse> publishLandingPage(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long authId = principal != null ? principal.getAuthId() : null;
        CmsLandingPageAdminResponse published = cmsService.publishLandingPage(authId);
        platformAuditService.log(authId, "CMS_LANDING_PUBLISHED", "CmsLandingPage",
                String.valueOf(published.getId()), "published_at=" + published.getPublishedAt());
        return ResponseEntity.ok(published);
    }

    @GetMapping("/global-settings")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get global site settings", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<CmsGlobalSettingsResponse> getGlobalSettings() {
        return ResponseEntity.ok(cmsService.getGlobalSettingsAdmin());
    }

    @PutMapping("/global-settings")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Update global site settings", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<CmsGlobalSettingsResponse> saveGlobalSettings(
            @Valid @RequestBody CmsGlobalSettingsUpdateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long authId = principal != null ? principal.getAuthId() : null;
        CmsGlobalSettingsResponse saved = cmsService.saveGlobalSettings(request.getContent(), authId);
        platformAuditService.log(authId, "CMS_GLOBAL_SETTINGS_UPDATED", "CmsGlobalSettings",
                String.valueOf(saved.getId()), "settings updated");
        return ResponseEntity.ok(saved);
    }

    @PostMapping(value = "/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Upload CMS media image", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<CmsMediaUploadResponse> uploadMedia(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "altText", required = false) String altText,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long authId = principal != null ? principal.getAuthId() : null;
        CmsMediaUploadResponse uploaded = cmsService.uploadMedia(file, altText, authId);
        platformAuditService.log(authId, "CMS_MEDIA_UPLOADED", "CmsMediaAsset",
                String.valueOf(uploaded.getId()), "filename=" + uploaded.getFilename());
        return ResponseEntity.ok(uploaded);
    }

    @GetMapping("/learning-hubs")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "List Learning Hub articles", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<java.util.List<CmsLearningHubArticleAdminResponse>> listLearningHubs() {
        return ResponseEntity.ok(learningHubService.listAdmin());
    }

    @GetMapping("/learning-hubs/{id}")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get Learning Hub article", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<CmsLearningHubArticleAdminResponse> getLearningHub(@PathVariable Long id) {
        return ResponseEntity.ok(learningHubService.getAdmin(id));
    }

    @PostMapping("/learning-hubs")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Create Learning Hub article", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<CmsLearningHubArticleAdminResponse> createLearningHub(
            @Valid @RequestBody CmsLearningHubArticleUpsertRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long authId = principal != null ? principal.getAuthId() : null;
        CmsLearningHubArticleAdminResponse created = learningHubService.create(
                request.getTitle(), request.getSlug(), request.getDraftContent(), authId);
        platformAuditService.log(authId, "CMS_LH_ARTICLE_CREATED", "CmsLearningHubArticle",
                String.valueOf(created.getId()), "slug=" + created.getSlug());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/learning-hubs/{id}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Update Learning Hub article draft", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<CmsLearningHubArticleAdminResponse> updateLearningHub(
            @PathVariable Long id,
            @Valid @RequestBody CmsLearningHubArticleUpsertRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long authId = principal != null ? principal.getAuthId() : null;
        CmsLearningHubArticleAdminResponse saved = learningHubService.updateDraft(
                id, request.getTitle(), request.getSlug(), request.getDraftContent(), authId);
        platformAuditService.log(authId, "CMS_LH_ARTICLE_DRAFT_SAVED", "CmsLearningHubArticle",
                String.valueOf(saved.getId()), "slug=" + saved.getSlug());
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/learning-hubs/{id}/publish")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Publish Learning Hub article", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<CmsLearningHubArticleAdminResponse> publishLearningHub(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long authId = principal != null ? principal.getAuthId() : null;
        CmsLearningHubArticleAdminResponse published = learningHubService.publish(id, authId);
        platformAuditService.log(authId, "CMS_LH_ARTICLE_PUBLISHED", "CmsLearningHubArticle",
                String.valueOf(published.getId()), "slug=" + published.getSlug());
        return ResponseEntity.ok(published);
    }

    @DeleteMapping("/learning-hubs/{id}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Delete Learning Hub article", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Void> deleteLearningHub(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long authId = principal != null ? principal.getAuthId() : null;
        learningHubService.delete(id);
        platformAuditService.log(authId, "CMS_LH_ARTICLE_DELETED", "CmsLearningHubArticle",
                String.valueOf(id), "deleted");
        return ResponseEntity.noContent().build();
    }
}
