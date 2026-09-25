import { lazy, Suspense } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useAuthBootstrap } from "@/hooks/useAuthBootstrap";
import type { StaffRouteId } from "@/utils/staffPermissions";
import UserLayout from "../layouts/UserLayout";
import AuthLayout from "../layouts/AuthLayout";
import TherapistLayout from "../layouts/TherapistLayout";
import AdminLayout from "../layouts/AdminLayout";
import StaffLayout from "../layouts/StaffLayout";
import ErrorScreen from "../components/shared/ErrorScreen";
import { StaffRouteGuard } from "@/components/staff/StaffRouteGuard";
import { RoleRouteGuard } from "@/components/auth/RoleRouteGuard";
import { StaffIndexRedirect } from "@/components/staff/StaffIndexRedirect";
import SuperAdminLayout from "@/layouts/SuperAdminLayout";

const Invoices = lazy(() => import("../pages/user/invoices"));
const Documents = lazy(() => import("../pages/user/documents"));
const ClinicalForms = lazy(() => import("../pages/user/clinical-forms"));
const ClinicalFormDetail = lazy(() => import("../pages/user/clinical-form-detail"));
const PrivacySettings = lazy(() => import("../pages/user/privacy-settings"));
const ClientMyProfile = lazy(() => import("../pages/user/my-profile"));
const Login = lazy(() => import("../pages/auth/login"));
const ClientActivateAccount = lazy(() => import("../pages/auth/activate-your-account"));
const ClientSetNewPassword = lazy(() => import("../pages/auth/set-new-password"));
const PortalActivatePage = lazy(() => import("../pages/portal/activate"));
const PortalInvoicesRedirect = lazy(() => import("../pages/portal/invoices-redirect"));
const ResetPassword = lazy(() => import("../pages/auth/forgot-password"));
const CheckEmail = lazy(() => import("../pages/auth/check-email"));
const Appointments = lazy(() => import("../pages/user/appointments"));
const BookedSessions = lazy(() => import("../pages/user/booked-sessions"));
const BookedSessionDetail = lazy(() => import("../pages/user/booked-sessions/detail"));
const Dashboard = lazy(() => import("../pages/therapist/dashboard"));
const Clients = lazy(() => import("../pages/therapist/clients"));
const Scheduling = lazy(() => import("../pages/therapist/scheduling"));
const Billings = lazy(() => import("../pages/therapist/billings"));
const Tasks = lazy(() => import("../pages/therapist/tasks"));
const TaskHistory = lazy(() => import("../pages/therapist/tasks/history"));
const TherapistSystemNotifications = lazy(() => import("@/pages/therapist/system-notifications"));
const SecuritySettingsPage = lazy(() => import("@/pages/shared/security"));
const Settings = lazy(() => import("../pages/admin/settings"));
const PaymentsAndSubscription = lazy(() => import("@/pages/admin/payments-and-subscription"));
const AdminDashboard = lazy(() => import("../pages/admin/dashboard"));
const AdminClients = lazy(() => import("../pages/admin/clients"));
const TherapistLogin = lazy(() => import("@/pages/auth/therapist/login"));
const TherapistForgotPassword = lazy(() => import("@/pages/auth/therapist/forgot-password"));
const TherapistPasswordResetSuccess = lazy(() => import("@/pages/auth/therapist/password-reset-success"));
const TherapistSetNewPassword = lazy(() => import("@/pages/auth/therapist/set-new-password"));
const TherapistActivateYourAccount = lazy(() => import("@/pages/auth/therapist/activate-your-account"));
const TherapistCheckEmail = lazy(() => import("@/pages/auth/therapist/check-email"));
const SuperAdminLogin = lazy(() => import("@/pages/auth/super-admin/login"));
const SuperAdminForgotPassword = lazy(() => import("@/pages/auth/super-admin/forgot-password"));
const AdminScheduling = lazy(() => import("@/pages/admin/scheduling"));
const AdminBillings = lazy(() => import("@/pages/admin/billings"));
const AdminTasksPage = lazy(() => import("@/pages/admin/tasks"));
const AdminTaskHistory = lazy(() => import("@/pages/admin/tasks/history"));
const AdminSystemNotifications = lazy(() => import("@/pages/admin/system-notifications"));
const AdminLibrary = lazy(() => import("@/pages/admin/content/library"));
const AdminAssessment = lazy(() => import("@/pages/admin/content/assessment"));
const AdminClinicalForms = lazy(() => import("@/pages/admin/content/clinical-forms"));
const CreateConsent = lazy(() => import("@/pages/admin/content/clinical-forms/consent"));
const AdminProcessCheckLists = lazy(() => import("@/pages/admin/content/process-checklists"));
const AdminReportTemplates = lazy(() => import("@/pages/admin/content/report-templates"));
const PrivacyPolicy = lazy(() => import("@/pages/admin/compliance/privacy"));
const UserProfiles = lazy(() => import("@/pages/admin/user-access/profiles"));
const Hipaa = lazy(() => import("@/pages/admin/compliance/hipaa"));
const Roles = lazy(() => import("@/pages/admin/user-access/roles"));
const CreateAssessment = lazy(() => import("@/pages/admin/content/assessment/create-assessment"));
const DuplicateDetection = lazy(() => import("@/pages/admin/user-access/duplicate-detection"));
const StaffClients = lazy(() => import("@/pages/staff/clients"));
const StaffScheduling = lazy(() => import("@/pages/staff/scheduling"));
const StaffBillings = lazy(() => import("@/pages/staff/billings"));
const StaffTasksPage = lazy(() => import("@/pages/staff/tasks"));
const StaffTaskHistory = lazy(() => import("@/pages/staff/tasks/history"));
const StaffLibrary = lazy(() => import("@/pages/staff/content/library"));
const StaffAssessment = lazy(() => import("@/pages/staff/content/assessment"));
const StaffClinicalForms = lazy(() => import("@/pages/staff/content/clinical-forms"));
const StaffProcessCheckLists = lazy(() => import("@/pages/staff/content/process-checklists"));
const StaffHipaa = lazy(() => import("@/pages/staff/compliance/hipaa"));
const StaffPrivacy = lazy(() => import("@/pages/staff/compliance/privacy"));
const StaffSystemNotifications = lazy(() => import("@/pages/staff/system/notifications"));
const StaffUserProfiles = lazy(() => import("@/pages/staff/user-access/profiles"));
const StaffNoAccess = lazy(() => import("@/pages/staff/no-access"));
const SuperAdminDashboard = lazy(() => import("@/pages/super-admin/dashboard"));
const FeatureFlagsCatalog = lazy(() => import("@/pages/super-admin/feature-flags"));
const FeatureFlagsRollout = lazy(() => import("@/pages/super-admin/feature-flags/rollout"));
const CreateFeature = lazy(() => import("@/pages/super-admin/feature-flags/create"));
const CreatePlan = lazy(() => import("@/pages/super-admin/billings-and-plans/create-plan"));
const InvoiceDetails = lazy(() => import("@/pages/super-admin/billings-and-plans/invoice-details"));
const Organisations = lazy(() => import("@/pages/super-admin/organisations"));
const OrganisationDetails = lazy(() => import("@/pages/super-admin/organisations/details"));
const TenantSettings = lazy(() => import("@/pages/super-admin/organisations/settings"));
const CreateOrganisation = lazy(() => import("@/pages/super-admin/organisations/create"));
const ImpersonateAdmin = lazy(() => import("@/pages/super-admin/organisations/impersonate"));
const FeatureToggles = lazy(() => import("@/pages/super-admin/organisations/feature-toggles"));
const InvoicesListScreen = lazy(() => import("@/pages/super-admin/organisations/invoices"));
const RolesAndPermissions = lazy(() => import("@/pages/super-admin/roles-and-permissions"));
const CreateCustomRole = lazy(() => import("@/pages/super-admin/roles-and-permissions/create"));
const MatchDesignToFrame = lazy(() => import("@/pages/super-admin/billings-and-plans"));
const SystemSettings = lazy(() => import("@/pages/super-admin/system-settings"));
const MyProfilePage = lazy(() => import("@/pages/super-admin/my-profile"));
const PlanEntitlements = lazy(() => import("@/pages/super-admin/billings-and-plans/plan-entitlements"));
const AuditLogs = lazy(() => import("@/pages/super-admin/audit-logs"));
const Integrations = lazy(() => import("@/pages/super-admin/integrations"));
const CmsHub = lazy(() => import("@/pages/super-admin/cms"));
const CmsLandingPageEditor = lazy(() => import("@/pages/super-admin/cms/landing-page"));
const CmsLearningHubList = lazy(() => import("@/pages/super-admin/cms/learning-hub"));
const CmsLearningHubEditor = lazy(() => import("@/pages/super-admin/cms/learning-hub/edit"));
const SuperAdminChangePasswordPage = lazy(() => import("@/pages/super-admin/my-profile/change-password"));
const DunningPolicyPage = lazy(() => import("@/pages/super-admin/billings-and-plans/dunning-policy"));
const BookingRequestsPage = lazy(() => import("@/pages/super-admin/booking-requests"));
const BookingRequestDetailsPage = lazy(() => import("@/pages/super-admin/booking-requests/[id]"));
const StripeConnectSuccessPage = lazy(() => import("@/pages/stripe/success"));
const SubscriptionBillingPage = lazy(() => import("@/pages/billing/subscription"));
const SubscriptionSuccessPage = lazy(() => import("@/pages/billing/subscription/success"));
const BillingSubscriptionRedirect = lazy(() => import("@/pages/billing/subscription/redirect"));

