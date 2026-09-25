import { baseApi } from "../baseApi";

export type CmsMedia = {
  url?: string | null;
  alternativeText?: string | null;
  width?: number | null;
  height?: number | null;
  formats?: Record<string, { url?: string | null; width?: number | null; height?: number | null }>;
};

export type CmsLinkItem = { Label?: string; URL?: string };
export type CmsSocialLink = { Platform?: string; URL?: string };
export type CmsFeatureTab = {
  Title?: string;
  Description?: string;
  Preview_Image?: CmsMedia | null;
  Icon?: string;
};
export type CmsGridCard = {
  Title?: string;
  Description?: string;
  Graphic_Image?: CmsMedia | null;
  Layout_Type?: string;
};
export type CmsCircularCard = {
  Title?: string;
  Description?: string;
  Step_Number?: string;
};
export type CmsSecurityCard = {
  Title?: string;
  Description?: string;
  Image?: CmsMedia | null;
};
export type CmsBenefitCard = {
  Title?: string;
  Description?: string;
  icon?: string;
};
export type CmsFaqItem = { Question?: string; Answer?: string };
export type CmsSeo = {
  Meta_Title?: string;
  Meta_Description?: string;
  Canonical_URL?: string;
  OG_Title?: string;
  OG_Description?: string;
  OG_Image?: CmsMedia | null;
  Twitter_Card?: string;
  Robots?: string;
};

export type LandingPageContent = {
  Header_Logo?: CmsMedia | null;
  Header_Nav_Links?: CmsLinkItem[];
  Header_CTA_Text?: string;
  HeroTitle?: string;
  Hero_Subtitle?: string;
  Hero_Cta_Text?: string;
  Hero_Image?: CmsMedia | null;
  Features_Section_Heading?: string;
  Features_Tabs_List?: CmsFeatureTab[];
  Customization_Title?: string;
  Customization_Description?: string;
  Customization_Cards?: CmsGridCard[];
  Communication_Title?: string;
  Communication_Description?: string;
  Communication_Cta_Text?: string;
  Communication_Cards?: CmsCircularCard[];
  Security_Heading?: string;
  Security_Description?: string;
  Security_Cta_Text?: string;
  Security_Cards?: CmsSecurityCard[];
  Benefits_Heading?: string;
  Benefits_Cards?: CmsBenefitCard[];
  FAQ_Heading?: string;
  Faq_List?: CmsFaqItem[];
  Bottom_Cta_Heading?: string;
  Bottom_Cta_Description?: string;
  Bottom_Cta_Button_Text?: string;
  Footer_Logo?: CmsMedia | null;
  Footer_Nav_Links?: CmsLinkItem[];
  Footer_Socials?: CmsSocialLink[];
  Copyright_Text?: string;
  Footer_Legal_Links?: CmsLinkItem[];
  SEO?: CmsSeo | null;
};

export type CmsGlobalSettingsContent = {
  Site_Name?: string;
  Landing_Domain?: string;
  LearningHub_Domain?: string;
  Default_OG_Image?: CmsMedia | null;
  Twitter_Handle?: string;
  Organization_Name?: string;
  Organization_Logo?: CmsMedia | null;
  Organization_URL?: string;
  Google_Site_Verification?: string;
  Landing_SEO?: CmsSeo | null;
  LearningHub_SEO?: CmsSeo | null;
};

export type CmsLandingPageAdmin = {
  id: number;
  draftContent: LandingPageContent;
  publishedContent: LandingPageContent | null;
  publishedAt: string | null;
  updatedAt: string | null;
  updatedByAuthId: number | null;
  hasUnpublishedChanges: boolean;
};

export type CmsGlobalSettingsAdmin = {
  id: number;
  content: CmsGlobalSettingsContent;
  updatedAt: string | null;
  updatedByAuthId: number | null;
};

export type CmsMediaUploadResult = {
  id: number;
  url: string;
  alternativeText?: string | null;
  width?: number | null;
  height?: number | null;
  filename?: string;
  mimeType?: string;
  sizeBytes?: number;
};

function asObject(value: unknown): Record<string, unknown> {
  return value && typeof value === "object" && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : {};
}

