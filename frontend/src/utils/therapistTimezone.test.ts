import assert from "node:assert/strict";
import test from "node:test";

import { resolveScheduleFormTimezone } from "./therapistTimezone.ts";

const PRACTICE = "America/Toronto";

test("a therapist who sits outside the clinic keeps their own zone on reload", () => {
  assert.equal(
    resolveScheduleFormTimezone("Asia/Karachi", PRACTICE),
    "Asia/Karachi",
  );
});

test("the clinic zone is the default only until the profile has one of its own", () => {
  assert.equal(resolveScheduleFormTimezone(null, PRACTICE), PRACTICE);
  assert.equal(resolveScheduleFormTimezone(undefined, PRACTICE), PRACTICE);
  assert.equal(resolveScheduleFormTimezone("", PRACTICE), PRACTICE);
  assert.equal(resolveScheduleFormTimezone("   ", PRACTICE), PRACTICE);
  // The API renders an unset timezone as the string "null".
  assert.equal(resolveScheduleFormTimezone("null", PRACTICE), PRACTICE);
});

test("a therapist who matches the clinic is unaffected by the precedence", () => {
  assert.equal(resolveScheduleFormTimezone(PRACTICE, PRACTICE), PRACTICE);
});

test("a saved value that is not a real zone falls back rather than blanking the picker", () => {
  assert.equal(
    resolveScheduleFormTimezone("Mars/Olympus_Mons", PRACTICE),
    PRACTICE,
  );
});

test("with neither side set the picker stays empty so its required rule can fire", () => {
  assert.equal(resolveScheduleFormTimezone(null, null), "");
  assert.equal(resolveScheduleFormTimezone("null", undefined), "");
});

test("a saved zone still wins when the clinic has none configured", () => {
  assert.equal(
    resolveScheduleFormTimezone("Europe/Berlin", ""),
    "Europe/Berlin",
  );
});

test("surrounding whitespace does not hide a saved zone behind the clinic default", () => {
  assert.equal(
    resolveScheduleFormTimezone("  Asia/Karachi  ", PRACTICE),
    "Asia/Karachi",
  );
});
