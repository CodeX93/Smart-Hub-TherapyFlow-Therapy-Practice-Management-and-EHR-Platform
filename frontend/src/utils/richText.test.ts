import assert from "node:assert/strict";
import test from "node:test";

import {
  hasRichTextContent,
  isEmptyRichText,
  richTextToPlainText,
} from "./richText.ts";

test("nothing at all is empty", () => {
  assert.equal(isEmptyRichText(""), true);
  assert.equal(isEmptyRichText(null), true);
  assert.equal(isEmptyRichText(undefined), true);
  assert.equal(isEmptyRichText("   \n  "), true);
});

test("what an editor leaves behind when the user clears it is empty", () => {
  // These are the shapes that kept Save enabled and the step ticked on a note
  // whose editor looked blank.
  for (const markup of [
    "<p><br></p>",
    "<p><br/></p>",
    "<p></p>",
    "<div><br></div>",
    "<p>&nbsp;</p>",
    "<p>&nbsp;&nbsp;</p>",
    "<p> </p>",
    "<p><br></p><p><br></p>",
    "<div><p><br></p></div>",
  ]) {
    assert.equal(isEmptyRichText(markup), true, `${markup} should be empty`);
  }
});

test("real text is not empty, however it is wrapped", () => {
  for (const markup of [
    "Plain text",
    "<p>Client reported improved sleep.</p>",
    "<div><span>Nested</span></div>",
    "<ul><li>One</li></ul>",
    "<p><strong>Bold</strong></p>",
    "<p><br></p><p>Second paragraph has text.</p>",
  ]) {
    assert.equal(isEmptyRichText(markup), false, `${markup} should not be empty`);
  }
});

test("an image or a table counts as content even with no text", () => {
  assert.equal(isEmptyRichText('<p><img src="x.png"></p>'), false);
  assert.equal(isEmptyRichText("<table><tr><td></td></tr></table>"), false);
});

test("hasRichTextContent is the inverse", () => {
  assert.equal(hasRichTextContent("<p><br></p>"), false);
  assert.equal(hasRichTextContent("<p>Something</p>"), true);
});

test("plain text keeps the words and drops the markup", () => {
  const text = richTextToPlainText(
    "<p>First line.</p><p>Second &amp; third.</p>",
  );

  assert.match(text, /First line\./);
  assert.match(text, /Second & third\./);
  assert.equal(text.includes("<"), false);
});
