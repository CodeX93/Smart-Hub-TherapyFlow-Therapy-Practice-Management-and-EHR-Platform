import type { AuthMeResponse } from "@/store/api/authApi";
import type { Client } from "@/types/client.type";
import { matchesOptionKey } from "@/utils/systemOptions";

export function isClientInactive(client: Pick<Client, "clientStatus">): boolean {
  return matchesOptionKey(client.clientStatus, "inactive");
}

export function isClientPending(client: Pick<Client, "clientStatus">): boolean {
  return matchesOptionKey(client.clientStatus, "pending");
}

export function isClientClosedFile(
  client: Pick<Client, "clientStatus" | "clientStage">,
): boolean {
  return (
    isClientInactive(client) &&
    matchesOptionKey(client.clientStage, "closed")
  );
}

/** Backend treats active and on_hold as service-eligible (legacy on_hold may exist). */
export function isClientServiceEligible(
  client: Pick<Client, "clientStatus">,
): boolean {
  const status = client.clientStatus ?? "";
  return (
    matchesOptionKey(status, "active") || matchesOptionKey(status, "on_hold")
  );
}

export function shouldDisableClientForScheduling(
  client: Pick<Client, "clientStatus" | "clientStage">,
): boolean {
  if (isClientClosedFile(client)) return true;
  if (isClientInactive(client)) return true;
  if (isClientPending(client)) return true;
  return !isClientServiceEligible(client);
}

/**
 * Clear guidance when scheduling/notes are blocked by client status.
 * Prefer this over the old generic "inactive client file" wording.
 */
export function getClientSchedulingBlockMessage(
  client: Pick<Client, "clientStatus" | "clientStage">,
  statusLabel?: string,
): string | null {
  if (!shouldDisableClientForScheduling(client)) return null;

  const label =
    statusLabel?.trim() ||
    client.clientStatus?.trim() ||
    "unavailable";

  if (isClientPending(client)) {
    return (
      "Cannot schedule sessions while this client's status is Pending. " +
      "Open Edit → Clinical tab and set Status to Active."
    );
  }
  if (isClientInactive(client) || isClientClosedFile(client)) {
    return (
      "Cannot schedule sessions for a closed (Inactive) client file. " +
      "Use Open File, or open Edit → Clinical tab and set Status to Active."
    );
  }
  if (matchesOptionKey(client.clientStatus, "discharged")) {
    return (
      "Cannot schedule sessions while this client's status is Discharged. " +
      "Open Edit → Clinical tab and set Status to Active if care should resume."
    );
  }
  if (matchesOptionKey(client.clientStatus, "waitlist")) {
    return (
      "Cannot schedule sessions while this client's status is Waitlist. " +
      "Open Edit → Clinical tab and set Status to Active when ready to schedule."
    );
  }
  return (
    `Cannot schedule sessions while this client's status is ${label}. ` +
    "Open Edit → Clinical tab and set Status to Active."
  );
}

export function canReopenClientFile(authMe?: AuthMeResponse | null): boolean {
  if (authMe?.isTenantAdmin) return true;
  const roles = (authMe?.roles ?? []).map((r) => String(r).toUpperCase());
  const permissions = (authMe?.permissions ?? []).map((p) => String(p).toUpperCase());
  return roles.includes("SUPERVISOR") && permissions.includes("CLIENT_EDIT");
}
