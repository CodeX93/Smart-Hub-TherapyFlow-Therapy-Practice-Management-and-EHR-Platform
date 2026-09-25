export const EDIT_FEATURE_LIMITS = {
  nameMax: 80,
  descriptionMax: 500,
} as const;

export const EDIT_FEATURE_SCOPE_OPTIONS = ["Tenant", "Global"] as const;
export const EDIT_FEATURE_TYPE_OPTIONS = ["Core", "Custom", "Toggle", "Limit"] as const;

export function sanitizeFeatureName(value: string): string {
  return value.slice(0, EDIT_FEATURE_LIMITS.nameMax);
}

export function sanitizeFeatureDescription(value: string): string {
  return value.slice(0, EDIT_FEATURE_LIMITS.descriptionMax);
}

export function validateEditFeatureForm(values: {
  name: string;
  description: string;
  scope: string;
  type: string;
}): string | null {
  const name = values.name.trim();
  if (!name) return "Feature name is required.";
  if (name.length > EDIT_FEATURE_LIMITS.nameMax) {
    return `Feature name must be ${EDIT_FEATURE_LIMITS.nameMax} characters or less.`;
  }

  if (values.description.length > EDIT_FEATURE_LIMITS.descriptionMax) {
    return `Description must be ${EDIT_FEATURE_LIMITS.descriptionMax} characters or less.`;
  }

  if (
    !EDIT_FEATURE_SCOPE_OPTIONS.includes(
      values.scope as (typeof EDIT_FEATURE_SCOPE_OPTIONS)[number],
    )
  ) {
    return "Select a valid scope.";
  }

  if (
    !EDIT_FEATURE_TYPE_OPTIONS.includes(
      values.type as (typeof EDIT_FEATURE_TYPE_OPTIONS)[number],
    )
  ) {
    return "Select a valid type.";
  }

  return null;
}
