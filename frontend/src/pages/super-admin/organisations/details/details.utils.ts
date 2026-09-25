import type { OrganisationStatus } from "../organisations.data";

export type StatusVariant = "green" | "yellow" | "gray";

export function getStatusVariant(status: OrganisationStatus): StatusVariant {
  if (status === "Active") {
    return "green";
  }

  if (status === "Pending Activation") {
    return "yellow";
  }

  return "gray";
}
