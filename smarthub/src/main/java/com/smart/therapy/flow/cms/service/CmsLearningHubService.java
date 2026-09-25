package com.smart.therapy.flow.cms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smart.therapy.flow.cms.dto.CmsLearningHubArticleAdminResponse;
import com.smart.therapy.flow.cms.dto.CmsPublicDataResponse;
import com.smart.therapy.flow.cms.dto.CmsPublicListResponse;
import com.smart.therapy.flow.cms.entity.CmsLearningHubArticle;
import com.smart.therapy.flow.cms.repository.CmsLearningHubArticleRepository;
import com.smart.therapy.flow.common.exception.StoryApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CmsLearningHubService {

    private static final String EMPTY_JSON = "{}";

    private final CmsLearningHubArticleRepository articleRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public CmsPublicListResponse listPublished(boolean featuredOnly) {
        List<CmsLearningHubArticle> articles = articleRepository.findByIsPublishedTrueOrderBySortOrderAscIdAsc();
        List<JsonNode> data = new ArrayList<>();
        for (CmsLearningHubArticle article : articles) {
            if (featuredOnly && !Boolean.TRUE.equals(article.getIsFeatured())) {
                continue;
            }
            JsonNode node = toPublicNode(article);
            if (node != null) {
                data.add(node);
            }
        }
        return CmsPublicListResponse.builder().data(data).build();
    }

    @Transactional(readOnly = true)
    public CmsPublicDataResponse getPublishedBySlug(String slug) {
        CmsLearningHubArticle article = articleRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "CMS_ARTICLE_NOT_FOUND",
                        "Article not found: " + slug));
        return CmsPublicDataResponse.builder().data(toPublicNode(article)).build();
    }

    @Transactional(readOnly = true)
    public List<CmsLearningHubArticleAdminResponse> listAdmin() {
        return articleRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(this::toAdminResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CmsLearningHubArticleAdminResponse getAdmin(Long id) {
        return toAdminResponse(requireArticle(id));
    }

    @Transactional
    public CmsLearningHubArticleAdminResponse create(String title, String slug, JsonNode draftContent, Long authId) {
        String normalizedSlug = normalizeSlug(slug);
        if (articleRepository.existsBySlug(normalizedSlug)) {
            throw new StoryApiException(HttpStatus.CONFLICT, "CMS_SLUG_EXISTS", "Slug already exists: " + normalizedSlug);
        }
        JsonNode content = requireObject(draftContent);
        CmsLearningHubArticle article = CmsLearningHubArticle.builder()
                .title(title.trim())
                .slug(normalizedSlug)
                .draftContent(writeJson(mergeIdentityFields(content, title.trim(), normalizedSlug)))
                .build();
        applyIndexedFields(article, parseJson(article.getDraftContent()));
        article.setUpdatedByAuthId(authId);
        articleRepository.save(article);
        return toAdminResponse(article);
    }

    @Transactional
    public CmsLearningHubArticleAdminResponse updateDraft(Long id, String title, String slug, JsonNode draftContent, Long authId) {
        CmsLearningHubArticle article = requireArticle(id);
        String normalizedSlug = normalizeSlug(slug);
        if (articleRepository.existsBySlugAndIdNot(normalizedSlug, id)) {
            throw new StoryApiException(HttpStatus.CONFLICT, "CMS_SLUG_EXISTS", "Slug already exists: " + normalizedSlug);
        }
        JsonNode content = requireObject(draftContent);
        article.setTitle(title.trim());
        article.setSlug(normalizedSlug);
        article.setDraftContent(writeJson(mergeIdentityFields(content, title.trim(), normalizedSlug)));
        applyIndexedFields(article, parseJson(article.getDraftContent()));
        article.setUpdatedByAuthId(authId);
        articleRepository.save(article);
        return toAdminResponse(article);
    }

    @Transactional
    public CmsLearningHubArticleAdminResponse publish(Long id, Long authId) {
        CmsLearningHubArticle article = requireArticle(id);
        String draft = article.getDraftContent() == null ? EMPTY_JSON : article.getDraftContent();
        article.setPublishedContent(draft);
        article.setPublishedAt(Instant.now());
        article.setIsPublished(true);
        applyIndexedFields(article, parseJson(draft));
        article.setUpdatedByAuthId(authId);
        articleRepository.save(article);
        return toAdminResponse(article);
    }

    @Transactional
    public void delete(Long id) {
        articleRepository.delete(requireArticle(id));
    }

    /** Upsert used by migration scripts. */
    @Transactional
    public CmsLearningHubArticleAdminResponse upsertMigrated(String title, String slug, JsonNode content, boolean publish) {
        String normalizedSlug = normalizeSlug(slug);
        JsonNode merged = mergeIdentityFields(requireObject(content), title.trim(), normalizedSlug);
        CmsLearningHubArticle article = articleRepository.findBySlug(normalizedSlug).orElseGet(CmsLearningHubArticle::new);
        article.setTitle(title.trim());
        article.setSlug(normalizedSlug);
        article.setDraftContent(writeJson(merged));
        applyIndexedFields(article, merged);
        if (publish) {
            article.setPublishedContent(writeJson(merged));
            article.setPublishedAt(Instant.now());
            article.setIsPublished(true);
        }
        articleRepository.save(article);
        return toAdminResponse(article);
    }

    private CmsLearningHubArticle requireArticle(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "CMS_ARTICLE_NOT_FOUND",
                        "Article not found: " + id));
    }

    private void applyIndexedFields(CmsLearningHubArticle article, JsonNode content) {
        if (content == null) return;
        if (content.hasNonNull("Order")) {
            article.setSortOrder(content.get("Order").asInt(0));
        } else if (content.hasNonNull("order")) {
            article.setSortOrder(content.get("order").asInt(0));
        }
        if (content.has("Chapter")) {
            article.setChapter(textOrNull(content.get("Chapter")));
        }
        if (content.has("Section")) {
            article.setSectionName(textOrNull(content.get("Section")));
        }
        if (content.has("Is_Featured")) {
            article.setIsFeatured(content.get("Is_Featured").asBoolean(false));
        }
    }

    private JsonNode toPublicNode(CmsLearningHubArticle article) {
        JsonNode published = parseJson(article.getPublishedContent());
        if (published == null || published.isNull() || (published.isObject() && published.isEmpty())) {
            return null;
        }
        ObjectNode node = published.isObject()
                ? ((ObjectNode) published).deepCopy()
                : objectMapper.createObjectNode();
        node.put("id", article.getId());
        if (!node.hasNonNull("title")) node.put("title", article.getTitle());
        if (!node.hasNonNull("slug")) node.put("slug", article.getSlug());
        if (!node.has("Order") && article.getSortOrder() != null) node.put("Order", article.getSortOrder());
        if (!node.has("Chapter") && article.getChapter() != null) node.put("Chapter", article.getChapter());
        if (!node.has("Section") && article.getSectionName() != null) node.put("Section", article.getSectionName());
        if (!node.has("Is_Featured")) node.put("Is_Featured", Boolean.TRUE.equals(article.getIsFeatured()));
        if (article.getPublishedAt() != null) {
            node.put("publishedAt", article.getPublishedAt().toString());
            node.put("Publish_Date", article.getPublishedAt().toString());
        }
        if (article.getUpdatedAt() != null) {
            node.put("updatedAt", article.getUpdatedAt().toString());
        }
        return node;
    }

    private CmsLearningHubArticleAdminResponse toAdminResponse(CmsLearningHubArticle article) {
        JsonNode draft = parseJson(article.getDraftContent());
        JsonNode published = parseJson(article.getPublishedContent());
        boolean dirty = !Objects.equals(
                article.getDraftContent() == null ? EMPTY_JSON : article.getDraftContent(),
                article.getPublishedContent() == null ? "" : article.getPublishedContent()
        );
        return CmsLearningHubArticleAdminResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .slug(article.getSlug())
                .draftContent(draft)
                .publishedContent(published)
                .publishedAt(article.getPublishedAt())
                .updatedAt(article.getUpdatedAt())
                .sortOrder(article.getSortOrder())
                .chapter(article.getChapter())
                .sectionName(article.getSectionName())
                .isFeatured(article.getIsFeatured())
                .isPublished(article.getIsPublished())
                .hasUnpublishedChanges(dirty)
                .build();
    }

    private ObjectNode mergeIdentityFields(JsonNode content, String title, String slug) {
        ObjectNode node = content.isObject()
                ? ((ObjectNode) content).deepCopy()
                : objectMapper.createObjectNode();
        node.put("title", title);
        node.put("slug", slug);
        if (!node.hasNonNull("breadcrumb")) {
            node.put("breadcrumb", slug);
        }
        return node;
    }

    private JsonNode requireObject(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "CMS_INVALID_CONTENT",
                    "draftContent must be a JSON object");
        }
        return node;
    }

    private String normalizeSlug(String slug) {
        if (!StringUtils.hasText(slug)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "CMS_INVALID_SLUG", "slug is required");
        }
        String normalized = slug.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (normalized.isBlank()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "CMS_INVALID_SLUG", "slug is invalid");
        }
        return normalized;
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) return null;
        String text = node.asText();
        return text.isBlank() ? null : text;
    }

    private JsonNode parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            log.warn("Failed to parse learning hub JSON: {}", e.getMessage());
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