const RootRedirect = () => {
  const hostname = window.location.hostname;
  if (hostname.startsWith("superadmin.") || hostname.startsWith("super-admin.")) {
    return <Navigate to="/super-admin/login" replace />;
  }
  return <Navigate to="/auth/login" replace />;
};

/**
 * Staff pages that render outside `StaffLayout` still need the permissions its
 * bootstrap fetches before `StaffRouteGuard` can decide.
 */
const StandaloneStaffRoute = ({
  routeId,
  children,
}: {
  routeId: StaffRouteId;
  children: React.ReactNode;
}) => {
  useAuthBootstrap();
  return <StaffRouteGuard routeId={routeId}>{children}</StaffRouteGuard>;
};

const AppRoutes: React.FC = () => {
  return (
    <Suspense fallback={<ContentLoader className="h-screen" />}>
    <Routes>
      <Route path="/" element={<RootRedirect />} />
      {/* *************  Auth Routes ************ */}
      <Route path="/auth" element={<AuthLayout />}>

       {/* *************  User Auth Routes ************ */}
        <Route path="login" element={<Login />} />
        <Route path="forgot-password" element={<ResetPassword />} />
        <Route path="check-email" element={<CheckEmail />} />
        <Route
          path="password-reset-success"
          element={<TherapistPasswordResetSuccess />}
        />
        <Route
          path="set-new-password"
          element={<ClientSetNewPassword />}
        />
        <Route
          path="activate-your-account"
          element={<ClientActivateAccount />}
        />

       {/* *************  Therapist Auth Routes ************ */}
        <Route path="staff/login" element={<TherapistLogin />} />
        <Route
          path="staff/forgot-password"
          element={<TherapistForgotPassword />}
        />
        <Route path="staff/check-email" element={<TherapistCheckEmail />} />
        <Route
          path="staff/password-reset-success"
          element={<TherapistPasswordResetSuccess />}
        />
        <Route
          path="staff/set-new-password"
          element={<TherapistSetNewPassword />}
        />
        <Route
          path="staff/activate-your-account"
          element={<TherapistActivateYourAccount />}
        />
        <Route path="therapist/login" element={<Navigate to="/auth/staff/login" replace />} />
        <Route
          path="therapist/forgot-password"
          element={<Navigate to="/auth/staff/forgot-password" replace />}
        />
        <Route
          path="therapist/check-email"
          element={<Navigate to="/auth/staff/check-email" replace />}
        />
        <Route
          path="therapist/password-reset-success"
          element={<Navigate to="/auth/staff/password-reset-success" replace />}
        />
        <Route
          path="therapist/set-new-password"
          element={<Navigate to="/auth/staff/set-new-password" replace />}
        />
        <Route
          path="therapist/activate-your-account"
          element={<Navigate to="/auth/staff/activate-your-account" replace />}
        />
      </Route>

      <Route path="/portal" element={<AuthLayout />}>
        <Route path="activate/:token" element={<PortalActivatePage />} />
      </Route>
      <Route path="/portal/invoices" element={<PortalInvoicesRedirect />} />
      <Route path="/stripe/success" element={<StripeConnectSuccessPage />} />
      <Route path="/billing/subscription" element={<BillingSubscriptionRedirect />} />
      <Route path="/billing/subscription/success" element={<SubscriptionSuccessPage />} />

      <Route path="/super-admin/login" element={<SuperAdminLogin />} />
      <Route path="/super-admin/forgot-password" element={<SuperAdminForgotPassword />} />

      {/* *************  User Routes ************ */}
      <Route
        path="/user"
        element={
          <RoleRouteGuard allow={["user"]}>
            <UserLayout />
          </RoleRouteGuard>
        }
      >
        <Route path="appointments" element={<Appointments />} />
        <Route path="booked-sessions" element={<BookedSessions />} />
        <Route path="booked-sessions/:sessionId" element={<BookedSessionDetail />} />
        <Route path="invoices" element={<Invoices />} />
        <Route path="documents" element={<Documents />} />
        <Route path="clinical-forms" element={<ClinicalForms />} />
        <Route path="clinical-forms/:formId" element={<ClinicalFormDetail />} />
        <Route path="privacy-settings" element={<PrivacySettings />} />
        <Route path="my-profile" element={<ClientMyProfile />} />
      </Route>

      <Route
        path="/therapist"
        element={
          <RoleRouteGuard allow={["therapist"]}>
            <TherapistLayout />
          </RoleRouteGuard>
        }
      >
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="clients" element={<Clients />} />
        <Route path="clients/:clientId" element={<Clients />} />
        <Route path="scheduling" element={<Scheduling />} />
        <Route path="billings" element={<Billings />} />
        <Route path="tasks" element={<Tasks />} />
        <Route path="tasks/history" element={<TaskHistory />} />
        <Route path="system/notifications" element={<TherapistSystemNotifications />} />
        <Route path="system/notification" element={<Navigate to="/therapist/system/notifications" replace />} />
        <Route path="security" element={<SecuritySettingsPage />} />
      </Route>

      <Route
        path="/admin"
        element={
          <RoleRouteGuard allow={["admin"]}>
            <AdminLayout />
          </RoleRouteGuard>
        }
      >
        <Route path="dashboard" element={<AdminDashboard />} />
        <Route path="scheduling" element={<AdminScheduling />} />
        <Route path="settings" element={<Settings />} />
        <Route path="payments-and-subscription" element={<PaymentsAndSubscription />} />
        <Route path="security" element={<SecuritySettingsPage />} />
        <Route path="clients" element={<AdminClients />} />
        <Route path="billings" element={<AdminBillings />} />
        <Route path="tasks" element={<AdminTasksPage />} />
        <Route path="tasks/history" element={<AdminTaskHistory />} />
        <Route path="system/notifications" element={<AdminSystemNotifications />} />
        <Route path="system/notification" element={<Navigate to="/admin/system/notifications" replace />} />
        <Route path="content/library" element={<AdminLibrary />} />
        <Route path="content/assessment" element={<AdminAssessment />} />
        <Route path="content/clinical-forms" element={<AdminClinicalForms />} />
        <Route path="content/process-checklists" element={<AdminProcessCheckLists />} />
        <Route path="content/report-templates" element={<AdminReportTemplates />} />
        <Route path="compliance/privacy" element={<PrivacyPolicy />} />
        <Route path="compliance/hipaa" element={<Hipaa />} />
        <Route path="user-access/profiles" element={<UserProfiles />} />
        <Route path="user-access/roles" element={<Roles />} />
        <Route path="user-access/duplicate-detection" element={<DuplicateDetection />} />
        <Route path="billing/subscription" element={<SubscriptionBillingPage />} />
      </Route>

      <Route
        path="/staff"
        element={
          <RoleRouteGuard allow={["staff"]}>
            <StaffLayout />
          </RoleRouteGuard>
        }
      >
        <Route index element={<StaffIndexRedirect />} />
        <Route path="no-access" element={<StaffNoAccess />} />
        <Route
          path="clients"
          element={
            <StaffRouteGuard routeId="clients">
              <StaffClients />
            </StaffRouteGuard>
          }
        />
        <Route
          path="clients/:clientId"
          element={
            <StaffRouteGuard routeId="clients">
              <StaffClients />
            </StaffRouteGuard>
          }
        />
        <Route
          path="scheduling"
          element={
            <StaffRouteGuard routeId="scheduling">
              <StaffScheduling />
            </StaffRouteGuard>
          }
        />
        <Route
          path="billings"
          element={
            <StaffRouteGuard routeId="billings">
              <StaffBillings />
            </StaffRouteGuard>
          }
        />
        <Route
          path="tasks"
          element={
            <StaffRouteGuard routeId="tasks">
              <StaffTasksPage />
            </StaffRouteGuard>
          }
        />
        <Route
          path="tasks/history"
          element={
            <StaffRouteGuard routeId="tasks">
              <StaffTaskHistory />
            </StaffRouteGuard>
          }
        />
        <Route
          path="user-access/profiles"
          element={
            <StaffRouteGuard routeId="user-profiles">
              <StaffUserProfiles />
            </StaffRouteGuard>
          }
        />
        <Route
          path="system/notifications"
          element={
            <StaffRouteGuard routeId="system-notifications">
              <StaffSystemNotifications />
            </StaffRouteGuard>
          }
        />
        <Route path="system/notification" element={<Navigate to="/staff/system/notifications" replace />} />
        <Route path="security" element={<SecuritySettingsPage />} />
        <Route
          path="content/library"
          element={
            <StaffRouteGuard routeId="content-library">
              <StaffLibrary />
            </StaffRouteGuard>
          }
        />
        <Route
          path="content/assessment"
          element={
            <StaffRouteGuard routeId="content-assessment">
              <StaffAssessment />
            </StaffRouteGuard>
          }
        />
        <Route
          path="content/clinical-forms"
          element={
            <StaffRouteGuard routeId="content-clinical-forms">
              <StaffClinicalForms />
            </StaffRouteGuard>
          }
        />
        <Route
          path="content/process-checklists"
          element={
            <StaffRouteGuard routeId="content-process-checklists">
              <StaffProcessCheckLists />
            </StaffRouteGuard>
          }
        />
        <Route
          path="compliance/privacy"
          element={
            <StaffRouteGuard routeId="compliance-privacy">
              <StaffPrivacy />
            </StaffRouteGuard>
          }
        />
        <Route
          path="compliance/hipaa"
          element={
            <StaffRouteGuard routeId="compliance-hipaa">
              <StaffHipaa />
            </StaffRouteGuard>
          }
        />
        <Route path="billing/subscription" element={<SubscriptionBillingPage />} />
      </Route>
      <Route
        path="admin/content/clinical-forms/consent/:formId"
        element={
          <RoleRouteGuard allow={["admin"]}>
            <CreateConsent />
          </RoleRouteGuard>
        }
      />
      <Route
        path="admin/content/assessment/create-assessment"
        element={
          <RoleRouteGuard allow={["admin"]}>
            <CreateAssessment />
          </RoleRouteGuard>
        }
      />
      <Route
        path="staff/content/clinical-forms/consent/:formId"
        element={
          <RoleRouteGuard allow={["staff"]}>
            <StandaloneStaffRoute routeId="content-clinical-forms">
              <CreateConsent />
            </StandaloneStaffRoute>
          </RoleRouteGuard>
        }
      />
      <Route
        path="staff/content/assessment/create-assessment"
        element={
          <RoleRouteGuard allow={["staff"]}>
            <StandaloneStaffRoute routeId="content-assessment">
              <CreateAssessment />
            </StandaloneStaffRoute>
          </RoleRouteGuard>
        }
      />

      <Route
        path="/super-admin"
        element={
          <RoleRouteGuard allow={["super-admin"]}>
            <SuperAdminLayout />
          </RoleRouteGuard>
        }
      >
        <Route path="dashboard" element={<SuperAdminDashboard />} />
        <Route path="organisations" element={<Organisations />} />
        <Route path="organisations/new" element={<CreateOrganisation />} />
        <Route path="organisations/:slug" element={<OrganisationDetails />} />
        <Route path="organisations/:slug/settings" element={<TenantSettings />} />
        <Route
          path="organisations/:slug/impersonate"
          element={<ImpersonateAdmin />}
        />
        <Route
          path="organisations/:slug/feature-toggles"
          element={<FeatureToggles />}
        />
        <Route
          path="organisations/:slug/invoices"
          element={<InvoicesListScreen />}
        />
        <Route
          path="roles-and-permissions"
          element={<RolesAndPermissions />}
        />
        <Route
          path="roles-and-permissions/create"
          element={<CreateCustomRole />}
        />
        <Route path="feature-flags" element={<FeatureFlagsCatalog />} />
        <Route path="feature-flags/rollout" element={<FeatureFlagsRollout />} />
        <Route path="feature-flags/create" element={<CreateFeature />} />
        <Route path="billings-and-plans" element={<MatchDesignToFrame />} />
        <Route path="billings-and-plans/create-plan" element={<CreatePlan />} />
        <Route path="billings-and-plans/plan-entitlements" element={<PlanEntitlements />} />
        <Route path="billings-and-plans/dunning-policy" element={<DunningPolicyPage />} />
        <Route
          path="billings-and-plans/invoices/:invoiceId"
          element={<InvoiceDetails />}
        />
        <Route path="my-profile" element={<MyProfilePage />} />
        <Route path="my-profile/change-password" element={<SuperAdminChangePasswordPage />} />
        <Route path="security" element={<SecuritySettingsPage />} />
        <Route path="system-settings" element={<SystemSettings />} />
        <Route path="integrations" element={<Integrations />} />
        <Route path="cms" element={<CmsHub />} />
        <Route path="cms/landing-page" element={<CmsLandingPageEditor />} />
        <Route path="cms/learning-hub" element={<CmsLearningHubList />} />
        <Route path="cms/learning-hub/new" element={<CmsLearningHubEditor />} />
        <Route path="cms/learning-hub/:id" element={<CmsLearningHubEditor />} />
        <Route path="audit-logs" element={<AuditLogs />} />
        <Route path="booking-requests" element={<BookingRequestsPage />} />
        <Route path="booking-requests/:id" element={<BookingRequestDetailsPage />} />
      </Route>

      {/* *************  Error Screen Test Routes ************ */}
      <Route path="/errors/404" element={<ErrorScreen type="404" onAction={() => window.location.href = '/'} />} />
      <Route path="/errors/401" element={<ErrorScreen type="401" onAction={() => window.location.href = '/auth/login'} />} />
      <Route path="/errors/403" element={<ErrorScreen type="403" onAction={() => window.history.back()} />} />
      <Route path="/errors/500" element={<ErrorScreen type="500" onAction={() => window.location.reload()} />} />
      <Route path="/errors/503" element={<ErrorScreen type="503" onAction={() => window.location.reload()} />} />
      <Route path="/errors/session-expired" element={<ErrorScreen type="session-expired" onAction={() => window.location.href = '/auth/login'} />} />
      <Route path="/errors/offline" element={<ErrorScreen type="offline" onAction={() => window.location.reload()} />} />
      <Route path="/errors/maintenance" element={<ErrorScreen type="maintenance" onAction={() => window.location.reload()} />} />

      <Route path="*" element={<ErrorScreen type="404" onAction={() => window.location.href = '/'} />} />
    </Routes>
    </Suspense>
  );
};

export default AppRoutes;
