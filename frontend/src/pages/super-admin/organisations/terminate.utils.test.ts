import assert from "node:assert/strict";
import test from "node:test";

import {
  RETENTION_DAYS_DEFAULT,
  RETENTION_DAYS_MAX,
  RETENTION_DAYS_MIN,
  sanitizeRetentionDaysInput,
  validateRetentionDaysInput,
} from "./terminate.utils.ts";

test("the seeded default is a value the API accepts", () => {
  assert.equal(validateRetentionDaysInput(RETENTION_DAYS_DEFAULT), null);
});

test("the bounds the API enforces are accepted", () => {
  assert.equal(validateRetentionDaysInput(String(RETENTION_DAYS_MIN)), null);
  assert.equal(validateRetentionDaysInput(String(RETENTION_DAYS_MAX)), null);
  assert.equal(validateRetentionDaysInput(" 90 "), null);
});

test("values the API would refuse are refused in the form", () => {
  const expected = `Retention days must be between ${RETENTION_DAYS_MIN} and ${RETENTION_DAYS_MAX}.`;
  // The old placeholder, and the old fallback, was 0 — the one value the API always rejects.
  assert.equal(validateRetentionDaysInput("0"), expected);
  assert.equal(validateRetentionDaysInput(String(RETENTION_DAYS_MIN - 1)), expected);
  assert.equal(validateRetentionDaysInput(String(RETENTION_DAYS_MAX + 1)), expected);
});

test("a blank or non-numeric entry is refused before submitting", () => {
  assert.equal(validateRetentionDaysInput(""), "Retention days is required.");
  assert.equal(validateRetentionDaysInput("   "), "Retention days is required.");
  assert.equal(
    validateRetentionDaysInput("30.5"),
    "Retention days must be a whole number.",
  );
});

test("typing keeps the field to digits that can still be in range", () => {
  assert.equal(sanitizeRetentionDaysInput("3o0"), "30");
  assert.equal(sanitizeRetentionDaysInput("-30"), "30");
  // Three digits is the widest in-range value, so longer input is truncated.
  assert.equal(sanitizeRetentionDaysInput("36500"), "365");
});
