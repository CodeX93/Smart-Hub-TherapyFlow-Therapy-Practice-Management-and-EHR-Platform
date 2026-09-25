const STORAGE_PREFIX = "tf.deviceTrust.";

function storageKey(loginKey: string): string {
  return `${STORAGE_PREFIX}${loginKey.trim().toLowerCase()}`;
}

/** Opaque device trust token returned after MFA with trustDevice=true. */
export function getDeviceTrustToken(loginKey: string): string | null {
  if (!loginKey.trim() || typeof window === "undefined") {
    return null;
  }
  try {
    const value = window.localStorage.getItem(storageKey(loginKey));
    return value && value.trim() ? value.trim() : null;
  } catch {
    return null;
  }
}

export function setDeviceTrustToken(loginKey: string, token: string): void {
  if (!loginKey.trim() || !token.trim() || typeof window === "undefined") {
    return;
  }
  try {
    window.localStorage.setItem(storageKey(loginKey), token.trim());
  } catch {
    // Ignore quota / private mode failures.
  }
}

export function clearDeviceTrustToken(loginKey: string): void {
  if (!loginKey.trim() || typeof window === "undefined") {
    return;
  }
  try {
    window.localStorage.removeItem(storageKey(loginKey));
  } catch {
    // Ignore.
  }
}

/** Best-effort token for marking "this device" in the devices list. */
export function findAnyDeviceTrustToken(): string | null {
  if (typeof window === "undefined") {
    return null;
  }
  try {
    for (let i = 0; i < window.localStorage.length; i += 1) {
      const key = window.localStorage.key(i);
      if (key?.startsWith(STORAGE_PREFIX)) {
        const value = window.localStorage.getItem(key);
        if (value?.trim()) {
          return value.trim();
        }
      }
    }
  } catch {
    return null;
  }
  return null;
}
