import { baseApi } from "../baseApi";
import * as Shared from "./shared";

export const superAdminAuditApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getAuditLogs: builder.query<Shared.AuditLogsResult, Shared.AuditLogFilters>({
      query: (filters) => {
        const params = new URLSearchParams();
        params.set("page", String(filters.page ?? 0));
        params.set("size", String(filters.size ?? 50));
        params.set("sort", filters.sort ?? "createdAt");
        params.set("order", filters.order ?? "desc");
        if (filters.mine !== undefined) params.set("mine", String(filters.mine));
        if (filters.authId) params.set("authId", String(filters.authId));
        if (filters.action) params.set("action", filters.action);
        if (filters.resourceType) params.set("resourceType", filters.resourceType);
        if (filters.resourceId) params.set("resourceId", filters.resourceId);
        if (filters.q) params.set("q", filters.q);
        if (filters.createdFrom) params.set("createdFrom", filters.createdFrom);
        if (filters.createdTo) params.set("createdTo", filters.createdTo);
        return `/api/v1/super-admin/audit-logs?${params.toString()}`;
      },
      transformResponse: (payload) => {
        // API returns a plain array (not paginated wrapper)
        const rows = Array.isArray(payload) ? payload : [];
        return {
          rows: rows
            .map((entry: unknown) => {
              if (!Shared.isRecord(entry)) return null;
              return {
                id: Shared.getNumber(entry, ["id"]),
                authId:
                  typeof entry.authId === "number" && Number.isFinite(entry.authId)
                    ? entry.authId
                    : null,
                action: Shared.getString(entry, ["action"]),
                logLevel: Shared.getString(entry, ["logLevel"]) || null,
                resourceType: Shared.getString(entry, ["resourceType"]),
                resourceId: Shared.getString(entry, ["resourceId"]),
                organisationId:
                  typeof entry.organisationId === "number" &&
                  Number.isFinite(entry.organisationId)
                    ? entry.organisationId
                    : null,
                organisationName:
                  Shared.getString(entry, ["organisationName", "organizationName"]) || null,
                actionSummary:
                  typeof entry.actionSummary === "string" ? entry.actionSummary : null,
                details: typeof entry.details === "string" ? entry.details : null,
                before: Shared.isRecord(entry.before) ? entry.before : null,
                after: Shared.isRecord(entry.after) ? entry.after : null,
                createdAt: Shared.getString(entry, ["createdAt"]),
              } as Shared.AuditLogEntry;
            })
            .filter(Boolean) as Shared.AuditLogEntry[],
          total: rows.length,
        };
      },
    }),
    getOrganisationAuditLogs: builder.query<Shared.AuditLogsResult, Shared.OrganisationAuditLogFilters>({
      query: (filters) => {
        const params = new URLSearchParams();
        params.set("page", String(filters.page ?? 0));
        params.set("size", String(filters.size ?? 50));
        params.set("sort", filters.sort ?? "createdAt");
        params.set("order", filters.order ?? "desc");
        if (filters.action) params.set("action", filters.action);
        if (filters.resourceType) params.set("resourceType", filters.resourceType);
        if (filters.q) params.set("q", filters.q);
        if (filters.createdFrom) params.set("createdFrom", filters.createdFrom);
        if (filters.createdTo) params.set("createdTo", filters.createdTo);
        return `/api/v1/super-admin/organisations/${filters.id}/audit-logs?${params.toString()}`;
      },
      transformResponse: (payload) => {
        const rows = Array.isArray(payload) ? payload : Shared.getArrayPayload(payload);
        return {
          rows: rows
            .map((entry: unknown) => {
              if (!Shared.isRecord(entry)) return null;
              return {
                id: Shared.getNumber(entry, ["id"]),
                authId:
                  typeof entry.authId === "number" && Number.isFinite(entry.authId)
                    ? entry.authId
                    : null,
                action: Shared.getString(entry, ["action"]),
                logLevel: Shared.getString(entry, ["logLevel"]) || null,
                resourceType: Shared.getString(entry, ["resourceType"]),
                resourceId: Shared.getString(entry, ["resourceId"]),
                organisationId:
                  typeof entry.organisationId === "number" &&
                  Number.isFinite(entry.organisationId)
                    ? entry.organisationId
                    : null,
                organisationName:
                  Shared.getString(entry, ["organisationName", "organizationName"]) || null,
                actionSummary:
                  typeof entry.actionSummary === "string" ? entry.actionSummary : null,
                details: typeof entry.details === "string" ? entry.details : null,
                before: Shared.isRecord(entry.before) ? entry.before : null,
                after: Shared.isRecord(entry.after) ? entry.after : null,
                createdAt: Shared.getString(entry, ["createdAt"]),
              } as Shared.AuditLogEntry;
            })
            .filter(Boolean) as Shared.AuditLogEntry[],
          total: rows.length,
        };
      },
    }),
  }),
});

export const {
  useGetAuditLogsQuery,
  useGetOrganisationAuditLogsQuery,
} = superAdminAuditApi;
