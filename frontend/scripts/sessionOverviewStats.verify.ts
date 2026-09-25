/**
 * Verification for scheduling overview card counts.
 * Run: npx --yes tsx scripts/sessionOverviewStats.verify.ts
 */
import assert from "node:assert/strict";
import { DateTime } from "luxon";
import {
  buildOverviewStatsArgs,
  getOverviewCardValue,
} from "../src/components/scheduling-sections/sessionOverviewStats";
import {
  resolveScheduleTimezone,
  toIsoRangeEndInTimezone,
  toIsoRangeStartInTimezone,
} from "../src/utils/scheduleTimezone";

function constructLocalDate(year: number, monthIndex: number, day: number): Date {
  return new Date(year, monthIndex, day);
}

function countInMonth(
  sessionIsos: string[],
  year: number,
  month: number,
  zone: string,
): number {
  return sessionIsos.filter((iso) => {
    const local = DateTime.fromISO(iso, { zone: "utc" }).setZone(zone);
    return local.year === year && local.month === month;
  }).length;
}

const practiceZone = "America/Toronto";
const browserZone = "Asia/Karachi";

assert.equal(resolveScheduleTimezone(practiceZone), practiceZone);
assert.equal(resolveScheduleTimezone(null), "America/New_York");

const fixtures = [
  "2026-08-31T12:00:00.000Z", // Aug in both
  "2026-08-31T20:00:00.000Z", // Aug Toronto / Sep Karachi
  "2026-09-01T02:00:00.000Z", // Aug Toronto / Sep Karachi
  "2026-09-15T15:00:00.000Z", // Sep in both
  "2026-09-30T22:00:00.000Z", // Sep Toronto / Oct Karachi
];

const practiceSept = countInMonth(fixtures, 2026, 9, practiceZone);
const browserSept = countInMonth(fixtures, 2026, 9, browserZone);
assert.equal(practiceSept, 2, `practice Sept expected 2, got ${practiceSept}`);
assert.equal(browserSept, 3, `browser Sept expected 3, got ${browserSept}`);
assert.notEqual(practiceSept, browserSept);

const monthStart = constructLocalDate(2026, 8, 1);
const monthEnd = constructLocalDate(2026, 8, 30);
const args = buildOverviewStatsArgs(monthStart, monthEnd, undefined, practiceZone);
assert.equal(args.timezone, practiceZone);
assert.equal(args.startDate, toIsoRangeStartInTimezone(monthStart, practiceZone));
assert.equal(args.endDate, toIsoRangeEndInTimezone(monthEnd, practiceZone));
assert.equal(
  toIsoRangeStartInTimezone(constructLocalDate(2026, 8, 1), practiceZone),
  "2026-09-01T04:00:00.000Z",
);

const stats = {
  totalSessions: 152,
  todaySessions: 0,
  thisWeekSessions: 0,
  thisMonthSessions: 60,
  upcomingSessions: 128,
  completedSessions: 7,
  cancelledSessions: 0,
};

assert.equal(getOverviewCardValue("This Month", stats), "152");
assert.equal(getOverviewCardValue("This Month", undefined, 152), "152");
assert.equal(getOverviewCardValue("Completed", stats, 7895), "7");
assert.equal(getOverviewCardValue("Upcoming", stats), "128");
assert.equal(getOverviewCardValue("Total Sessions", undefined, 7895), "7895");
assert.equal(getOverviewCardValue("Total Sessions", undefined), "0");

console.log("OK: overview card logic + timezone boundaries");
console.log(
  JSON.stringify(
    {
      practiceZone,
      prodDbSeptemberPractice: 152,
      prodDbSeptemberBrowserKarachi: 163,
      fixturePracticeSept: practiceSept,
      fixtureBrowserSept: browserSept,
      monthRangeStartIso: args.startDate,
    },
    null,
    2,
  ),
);
