import { isUrlOnlyText } from "./videoUrl";
type TextChild = { type: "text"; text: string; bold?: boolean; italic?: boolean };

export type BodyBlock =
  | { id: string; kind: "paragraph"; text: string }
  | { id: string; kind: "heading"; level: 2 | 3; text: string }
  | { id: string; kind: "video"; url: string }
  | { id: string; kind: "list"; format: "unordered" | "ordered"; items: string[] }
  | { id: string; kind: "quote"; text: string }
  | { id: string; kind: "preserved"; label: string; raw: unknown };

export function newId() {
  return `b_${Math.random().toString(36).slice(2, 10)}`;
}

function inlineText(children: unknown): string {
  if (!Array.isArray(children)) return "";
  return children
    .map((c) => {
      if (!c || typeof c !== "object") return "";
      return String((c as { text?: string }).text || "");
    })
    .join("");
}

function listItemsFromBlock(children: unknown): string[] {
  if (!Array.isArray(children)) return [""];
  const items = children.map((item) => {
    if (!item || typeof item !== "object") return "";
    const node = item as { children?: unknown };
    return inlineText(node.children);
  });
  return items.length > 0 ? items : [""];
}

export function blocksFromContent(content: unknown): BodyBlock[] {
  if (!Array.isArray(content) || content.length === 0) {
    return [{ id: newId(), kind: "paragraph", text: "" }];
  }

  const blocks: BodyBlock[] = [];
  for (const item of content) {
    if (!item || typeof item !== "object") continue;
    const block = item as {
      type?: string;
      level?: number;
      format?: string;
      children?: unknown;
      image?: { url?: string; alternativeText?: string };
    };

    if (block.type === "paragraph") {
      const text = inlineText(block.children);
      if (isUrlOnlyText(text)) {
        blocks.push({ id: newId(), kind: "video", url: text.trim() });
      } else {
        blocks.push({ id: newId(), kind: "paragraph", text });
      }
      continue;
    }

    if (block.type === "heading") {
      const level = block.level === 3 ? 3 : 2;
      blocks.push({ id: newId(), kind: "heading", level, text: inlineText(block.children) });
      continue;
    }

    if (block.type === "list") {
      blocks.push({
        id: newId(),
        kind: "list",
        format: block.format === "ordered" ? "ordered" : "unordered",
        items: listItemsFromBlock(block.children),
      });
      continue;
    }

    if (block.type === "quote") {
      blocks.push({ id: newId(), kind: "quote", text: inlineText(block.children) });
      continue;
    }

    if (block.type === "code") {
      blocks.push({ id: newId(), kind: "preserved", label: "Code block", raw: item });
      continue;
    }

    if (block.type === "image") {
      blocks.push({
        id: newId(),
        kind: "preserved",
        label: block.image?.alternativeText || "Image",
        raw: item,
      });
      continue;
    }

    blocks.push({ id: newId(), kind: "preserved", label: block.type || "Block", raw: item });
  }

  return blocks.length > 0 ? blocks : [{ id: newId(), kind: "paragraph", text: "" }];
}

export function contentFromBlocks(blocks: BodyBlock[]): unknown[] {
  const out: unknown[] = [];

  for (const block of blocks) {
    if (block.kind === "preserved") {
      out.push(block.raw);
      continue;
    }

    if (block.kind === "video") {
      const url = block.url.trim();
      if (!url) continue;
      out.push({
        type: "paragraph",
        children: [{ type: "text", text: url } satisfies TextChild],
      });
      continue;
    }

    if (block.kind === "list") {
      const items = block.items.map((t) => t.trim()).filter(Boolean);
      if (items.length === 0) continue;
      out.push({
        type: "list",
        format: block.format,
        children: items.map((text) => ({
          type: "list-item",
          children: [{ type: "text", text } satisfies TextChild],
        })),
      });
      continue;
    }

    if (block.kind === "quote") {
      const text = block.text.trim();
      if (!text) continue;
      out.push({
        type: "quote",
        children: [{ type: "text", text } satisfies TextChild],
      });
      continue;
    }

    const text = block.text.trim();
    if (!text && block.kind === "paragraph") continue;

    if (block.kind === "heading") {
      if (!text) continue;
      out.push({
        type: "heading",
        level: block.level,
        children: [{ type: "text", text } satisfies TextChild],
      });
      continue;
    }

    out.push({
      type: "paragraph",
      children: [{ type: "text", text: block.text } satisfies TextChild],
    });
  }

  return out;
}