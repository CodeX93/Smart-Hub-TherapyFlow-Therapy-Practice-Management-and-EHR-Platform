export function formatAdminUserRoleLabel(role: string): string {
  const trimmed = role.trim();
  if (!trimmed) return "";

  return trimmed
    .split(/[_\s-]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(" ");
}

export function formatAdminUserRoleLabels(roles: string[] = []): string[] {
  return roles.map(formatAdminUserRoleLabel).filter(Boolean);
}
