package com.smart.therapy.flow.cms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.cms.dto.*;
import com.smart.therapy.flow.cms.entity.CmsGlobalSettings;
import com.smart.therapy.flow.cms.entity.CmsLandingPage;
import com.smart.therapy.flow.cms.entity.CmsMediaAsset;
import com.smart.therapy.flow.cms.repository.CmsGlobalSettingsRepository;
import com.smart.therapy.flow.cms.repository.CmsLandingPageRepository;
import com.smart.therapy.flow.cms.repository.CmsMediaAssetRepository;
import com.smart.therapy.flow.cms.util.CmsFrontendDeployTrigger;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.document.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.InputStream;
import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CmsService {

    private static final String EMPTY_JSON = "{}";

    private final CmsLandingPageRepository landingPageRepository;
    private final CmsGlobalSettingsRepository globalSettingsRepository;
    private final CmsMediaAssetRepository mediaAssetRepository;
    private final StorageService storageService;
    private final CmsFrontendDeployTrigger frontendDeployTrigger;
    private final ObjectMapper objectMapper;

    // ── Landing page ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CmsPublicDataResponse getPublishedLandingPage() {
        CmsLandingPage page = requireLandingPage();
        JsonNode published = parseJson(page.getPublishedContent());
        if (published == null || published.isNull() || (published.isObject() && published.isEmpty())) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "CMS_LANDING_NOT_PUBLISHED",
                    "Landing page has not been published yet");
        }
        return CmsPublicDataResponse.builder().data(published).build();
    }

    @Transactional(readOnly = true)
    public CmsLandingPageAdminResponse getLandingPageAdmin() {
        CmsLandingPage page = requireLandingPage();
        JsonNode draft = parseJson(page.getDraftContent());
        JsonNode published = parseJson(page.getPublishedContent());
        boolean dirty = !Objects.equals(
                page.getDraftContent() == null ? EMPTY_JSON : page.getDraftContent(),
                page.getPublishedContent() == null ? "" : page.getPublishedContent()
        );
        return CmsLandingPageAdminResponse.builder()
                .id(page.getId())
                .draftContent(draft)
                .publishedContent(published)
                .publishedAt(page.getPublishedAt())
                .updatedAt(page.getUpdatedAt())
                .updatedByAuthId(page.getUpdatedByAuthId())
                .hasUnpublishedChanges(dirty)
                .build();
    }

    @Transactional
    public CmsLandingPageAdminResponse saveLandingPageDraft(JsonNode draftContent, Long authId) {
        if (draftContent == null || !draftContent.isObject()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "CMS_INVALID_DRAFT",
                    "draftContent must be a JSON object");
        }
        CmsLandingPage page = requireLandingPage();
        page.setDraftContent(writeJson(draftContent));
        page.setUpdatedByAuthId(authId);
        landingPageRepository.save(page);
        return getLandingPageAdmin();
    }

    @Transactional
    public CmsLandingPageAdminResponse publishLandingPage(Long authId) {
        CmsLandingPage page = requireLandingPage();
        String draft = page.getDraftContent() == null ? EMPTY_JSON : page.getDraftContent();
        page.setPublishedContent(draft);
        page.setPublishedAt(Instant.now());
        page.setUpdatedByAuthId(authId);
        landingPageRepository.save(page);
        frontendDeployTrigger.scheduleLandingDeploy("api::cms.landing-page");
        return getLandingPageAdmin();
    }

    // ── Global settings ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CmsPublicDataResponse getPublishedGlobalSettings() {
        CmsGlobalSettings settings = requireGlobalSettings();
        return CmsPublicDataResponse.builder()
                .data(parseJson(settings.getContent()))
                .build();
    }

    @Transactional(readOnly = true)
    public CmsGlobalSettingsResponse getGlobalSettingsAdmin() {
        CmsGlobalSettings settings = requireGlobalSettings();
        return CmsGlobalSettingsResponse.builder()
                .id(settings.getId())
                .content(parseJson(settings.getContent()))
                .updatedAt(settings.getUpdatedAt())
                .updatedByAuthId(settings.getUpdatedByAuthId())
                .build();
    }

    @Transactional
    public CmsGlobalSettingsResponse saveGlobalSettings(JsonNode content, Long authId) {
        if (content == null || !content.isObject()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "CMS_INVALID_SETTINGS",
                    "content must be a JSON object");
        }
        CmsGlobalSettings settings = requireGlobalSettings();
        settings.setContent(writeJson(content));
        settings.setUpdatedByAuthId(authId);
        globalSettingsRepository.save(settings);
        frontendDeployTrigger.scheduleLandingDeploy("api::cms.global-settings");
        return getGlobalSettingsAdmin();
    }

    // ── Media ────────────────────────────────────────────────────────────────

    @Transactional
    public CmsMediaUploadResponse uploadMedia(MultipartFile file, String altText, Long authId) {
        if (file == null || file.isEmpty()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "CMS_MEDIA_EMPTY", "File is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "CMS_MEDIA_TYPE",
                    "Only image uploads are supported");
        }

        try {
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.bin";
            String storageKey = storageService.uploadFile(file, "cms", originalName);

            CmsMediaAsset asset = CmsMediaAsset.builder()
                    .storageKey(storageKey)
                    .publicUrl("") // set after save so we know the id
                    .filename(originalName)
                    .mimeType(contentType)
                    .sizeBytes(file.getSize())
                    .altText(altText)
                    .uploadedByAuthId(authId)
                    .build();
            mediaAssetRepository.save(asset);

            String publicUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/public/cms/media/")
                    .path(String.valueOf(asset.getId()))
                    .toUriString();
            asset.setPublicUrl(publicUrl);
            mediaAssetRepository.save(asset);

            return CmsMediaUploadResponse.builder()
                    .id(asset.getId())
                    .url(publicUrl)
                    .alternativeText(altText)
                    .filename(originalName)
                    .mimeType(contentType)
                    .sizeBytes(file.getSize())
                    .build();
        } catch (StoryApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("CMS media upload failed", e);
            throw new StoryApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CMS_MEDIA_UPLOAD_FAILED",
                    "Failed to upload media: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public CmsMediaAsset requireMediaAsset(Long id) {
        return mediaAssetRepository.findById(id)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "CMS_MEDIA_NOT_FOUND",
                        "Media asset not found: " + id));
    }

    @Transactional(readOnly = true)
    public InputStream openMediaStream(Long id) throws Exception {
        CmsMediaAsset asset = requireMediaAsset(id);
        return storageService.downloadFile(asset.getStorageKey());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private CmsLandingPage requireLandingPage() {
        return landingPageRepository.findAll().stream().findFirst()
                .orElseGet(() -> landingPageRepository.save(CmsLandingPage.builder()
                        .draftContent(EMPTY_JSON)
                        .build()));
    }

    private CmsGlobalSettings requireGlobalSettings() {
        return globalSettingsRepository.findAll().stream().findFirst()
                .orElseGet(() -> globalSettingsRepository.save(CmsGlobalSettings.builder()
                        .content(EMPTY_JSON)
                        .build()));
    }

    private JsonNode parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            log.warn("Failed to parse CMS JSON: {}", e.getMessage());
            return objectMapper.createObjectNode();
        }
    }

    private String writeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "CMS_JSON_WRITE_FAILED",
                    "Failed to serialize content");
        }
    }
}
