import assert from "node:assert/strict";
import test from "node:test";

import {
  buildSessionNoteGenerateFormData,
  hasClinicalFieldsForGenerate,
  type SessionNoteGenerateFormData,
} from "./sessionNoteAiGenerate.ts";

const EMPTY: SessionNoteGenerateFormData = {
  sessionFocus: "",
  symptoms: "",
  shortTermGoals: "",
  intervention: "",
  progress: "",
  remarks: "",
  recommendations: "",
};

test("an empty note has nothing to generate from", () => {
  assert.equal(hasClinicalFieldsForGenerate(EMPTY), false);
});

test("whitespace is not content", () => {
  assert.equal(
    hasClinicalFieldsForGenerate({ ...EMPTY, sessionFocus: "   \n  " }),
    false,
  );
});

test("any single filled field is enough to generate", () => {
  for (const key of Object.keys(EMPTY) as (keyof SessionNoteGenerateFormData)[]) {
    assert.equal(
      hasClinicalFieldsForGenerate({ ...EMPTY, [key]: "something clinical" }),
      true,
      `${key} should enable generate`,
    );
  }
});

test("recommendations alone enables generate", () => {
  // It is sent to the model either way, so a note carrying only this is not
  // empty, and Generate must not sit disabled telling the clinician to fill
  // in a field they already filled.
  const formData = { ...EMPTY, recommendations: "Continue weekly sessions." };

  assert.equal(hasClinicalFieldsForGenerate(formData), true);
  assert.equal(
    buildSessionNoteGenerateFormData(formData).recommendations,
    "Continue weekly sessions.",
  );
});

test("every field that is sent is also a field that counts", () => {
  const sent = Object.keys(
    buildSessionNoteGenerateFormData({ ...EMPTY, sessionFocus: "x" }),
  );

  for (const key of sent) {
    assert.equal(
      hasClinicalFieldsForGenerate({
        ...EMPTY,
        [key as keyof SessionNoteGenerateFormData]: "x",
      }),
      true,
      `${key} is sent to the model but does not count`,
    );
  }
});

test("the payload trims what it sends", () => {
  const payload = buildSessionNoteGenerateFormData({
    ...EMPTY,
    symptoms: "  Improved sleep onset.  ",
  });

  assert.equal(payload.symptoms, "Improved sleep onset.");
  assert.equal(payload.remarks, "");
});
