const BILLING_TOAST_STORAGE_KEY = "super-admin-billing-toast";

export type BillingToastType = "success" | "error" | "info";

export type BillingToastPayload = {
  message: string;
  type: BillingToastType;
};

let pendingBillingToast: BillingToastPayload | null = null;

function readStoredBillingToast(): BillingToastPayload | null {
  try {
    const raw = sessionStorage.getItem(BILLING_TOAST_STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<BillingToastPayload>;
    if (typeof parsed.message !== "string" || !parsed.message.trim()) {
      return null;
    }
    const type =
      parsed.type === "error" || parsed.type === "info" || parsed.type === "success"
        ? parsed.type
        : "success";
    return { message: parsed.message, type };
  } catch {
    return null;
  }
}

export function stashBillingToast(
  message: string,
  type: BillingToastType = "success",
): void {
  pendingBillingToast = { message, type };
  try {
    sessionStorage.setItem(
      BILLING_TOAST_STORAGE_KEY,
      JSON.stringify(pendingBillingToast),
    );
  } catch {
    // Ignore storage failures (private mode, quota, etc.)
  }
}

export function getBillingToast(): BillingToastPayload | null {
  if (pendingBillingToast) return pendingBillingToast;
  pendingBillingToast = readStoredBillingToast();
  return pendingBillingToast;
}

export function clearBillingToast(): void {
  pendingBillingToast = null;
  try {
    sessionStorage.removeItem(BILLING_TOAST_STORAGE_KEY);
  } catch {
    // Ignore storage failures
  }
}
