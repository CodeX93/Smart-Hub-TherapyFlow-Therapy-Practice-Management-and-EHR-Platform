import { Button } from "@/components/ui/button";
import { useState } from "react";
import SemanticStatusBadge from "@/components/shared/SemanticStatusBadge";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import Toast from "@/components/shared/Toast";
import {
  useBackupOrganisationMutation,
  useBootstrapOrganisationStaffProfilesMutation,
  useGetOrganisationHealthQuery,
  useGetOrganisationSchemaVersionQuery,
  useLockOrganisationMutation,
  useProvisionOrganisationSchemaMutation,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";

function toDisplayValue(value: unknown): string {
  if (value === null || value === undefined || value === "") return "—";
  if (typeof value === "boolean") return value ? "Yes" : "No";
  return String(value);
}

function ProvisionTab(props: { organisationId: number | null }) {
  const {
    data: schemaVersion,
    isLoading,
    isError,
    error,
    refetch,
  } = useGetOrganisationSchemaVersionQuery(props.organisationId ?? 0, {
    skip: !props.organisationId,
  });
  const [provisionSchema, { isLoading: isProvisioning, error: provisionError }] =
    useProvisionOrganisationSchemaMutation();
  const {
    data: healthData,
    isLoading: isHealthLoading,
    isError: isHealthError,
    error: healthError,
    refetch: refetchHealth,
  } = useGetOrganisationHealthQuery(props.organisationId ?? 0, {
    skip: !props.organisationId,
  });
  const [lockOrganisation, { isLoading: isLocking, error: lockError }] =
    useLockOrganisationMutation();
  const [backupOrganisation, { isLoading: isBackingUp, error: backupError }] =
    useBackupOrganisationMutation();
  const [
    bootstrapOrganisationStaffProfiles,
    { isLoading: isBootstrappingOrg, error: bootstrapOrgError },
  ] = useBootstrapOrganisationStaffProfilesMutation();
  const [actionSuccessMessage, setActionSuccessMessage] = useState<string | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [isProvisionConfirmOpen, setIsProvisionConfirmOpen] = useState(false);

  const hasSchemaVersion = Boolean(schemaVersion?.version);
  const schemaMissing = !hasSchemaVersion && (isError || !isLoading);

  async function handleProvision() {
    if (!props.organisationId) return;
    try {
      await provisionSchema(props.organisationId).unwrap();
      setIsProvisionConfirmOpen(false);
      await Promise.all([refetch(), refetchHealth()]);
      setToastType("success");
      setToastMessage(
        hasSchemaVersion
          ? "Schema re-provision queued successfully."
          : "Schema provision queued successfully.",
      );
    } catch (err) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(err));
    }
  }

  async function handleBootstrapOrg() {
    if (!props.organisationId) return;
    try {
      await bootstrapOrganisationStaffProfiles(props.organisationId).unwrap();
    } catch {
      // error rendered below
    }
  }

  async function handleLockOrganisation() {
    if (!props.organisationId) return;
    try {
      setActionSuccessMessage(null);
      const response = await lockOrganisation(props.organisationId).unwrap();
      setActionSuccessMessage(response.message || "Tenant locked for maintenance.");
      await refetchHealth();
    } catch {
      // error rendered below
    }
  }

  async function handleBackupOrganisation() {
    if (!props.organisationId) return;
    try {
      setActionSuccessMessage(null);
      const response = await backupOrganisation(props.organisationId).unwrap();
      setActionSuccessMessage(
        response.message || "Tenant backup job queued."
      );
    } catch {
      // error rendered below
    }
  }

  return (
    <div className="w-full rounded-[1rem] border border-[#e3ebf3] bg-white px-5 py-5 shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      <div className="text-[1rem] font-semibold leading-6 text-[#1f2d38]">Provision</div>
      <div className="mt-1 text-[0.875rem] leading-[1.375rem] text-[#8f9aa6]">
        Tenant schema provisioning and migration status.
      </div>

      {isLoading ? (
        <div className="mt-5 text-sm text-[#667483]">Loading schema version...</div>
      ) : null}

      {hasSchemaVersion ? (
        <div className="mt-5 rounded-[0.75rem] border border-[#e3ebf3] bg-[#f8fbfd] px-4 py-3">
          <div className="text-[0.8125rem] font-medium text-[#1f2d38]">
            Schema Version: {schemaVersion?.version}
          </div>
          {schemaVersion?.updatedAt ? (
            <div className="mt-1 text-[0.75rem] text-[#7b8794]">
              Updated: {schemaVersion.updatedAt}
            </div>
          ) : null}
        </div>
      ) : null}

      {schemaMissing ? (
        <div className="mt-5 rounded-[0.75rem] border border-[#f7d3d7] bg-[#fff4f5] px-4 py-3 text-[0.8125rem] text-[#b42318]">
          {isError ? getApiErrorMessage(error) : "No schema version found."}
        </div>
      ) : null}

      {isHealthLoading ? (
        <div className="mt-5 text-sm text-[#667483]">Loading tenant health...</div>
      ) : null}
      {healthData ? (
        <div className="mt-5 rounded-[0.75rem] border border-[#e3ebf3] bg-[#f8fbfd] px-4 py-3">
          <div className="text-[0.8125rem] font-medium text-[#1f2d38]">Tenant Health</div>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <SemanticStatusBadge
              status={toDisplayValue((healthData as Record<string, unknown>).status)}
              className="rounded-full border px-2.5 py-1 text-[0.6875rem] font-semibold"
            >
              Status: {toDisplayValue((healthData as Record<string, unknown>).status)}
            </SemanticStatusBadge>
            <SemanticStatusBadge
              status={toDisplayValue((healthData as Record<string, unknown>).migrationStatus)}
              className="rounded-full border px-2.5 py-1 text-[0.6875rem] font-semibold"
            >
              Migration: {toDisplayValue((healthData as Record<string, unknown>).migrationStatus)}
            </SemanticStatusBadge>
          </div>
          <div className="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
            <div className="rounded-[0.625rem] border border-[#e3ebf3] bg-white px-3 py-2">
              <div className="text-[0.6875rem] font-semibold uppercase tracking-[0.015rem] text-[#8a96a3]">
                Organisation ID
              </div>
              <div className="mt-1 text-[0.8125rem] font-medium text-[#1f2d38]">
                {toDisplayValue((healthData as Record<string, unknown>).organisationId)}
              </div>
            </div>
            <div className="rounded-[0.625rem] border border-[#e3ebf3] bg-white px-3 py-2">
              <div className="text-[0.6875rem] font-semibold uppercase tracking-[0.015rem] text-[#8a96a3]">
                Schema Name
              </div>
              <div className="mt-1 text-[0.8125rem] font-medium text-[#1f2d38]">
                {toDisplayValue((healthData as Record<string, unknown>).schemaName)}
              </div>
            </div>
            <div className="rounded-[0.625rem] border border-[#e3ebf3] bg-white px-3 py-2">
              <div className="text-[0.6875rem] font-semibold uppercase tracking-[0.015rem] text-[#8a96a3]">
                Version
              </div>
              <div className="mt-1 text-[0.8125rem] font-medium text-[#1f2d38]">
                {toDisplayValue((healthData as Record<string, unknown>).version)}
              </div>
            </div>
            <div className="rounded-[0.625rem] border border-[#e3ebf3] bg-white px-3 py-2">
              <div className="text-[0.6875rem] font-semibold uppercase tracking-[0.015rem] text-[#8a96a3]">
                Schema Exists
              </div>
              <div className="mt-1 text-[0.8125rem] font-medium text-[#1f2d38]">
                {toDisplayValue((healthData as Record<string, unknown>).schemaExists)}
              </div>
            </div>
            <div className="rounded-[0.625rem] border border-[#e3ebf3] bg-white px-3 py-2 sm:col-span-2">
              <div className="text-[0.6875rem] font-semibold uppercase tracking-[0.015rem] text-[#8a96a3]">
                Migrated At
              </div>
              <div className="mt-1 text-[0.8125rem] font-medium text-[#1f2d38]">
                {toDisplayValue((healthData as Record<string, unknown>).migratedAt)}
              </div>
            </div>
          </div>
        </div>
      ) : null}
      {isHealthError ? (
        <div className="mt-4 rounded-[0.75rem] border border-[#f7d3d7] bg-[#fff4f5] px-4 py-3 text-[0.8125rem] text-[#b42318]">
          {getApiErrorMessage(healthError)}
        </div>
      ) : null}

      {provisionError ? (
        <div className="mt-4 rounded-[0.75rem] border border-[#f7d3d7] bg-[#fff4f5] px-4 py-3 text-[0.8125rem] text-[#b42318]">
          {getApiErrorMessage(provisionError)}
        </div>
      ) : null}
      {lockError ? (
        <div className="mt-4 rounded-[0.75rem] border border-[#f7d3d7] bg-[#fff4f5] px-4 py-3 text-[0.8125rem] text-[#b42318]">
          {getApiErrorMessage(lockError)}
        </div>
      ) : null}
      {backupError ? (
        <div className="mt-4 rounded-[0.75rem] border border-[#f7d3d7] bg-[#fff4f5] px-4 py-3 text-[0.8125rem] text-[#b42318]">
          {getApiErrorMessage(backupError)}
        </div>
      ) : null}
      {bootstrapOrgError ? (
        <div className="mt-4 rounded-[0.75rem] border border-[#f7d3d7] bg-[#fff4f5] px-4 py-3 text-[0.8125rem] text-[#b42318]">
          {getApiErrorMessage(bootstrapOrgError)}
        </div>
      ) : null}
      {actionSuccessMessage ? (
        <div className="mt-4 rounded-[0.75rem] border border-[#d8ead7] bg-[#f4fbf3] px-4 py-3 text-[0.8125rem] text-[#166534]">
          {actionSuccessMessage}
        </div>
      ) : null}
      <div className="mt-5">
        <Button
          variant="primary"
          size="md"
          onClick={() => setIsProvisionConfirmOpen(true)}
          disabled={isProvisioning || !props.organisationId}
          loading={isProvisioning}
          loadingLabel="Provisioning..."
        >
          {hasSchemaVersion ? "Re-provision Schema" : "Provision Schema"}
        </Button>
      </div>

      <div className="mt-5 flex flex-wrap items-center gap-3">
        <Button
          variant="destructive"
          size="md"
          onClick={handleLockOrganisation}
          disabled={isLocking || !props.organisationId}
          loading={isLocking}
          loadingLabel="Locking..."
        >
          Lock Tenant
        </Button>
        <Button
          variant="primary"
          size="md"
          onClick={handleBackupOrganisation}
          disabled={isBackingUp || !props.organisationId}
          loading={isBackingUp}
          loadingLabel="Queuing backup..."
        >
          Backup Tenant
        </Button>
        <Button
          variant="primary"
          size="md"
          onClick={handleBootstrapOrg}
          disabled={isBootstrappingOrg || !props.organisationId}
          loading={isBootstrappingOrg}
          loadingLabel="Bootstrapping..."
        >
          Bootstrap Staff Profiles (Org)
        </Button>
      </div>

      <ConfirmationModal
        type={hasSchemaVersion ? "disable" : "activate"}
        isOpen={isProvisionConfirmOpen}
        onClose={() => {
          if (!isProvisioning) setIsProvisionConfirmOpen(false);
        }}
        onConfirm={() => void handleProvision()}
        title={
          hasSchemaVersion
            ? "Re-provision tenant schema?"
            : "Provision tenant schema?"
        }
        description={
          hasSchemaVersion
            ? "This queues a re-run of tenant database setup for this organisation. Review what will happen before continuing."
            : "This queues first-time tenant database setup for this organisation. Review what will happen before continuing."
        }
        items={
          hasSchemaVersion
            ? [
                "Marks the tenant for migration and runs pending Flyway tenant migrations",
                "Re-applies default seeds (staff profiles, system options, library, billing services) where needed",
                "Does not delete the organisation or wipe clinical client data by design",
                "Existing logins and passwords are not reset",
                "The job is queued in the background and may take a short time to finish",
              ]
            : [
                "Creates the tenant schema if it does not already exist",
                "Runs Flyway tenant migrations to the latest version",
                "Seeds default staff profiles, system options, library, and billing services",
                "The job is queued in the background and may take a short time to finish",
              ]
        }
        confirmButtonText={
          hasSchemaVersion ? "Yes, re-provision schema" : "Yes, provision schema"
        }
        confirmButtonLoading={isProvisioning}
        confirmButtonLoadingText="Provisioning..."
        confirmButtonDisabled={isProvisioning}
      />
    </div>
  );
}

export default ProvisionTab;
