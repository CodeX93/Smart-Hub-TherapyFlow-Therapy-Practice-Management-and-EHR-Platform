
export interface BillingPlanPricingTier {
  minTherapists: number;
  maxTherapists: number;
  pricePerTherapistUsd: number;
  includedSupervisors: number;
  includedClients: number;
}

export interface BillingPlan {
  planCode: string;
  planName: string;
  description: string;
  basePrice: number;
  annualPrice: number;
  billingCycle: string;
  trialDays: number;
  providerPriceIdMonthly: string;
  providerPriceIdAnnual: string;
  status: string;
  therapistLimit: number | null;
  supervisorLimit: number | null;
  clientLimit: number | null;
  entitlements: Array<{
    featureCode: string;
    enabled: boolean;
    usageLimit: number | null;
  }>;
  pricingTiers: BillingPlanPricingTier[];
}

export interface CreateBillingPlanPayload {
  code: string;
  name: string;
  billingCycle: string;
  basePrice: number;
  annualPrice: number;
  description: string;
  trialDays: number;
  status: string;
  providerPriceIdMonthly: string;
  providerPriceIdAnnual: string;
}

export interface UpdateBillingPlanPayload {
  planName: string;
  body: {
    name: string;
    description: string;
    basePrice: number;
    annualPrice: number;
    billingCycle: string;
    trialDays: number;
    status: string;
    providerPriceIdMonthly: string;
    providerPriceIdAnnual: string;
  };
}

export interface PlanEntitlementItem {
  key: string;
  featureCode: string;
  enabled: boolean;
  usageLimit: number | null;
}

export interface PlanEntitlementsResponse {
  plan: string;
  items: PlanEntitlementItem[];
  features: PlanEntitlementItem[];
  updatedAt: string | null;
}

export interface PlanEntitlementFeaturePayload {
  key: string;
  enabled: boolean;
  usageLimit?: number | null;
}

export interface UpdatePlanEntitlementsPayload {
  planName: string;
  body: {
    features: PlanEntitlementFeaturePayload[];
  };
}

export function mapPlanEntitlementItem(entry: unknown, index: number): PlanEntitlementItem | null {
  if (!isRecord(entry)) return null;

  const usageLimitRaw =
    entry.usageLimit ?? entry.limit ?? entry.usage_limit ?? entry.featureLimit ?? null;
  const usageLimit =
    typeof usageLimitRaw === "number" && Number.isFinite(usageLimitRaw)
      ? usageLimitRaw
      : typeof usageLimitRaw === "string" && usageLimitRaw.trim()
        ? Number.parseFloat(usageLimitRaw)
        : null;

  const key =
    getString(entry, ["key", "featureCode", "code", "keyName"]) || `feature-${index + 1}`;

  return {
    key,
    featureCode: getString(entry, ["featureCode", "key", "code", "keyName"]) || key,
    enabled: entry.enabled === undefined ? true : Boolean(entry.enabled),
    usageLimit: usageLimit !== null && Number.isFinite(usageLimit) ? usageLimit : null,
  };
}

export function mapPlanEntitlementsResponse(payload: unknown): PlanEntitlementsResponse {
  const root = extractRoot(payload);
  const itemsSource = Array.isArray(root.items) ? root.items : [];
  const featuresSource = Array.isArray(root.features)
    ? root.features
    : Array.isArray(payload)
      ? payload
      : [];

  return {
    plan: getString(root, ["plan", "planCode", "planName", "code"]) || "",
    items: itemsSource
      .map((entry, index) => mapPlanEntitlementItem(entry, index))
      .filter(Boolean) as PlanEntitlementItem[],
    features: featuresSource
      .map((entry, index) => mapPlanEntitlementItem(entry, index))
      .filter(Boolean) as PlanEntitlementItem[],
    updatedAt: getString(root, ["updatedAt"]) || null,
  };
}

export interface TenantRoutingSettings {
  emailAutoRouting: boolean;
  pathBasedRouting: boolean;
  pathPrefix: string;
  orgIdentifier: string;
  updatedAt: string;
}

export interface ImpersonationPolicy {
  id?: number;
  enabled: boolean;
  requireReason: boolean;
  minReasonLength: number;
  maxDurationMinutes: number;
  allowCrossOrganisation: boolean;
  allowedRoles: string[];
  deniedRoles: string[];
  allowedOrgIds: number[];
  deniedOrgIds: number[];
  updatedByAuthId?: number;
  createdAt?: string;
  updatedAt?: string;
}

export type UpdateImpersonationPolicyPayload = Omit<
  ImpersonationPolicy,
  "id" | "updatedByAuthId" | "createdAt" | "updatedAt"
>;

