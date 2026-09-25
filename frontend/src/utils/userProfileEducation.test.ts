import assert from "node:assert/strict";
import test from "node:test";

import {
  mapEducationFormToPayload,
  mapEducationResponseToForm,
} from "./userProfileEducation.ts";

function withTimezone(timezone: string, run: () => void) {
  const original = process.env.TZ;
  process.env.TZ = timezone;
  try {
    run();
  } finally {
    process.env.TZ = original;
  }
}

test("a graduation date is saved as the day that was picked, east of UTC", () => {
  // The picker hands over a Date at local midnight; serialising through UTC
  // used to move it back a day for anyone ahead of UTC.
  withTimezone("Asia/Karachi", () => {
    const payloads = mapEducationFormToPayload([
      {
        degreeType: "PhD",
        fieldOfStudy: "Clinical Psychology",
        institution: "State University",
        graduationYear: "",
        graduationDate: new Date(2026, 8, 15),
        isAccredited: true,
        accreditationBody: "",
        notes: "",
      },
    ]);

    assert.equal(payloads[0].graduationDate, "2026-09-15");
    assert.equal(payloads[0].graduationYear, 2026);
  });
});

test("a graduation date survives the same day west of UTC", () => {
  withTimezone("America/Los_Angeles", () => {
    const payloads = mapEducationFormToPayload([
      {
        degreeType: "PhD",
        fieldOfStudy: "",
        institution: "State University",
        graduationYear: "",
        graduationDate: new Date(2026, 8, 15),
        isAccredited: true,
        accreditationBody: "",
        notes: "",
      },
    ]);

    assert.equal(payloads[0].graduationDate, "2026-09-15");
  });
});

test("loading then saving an entry leaves the graduation date untouched", () => {
  withTimezone("Asia/Karachi", () => {
    const payloads = mapEducationFormToPayload(
      mapEducationResponseToForm([
        {
          degreeType: "PhD",
          institution: "State University",
          graduationDate: "2026-09-15",
          graduationYear: 2026,
        },
      ]),
    );

    assert.equal(payloads[0].graduationDate, "2026-09-15");
  });
});

test("a loaded graduation date is the same plain date the picker makes", () => {
  // The picker emits new Date(year, month, day) — local midnight, no time of
  // day. Loading has to produce exactly that, or the two disagree about what
  // the stored day means.
  withTimezone("Asia/Karachi", () => {
    const [form] = mapEducationResponseToForm([
      {
        degreeType: "PhD",
        institution: "State University",
        graduationDate: "2026-09-15",
      },
    ]);

    assert.deepEqual(form.graduationDate, new Date(2026, 8, 15));
  });
});

test("an entry with no graduation date loads as empty", () => {
  const [form] = mapEducationResponseToForm([
    { degreeType: "PhD", institution: "State University" },
  ]);

  assert.equal(form.graduationDate, null);
});

test("an entry with no graduation date sends none", () => {
  const payloads = mapEducationFormToPayload([
    {
      degreeType: "PhD",
      fieldOfStudy: "",
      institution: "State University",
      graduationYear: "2020",
      graduationDate: null,
      isAccredited: true,
      accreditationBody: "",
      notes: "",
    },
  ]);

  assert.equal(payloads[0].graduationDate, undefined);
  assert.equal(payloads[0].graduationYear, 2020);
});
