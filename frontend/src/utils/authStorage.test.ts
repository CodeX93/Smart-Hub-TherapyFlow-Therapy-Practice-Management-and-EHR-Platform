import assert from "node:assert/strict";
import test from "node:test";

const store = new Map<string, string>();
Object.assign(globalThis, {
  localStorage: {
    getItem: (key: string) => store.get(key) ?? null,
    setItem: (key: string, value: string) => void store.set(key, value),
    removeItem: (key: string) => void store.delete(key),
  },
});

const { clearAuthSession, getAuthSession, setAuthSession } = await import("./authStorage.ts");

test("keeps the access token in memory and out of localStorage", () => {
  clearAuthSession();
  setAuthSession({ accessToken: "access-jwt", role: "admin", tenantSchema: "tenant_1" });

  assert.equal(getAuthSession()?.accessToken, "access-jwt");
  const persisted = store.get("auth_session") ?? "";
  assert.ok(!persisted.includes("access-jwt"));
  assert.deepEqual(JSON.parse(persisted), { role: "admin", tenantSchema: "tenant_1" });
});

test("scrubs tokens a session saved before this change still holds", async () => {
  clearAuthSession();
  store.set(
    "auth_session",
    JSON.stringify({ accessToken: "old-access", refreshToken: "old-refresh", role: "therapist" }),
  );

  // A fresh module copy stands in for a reload: nothing is in memory yet.
  const reloaded = await import(`./authStorage.ts?reload=${Date.now()}`);
  const session = reloaded.getAuthSession();

  assert.equal(session?.role, "therapist");
  assert.equal(session?.accessToken, "");
  assert.deepEqual(JSON.parse(store.get("auth_session") ?? ""), { role: "therapist" });
});

test("holds a detached copy, so a revoked Immer draft passed in cannot break later reads", () => {
  clearAuthSession();
  // authSlice hands over draft proxies that Immer revokes once the reducer returns.
  const { proxy: user, revoke } = Proxy.revocable({ fullName: "Super Admin" }, {});
  setAuthSession({ accessToken: "access-jwt", role: "super-admin", user });
  revoke();

  const session = getAuthSession();
  assert.deepEqual(session?.user, { fullName: "Super Admin" });
  assert.equal(session?.accessToken, "access-jwt");
});
