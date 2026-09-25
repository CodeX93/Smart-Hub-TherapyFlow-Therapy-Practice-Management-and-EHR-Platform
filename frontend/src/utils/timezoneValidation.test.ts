import assert from "node:assert/strict";
import test from "node:test";

import {
  getSafeBrowserTimezone,
  hasPortalTimezone,
  isValidIanaTimezone,
} from "./timezoneValidation.ts";

test("a saved timezone is recognised so the seed never runs again", () => {
  assert.equal(hasPortalTimezone("Asia/Karachi"), true);
});

test("an absent timezone is what lets the first-login seed run", () => {
  assert.equal(hasPortalTimezone(null), false);
  assert.equal(hasPortalTimezone(undefined), false);
  assert.equal(hasPortalTimezone(""), false);
  assert.equal(hasPortalTimezone("   "), false);
  // The portal API returns the string "null" when unset.
  assert.equal(hasPortalTimezone("null"), false);
  assert.equal(hasPortalTimezone("NULL"), false);
});

test("detects the device timezone for the seed", () => {
  const original = process.env.TZ;
  process.env.TZ = "Asia/Karachi";
  try {
    assert.equal(getSafeBrowserTimezone(), "Asia/Karachi");
  } finally {
    process.env.TZ = original;
  }
});

test("rejects timezones that are not real zones", () => {
  assert.equal(isValidIanaTimezone("Not/AZone"), false);
  assert.equal(isValidIanaTimezone(""), false);
  assert.equal(isValidIanaTimezone("  "), false);
  assert.equal(isValidIanaTimezone("Europe/London"), true);
});