function normalizeLandingAdmin(payload: unknown): CmsLandingPageAdmin {
  const root = asObject(payload);
  return {
    id: Number(root.id) || 0,
    draftContent: asObject(root.draftContent) as LandingPageContent,
    publishedContent: root.publishedContent
      ? (asObject(root.publishedContent) as LandingPageContent)
      : null,
    publishedAt: (root.publishedAt as string) || null,
    updatedAt: (root.updatedAt as string) || null,
    updatedByAuthId: root.updatedByAuthId == null ? null : Number(root.updatedByAuthId),
    hasUnpublishedChanges: Boolean(root.hasUnpublishedChanges),
  };
}

function normalizeGlobalAdmin(payload: unknown): CmsGlobalSettingsAdmin {
  const root = asObject(payload);
  return {
    id: Number(root.id) || 0,
    content: asObject(root.content) as CmsGlobalSettingsContent,
    updatedAt: (root.updatedAt as string) || null,
    updatedByAuthId: root.updatedByAuthId == null ? null : Number(root.updatedByAuthId),
  };
}

export type CmsLearningHubArticleContent = {
  title?: string;
  slug?: string;
  breadcrumb?: string;
  Description?: string;
  Chapter?: string;
  Section?: string;
  Order?: number;
  Category_Tag?: string;
  Is_Featured?: boolean;
  Reading_Time?: number;
  Publish_Date?: string;
  Card_Image?: CmsMedia | null;
  Content?: unknown;
  Related_Links?: Array<{ label?: string; url?: string; link_type?: string }>;
  SEO?: CmsSeo | null;
};

export type CmsLearningHubArticleAdmin = {
  id: number;
  title: string;
  slug: string;
  draftContent: CmsLearningHubArticleContent;
  publishedContent: CmsLearningHubArticleContent | null;
  publishedAt: string | null;
  updatedAt: string | null;
  sortOrder: number;
  chapter: string | null;
  sectionName: string | null;
  isFeatured: boolean;
  isPublished: boolean;
  hasUnpublishedChanges: boolean;
};

function normalizeLhArticle(payload: unknown): CmsLearningHubArticleAdmin {
  const root = asObject(payload);
  return {
    id: Number(root.id) || 0,
    title: String(root.title || ""),
    slug: String(root.slug || ""),
    draftContent: asObject(root.draftContent) as CmsLearningHubArticleContent,
    publishedContent: root.publishedContent
      ? (asObject(root.publishedContent) as CmsLearningHubArticleContent)
      : null,
    publishedAt: (root.publishedAt as string) || null,
    updatedAt: (root.updatedAt as string) || null,
    sortOrder: Number(root.sortOrder) || 0,
    chapter: (root.chapter as string) || null,
    sectionName: (root.sectionName as string) || null,
    isFeatured: Boolean(root.isFeatured),
    isPublished: Boolean(root.isPublished),
    hasUnpublishedChanges: Boolean(root.hasUnpublishedChanges),
  };
}

