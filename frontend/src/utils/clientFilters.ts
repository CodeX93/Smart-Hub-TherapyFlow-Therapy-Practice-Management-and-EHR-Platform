import type { ClientFilters } from "@/types/client.type";

export function areClientFiltersEqual(
  left: ClientFilters,
  right: ClientFilters,
): boolean {
  return JSON.stringify(left) === JSON.stringify(right);
}
