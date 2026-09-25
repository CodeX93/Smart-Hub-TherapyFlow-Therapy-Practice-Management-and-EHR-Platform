import assert from "node:assert/strict";
import test from "node:test";

import { formatPortalSlotStartTime } from "./portalSessionDisplay.ts";

// 18:00 UTC is 2:00 PM in New York (EDT on this date) and 11:00 PM in Karachi.
const START_UTC = "2026-09-23T18:00:00Z";

test("renders a slot in the timezone the API supplied", () => {
  assert.equal(
    formatPortalSlotStartTime("2026-09-23", "14:00", START_UTC, "America/New_York"),
    "02:00 PM",
  );
  assert.equal(
    formatPortalSlotStartTime("2026-09-23", "14:00", START_UTC, "Asia/Karachi"),
    "11:00 PM",
  );
});

test("ignores the device timezone when the API supplied one", () => {
  const original = process.env.TZ;
  process.env.TZ = "Asia/Tokyo";
  try {
    assert.equal(
      formatPortalSlotStartTime("2026-09-23", "14:00", START_UTC, "America/New_York"),
      "02:00 PM",
    );
  } finally {
    process.env.TZ = original;
  }
});

test("echoes the wall-clock time back when there is no UTC instant", () => {
  // No instant to convert, so the therapist-local HH:mm is returned unshifted
  // rather than being reinterpreted as if it were UTC.
  assert.equal(
    formatPortalSlotStartTime("2026-09-23", "14:00", undefined, "Asia/Karachi"),
    "02:00 PM",
  );
});

test("falls back to the raw value for an unparseable time", () => {
  assert.equal(
    formatPortalSlotStartTime("2026-09-23", "not-a-time", undefined, "Asia/Karachi"),
    "not-a-time",
  );
});

test("falls back to the wall-clock path when startUtc is unparseable", () => {
  assert.equal(
    formatPortalSlotStartTime("2026-09-23", "14:00", "garbage", "Asia/Karachi"),
    "02:00 PM",
  );
});