export const superAdminCmsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getCmsLandingPage: builder.query<CmsLandingPageAdmin, void>({
      query: () => "/api/v1/super-admin/cms/landing-page",
      transformResponse: normalizeLandingAdmin,
      providesTags: ["SuperAdminCmsLanding"],
    }),
    saveCmsLandingPageDraft: builder.mutation<CmsLandingPageAdmin, LandingPageContent>({
      query: (draftContent) => ({
        url: "/api/v1/super-admin/cms/landing-page",
        method: "PUT",
        body: { draftContent },
      }),
      transformResponse: normalizeLandingAdmin,
      invalidatesTags: ["SuperAdminCmsLanding"],
    }),
    publishCmsLandingPage: builder.mutation<CmsLandingPageAdmin, void>({
      query: () => ({
        url: "/api/v1/super-admin/cms/landing-page/publish",
        method: "POST",
      }),
      transformResponse: normalizeLandingAdmin,
      invalidatesTags: ["SuperAdminCmsLanding"],
    }),
    getCmsGlobalSettings: builder.query<CmsGlobalSettingsAdmin, void>({
      query: () => "/api/v1/super-admin/cms/global-settings",
      transformResponse: normalizeGlobalAdmin,
      providesTags: ["SuperAdminCmsSettings"],
    }),
    saveCmsGlobalSettings: builder.mutation<CmsGlobalSettingsAdmin, CmsGlobalSettingsContent>({
      query: (content) => ({
        url: "/api/v1/super-admin/cms/global-settings",
        method: "PUT",
        body: { content },
      }),
      transformResponse: normalizeGlobalAdmin,
      invalidatesTags: ["SuperAdminCmsSettings"],
    }),
    uploadCmsMedia: builder.mutation<CmsMediaUploadResult, { file: File; altText?: string }>({
      query: ({ file, altText }) => {
        const form = new FormData();
        form.append("file", file);
        if (altText) form.append("altText", altText);
        return {
          url: "/api/v1/super-admin/cms/media",
          method: "POST",
          body: form,
        };
      },
      transformResponse: (payload: unknown) => {
        const root = asObject(payload);
        return {
          id: Number(root.id) || 0,
          url: String(root.url || ""),
          alternativeText: (root.alternativeText as string) || null,
          width: root.width == null ? null : Number(root.width),
          height: root.height == null ? null : Number(root.height),
          filename: root.filename as string | undefined,
          mimeType: root.mimeType as string | undefined,
          sizeBytes: root.sizeBytes == null ? undefined : Number(root.sizeBytes),
        };
      },
    }),
    getCmsLearningHubArticles: builder.query<CmsLearningHubArticleAdmin[], void>({
      query: () => "/api/v1/super-admin/cms/learning-hubs",
      transformResponse: (payload: unknown) => {
        if (!Array.isArray(payload)) return [];
        return payload.map(normalizeLhArticle);
      },
      providesTags: ["SuperAdminCmsLearningHub"],
    }),
    getCmsLearningHubArticle: builder.query<CmsLearningHubArticleAdmin, number>({
      query: (id) => `/api/v1/super-admin/cms/learning-hubs/${id}`,
      transformResponse: normalizeLhArticle,
      providesTags: (_r, _e, id) => [{ type: "SuperAdminCmsLearningHub", id }],
    }),
    createCmsLearningHubArticle: builder.mutation<
      CmsLearningHubArticleAdmin,
      { title: string; slug: string; draftContent: CmsLearningHubArticleContent }
    >({
      query: (body) => ({
        url: "/api/v1/super-admin/cms/learning-hubs",
        method: "POST",
        body,
      }),
      transformResponse: normalizeLhArticle,
      invalidatesTags: ["SuperAdminCmsLearningHub"],
    }),
    updateCmsLearningHubArticle: builder.mutation<
      CmsLearningHubArticleAdmin,
      { id: number; title: string; slug: string; draftContent: CmsLearningHubArticleContent }
    >({
      query: ({ id, ...body }) => ({
        url: `/api/v1/super-admin/cms/learning-hubs/${id}`,
        method: "PUT",
        body,
      }),
      transformResponse: normalizeLhArticle,
      invalidatesTags: ["SuperAdminCmsLearningHub"],
    }),
    publishCmsLearningHubArticle: builder.mutation<CmsLearningHubArticleAdmin, number>({
      query: (id) => ({
        url: `/api/v1/super-admin/cms/learning-hubs/${id}/publish`,
        method: "POST",
      }),
      transformResponse: normalizeLhArticle,
      invalidatesTags: ["SuperAdminCmsLearningHub"],
    }),
    deleteCmsLearningHubArticle: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/super-admin/cms/learning-hubs/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: ["SuperAdminCmsLearningHub"],
    }),
  }),
});

export const {
  useGetCmsLandingPageQuery,
  useSaveCmsLandingPageDraftMutation,
  usePublishCmsLandingPageMutation,
  useGetCmsGlobalSettingsQuery,
  useSaveCmsGlobalSettingsMutation,
  useUploadCmsMediaMutation,
  useGetCmsLearningHubArticlesQuery,
  useGetCmsLearningHubArticleQuery,
  useCreateCmsLearningHubArticleMutation,
  useUpdateCmsLearningHubArticleMutation,
  usePublishCmsLearningHubArticleMutation,
  useDeleteCmsLearningHubArticleMutation,
} = superAdminCmsApi;
