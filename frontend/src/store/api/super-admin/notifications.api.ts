import { baseApi } from "../baseApi";
import * as Shared from "./shared";

export const superAdminNotificationsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getSuperAdminEmailTemplates: builder.query<Shared.SuperAdminEmailTemplate[], void>({
      query: () => "/api/v1/super-admin/email-templates",
      transformResponse: (payload) =>
        Shared.getArrayPayload(payload)
          .map((entry) => Shared.mapSuperAdminEmailTemplate(entry))
          .filter(Boolean) as Shared.SuperAdminEmailTemplate[],
    }),
    getSuperAdminEmailTemplateByKey: builder.query<Shared.SuperAdminEmailTemplate, string>({
      query: (templateKey) =>
        `/api/v1/super-admin/email-templates/${encodeURIComponent(templateKey)}`,
      transformResponse: (payload) => {
        const template = Shared.mapSuperAdminEmailTemplate(Shared.extractRoot(payload));
        return (
          template ?? {
            id: 0,
            templateKey: "",
            subjectTemplate: "",
            bodyTemplate: "",
            isActive: true,
          }
        );
      },
    }),
    updateSuperAdminEmailTemplateByKey: builder.mutation<
      Shared.SuperAdminEmailTemplate,
      Shared.UpdateSuperAdminEmailTemplatePayload
    >({
      query: ({ templateKey, body }) => ({
        url: `/api/v1/super-admin/email-templates/${encodeURIComponent(templateKey)}`,
        method: "PUT",
        body,
      }),
      transformResponse: (payload) => {
        const template = Shared.mapSuperAdminEmailTemplate(Shared.extractRoot(payload));
        return (
          template ?? {
            id: 0,
            templateKey: "",
            subjectTemplate: "",
            bodyTemplate: "",
            isActive: true,
          }
        );
      },
    }),
    getSuperAdminNotificationTemplates: builder.query<
      Shared.SuperAdminNotificationTemplate[],
      void
    >({
      query: () => "/api/v1/super-admin/notifications/templates",
      transformResponse: (payload) =>
        Shared.getArrayPayload(payload)
          .map((entry) => Shared.mapSuperAdminNotificationTemplate(entry))
          .filter(Boolean) as Shared.SuperAdminNotificationTemplate[],
    }),
    updateSuperAdminNotificationTemplateByKey: builder.mutation<
      Shared.SuperAdminNotificationTemplate,
      Shared.UpdateSuperAdminNotificationTemplatePayload
    >({
      query: ({ templateKey, body }) => ({
        url: `/api/v1/super-admin/notifications/templates/${encodeURIComponent(templateKey)}`,
        method: "PUT",
        body,
      }),
      transformResponse: (payload) => {
        const template = Shared.mapSuperAdminNotificationTemplate(Shared.extractRoot(payload));
        return (
          template ?? {
            id: 0,
            templateKey: "",
            subjectTemplate: "",
            bodyTemplate: "",
            isActive: true,
          }
        );
      },
    }),
    deleteSuperAdminNotificationTemplateByKey: builder.mutation<void, string>({
      query: (templateKey) => ({
        url: `/api/v1/super-admin/notifications/templates/${encodeURIComponent(templateKey)}`,
        method: "DELETE",
      }),
    }),
    getSuperAdminNotificationHistory: builder.query<
      Shared.SuperAdminNotificationHistoryItem[],
      Shared.SuperAdminNotificationHistoryFilters | void
    >({
      query: (filters) => {
        const query = new URLSearchParams();
        if (filters?.orgId !== undefined && String(filters.orgId).trim()) {
          query.set("orgId", String(filters.orgId).trim());
        }
        if (typeof filters?.page === "number" && Number.isFinite(filters.page)) {
          query.set("page", String(filters.page));
        }
        if (typeof filters?.size === "number" && Number.isFinite(filters.size)) {
          query.set("size", String(filters.size));
        }
        const qs = query.toString();
        return `/api/v1/super-admin/notifications/history${qs ? `?${qs}` : ""}`;
      },
      transformResponse: (payload) =>
        Shared.getArrayPayload(payload)
          .map((entry) => Shared.mapSuperAdminNotificationHistoryItem(entry))
          .filter(Boolean) as Shared.SuperAdminNotificationHistoryItem[],
      providesTags: ["NotificationHistory"],
    }),
    getSuperAdminUnreadNotificationCount: builder.query<number, { orgId?: string | number } | void>({
      query: (params) => {
        const query = new URLSearchParams();
        if (params?.orgId !== undefined && String(params.orgId).trim()) {
          query.set("orgId", String(params.orgId).trim());
        }
        const qs = query.toString();
        return `/api/v1/super-admin/notifications/unread-count${qs ? `?${qs}` : ""}`;
      },
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        if (typeof root === "number") return root;
        // In case the API wraps the count in an object like { count: 5 }
        if (typeof root === "object" && root !== null && "count" in root) {
          return Number(root.count);
        }
        return Number(root) || 0;
      },
      providesTags: [{ type: "NotificationHistory", id: "LIST" }], // we might use tags or just manual invalidate
    }),
    readSuperAdminNotification: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/super-admin/notifications/${id}/read`,
        method: "PATCH",
      }),
      invalidatesTags: ["NotificationHistory"],
    }),
    readAllSuperAdminNotifications: builder.mutation<void, { orgId?: string | number } | void>({
      query: (params) => {
        const query = new URLSearchParams();
        if (params?.orgId !== undefined && String(params.orgId).trim()) {
          query.set("orgId", String(params.orgId).trim());
        }
        const qs = query.toString();
        return {
          url: `/api/v1/super-admin/notifications/read-all${qs ? `?${qs}` : ""}`,
          method: "PATCH",
        };
      },
      invalidatesTags: ["NotificationHistory"],
    }),
    getSuperAdminNotificationTriggers: builder.query<
      Shared.SuperAdminNotificationTrigger[],
      void
    >({
      query: () => "/api/v1/super-admin/notifications/triggers",
      providesTags: [{ type: "NotificationTriggers", id: "LIST" }],
      transformResponse: (payload) =>
        Shared.getArrayPayload(payload)
          .map((entry) => Shared.mapSuperAdminNotificationTrigger(entry))
          .filter(Boolean) as Shared.SuperAdminNotificationTrigger[],
    }),
    getSuperAdminNotificationTriggersMetadata: builder.query<
      Shared.SuperAdminNotificationTriggerMetadata,
      void
    >({
      query: () => "/api/v1/super-admin/notifications/triggers/metadata",
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          eventTypes: Array.isArray(root.eventTypes)
            ? root.eventTypes.filter(
                (item): item is string => typeof item === "string" && item.trim().length > 0
              )
            : [],
          entityTypes: Array.isArray(root.entityTypes)
            ? root.entityTypes.filter(
                (item): item is string => typeof item === "string" && item.trim().length > 0
              )
            : [],
        };
      },
    }),
    createSuperAdminNotificationTrigger: builder.mutation<
      Shared.SuperAdminNotificationTrigger,
      Shared.SuperAdminNotificationTriggerPayload
    >({
      query: (body) => ({
        url: "/api/v1/super-admin/notifications/triggers",
        method: "POST",
        body,
      }),
      invalidatesTags: [{ type: "NotificationTriggers", id: "LIST" }],
      transformResponse: (payload) => {
        const trigger = Shared.mapSuperAdminNotificationTrigger(Shared.extractRoot(payload));
        if (!trigger) {
          throw new Error("Unable to create trigger.");
        }
        return trigger;
      },
    }),
    updateSuperAdminNotificationTrigger: builder.mutation<
      Shared.SuperAdminNotificationTrigger,
      { id: number; body: Partial<Shared.SuperAdminNotificationTriggerPayload> }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/super-admin/notifications/triggers/${id}`,
        method: "PUT",
        body,
      }),
      invalidatesTags: [{ type: "NotificationTriggers", id: "LIST" }],
      transformResponse: (payload) => {
        const trigger = Shared.mapSuperAdminNotificationTrigger(Shared.extractRoot(payload));
        if (!trigger) {
          throw new Error("Unable to update trigger.");
        }
        return trigger;
      },
    }),
    deleteSuperAdminNotificationTrigger: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/super-admin/notifications/triggers/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: [{ type: "NotificationTriggers", id: "LIST" }],
    }),
  }),
});

export const {
  useGetSuperAdminEmailTemplatesQuery,
  useGetSuperAdminEmailTemplateByKeyQuery,
  useUpdateSuperAdminEmailTemplateByKeyMutation,
  useGetSuperAdminNotificationTemplatesQuery,
  useUpdateSuperAdminNotificationTemplateByKeyMutation,
  useDeleteSuperAdminNotificationTemplateByKeyMutation,
  useGetSuperAdminNotificationHistoryQuery,
  useReadSuperAdminNotificationMutation,
  useReadAllSuperAdminNotificationsMutation,
  useGetSuperAdminNotificationTriggersQuery,
  useGetSuperAdminNotificationTriggersMetadataQuery,
  useCreateSuperAdminNotificationTriggerMutation,
  useUpdateSuperAdminNotificationTriggerMutation,
  useDeleteSuperAdminNotificationTriggerMutation,
  useGetSuperAdminUnreadNotificationCountQuery,
} = superAdminNotificationsApi;
