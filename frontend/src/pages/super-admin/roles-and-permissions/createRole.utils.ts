export const CREATE_ROLE_FIELD_LIMITS = {
  roleName: 50,
  displayName: 100,
  description: 500,
} as const;

const ROLE_NAME_REGEX = /^[A-Za-z0-9_]+$/;

export function sanitizeRoleName(value: string): string {
  return value
    .replace(/[^A-Za-z0-9_]/g, "")
    .slice(0, CREATE_ROLE_FIELD_LIMITS.roleName);
}

export function sanitizeDisplayName(value: string): string {
  return value.slice(0, CREATE_ROLE_FIELD_LIMITS.displayName);
}

export function sanitizeRoleDescription(value: string): string {
  return value.slice(0, CREATE_ROLE_FIELD_LIMITS.description);
}

export function validateCreateRoleForm(values: {
  roleName: string;
  displayName: string;
  description: string;
}): string | null {
  const roleName = values.roleName.trim();
  if (!roleName) return "Role name is required.";
  if (roleName.length > CREATE_ROLE_FIELD_LIMITS.roleName) {
    return `Role name must be ${CREATE_ROLE_FIELD_LIMITS.roleName} characters or less.`;
  }
  if (!ROLE_NAME_REGEX.test(roleName)) {
    return "Role name must use letters, numbers, and underscores only.";
  }

  const displayName = values.displayName.trim();
  if (!displayName) return "Display name is required.";
  if (displayName.length > CREATE_ROLE_FIELD_LIMITS.displayName) {
    return `Display name must be ${CREATE_ROLE_FIELD_LIMITS.displayName} characters or less.`;
  }

  if (values.description.length > CREATE_ROLE_FIELD_LIMITS.description) {
    return `Description must be ${CREATE_ROLE_FIELD_LIMITS.description} characters or less.`;
  }

  return null;
}

export const validateEditRoleForm = validateCreateRoleForm;