export interface SuperAdminEmailTemplate {
  id: number;
  templateKey: string;
  subjectTemplate: string;
  bodyTemplate: string;
  isActive: boolean;
  updatedByAuthId?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface UpdateSuperAdminEmailTemplatePayload {
  templateKey: string;
  body: {
    subjectTemplate: string;
    bodyTemplate: string;
    active?: boolean;
  };
}

export interface SuperAdminNotificationTemplate {
  id: number;
  templateKey: string;
  subjectTemplate: string;
  bodyTemplate: string;
  isActive: boolean;
  updatedByAuthId?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface UpdateSuperAdminNotificationTemplatePayload {
  templateKey: string;
  body: {
    subjectTemplate: string;
    bodyTemplate: string;
    active: boolean;
  };
}

export interface SuperAdminNotificationHistoryItem {
  id: number;
  jobType: string;
  title: string;
  message: string;
  targetJson: string;
  channel: string;
  status: string;
  scheduledAt: string;
  sentAt: string;
  errorMessage: string;
  createdByAuthId: number | null;
  createdAt: string;
  updatedAt: string;
  isRead: boolean;
  readAt: string | null;
}

export interface SuperAdminNotificationHistoryFilters {
  orgId?: string | number;
  page?: number;
  size?: number;
}

export interface SuperAdminNotificationTrigger {
  id: number;
  name: string;
  description: string;
  eventType: string;
  entityType: string;
  conditionRules: string;
  recipientRules: string;
  priority: string;
  isScheduled: boolean;
  scheduleOffsetMinutes: number;
  batchWindowMinutes: number;
  maxBatchSize: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SuperAdminNotificationTriggerPayload {
  name: string;
  description: string;
  eventType: string;
  entityType?: string | null;
  conditionRules: string;
  recipientRules: string;
  priority: string;
  isScheduled: boolean;
  scheduleOffsetMinutes: number;
  batchWindowMinutes: number;
  maxBatchSize: number;
  isActive: boolean;
}

export interface SuperAdminNotificationTriggerMetadata {
  eventTypes: string[];
  entityTypes: string[];
}

export interface SuperAdminSecuritySettings {
  id?: number;
  enabled: boolean;
  requireReason: boolean;
  minReasonLength: number;
  maxDurationMinutes: number;
  allowCrossOrganisation: boolean;
  allowedRoles: string[];
  deniedRoles: string[];
  allowedOrgIds: number[];
  deniedOrgIds: number[];
  updatedByAuthId?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface AuditLogEntry {
  id: number;
  authId: number | null;
  action: string;
  logLevel?: string | null;
  resourceType: string;
  resourceId: string;
  organisationId?: number | null;
  organisationName?: string | null;
  actionSummary?: string | null;
  details: string | null;
  before: Record<string, unknown> | null;
  after: Record<string, unknown> | null;
  createdAt: string;
}

export interface AuditLogFilters {
  page: number;
  size: number;
  authId?: number;
  mine?: boolean;
  action?: string;
  resourceType?: string;
  resourceId?: string;
  q?: string;
  createdFrom?: string;
  createdTo?: string;
  sort?: string;
  order?: "asc" | "desc";
}

export interface AuditLogsResult {
  rows: AuditLogEntry[];
  total: number;
}

export interface OrganisationAuditLogFilters {
  id: number;
  page?: number;
  size?: number;
  action?: string;
  resourceType?: string;
  q?: string;
  createdFrom?: string;
  createdTo?: string;
  sort?: string;
  order?: "asc" | "desc";
}

export interface OrganisationListRow {
  id: string;
  name: string;
  slug: string;
  status: string;
  plan: string;
  users: number;
  createdAt: string;
  region: string;
  dataResidency: string;
}

export interface OrganisationListFilters {
  search: string;
  status: string;
  plan: string;
  createdFrom: string;
  createdTo: string;
  region: string;
  dataResidency: string;
  page: number;
  pageSize: number;
  sort: string;
  order: "asc" | "desc";
  exportData: boolean;
}

export interface OrganisationListResult {
  rows: OrganisationListRow[];
  currentPage: number;
  pageSize: number;
  totalPages: number;
  totalItems: number;
}

export interface OrganisationDetailsResult {
  id: number;
  name: string;
  slug: string;
  status: string;
  subdomain: string;
  schemaName: string;
  timezone: string;
  region: string;
  dataResidency: string;
  locale: string;
  logoUrl: string;
  /** The organisation's primary administrator, created at onboarding. */
  primaryAdminEmail: string;
  supportEmail: string;
  supportAddress: string;
  totalUserCount?: number | null;
  subscriptionDetails: {
    organisationId: number;
    subscriptionId: number;
    plan: string;
    status: string;
    billingCycle: string;
    priceAtTime: number;
    startAt: string;
    endAt: string;
    trialEndsAt: string;
    providerCustomerId: string;
    providerSubscriptionId: string;
    userLimits: {
      therapistLimit: number | null;
      supervisorLimit: number | null;
      clientLimit: number | null;
    };
    userUsage: {
      therapistUsers: number;
      supervisorUsers: number;
      clientUsers: number;
      totalUsers: number;
    };
  } | null;
  extra: Record<string, unknown>;
  createdAt: string;
}

export interface OrganisationFeatureState {
  enabled: boolean;
  usageLimit: number | null;
}

export type OrganisationFeaturesResponse = Record<string, OrganisationFeatureState>;

export interface OrganisationSettings {
  timezone: string;
  region: string;
  dataResidency: string;
  locale: string;
  logoUrl: string;
  brandPrimaryColor: string;
  brandSecondaryColor: string;
  brandAccentColor: string;
  supportEmail: string;
  supportAddress: string;
}

export interface OrganisationSchemaVersion {
  version: string;
  description?: string;
  updatedAt?: string;
}

export interface OrganisationSubscription {
  organisationId: number;
  subscriptionId: number;
  plan: string;
  status: string;
  billingCycle: string;
  trialDays: number;
  startAt: string;
  endAt: string;
  trialEndsAt: string;
  userLimits: {
    therapistLimit: number | null;
    supervisorLimit: number | null;
    clientLimit: number | null;
  };
  userUsage: {
    therapistUsers: number;
    supervisorUsers: number;
    clientUsers: number;
    totalUsers: number;
  };
}

export interface UpdateOrganisationSubscriptionPayload {
  id: number;
  body: {
    plan?: string;
    billingCycle?: string;
    trialDays?: number;
    effectiveDate?: string;
    prorate?: boolean;
    userLimits?: {
      therapistLimit?: number | null;
      supervisorLimit?: number | null;
      clientLimit?: number | null;
    };
  };
}

export interface CreateOrganisationPayload {
  name: string;
  slug: string;
  subdomain: string;
  primaryAdminEmail: string;
  primaryAdminFirstName: string;
  primaryAdminLastName: string;
  plan: string;
  billingCycle: string;
  trialDays: number;
  timezone: string;
  region: string;
  dataResidency: string;
  provisionTenant: boolean;
}

export interface UpdateOrganisationPayload {
  id: number;
  body: Record<string, unknown>;
}

export interface OrganisationUserListItem {
  id: string;
  name: string;
  email: string;
  role: string;
  status: string;
  lastLogin: string;
}

export interface OrganisationUsersQueryParams {
  id: number;
  search?: string;
  role?: string;
  status?: string;
  page?: number;
  pageSize?: number;
}

export interface OrganisationInvoiceListItem {
  id: string;
  invoiceId: string;
  invoiceNumericId?: number;
  period: string;
  dueDate: string;
  amount: number;
  currentBalance: number;
  status: string;
  paidAt: string;
  organisation: string;
}

export interface OrganisationInvoicesQueryParams {
  id: number;
  status?: string;
  sort?: string;
  page?: number;
  pageSize?: number;
}

export interface OrganisationAddOnListItem {
  purchaseId: number | null;
  featureCode: string;
  featureName: string;
  quantity: number;
  pricePerUnitAtTime: number;
  unitValue: number;
  billingCycle: string;
  startAt: string | null;
  endAt: string | null;
  /** @deprecated Prefer featureCode */
  id: string;
  /** @deprecated Prefer featureCode */
  code: string;
  /** @deprecated Prefer featureName */
  name: string;
  description: string;
  priceUsd: number;
  status: string;
  isActive: boolean;
}

export interface AssignOrganisationAddOnPayload {
  id: number;
  body: {
    featureCode: string;
    quantity: number;
  };
}

export interface UnassignOrganisationAddOnByFeaturePayload {
  organisationId: number;
  featureCode: string;
}

export interface UnassignOrganisationAddOnByPurchasePayload {
  organisationId: number;
  purchaseId: number;
}

export interface SuperAdminBillingInvoiceItem {
  id: string;
  invoiceId: string;
  invoiceNumericId?: number;
  organisation: string;
  organisationId?: number;
  subscriptionId?: number;
  plan?: string;
  period: string;
  dueDate: string;
  amount: number;
  currentBalance: number;
  status: string;
  totalPaid?: number;
  refundedAmount?: number;
  paidAt?: string;
  createdAt?: string;
  billingPeriodStart?: string;
  billingPeriodEnd?: string;
}

export interface SuperAdminBillingInvoicesQueryParams {
  orgId?: number;
  organisationId?: number;
  status?: string;
  sort?: string;
  page?: number;
  pageSize?: number;
}

export interface SuperAdminBillingInvoicesResult {
  items: SuperAdminBillingInvoiceItem[];
  page: number;
  pageSize: number;
  totalPages: number;
  totalItems: number;
}

export interface SuperAdminBillingInvoicesExportQueryParams {
  organisationId?: number;
  status?: string;
}

export interface DunningPolicyStep {
  day: number;
  action: string;
}

export interface DunningPolicy {
  steps: DunningPolicyStep[];
  gracePeriodDays: number;
  trialNoticeDays: number;
}

export interface ApplySuperAdminInvoiceRefundPayload {
  invoiceId: number;
  body: {
    amountUsd: number;
    reason: string;
  };
}

export interface ApplySuperAdminInvoiceCreditPayload {
  invoiceId: number;
  body: {
    amountUsd: number;
    reason: string;
  };
}

export interface SuperAdminInvoiceSendReminderResult {
  invoiceId: number;
  organisationId: number | null;
  emailsSent: number;
  emailsFailed: number;
  inAppNotificationsCreated: number;
  emailRecipients: string[];
  warning: string | null;
}

export interface SuperAdminBillingInvoiceAdjustment {
  adjustmentId: number;
  invoiceId: number;
  type: string;
  amount: number;
  reason: string;
  status: string;
  createdAt: string;
}

export interface SuperAdminBillingInvoiceDispute {
  disputeId: number;
  invoiceId: number;
  externalCaseId: string;
  status: string;
  amountUsd: number;
  reason: string;
  openedAt: string;
  resolvedAt: string | null;
}

export interface SuperAdminBillingInvoiceDetail {
  invoiceId: number;
  organisationId: number | null;
  organisationName: string;
  subscriptionId: number | null;
  planCode: string;
  planName: string;
  billingCycle: string;
  status: string;
  amount: number;
  outstandingBalance: number;
  totalPaid: number;
  refundedAmount: number;
  dueDate: string;
  billingPeriodStart: string;
  billingPeriodEnd: string;
  paidAt: string | null;
  createdAt: string;
  providerInvoiceId: string | null;
  providerChargeId: string | null;
  providerPaymentIntentId: string | null;
  adjustments: SuperAdminBillingInvoiceAdjustment[];
  disputes: SuperAdminBillingInvoiceDispute[];
}

export interface UpdateOrganisationFeaturesPayload {
  id: number;
  body: OrganisationFeaturesResponse;
}

export interface SuspendOrganisationPayload {
  reason: string;
}

export interface ReactivateOrganisationPayload {
  reason: string;
}

export interface TerminateOrganisationPayload {
  reason: string;
  retentionDays: number;
}

export interface OrganisationOperationResponse {
  success: boolean;
  organisationId: number;
  status: string;
  message: string;
  jobId?: number;
}

export interface PlanCatalogOption {
  planCode: string;
  planName: string;
  trialDays: number;
  billingCycles: string[];
  defaultBillingCycle: string;
  description: string;
}

export interface RevenueAnalyticsRow {
  month: string;
  mrr: number;
  arr: number;
  activeSubscriptions: number;
  endedSubscriptions: number;
  churnRatePct: number;
}

export interface RevenueAnalyticsResponse {
  months: number;
  generatedAt: string;
  rows: RevenueAnalyticsRow[];
}

export interface DashboardKpisResponse {
  activeTenants: number;
  totalEndUsers: number;
  mrr: number;
  churnThisMonth: number;
  platformUptimePercent: number;
  timezone?: string | null;
  currency?: string | null;
  lastUpdated?: string | null;
}

export interface DashboardOrganisationItem {
  name: string;
  planLabel: string;
  usersLabel: string;
}

export interface DashboardOrganisationsResponse {
  total: number;
  statuses: Array<{ label: string; count: number }>;
  tenants: DashboardOrganisationItem[];
}

export interface DashboardRevenuePlanItem {
  label: string;
  value: number;
  percent: number;
}

export interface DashboardRevenueOverviewResponse {
  miniMetrics: Array<{ key: string; label: string; value: number; trend?: "up" | "down" }>;
  totalLabel: string;
  plans: DashboardRevenuePlanItem[];
  upgradesText: string;
  downgradesText: string;
  lastUpdatedText?: string | null;
}

export interface DashboardTenantGrowthPoint {
  label: string;
  from?: string | null;
  to?: string | null;
  newTenants: number;
  churnedTenants: number;
  netGrowth: number;
}

export interface DashboardTenantGrowthResponse {
  range: string;
  timezone?: string | null;
  from?: string | null;
  to?: string | null;
  points: DashboardTenantGrowthPoint[];
  totalNewTenants: number;
  totalChurnedTenants: number;
  netGrowth: number;
  generatedAt?: string | null;
}

export interface DashboardPlanDistributionPlanItem {
  tier: string;
  planName?: string;
  planCode?: string;
  tenants: number;
  percentage: number;
}

export interface DashboardPlanDistributionResponse {
  totalTenants: number;
  plans: DashboardPlanDistributionPlanItem[];
  generatedAt?: string | null;
}

export interface DashboardMrrBreakdownResponse {
  currency: string;
  enterpriseAndPro: number;
  growthAndStarter: number;
  totalMrr: number;
  generatedAt?: string | null;
}

export interface DashboardSystemHealthService {
  name: string;
  status: string;
}

export interface DashboardSystemHealthResponse {
  overallStatus: string;
  openIncidents: number;
  services: DashboardSystemHealthService[];
  generatedAt?: string | null;
}

export interface FeatureCatalogItem {
  name: string;
  keyName: string;
  description: string;
  scope: "Tenant" | "Global";
  type: string;
  globalDefault: boolean;
}

export interface FeatureCatalogHistoryEntry {
  id: number;
  authId: number | null;
  action: string;
  resourceType: string;
  resourceId: string;
  details: string | null;
  createdAt: string;
}

export type AddOnBillingCycle = "MONTHLY" | "ANNUAL";
export type AddOnCatalogStatus = "ACTIVE" | "INACTIVE";

export interface AddOnCatalogItem {
  id: string;
  code: string;
  name: string;
  description: string;
  priceUsd: number;
  billingCycle: AddOnBillingCycle;
  status: AddOnCatalogStatus;
}

export interface UpdateAddOnCatalogItemPayload {
  code: string;
  body: {
    name: string;
    description: string;
    priceUsd: number;
    billingCycle: AddOnBillingCycle;
    status: AddOnCatalogStatus;
  };
}

export interface CreateAddOnCatalogItemPayload {
  code: string;
  name: string;
  description: string;
  priceUsd: number;
  billingCycle: AddOnBillingCycle;
  status: AddOnCatalogStatus;
}

export interface SuperAdminProfileInfo {
  id: number;
  username: string;
  fullName: string;
  email: string;
  phone: string;
  profilePicture: string;
  active: boolean;
  roles: string[];
  createdAt: string;
  updatedAt: string;
}

export interface UpdateSuperAdminProfilePayload {
  username: string;
  fullName: string;
  email: string;
  phone: string;
  active: boolean;
  roles: string[];
}

export interface SuperAdminMeProfile {
  id?: number;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phone: string;
  profilePicture: string;
  timezone: string;
  locale: string;
  updatedAt?: string;
}

export interface SuperAdminProfilePictureUploadResponse extends SuperAdminMeProfile {
  supported?: boolean;
  message?: string;
}

export interface UpdateSuperAdminMeProfilePayload {
  firstName?: string;
  lastName?: string;
  fullName?: string;
  email?: string;
  phone?: string;
  timezone?: string;
  locale?: string;
}

export interface SuperAdmin2faStatus {
  message: string;
  supported: boolean;
  enabled: boolean;
}

export interface SuperAdminChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

export interface SuperAdminChangePasswordResponse {
  message: string;
}

export interface RolesPermissionsMatrixFilters {
  module?: string;
  permissionGroup?: string;
}

export interface RolesPermissionsMatrixRow {
  key: string;
  module: string;
  group: string;
  roles: Record<string, boolean>;
}

export interface RolesPermissionsDetailedPermission {
  permissionId: number;
  name: string;
  displayName: string;
  category: string;
  active: boolean;
}

export interface RolesPermissionsDetailedRole {
  roleId: number;
  name: string;
  displayName: string;
  description?: string;
  organisationId: number | null;
  isSystem: boolean;
  active: boolean;
  permissions: string[];
}

export interface RolesPermissionsMatrixDetailed {
  module: string | null;
  permissionGroup: string | null;
  permissions: RolesPermissionsDetailedPermission[];
  permissionsByCategory: Record<string, RolesPermissionsDetailedPermission[]>;
  roles: RolesPermissionsDetailedRole[];
  generatedAt: string | null;
}

export interface RolesPermissionsMatrixUpdateItem {
  roleName: string;
  permissionName: string;
  granted: boolean;
}

export interface PermissionCatalogItem {
  id: number;
  key: string;
  description: string;
  category: string;
}

export interface PermissionCatalogSection {
  title: string;
  items: PermissionCatalogItem[];
}

export interface CreateSuperAdminRolePayload {
  name: string;
  displayName: string;
  description: string;
  isActive: boolean;
  permissions: number[];
}

export interface UpdateSuperAdminRoleBody {
  name: string;
  displayName: string;
  description: string;
  isActive: boolean;
  permissions: number[];
}

export interface UpdateSuperAdminRolePayload {
  roleId: number;
  body: UpdateSuperAdminRoleBody;
}

export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

export function getString(record: Record<string, unknown>, keys: string[]): string {
  for (const key of keys) {
    const value = record[key];
    if (typeof value === "string" && value.trim()) {
      return value.trim();
    }
  }
  return "";
}

export function getNumber(record: Record<string, unknown>, keys: string[]): number {
  for (const key of keys) {
    const value = record[key];
    if (typeof value === "number" && Number.isFinite(value)) {
      return value;
    }
    if (typeof value === "string" && value.trim()) {
      const parsed = Number.parseFloat(value);
      if (!Number.isNaN(parsed)) {
        return parsed;
      }
    }
  }
  return 0;
}

export function getArrayPayload(payload: unknown): unknown[] {
  if (Array.isArray(payload)) return payload;
  if (!isRecord(payload)) return [];
  if (Array.isArray(payload.data)) return payload.data;
  if (isRecord(payload.data)) {
    const candidates = [
      payload.data.items,
      payload.data.content,
      payload.data.rows,
      payload.data.results,
      payload.data.plans,
      payload.data.organisations,
      payload.data.organizations,
    ];
    for (const candidate of candidates) {
      if (Array.isArray(candidate)) return candidate;
    }
  }
  const candidates = [payload.items, payload.content, payload.rows, payload.results, payload.plans];
  for (const candidate of candidates) {
    if (Array.isArray(candidate)) return candidate;
  }
  return [];
}

export function mapSuperAdminEmailTemplate(entry: unknown): SuperAdminEmailTemplate | null {
  if (!isRecord(entry)) return null;
  return {
    id: getNumber(entry, ["id"]),
    templateKey: getString(entry, ["templateKey", "key"]),
    subjectTemplate: getString(entry, ["subjectTemplate", "subject"]),
    bodyTemplate: getString(entry, ["bodyTemplate", "body"]),
    isActive:
      typeof entry.isActive === "boolean"
        ? entry.isActive
        : typeof entry.active === "boolean"
          ? entry.active
          : true,
    updatedByAuthId:
      typeof entry.updatedByAuthId === "number" ? entry.updatedByAuthId : undefined,
    createdAt: getString(entry, ["createdAt"]) || undefined,
    updatedAt: getString(entry, ["updatedAt"]) || undefined,
  };
}

export function mapSuperAdminNotificationTemplate(
  entry: unknown
): SuperAdminNotificationTemplate | null {
  if (!isRecord(entry)) return null;
  return {
    id: getNumber(entry, ["id"]),
    templateKey: getString(entry, ["templateKey", "key"]),
    subjectTemplate: getString(entry, ["subjectTemplate", "subject"]),
    bodyTemplate: getString(entry, ["bodyTemplate", "body"]),
    isActive:
      typeof entry.isActive === "boolean"
        ? entry.isActive
        : typeof entry.active === "boolean"
          ? entry.active
          : true,
    updatedByAuthId:
      typeof entry.updatedByAuthId === "number" ? entry.updatedByAuthId : undefined,
    createdAt: getString(entry, ["createdAt"]) || undefined,
    updatedAt: getString(entry, ["updatedAt"]) || undefined,
  };
}

export function mapSuperAdminNotificationHistoryItem(
  entry: unknown
): SuperAdminNotificationHistoryItem | null {
  if (!isRecord(entry)) return null;
  return {
    id: getNumber(entry, ["id"]),
    jobType: getString(entry, ["jobType"]),
    title: getString(entry, ["title"]),
    message: getString(entry, ["message"]),
    targetJson: getString(entry, ["targetJson"]),
    channel: getString(entry, ["channel"]),
    status: getString(entry, ["status"]),
    scheduledAt: getString(entry, ["scheduledAt"]),
    sentAt: getString(entry, ["sentAt"]),
    errorMessage: getString(entry, ["errorMessage"]),
    createdByAuthId:
      typeof entry.createdByAuthId === "number" ? entry.createdByAuthId : null,
    createdAt: getString(entry, ["createdAt"]),
    updatedAt: getString(entry, ["updatedAt"]),
    isRead: Boolean(entry.isRead),
    readAt: getString(entry, ["readAt"]),
  };
}

export function mapSuperAdminNotificationTrigger(
  entry: unknown
): SuperAdminNotificationTrigger | null {
  if (!isRecord(entry)) return null;
  return {
    id: getNumber(entry, ["id"]),
    name: getString(entry, ["name"]),
    description: getString(entry, ["description"]),
    eventType: getString(entry, ["eventType"]),
    entityType: getString(entry, ["entityType"]),
    conditionRules: getString(entry, ["conditionRules"]),
    recipientRules: getString(entry, ["recipientRules"]),
    priority: getString(entry, ["priority"]),
    isScheduled: Boolean(entry.isScheduled),
    scheduleOffsetMinutes: getNumber(entry, ["scheduleOffsetMinutes"]),
    batchWindowMinutes: getNumber(entry, ["batchWindowMinutes"]),
    maxBatchSize: getNumber(entry, ["maxBatchSize"]),
    isActive:
      typeof entry.isActive === "boolean"
        ? entry.isActive
        : typeof entry.active === "boolean"
          ? entry.active
          : true,
    createdAt: getString(entry, ["createdAt"]),
    updatedAt: getString(entry, ["updatedAt"]),
  };
}

export function splitNameParts(fullName: string): { firstName: string; lastName: string } {
  const normalized = fullName.trim();
  if (!normalized) {
    return { firstName: "", lastName: "" };
  }
  const parts = normalized.split(/\s+/);
  return {
    firstName: parts[0] ?? "",
    lastName: parts.slice(1).join(" "),
  };
}

export function mapSuperAdminMeProfile(payload: unknown): SuperAdminMeProfile {
  const root = extractRoot(payload);
  const fullName = getString(root, ["fullName", "name", "displayName"]);
  const split = splitNameParts(fullName);
  const firstName = getString(root, ["firstName"]) || split.firstName;
  const lastName = getString(root, ["lastName"]) || split.lastName;

  return {
    id:
      typeof root.id === "number"
        ? root.id
        : typeof root.profileId === "number"
          ? root.profileId
          : undefined,
    firstName,
    lastName,
    fullName: fullName || `${firstName} ${lastName}`.trim(),
    email: getString(root, ["email"]),
    phone: getString(root, ["phone", "phoneNumber"]),
    profilePicture: getString(root, ["profilePicture", "avatarUrl", "photoUrl"]),
    timezone: getString(root, ["timezone", "timeZone"]),
    locale: getString(root, ["locale"]),
    updatedAt: getString(root, ["updatedAt"]) || undefined,
  };
}

export function mapPricingTier(entry: unknown): BillingPlanPricingTier | null {
  if (!isRecord(entry)) return null;
  return {
    minTherapists: getNumber(entry, ["minTherapists", "minUsers", "from"]),
    maxTherapists: getNumber(entry, ["maxTherapists", "maxUsers", "to"]),
    pricePerTherapistUsd: getNumber(entry, [
      "pricePerTherapistUsd",
      "pricePerUserUsd",
      "unitPriceUsd",
      "price",
    ]),
    includedSupervisors: getNumber(entry, ["includedSupervisors", "supervisors"]),
    includedClients: getNumber(entry, ["includedClients", "clients"]),
  };
}

export function mapBillingPlan(entry: Record<string, unknown>): BillingPlan {
  const pricingTiersCandidate = Array.isArray(entry.pricingTiers) ? entry.pricingTiers : [];
  const entitlementsCandidate = Array.isArray(entry.entitlements) ? entry.entitlements : [];
  const entitlements = entitlementsCandidate
    .map(function (item) {
      if (!isRecord(item)) return null;
      const usageLimitRaw = item.usageLimit;
      const usageLimit =
        typeof usageLimitRaw === "number" && Number.isFinite(usageLimitRaw)
          ? usageLimitRaw
          : typeof usageLimitRaw === "string" && usageLimitRaw.trim()
            ? Number.parseFloat(usageLimitRaw)
            : null;
      return {
        featureCode: getString(item, ["featureCode", "code", "keyName"]),
        enabled: Boolean(item.enabled),
        usageLimit: usageLimit !== null && Number.isFinite(usageLimit) ? usageLimit : null,
      };
    })
    .filter(Boolean) as BillingPlan["entitlements"];

  function getNullableNumber(keys: string[]): number | null {
    for (const key of keys) {
      const value = entry[key];
      if (value === null || value === undefined || value === "") {
        continue;
      }
      if (typeof value === "number" && Number.isFinite(value)) {
        return value;
      }
      if (typeof value === "string" && value.trim()) {
        const parsed = Number.parseFloat(value);
        if (!Number.isNaN(parsed)) {
          return parsed;
        }
      }
    }
    return null;
  }

  function getLimitFromEntitlements(featureCodes: string[]): number | null {
    const matched = entitlements.find((item) => {
      return featureCodes.includes(item.featureCode.trim().toUpperCase()) && item.enabled;
    });
    return matched?.usageLimit ?? null;
  }

  // Prefer plan-level limits; only fall back to entitlements when the plan field is absent.
  const hasExplicitTherapistLimit = Object.prototype.hasOwnProperty.call(entry, "therapistLimit");
  const hasExplicitSupervisorLimit = Object.prototype.hasOwnProperty.call(entry, "supervisorLimit");
  const hasExplicitClientLimit = Object.prototype.hasOwnProperty.call(entry, "clientLimit");

  const therapistLimit = hasExplicitTherapistLimit
    ? getNullableNumber(["therapistLimit"])
    : getLimitFromEntitlements(["THERAPIST_SEATS", "THERAPIST_LIMIT"]);
  const supervisorLimit = hasExplicitSupervisorLimit
    ? getNullableNumber(["supervisorLimit"])
    : getLimitFromEntitlements(["SUPERVISOR_SEATS", "SUPERVISOR_LIMIT"]);
  const clientLimit = hasExplicitClientLimit
    ? getNullableNumber(["clientLimit"])
    : getLimitFromEntitlements(["CLIENT_LIMIT"]);

  return {
    planCode: getString(entry, ["planCode", "code", "slug", "id"]),
    planName: getString(entry, ["planName", "name", "displayName"]),
    description: getString(entry, ["description"]),
    basePrice: getNumber(entry, ["basePrice"]),
    annualPrice: getNumber(entry, ["annualPrice"]),
    billingCycle: getString(entry, ["billingCycle"]),
    trialDays: getNumber(entry, ["trialDays"]),
    providerPriceIdMonthly: getString(entry, ["providerPriceIdMonthly"]),
    providerPriceIdAnnual: getString(entry, ["providerPriceIdAnnual"]),
    status: getString(entry, ["status"]),
    therapistLimit,
    supervisorLimit,
    clientLimit,
    entitlements,
    pricingTiers: pricingTiersCandidate
      .map(mapPricingTier)
      .filter(Boolean) as BillingPlanPricingTier[],
  };
}

export function isArchivedPlanStatus(status: string): boolean {
  return status.trim().toLowerCase().includes("archive");
}

export function mapPlansCatalogResponse(payload: unknown): PlanCatalogOption[] {
  const planMap = new Map<string, PlanCatalogOption>();

  getArrayPayload(payload).forEach(function (entry, index) {
    if (!isRecord(entry)) return;

    if (isArchivedPlanStatus(getString(entry, ["status"]))) return;

    const planCode = getString(entry, ["planCode", "code", "slug", "id"]) || `plan-${index + 1}`;
    const planName = getString(entry, ["planName", "name", "displayName"]) || planCode;
    const billingCycle =
      normalizeBillingCycle(getString(entry, ["billingCycle", "cycle"])) || "Monthly";

    const existingPlan = planMap.get(planCode);
    if (!existingPlan) {
      planMap.set(planCode, {
        planCode,
        planName,
        trialDays: getNumber(entry, ["trialDays"]),
        billingCycles: [billingCycle],
        defaultBillingCycle: billingCycle,
        description: getString(entry, ["description"]),
      });
      return;
    }

    if (!existingPlan.billingCycles.includes(billingCycle)) {
      existingPlan.billingCycles.push(billingCycle);
    }

    if (!existingPlan.description) {
      existingPlan.description = getString(entry, ["description"]) || existingPlan.description;
    }

    const trialDays = getNumber(entry, ["trialDays"]);
    if (trialDays) {
      existingPlan.trialDays = trialDays;
    }
  });

  return Array.from(planMap.values());
}

export function normalizeBillingCycle(value: string): string | null {
  if (!value) return null;
  const normalizedValue = value.trim().toLowerCase();
  if (normalizedValue.includes("year")) return "Yearly";
  if (normalizedValue.includes("month")) return "Monthly";
  return value;
}

export function normalizeStatus(value: string | null): string {
  if (!value) return "Unknown";
  const normalizedValue = value.trim().toLowerCase();
  if (normalizedValue.includes("pending")) return "Pending Activation";
  if (normalizedValue.includes("suspend")) return "Suspended";
  if (normalizedValue.includes("active")) return "Active";
  return value;
}

export function normalizePlan(value: string | null): string {
  if (!value) return "Unknown";
  const normalizedValue = value.trim().toLowerCase();
  if (normalizedValue.includes("enterprise")) return "Enterprise";
  if (normalizedValue === "pro" || normalizedValue.includes("professional")) return "Pro";
  if (normalizedValue.includes("trial")) return "Trial";
  return value;
}

const createdAtFormatter = new Intl.DateTimeFormat("en-US", {
  month: "short",
  day: "2-digit",
  year: "numeric",
});

export function formatCreatedAt(value: string | null): string {
  if (!value) return "-";
  const parsedDate = new Date(value);
  if (Number.isNaN(parsedDate.getTime())) return value;
  return createdAtFormatter.format(parsedDate);
}

export function mapOrganisationRow(entry: unknown, index: number): OrganisationListRow | null {
  if (!isRecord(entry)) return null;
  const name = getString(entry, ["name", "organisationName", "organizationName", "tenantName"]);
  if (!name) return null;
  const slug = getString(entry, ["slug", "organisationSlug", "organizationSlug"]) || name;
  const createdAtRaw = getString(entry, [
    "createdAt",
    "createdOn",
    "createdDate",
    "created",
    "created_at",
  ]);
  const users = getNumber(entry, ["users", "userCount", "usersCount", "activeUsers", "memberCount"]);
  const idFromString = getString(entry, ["id", "organisationId", "organizationId"]);
  const idFromNumber = getNumber(entry, ["id", "organisationId", "organizationId"]);
  const id =
    idFromString || (idFromNumber > 0 ? String(idFromNumber) : `${slug}-${index}`);

  return {
    id,
    name,
    slug,
    status: normalizeStatus(getString(entry, ["status", "organisationStatus", "organizationStatus"])),
    plan: normalizePlan(getString(entry, ["plan", "subscriptionPlan", "tier", "planName"])),
    users,
    createdAt: formatCreatedAt(createdAtRaw),
    region: getString(entry, ["region", "infrastructureRegion"]) || "-",
    dataResidency: getString(entry, ["dataResidency", "residency"]) || "-",
  };
}

export function mapOrganisationsListResponse(
  payload: unknown,
  filters: OrganisationListFilters
): OrganisationListResult {
  const responsePayload = isRecord(payload) && isRecord(payload.data) ? payload.data : payload;
  const rows = getArrayPayload(responsePayload)
    .map(function (entry, index) {
      return mapOrganisationRow(entry, index);
    })
    .filter(Boolean) as OrganisationListRow[];

  const meta = isRecord(responsePayload) ? responsePayload : {};
  const totalItems = getNumber(meta, ["totalItems", "totalElements", "total", "count"]) || rows.length;
  const pageSize = getNumber(meta, ["pageSize", "size", "perPage", "limit"]) || filters.pageSize;
  const totalPages =
    getNumber(meta, ["totalPages", "pages", "pageCount"]) ||
    Math.max(1, Math.ceil(totalItems / Math.max(pageSize, 1)));
  const currentPage =
    getNumber(meta, ["page", "pageNumber", "currentPage", "number"]) || filters.page;

  return {
    rows,
    currentPage: Math.max(1, currentPage),
    pageSize: Math.max(1, pageSize),
    totalPages: Math.max(1, totalPages),
    totalItems: Math.max(0, totalItems),
  };
}

export function mapOrganisationDetailsResponse(payload: unknown): OrganisationDetailsResult {
  const root = extractRoot(payload);
  const subscriptionDetailsRaw = isRecord(root.subscriptionDetails)
    ? root.subscriptionDetails
    : null;
  const userLimitsRaw =
    subscriptionDetailsRaw && isRecord(subscriptionDetailsRaw.userLimits)
      ? subscriptionDetailsRaw.userLimits
      : null;
  const userUsageRaw =
    subscriptionDetailsRaw && isRecord(subscriptionDetailsRaw.userUsage)
      ? subscriptionDetailsRaw.userUsage
      : null;
  const totalUserCountValue =
    typeof root.totalUserCount === "number" && Number.isFinite(root.totalUserCount)
      ? root.totalUserCount
      : typeof root.totalUserCount === "string" && root.totalUserCount.trim()
        ? Number.parseInt(root.totalUserCount, 10)
        : Number.NaN;
  const totalUserCount = Number.isNaN(totalUserCountValue)
    ? null
    : totalUserCountValue;

  return {
    id: getNumber(root, ["id", "organisationId", "organizationId"]),
    name: getString(root, ["name", "organisationName", "organizationName"]),
    slug: getString(root, ["slug", "organisationSlug", "organizationSlug"]),
    status: normalizeStatus(getString(root, ["status", "organisationStatus", "organizationStatus"])),
    subdomain: getString(root, ["subdomain"]),
    schemaName: getString(root, ["schemaName"]),
    timezone: getString(root, ["timezone"]),
    region: getString(root, ["region"]),
    dataResidency: getString(root, ["dataResidency", "residency"]),
    locale: getString(root, ["locale"]),
    logoUrl: getString(root, ["logoUrl"]),
    primaryAdminEmail: getString(root, ["primaryAdminEmail"]),
    supportEmail: getString(root, ["supportEmail"]),
    supportAddress: getString(root, ["supportAddress"]),
    totalUserCount,
    subscriptionDetails: subscriptionDetailsRaw
      ? {
          organisationId: getNumber(subscriptionDetailsRaw, ["organisationId"]),
          subscriptionId: getNumber(subscriptionDetailsRaw, ["subscriptionId"]),
          plan: getString(subscriptionDetailsRaw, ["plan"]),
          status: getString(subscriptionDetailsRaw, ["status"]),
          billingCycle: getString(subscriptionDetailsRaw, ["billingCycle"]),
          priceAtTime: getNumber(subscriptionDetailsRaw, ["priceAtTime"]),
          startAt: getString(subscriptionDetailsRaw, ["startAt"]),
          endAt: getString(subscriptionDetailsRaw, ["endAt"]),
          trialEndsAt: getString(subscriptionDetailsRaw, ["trialEndsAt"]),
          providerCustomerId: getString(subscriptionDetailsRaw, ["providerCustomerId"]),
          providerSubscriptionId: getString(subscriptionDetailsRaw, ["providerSubscriptionId"]),
          userLimits: {
            therapistLimit: userLimitsRaw
              ? (userLimitsRaw.therapistLimit as number | null)
              : null,
            supervisorLimit: userLimitsRaw
              ? (userLimitsRaw.supervisorLimit as number | null)
              : null,
            clientLimit: userLimitsRaw ? (userLimitsRaw.clientLimit as number | null) : null,
          },
          userUsage: {
            therapistUsers: userUsageRaw ? getNumber(userUsageRaw, ["therapistUsers"]) : 0,
            supervisorUsers: userUsageRaw ? getNumber(userUsageRaw, ["supervisorUsers"]) : 0,
            clientUsers: userUsageRaw ? getNumber(userUsageRaw, ["clientUsers"]) : 0,
            totalUsers: userUsageRaw ? getNumber(userUsageRaw, ["totalUsers"]) : 0,
          },
        }
      : null,
    extra: isRecord(root.extra) ? root.extra : {},
    createdAt: formatCreatedAt(
      getString(root, ["createdAt", "createdOn", "createdDate", "created", "created_at"])
    ),
  };
}

export function mapOrganisationFeaturesResponse(payload: unknown): OrganisationFeaturesResponse {
  const root = extractRoot(payload);
  const output: OrganisationFeaturesResponse = {};

  Object.entries(root).forEach(function ([key, value]) {
    if (!isRecord(value)) return;
    output[key] = {
      enabled: Boolean(value.enabled),
      usageLimit:
        typeof value.usageLimit === "number" && Number.isFinite(value.usageLimit)
          ? value.usageLimit
          : null,
    };
  });

  return output;
}

export function mapOrganisationSettingsResponse(payload: unknown): OrganisationSettings {
  const root = extractRoot(payload);
  return {
    timezone: getString(root, ["timezone"]),
    region: getString(root, ["region"]),
    dataResidency: getString(root, ["dataResidency"]),
    locale: getString(root, ["locale"]),
    logoUrl: getString(root, ["logoUrl"]),
    brandPrimaryColor: getString(root, ["brandPrimaryColor"]),
    brandSecondaryColor: getString(root, ["brandSecondaryColor"]),
    brandAccentColor: getString(root, ["brandAccentColor"]),
    supportEmail: getString(root, ["supportEmail"]),
    supportAddress: getString(root, ["supportAddress"]),
  };
}

export function mapOrganisationSubscriptionResponse(payload: unknown): OrganisationSubscription {
  const root = extractRoot(payload);
  const userLimitsRaw = isRecord(root.userLimits)
    ? root.userLimits
    : isRecord(root.limits)
      ? root.limits
      : {};
  const userUsageRaw = isRecord(root.userUsage)
    ? root.userUsage
    : isRecord(root.usage)
      ? root.usage
      : {};

  return {
    organisationId: getNumber(root, ["organisationId", "organizationId", "orgId"]),
    subscriptionId: getNumber(root, ["subscriptionId", "id"]),
    plan: getString(root, ["plan", "planName"]),
    status: getString(root, ["status"]),
    billingCycle: getString(root, ["billingCycle", "cycle"]),
    trialDays: getNumber(root, ["trialDays"]),
    startAt: getString(root, ["startAt", "startDate"]),
    endAt: getString(root, ["endAt", "endDate"]),
    trialEndsAt: getString(root, ["trialEndsAt", "trialEndDate"]),
    userLimits: {
      therapistLimit:
        getNumber(userLimitsRaw, ["therapistLimit", "therapists", "therapistUsersLimit"]) || null,
      supervisorLimit:
        getNumber(userLimitsRaw, ["supervisorLimit", "supervisors", "supervisorUsersLimit"]) ||
        null,
      clientLimit: getNumber(userLimitsRaw, ["clientLimit", "clients", "clientUsersLimit"]) || null,
    },
    userUsage: {
      therapistUsers: getNumber(userUsageRaw, ["therapistUsers", "therapists"]),
      supervisorUsers: getNumber(userUsageRaw, ["supervisorUsers", "supervisors"]),
      clientUsers: getNumber(userUsageRaw, ["clientUsers", "clients"]),
      totalUsers: getNumber(userUsageRaw, ["totalUsers", "total"]),
    },
  };
}

export function mapOrganisationSchemaVersionResponse(payload: unknown): OrganisationSchemaVersion {
  const root = extractRoot(payload);
  return {
    version:
      getString(root, ["version", "schemaVersion", "migrationVersion"]) ||
      getString(root, ["currentVersion", "value"]),
    description: getString(root, ["description", "details"]) || undefined,
    updatedAt: getString(root, ["updatedAt", "lastUpdatedAt"]) || undefined,
  };
}

export function normalizeScope(value: string): "Tenant" | "Global" {
  return value.trim().toLowerCase().includes("global") ? "Global" : "Tenant";
}

export function normalizeFeatureType(value: string): string {
  const trimmedValue = value.trim();
  if (!trimmedValue) {
    return "Core";
  }

  const normalizedValue = trimmedValue.toLowerCase();
  if (normalizedValue === "core") return "Core";
  if (normalizedValue === "custom") return "Custom";
  if (normalizedValue === "toggle") return "Toggle";
  if (normalizedValue === "limit") return "Limit";
  return trimmedValue.charAt(0).toUpperCase() + trimmedValue.slice(1);
}

export function mapFeatureCatalogItem(entry: unknown, index: number): FeatureCatalogItem | null {
  if (!isRecord(entry)) return null;

  const keyName =
    getString(entry, ["keyName", "key", "featureKey", "code", "id"]) || `FEATURE_${index + 1}`;
  const name =
    getString(entry, ["name", "displayName", "title", "featureName"]) || keyName;
  const scope = normalizeScope(getString(entry, ["scope", "featureScope"]));
  const typeRaw = getString(entry, ["type", "featureType"]);
  const type = normalizeFeatureType(typeRaw);
  const globalDefault = Boolean(
    (entry as Record<string, unknown>).globalDefault ??
      (entry as Record<string, unknown>).enabled ??
      (entry as Record<string, unknown>).defaultEnabled
  );

  return {
    name,
    keyName,
    description: getString(entry, ["description"]),
    scope,
    type,
    globalDefault,
  };
}

export function mapFeatureCatalogResponse(payload: unknown): FeatureCatalogItem[] {
  const rowsFromArray = getArrayPayload(payload)
    .map((entry, index) => mapFeatureCatalogItem(entry, index))
    .filter(Boolean) as FeatureCatalogItem[];

  if (rowsFromArray.length > 0) {
    return rowsFromArray;
  }

  const root = extractRoot(payload);
  const rowsFromRecord = Object.entries(root)
    .map(function ([keyName, value], index) {
      if (!isRecord(value)) return null;
      return mapFeatureCatalogItem(
        {
          keyName,
          name: getString(value, ["name", "displayName", "title"]) || keyName,
          scope: getString(value, ["scope"]),
          type: getString(value, ["type"]),
          enabled: value.enabled,
          usageLimit: value.usageLimit,
        },
        index
      );
    })
    .filter(Boolean) as FeatureCatalogItem[];

  return rowsFromRecord;
}

export function extractRoot(payload: unknown): Record<string, unknown> {
  return isRecord(payload) ? (isRecord(payload.data) ? payload.data : payload) : {};
}

export function toTitleCaseCategory(value: string): string {
  if (!value.trim()) return "General";
  return value
    .split("_")
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(" ");
}

export function mapRolesPermissionsMatrixResponse(payload: unknown): RolesPermissionsMatrixRow[] {
  const root = extractRoot(payload);

  const permissions = Array.isArray(root.permissions) ? root.permissions : [];
  const rolesList = Array.isArray(root.roles) ? root.roles : [];

  if (permissions.length > 0 && rolesList.length > 0) {
    const rolePermissionsMap = new Map<string, Set<string>>();

    rolesList.forEach((roleEntry) => {
      if (!isRecord(roleEntry)) return;
      const roleName =
        getString(roleEntry, ["name", "displayName", "role", "roleName"]).trim().toLowerCase();
      if (!roleName) return;
      const permissionsForRole = Array.isArray(roleEntry.permissions)
        ? roleEntry.permissions.filter((permission): permission is string => typeof permission === "string")
        : [];
      rolePermissionsMap.set(roleName, new Set(permissionsForRole));
    });

    return permissions
      .map((permissionEntry, index) => {
        if (!isRecord(permissionEntry)) return null;
        const permissionName =
          getString(permissionEntry, ["name", "permission", "permissionKey"]) ||
          `permission_${index + 1}`;
        const categoryRaw = getString(permissionEntry, ["category"]);
        const categoryLabel = toTitleCaseCategory(categoryRaw);

        const roles = Object.fromEntries(
          Array.from(rolePermissionsMap.entries()).map(([roleName, rolePermissions]) => {
            return [roleName, rolePermissions.has(permissionName)];
          })
        );

        return {
          key: permissionName,
          module: categoryLabel,
          group: categoryLabel,
          roles,
        } satisfies RolesPermissionsMatrixRow;
      })
      .filter(Boolean) as RolesPermissionsMatrixRow[];
  }

  const fallbackRows = getArrayPayload(payload);
  return fallbackRows
    .map((entry, index) => {
      if (!isRecord(entry)) return null;
      const rolesRaw = isRecord(entry.roles)
        ? entry.roles
        : isRecord(entry.rolePermissions)
          ? entry.rolePermissions
          : {};

      const roles = Object.entries(rolesRaw).reduce<Record<string, boolean>>((acc, [role, value]) => {
        acc[role.trim().toLowerCase()] = Boolean(value);
        return acc;
      }, {});

      return {
        key: getString(entry, ["key", "permissionKey", "permission", "code"]) || `permission_${index + 1}`,
        module: getString(entry, ["module", "moduleName"]) || "General",
        group: getString(entry, ["group", "permissionGroup", "groupName"]) || "General",
        roles,
      };
    })
    .filter(Boolean) as RolesPermissionsMatrixRow[];
}

export function mapRolesPermissionsMatrixDetailed(payload: unknown): RolesPermissionsMatrixDetailed {
  const root = extractRoot(payload);

  const permissions = (Array.isArray(root.permissions) ? root.permissions : [])
    .map((entry) => {
      if (!isRecord(entry)) return null;
      return {
        permissionId: getNumber(entry, ["permissionId", "id"]),
        name: getString(entry, ["name", "permissionName"]),
        displayName: getString(entry, ["displayName", "name"]),
        category: getString(entry, ["category"]),
        active: entry.active !== false,
      } as RolesPermissionsDetailedPermission;
    })
    .filter(Boolean) as RolesPermissionsDetailedPermission[];

  const permissionsByCategoryRaw = isRecord(root.permissionsByCategory)
    ? root.permissionsByCategory
    : {};
  const permissionsByCategory = Object.entries(permissionsByCategoryRaw).reduce<
    Record<string, RolesPermissionsDetailedPermission[]>
  >((acc, [category, list]) => {
    const mapped = (Array.isArray(list) ? list : [])
      .map((entry) => {
        if (!isRecord(entry)) return null;
        return {
          permissionId: getNumber(entry, ["permissionId", "id"]),
          name: getString(entry, ["name", "permissionName"]),
          displayName: getString(entry, ["displayName", "name"]),
          category: getString(entry, ["category"]) || category,
          active: entry.active !== false,
        } as RolesPermissionsDetailedPermission;
      })
      .filter(Boolean) as RolesPermissionsDetailedPermission[];
    acc[category] = mapped;
    return acc;
  }, {});

  const roles = (Array.isArray(root.roles) ? root.roles : [])
    .map((entry) => {
      if (!isRecord(entry)) return null;
      const permissionsList = Array.isArray(entry.permissions)
        ? entry.permissions.filter((item): item is string => typeof item === "string")
        : [];
      return {
        roleId: getNumber(entry, ["roleId", "id"]),
        name: getString(entry, ["name", "roleName"]),
        displayName: getString(entry, ["displayName", "name", "roleName"]),
        description: getString(entry, ["description"]) || undefined,
        organisationId:
          typeof entry.organisationId === "number"
            ? entry.organisationId
            : typeof entry.organizationId === "number"
              ? entry.organizationId
              : null,
        isSystem: Boolean(entry.isSystem),
        active: entry.active !== false,
        permissions: permissionsList,
      } as RolesPermissionsDetailedRole;
    })
    .filter(Boolean) as RolesPermissionsDetailedRole[];

  return {
    module: root.module == null ? null : String(root.module),
    permissionGroup: root.permissionGroup == null ? null : String(root.permissionGroup),
    permissions,
    permissionsByCategory,
    roles,
    generatedAt: getString(root, ["generatedAt"]) || null,
  };
}

export function mapPermissionsCatalogResponse(payload: unknown): PermissionCatalogSection[] {
  function mapPermissionCatalogItem(
    entry: unknown,
    fallbackCategory: string,
    index: number,
  ): PermissionCatalogItem | null {
    if (typeof entry === "string") {
      return {
        id: 0,
        key: entry,
        description: "",
        category: fallbackCategory,
      };
    }
    if (!isRecord(entry)) return null;

    const key =
      getString(entry, ["permissionName", "permission", "key", "name", "code"]) ||
      `${fallbackCategory}_${index + 1}`;

    return {
      id: getNumber(entry, ["id", "permissionId", "permission_id", "value"]),
      key,
      description: getString(entry, ["description", "label", "title"]),
      category:
        getString(entry, ["category", "group", "module"]) || fallbackCategory,
    };
  }

  const root = extractRoot(payload);
  const grouped = root.grouped;

  if (isRecord(grouped)) {
    const sections = Object.entries(grouped)
      .map(([category, values]) => {
        const itemsRaw = Array.isArray(values)
          ? values
          : isRecord(values)
            ? Array.isArray(values.items)
              ? values.items
              : Array.isArray(values.permissions)
                ? values.permissions
                : Array.isArray(values.content)
                  ? values.content
                  : []
            : [];
        const items = itemsRaw
          .map((entry, index) => mapPermissionCatalogItem(entry, category, index))
          .filter(Boolean) as PermissionCatalogItem[];

        return {
          title: toTitleCaseCategory(category),
          items,
        } as PermissionCatalogSection;
      })
      .filter((section) => section.items.length > 0);

    if (sections.length > 0) {
      return sections;
    }
  }

  const flatItems = getArrayPayload(payload)
    .map((entry, index) => mapPermissionCatalogItem(entry, "General", index))
    .filter(Boolean) as PermissionCatalogItem[];

  const groupedSections = flatItems.reduce<Record<string, PermissionCatalogItem[]>>(
    (accumulator, item) => {
      const categoryKey = toTitleCaseCategory(item.category);
      if (!accumulator[categoryKey]) {
        accumulator[categoryKey] = [];
      }
      accumulator[categoryKey].push(item);
      return accumulator;
    },
    {},
  );

  return Object.entries(groupedSections).map(([title, items]) => ({
    title,
    items,
  }));
}

export function mapSuperAdminBillingInvoice(
  entry: unknown,
  index: number
): SuperAdminBillingInvoiceItem | null {
  if (!isRecord(entry)) return null;

  const invoiceIdFromString = getString(entry, ["invoiceId", "id", "number"]);
  const invoiceIdFromNumber = getNumber(entry, ["invoiceId", "id", "number"]);
  const invoiceId =
    invoiceIdFromString ||
    (invoiceIdFromNumber > 0 ? String(invoiceIdFromNumber) : `inv-${index + 1}`);
  const organisationId = getNumber(entry, ["organisationId", "organizationId", "orgId"]);
  const idFromString = getString(entry, ["id"]);
  const idFromNumber = getNumber(entry, ["id"]);
  const organisation =
    getString(entry, [
      "organisationName",
      "organizationName",
      "orgName",
      "tenantName",
      "organisation",
      "organization",
      "companyName",
    ]) ||
    (organisationId > 0 ? `Organization #${organisationId}` : "-");

  const amount = getNumber(entry, ["amount", "totalAmount", "subtotal", "total", "priceAtTime"]);
  const balanceKeys = [
    "currentBalance",
    "balance",
    "balanceDue",
    "amountDue",
    "outstandingAmount",
    "outstandingBalance",
  ];
  let currentBalance: number | null = null;
  for (const key of balanceKeys) {
    if (!(key in entry)) continue;
    const raw = entry[key];
    if (typeof raw === "number" && Number.isFinite(raw)) {
      currentBalance = raw;
      break;
    }
    if (typeof raw === "string" && raw.trim()) {
      const parsed = Number.parseFloat(raw);
      if (!Number.isNaN(parsed)) {
        currentBalance = parsed;
        break;
      }
    }
  }
  if (currentBalance === null) {
    currentBalance = amount;
  }
  const billingPeriodStart = getString(entry, ["billingPeriodStart", "periodStart", "startDate"]);
  const billingPeriodEnd = getString(entry, ["billingPeriodEnd", "periodEnd", "endDate"]);
  const parsedBillingPeriodStart = billingPeriodStart ? new Date(billingPeriodStart) : null;
  const parsedBillingPeriodEnd = billingPeriodEnd ? new Date(billingPeriodEnd) : null;
  const period =
    getString(entry, ["period", "billingPeriod", "invoicePeriod"]) ||
    (parsedBillingPeriodStart &&
    !Number.isNaN(parsedBillingPeriodStart.getTime()) &&
    parsedBillingPeriodEnd &&
    !Number.isNaN(parsedBillingPeriodEnd.getTime())
      ? `${parsedBillingPeriodStart.toLocaleDateString("en-US", {
          month: "short",
          day: "2-digit",
          year: "numeric",
        })} - ${parsedBillingPeriodEnd.toLocaleDateString("en-US", {
          month: "short",
          day: "2-digit",
          year: "numeric",
        })}`
      : "-");

  return {
    id: idFromString || (idFromNumber > 0 ? String(idFromNumber) : invoiceId),
    invoiceId,
    invoiceNumericId: invoiceIdFromNumber > 0 ? invoiceIdFromNumber : undefined,
    organisation,
    organisationId: organisationId > 0 ? organisationId : undefined,
    subscriptionId: getNumber(entry, ["subscriptionId"]) || undefined,
    plan: getString(entry, ["plan", "planName", "subscriptionPlan"]) || undefined,
    period,
    dueDate: getString(entry, ["dueDate", "dueAt", "paymentDueDate"]) || "-",
    amount,
    currentBalance,
    status: getString(entry, ["status"]) || "Unknown",
    totalPaid: getNumber(entry, ["totalPaid"]) || undefined,
    refundedAmount: getNumber(entry, ["refundedAmount"]) || undefined,
    paidAt: getString(entry, ["paidAt"]) || undefined,
    createdAt: getString(entry, ["createdAt"]) || undefined,
    billingPeriodStart: billingPeriodStart || undefined,
    billingPeriodEnd: billingPeriodEnd || undefined,
  };
}

export function mapSuperAdminBillingInvoicesResponse(
  payload: unknown,
  filters: SuperAdminBillingInvoicesQueryParams
): SuperAdminBillingInvoicesResult {
  const responsePayload = isRecord(payload) && isRecord(payload.data) ? payload.data : payload;
  const items = getArrayPayload(responsePayload)
    .map((entry, index) => mapSuperAdminBillingInvoice(entry, index))
    .filter(Boolean) as SuperAdminBillingInvoiceItem[];

  const meta = isRecord(responsePayload) ? responsePayload : {};
  const page = getNumber(meta, ["page", "pageNumber", "currentPage", "number"]) || filters.page || 0;
  const pageSize = getNumber(meta, ["pageSize", "size", "perPage", "limit"]) || filters.pageSize || 50;
  const totalItems =
    getNumber(meta, ["totalItems", "totalElements", "totalCount", "total", "count"]) ||
    items.length;
  const totalPages =
    getNumber(meta, ["totalPages", "pages", "pageCount"]) ||
    Math.max(1, Math.ceil(totalItems / Math.max(pageSize, 1)));

  return {
    items,
    page: Math.max(0, page),
    pageSize: Math.max(1, pageSize),
    totalPages: Math.max(1, totalPages),
    totalItems: Math.max(0, totalItems),
  };
}

function mapNullableString(value: unknown): string | null {
  if (typeof value === "string" && value.trim()) return value.trim();
  return null;
}

function mapInvoiceAdjustment(entry: unknown): SuperAdminBillingInvoiceAdjustment | null {
  if (!isRecord(entry)) return null;
  return {
    adjustmentId: getNumber(entry, ["adjustmentId", "id"]),
    invoiceId: getNumber(entry, ["invoiceId"]),
    type: getString(entry, ["type"]) || "—",
    amount: getNumber(entry, ["amount", "amountUsd"]),
    reason: getString(entry, ["reason"]) || "—",
    status: getString(entry, ["status"]) || "—",
    createdAt: getString(entry, ["createdAt"]) || "",
  };
}

function mapInvoiceDispute(entry: unknown): SuperAdminBillingInvoiceDispute | null {
  if (!isRecord(entry)) return null;
  return {
    disputeId: getNumber(entry, ["disputeId", "id"]),
    invoiceId: getNumber(entry, ["invoiceId"]),
    externalCaseId: getString(entry, ["externalCaseId"]) || "—",
    status: getString(entry, ["status"]) || "—",
    amountUsd: getNumber(entry, ["amountUsd", "amount"]),
    reason: getString(entry, ["reason"]) || "—",
    openedAt: getString(entry, ["openedAt", "createdAt"]) || "",
    resolvedAt: mapNullableString(entry.resolvedAt),
  };
}

export function mapSuperAdminBillingInvoiceDetail(
  payload: unknown,
): SuperAdminBillingInvoiceDetail {
  const root = extractRoot(payload);
  if (!isRecord(root)) {
    throw new Error("Unable to load invoice details right now.");
  }

  const invoiceId = getNumber(root, ["invoiceId", "id"]);
  if (!invoiceId) {
    throw new Error("Invoice not found.");
  }

  const adjustmentsRaw = Array.isArray(root.adjustments) ? root.adjustments : [];
  const disputesRaw = Array.isArray(root.disputes) ? root.disputes : [];

  return {
    invoiceId,
    organisationId: getNumber(root, ["organisationId", "organizationId"]) || null,
    organisationName:
      getString(root, ["organisationName", "organizationName", "organisation"]) || "—",
    subscriptionId: getNumber(root, ["subscriptionId"]) || null,
    planCode: getString(root, ["planCode"]) || "",
    planName: getString(root, ["planName", "plan"]) || "—",
    billingCycle: getString(root, ["billingCycle"]) || "—",
    status: getString(root, ["status"]) || "Unknown",
    amount: getNumber(root, ["amount", "totalAmount"]),
    outstandingBalance: getNumber(root, [
      "outstandingBalance",
      "currentBalance",
      "amountDue",
      "balance",
    ]),
    totalPaid: getNumber(root, ["totalPaid"]),
    refundedAmount: getNumber(root, ["refundedAmount"]),
    dueDate: getString(root, ["dueDate"]) || "",
    billingPeriodStart: getString(root, ["billingPeriodStart", "periodStart"]) || "",
    billingPeriodEnd: getString(root, ["billingPeriodEnd", "periodEnd"]) || "",
    paidAt: mapNullableString(root.paidAt),
    createdAt: getString(root, ["createdAt"]) || "",
    providerInvoiceId: mapNullableString(root.providerInvoiceId),
    providerChargeId: mapNullableString(root.providerChargeId),
    providerPaymentIntentId: mapNullableString(root.providerPaymentIntentId),
    adjustments: adjustmentsRaw
      .map(mapInvoiceAdjustment)
      .filter(Boolean) as SuperAdminBillingInvoiceAdjustment[],
    disputes: disputesRaw
      .map(mapInvoiceDispute)
      .filter(Boolean) as SuperAdminBillingInvoiceDispute[],
  };
}
