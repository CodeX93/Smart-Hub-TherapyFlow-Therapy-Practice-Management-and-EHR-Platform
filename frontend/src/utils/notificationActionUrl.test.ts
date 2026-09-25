import assert from "node:assert/strict";
import test from "node:test";

import { resolveNotificationActionPath } from "./notificationActionUrl.ts";

const ORIGIN = "https://app.therapyflow.pro";

test("keeps in-app paths, with their query and hash", () => {
  assert.equal(resolveNotificationActionPath("/clients/456", ORIGIN), "/clients/456");
  assert.equal(
    resolveNotificationActionPath("  /scheduling?date=2026-09-24#slot ", ORIGIN),
    "/scheduling?date=2026-09-24#slot",
  );
});

test("reduces a same-origin absolute URL to its path", () => {
  assert.equal(
    resolveNotificationActionPath("https://app.therapyflow.pro/tasks/654", ORIGIN),
    "/tasks/654",
  );
});

test("refuses links that leave the app", () => {
  for (const url of [
    "https://evil.example/login",
    "http://app.therapyflow.pro/tasks/1", // other scheme is another origin
    "https://app.therapyflow.pro.evil.example/",
    "//evil.example/login",
    "/\\evil.example/login",
    "javascript:alert(1)",
    "data:text/html,hi",
    "https://user:pass@app.therapyflow.pro/tasks/1",
    "clients/456",
    "",
    "   ",
    null,
    undefined,
  ]) {
    assert.equal(resolveNotificationActionPath(url, ORIGIN), null, String(url));
  }
});
