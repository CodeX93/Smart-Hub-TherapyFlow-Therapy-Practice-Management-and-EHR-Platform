package com.smart.therapy.flow.cms.controller;

import com.smart.therapy.flow.cms.dto.CmsPublicDataResponse;
import com.smart.therapy.flow.cms.dto.CmsPublicListResponse;
import com.smart.therapy.flow.cms.entity.CmsMediaAsset;
import com.smart.therapy.flow.cms.service.CmsLearningHubService;
import com.smart.therapy.flow.cms.service.CmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;

@RestController
@RequestMapping("/api/v1/public/cms")
@RequiredArgsConstructor
@Tag(name = "Public CMS", description = "Public read APIs for TherapyFlow marketing sites")
public class PublicCmsController {

    private final CmsService cmsService;
    private final CmsLearningHubService learningHubService;

    @GetMapping("/landing-page")
    @Operation(summary = "Get published landing page content")
    public ResponseEntity<CmsPublicDataResponse> getLandingPage() {
        return ResponseEntity.ok(cmsService.getPublishedLandingPage());
    }

    @GetMapping("/global-settings")
    @Operation(summary = "Get global site settings")
    public ResponseEntity<CmsPublicDataResponse> getGlobalSettings() {
        return ResponseEntity.ok(cmsService.getPublishedGlobalSettings());
    }

    @GetMapping("/learning-hubs")
    @Operation(summary = "List published Learning Hub articles")
    public ResponseEntity<CmsPublicListResponse> listLearningHubs(
            @RequestParam(name = "featured", required = false, defaultValue = "false") boolean featured
    ) {
        return ResponseEntity.ok(learningHubService.listPublished(featured));
    }

    @GetMapping("/learning-hubs/slug/{slug}")
    @Operation(summary = "Get published Learning Hub article by slug")
    public ResponseEntity<CmsPublicDataResponse> getLearningHubBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(learningHubService.getPublishedBySlug(slug));
    }

    @GetMapping("/media/{id}")
    @Operation(summary = "Stream a CMS media asset")
    public ResponseEntity<InputStreamResource> getMedia(@PathVariable Long id) throws Exception {
        CmsMediaAsset asset = cmsService.requireMediaAsset(id);
        InputStream stream = cmsService.openMediaStream(id);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (asset.getMimeType() != null && !asset.getMimeType().isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(asset.getMimeType());
            } catch (Exception ignored) {
                // keep octet-stream
            }
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .contentType(mediaType)
                .body(new InputStreamResource(stream));
    }
}
