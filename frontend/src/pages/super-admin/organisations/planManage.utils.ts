export const PLAN_MANAGE_LIMITS = {
  trialDaysMax: 365,
} as const;

export const PLAN_MANAGE_TRIAL_DAYS_MAX_DIGITS = String(PLAN_MANAGE_LIMITS.trialDaysMax).length;

export function sanitizeTrialDaysInput(value: string): string {
  const digitsOnly = value.replace(/\D/g, "").slice(0, PLAN_MANAGE_TRIAL_DAYS_MAX_DIGITS);
  if (!digitsOnly) return "";

  const parsed = Number.parseInt(digitsOnly, 10);
  if (!Number.isFinite(parsed)) return "";
  if (parsed > PLAN_MANAGE_LIMITS.trialDaysMax) {
    return String(PLAN_MANAGE_LIMITS.trialDaysMax);
  }

  return digitsOnly;
}

export type PlanCatalogBillingDefaults = {
  billingCycle?: string | null;
  trialDays?: number | null;
};

export function resolvePlanBillingCycleValue(
  billingCycle?: string | null,
): "monthly" | "annual" {
  const normalized = (billingCycle ?? "").trim().toLowerCase();
  if (normalized.includes("year") || normalized === "annual") {
    return "annual";
  }
  return "monthly";
}

export function formatPlanBillingCycleLabel(cycle: "monthly" | "annual"): string {
  return cycle === "annual" ? "Annual" : "Monthly";
}

export function resolvePlanTrialDaysValue(trialDays?: number | null): string {
  if (typeof trialDays !== "number" || !Number.isFinite(trialDays)) {
    return "0";
  }
  return sanitizeTrialDaysInput(String(trialDays));
}

export function applyPlanCatalogDefaults(plan?: PlanCatalogBillingDefaults | null): {
  billingCycle: "monthly" | "annual";
  trialDays: string;
} {
  return {
    billingCycle: resolvePlanBillingCycleValue(plan?.billingCycle),
    trialDays: resolvePlanTrialDaysValue(plan?.trialDays),
  };
}

export function validatePlanManageInput(values: {
  plan: string;
  billingCycle: string;
  trialDays: string;
}): string | null {
  const trimmedPlan = values.plan.trim();
  if (!trimmedPlan) {
    return "Target plan is required.";
  }

  const cycle = values.billingCycle.trim().toLowerCase();
  if (!cycle || (cycle !== "monthly" && cycle !== "annual")) {
    return "Billing cycle must be Monthly or Annual.";
  }

  const trimmedTrialDays = values.trialDays.trim();
  if (trimmedTrialDays) {
    if (!/^\d+$/.test(trimmedTrialDays)) {
      return "Trial days must be a whole number.";
    }
    const parsedTrial = Number.parseInt(trimmedTrialDays, 10);
    if (
      !Number.isFinite(parsedTrial) ||
      parsedTrial < 0 ||
      parsedTrial > PLAN_MANAGE_LIMITS.trialDaysMax
    ) {
      return `Trial days must be between 0 and ${PLAN_MANAGE_LIMITS.trialDaysMax}.`;
    }
  }

  return null;
}
