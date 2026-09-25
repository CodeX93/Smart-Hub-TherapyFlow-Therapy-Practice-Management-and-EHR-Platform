import { TrashIcon } from "@/components/icons/commonIcons";
import { useEffect, useMemo, useRef, useState } from "react";
import { ArrowDown, ArrowUp, CheckCircle2, Film, Heading2, Link2, List, ListOrdered, Plus, Type } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";
import { detectVideo } from "./videoUrl";

import { newId, type BodyBlock } from "./articleBody";


type ArticleBodyEditorProps = {
  value: BodyBlock[];
  onChange: (next: BodyBlock[]) => void;
};

function VideoPreview({ url }: { url: string }) {
  const info = useMemo(() => detectVideo(url), [url]);

  if (!url.trim()) {
    return (
      <div className="rounded-xl border border-dashed border-[#d7dde5] bg-[#f8fafc] px-4 py-8 text-center text-sm text-[#97a3b1]">
        Paste a YouTube, Vimeo, or direct video (.mp4) URL to see a preview
      </div>
    );
  }

  if (info.kind === "youtube" || info.kind === "vimeo") {
    return (
      <div className="overflow-hidden rounded-xl border border-[#e6e9ee] bg-black">
        <div className="relative aspect-video w-full">
          <iframe
            src={info.embedUrl!}
            title="Video preview"
            className="absolute inset-0 h-full w-full"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
            allowFullScreen
          />
        </div>
      </div>
    );
  }

  if (info.kind === "direct") {
    return (
      <div className="overflow-hidden rounded-xl border border-[#e6e9ee] bg-black">
        <video className="aspect-video w-full" controls playsInline preload="metadata" src={url.trim()}>
          <track kind="captions" />
        </video>
      </div>
    );
  }

  if (info.kind === "link") {
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
        This URL will be saved. It is not a recognized video embed (YouTube / Vimeo / .mp4). On the hub it
        may show as a plain link.
        <a
          href={url.trim()}
          target="_blank"
          rel="noopener noreferrer"
          className="mt-2 flex items-center gap-1 font-medium text-[#2f3945] underline-offset-2 hover:underline"
        >
          <Link2 size={14} />
          Open URL
        </a>
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
      Enter a full URL starting with https://
    </div>
  );
}

const kindLabel: Record<BodyBlock["kind"], string> = {
  paragraph: "Paragraph",
  heading: "Heading",
  video: "Video URL",
  list: "List",
  quote: "Quote",
  preserved: "Existing block",
};

type AddKind = "paragraph" | "heading" | "video" | "bullet-list" | "numbered-list";

const addKindLabel: Record<AddKind, string> = {
  paragraph: "Paragraph",
  heading: "Heading",
  video: "Video URL",
  "bullet-list": "Bullet list",
  "numbered-list": "Numbered list",
};

const ArticleBodyEditor = ({ value, onChange }: ArticleBodyEditorProps) => {
  const [flashId, setFlashId] = useState<string | null>(null);
  const [justAddedLabel, setJustAddedLabel] = useState<string | null>(null);
  const focusIdRef = useRef<string | null>(null);
  const noticeTimerRef = useRef<number | null>(null);
  const flashTimerRef = useRef<number | null>(null);

  useEffect(() => {
    return () => {
      if (noticeTimerRef.current) window.clearTimeout(noticeTimerRef.current);
      if (flashTimerRef.current) window.clearTimeout(flashTimerRef.current);
    };
  }, []);

  useEffect(() => {
    const id = focusIdRef.current;
    if (!id) return;

    const frame = window.requestAnimationFrame(() => {
      const card = document.querySelector<HTMLElement>(`[data-body-block-id="${id}"]`);
      if (card) {
        card.scrollIntoView({ behavior: "smooth", block: "center" });
      }
      const field = document.querySelector<HTMLTextAreaElement | HTMLInputElement>(
        `[data-body-block-id="${id}"] [data-body-focus]`
      );
      if (field) {
        field.focus({ preventScroll: true });
      }
      focusIdRef.current = null;
    });

    return () => window.cancelAnimationFrame(frame);
  }, [value]);

  const revealNewBlock = (id: string, kind: AddKind) => {
    focusIdRef.current = id;
    setFlashId(id);
    setJustAddedLabel(addKindLabel[kind]);

    if (flashTimerRef.current) window.clearTimeout(flashTimerRef.current);
    flashTimerRef.current = window.setTimeout(() => setFlashId(null), 1800);

    if (noticeTimerRef.current) window.clearTimeout(noticeTimerRef.current);
    noticeTimerRef.current = window.setTimeout(() => setJustAddedLabel(null), 2200);
  };

  const updateAt = (index: number, patch: Partial<BodyBlock> | BodyBlock) => {
    onChange(value.map((block, i) => (i === index ? ({ ...block, ...patch } as BodyBlock) : block)));
  };

  const removeAt = (index: number) => {
    const next = value.filter((_, i) => i !== index);
    onChange(next.length > 0 ? next : [{ id: newId(), kind: "paragraph", text: "" }]);
  };

  const move = (index: number, direction: -1 | 1) => {
    const target = index + direction;
    if (target < 0 || target >= value.length) return;
    const next = [...value];
    const tmp = next[index];
    next[index] = next[target];
    next[target] = tmp;
    onChange(next);
  };

  const add = (kind: AddKind) => {
    const id = newId();
    let next: BodyBlock;

    if (kind === "paragraph") {
      next = { id, kind: "paragraph", text: "" };
    } else if (kind === "heading") {
      next = { id, kind: "heading", level: 2, text: "" };
    } else if (kind === "bullet-list") {
      next = { id, kind: "list", format: "unordered", items: [""] };
    } else if (kind === "numbered-list") {
      next = { id, kind: "list", format: "ordered", items: [""] };
    } else {
      next = { id, kind: "video", url: "" };
    }

    revealNewBlock(id, kind);
    onChange([...value, next]);
  };

  const updateListItem = (blockIndex: number, itemIndex: number, text: string) => {
    onChange(
      value.map((block, i) => {
        if (i !== blockIndex || block.kind !== "list") return block;
        return {
          ...block,
          items: block.items.map((item, j) => (j === itemIndex ? text : item)),
        };
      })
    );
  };

  const addListItem = (blockIndex: number) => {
    onChange(
      value.map((block, i) => {
        if (i !== blockIndex || block.kind !== "list") return block;
        return { ...block, items: [...block.items, ""] };
      })
    );
  };

  const removeListItem = (blockIndex: number, itemIndex: number) => {
    onChange(
      value.map((block, i) => {
        if (i !== blockIndex || block.kind !== "list") return block;
        const items = block.items.filter((_, j) => j !== itemIndex);
        return { ...block, items: items.length > 0 ? items : [""] };
      })
    );
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <label className="text-sm font-medium text-[#2f3945]">Article body</label>
          <p className="mt-1 max-w-2xl text-sm leading-6 text-[#697584]">
            Build the article section by section. Add paragraphs, headings, lists, or a{" "}
            <span className="font-medium text-[#2f3945]">Video URL</span> — video preview updates as you
            type.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button type="button" variant="outline" size="sm" onClick={() => add("paragraph")}>
            <Type size={14} className="mr-1.5" />
            Paragraph
          </Button>
          <Button type="button" variant="outline" size="sm" onClick={() => add("heading")}>
            <Heading2 size={14} className="mr-1.5" />
            Heading
          </Button>
          <Button type="button" variant="outline" size="sm" onClick={() => add("bullet-list")}>
            <List size={14} className="mr-1.5" />
            Bullet list
          </Button>
          <Button type="button" variant="outline" size="sm" onClick={() => add("numbered-list")}>
            <ListOrdered size={14} className="mr-1.5" />
            Numbered list
          </Button>
          <Button type="button" variant="outline" size="sm" onClick={() => add("video")}>
            <Film size={14} className="mr-1.5" />
            Video URL
          </Button>
        </div>
      </div>

      {justAddedLabel ? (
        <div className="flex items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800">
          <CheckCircle2 size={16} className="shrink-0" />
          <span>
            Added <span className="font-semibold">{justAddedLabel}</span> — scrolled to the new block below.
          </span>
        </div>
      ) : null}

      <div className="space-y-3">
        {value.map((block, index) => (
          <div
            key={block.id}
            data-body-block-id={block.id}
            className={cn(
              "overflow-hidden rounded-2xl border bg-[#fcfdff] shadow-[0_1px_2px_rgba(15,23,42,0.03)] transition-all duration-500",
              flashId === block.id
                ? "border-[#3b82f6] ring-2 ring-[#3b82f6]/30 shadow-[0_0_0_4px_rgba(59,130,246,0.12)]"
                : "border-[#e6e9ee]"
            )}
          >
            <div className="flex items-center justify-between gap-2 border-b border-[#eef1f5] bg-white px-3 py-2">
              <div className="flex min-w-0 items-center gap-2">
                <span className="inline-flex h-6 min-w-6 items-center justify-center rounded-md bg-[#eef2f7] px-1.5 text-[11px] font-semibold text-[#697584]">
                  {index + 1}
                </span>
                <span className="truncate text-xs font-semibold uppercase tracking-wide text-[#697584]">
                  {block.kind === "list"
                    ? block.format === "ordered"
                      ? "Numbered list"
                      : "Bullet list"
                    : kindLabel[block.kind]}
                  {block.kind === "preserved" ? ` · ${block.label}` : ""}
                </span>
              </div>
              <div className="flex items-center gap-0.5">
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8"
                  disabled={index === 0}
                  onClick={() => move(index, -1)}
                  aria-label="Move up"
                >
                  <ArrowUp size={14} />
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8"
                  disabled={index === value.length - 1}
                  onClick={() => move(index, 1)}
                  aria-label="Move down"
                >
                  <ArrowDown size={14} />
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8 text-red-600 hover:text-red-700"
                  onClick={() => removeAt(index)}
                  aria-label="Remove block"
                >
                  <TrashIcon size={14} />
                </Button>
              </div>
            </div>

            <div className="space-y-3 p-3 sm:p-4">
              {block.kind === "paragraph" ? (
                <Textarea
                  rows={4}
                  placeholder="Write this paragraph…"
                  data-body-focus
                  className="min-h-[110px] resize-y border-[#e6e9ee] bg-white text-[15px] leading-7"
                  value={block.text}
                  onChange={(e) => updateAt(index, { text: e.target.value })}
                />
              ) : null}

              {block.kind === "heading" ? (
                <div className="grid gap-3 sm:grid-cols-[140px_1fr]">
                  <Select
                    value={String(block.level)}
                    onValueChange={(v) => {
                      const level: 2 | 3 = Number(v) === 3 ? 3 : 2;
                      onChange(
                        value.map((b, i) =>
                          i === index && b.kind === "heading" ? { ...b, level } : b
                        )
                      );
                    }}
                  >
                    <SelectTrigger className="bg-white">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="2">Heading 2</SelectItem>
                      <SelectItem value="3">Heading 3</SelectItem>
                    </SelectContent>
                  </Select>
                  <Input
                    placeholder="Section heading"
                    data-body-focus
                    className="bg-white text-base font-semibold"
                    value={block.text}
                    onChange={(e) => updateAt(index, { text: e.target.value })}
                  />
                </div>
              ) : null}

              {block.kind === "list" ? (
                <div className="space-y-3">
                  <div className="flex flex-wrap items-center gap-2">
                    <Select
                      value={block.format}
                      onValueChange={(v) => {
                        onChange(
                          value.map((b, i) =>
                            i === index && b.kind === "list"
                              ? { ...b, format: v === "ordered" ? "ordered" : "unordered" }
                              : b
                          )
                        );
                      }}
                    >
                      <SelectTrigger className="w-[180px] bg-white">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="unordered">Bullet list</SelectItem>
                        <SelectItem value="ordered">Numbered list</SelectItem>
                      </SelectContent>
                    </Select>
                    <Button type="button" variant="outline" size="sm" onClick={() => addListItem(index)}>
                      <Plus size={14} className="mr-1" />
                      Add item
                    </Button>
                  </div>

                  <div className="space-y-2">
                    {block.items.map((item, itemIndex) => (
                      <div key={`${block.id}_${itemIndex}`} className="flex items-start gap-2">
                        <span className="mt-2.5 w-6 shrink-0 text-center text-sm font-medium text-[#97a3b1]">
                          {block.format === "ordered" ? `${itemIndex + 1}.` : "•"}
                        </span>
                        <Textarea
                          rows={2}
                          placeholder={`List item ${itemIndex + 1}`}
                          data-body-focus={itemIndex === 0 ? true : undefined}
                          className="min-h-[64px] resize-y border-[#e6e9ee] bg-white text-[15px] leading-6"
                          value={item}
                          onChange={(e) => updateListItem(index, itemIndex, e.target.value)}
                        />
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          className="mt-1 h-8 w-8 shrink-0 text-red-600 hover:text-red-700"
                          onClick={() => removeListItem(index, itemIndex)}
                          aria-label="Remove list item"
                        >
                          <TrashIcon size={14} />
                        </Button>
                      </div>
                    ))}
                  </div>

                  <div className="rounded-xl border border-[#eef1f5] bg-white px-4 py-3">
                    <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-[#97a3b1]">
                      Preview
                    </p>
                    {block.format === "ordered" ? (
                      <ol className="list-decimal space-y-1.5 pl-5 text-[15px] leading-6 text-[#2f3945]">
                        {block.items.filter((t) => t.trim()).map((t, i) => (
                          <li key={i}>{t}</li>
                        ))}
                      </ol>
                    ) : (
                      <ul className="list-disc space-y-1.5 pl-5 text-[15px] leading-6 text-[#2f3945]">
                        {block.items.filter((t) => t.trim()).map((t, i) => (
                          <li key={i}>{t}</li>
                        ))}
                      </ul>
                    )}
                    {block.items.every((t) => !t.trim()) ? (
                      <p className="text-sm text-[#97a3b1]">List items will appear here.</p>
                    ) : null}
                  </div>
                </div>
              ) : null}

              {block.kind === "quote" ? (
                <Textarea
                  rows={3}
                  placeholder="Quote text…"
                  data-body-focus
                  className="resize-y border-[#e6e9ee] bg-white text-[15px] italic leading-7"
                  value={block.text}
                  onChange={(e) => updateAt(index, { text: e.target.value })}
                />
              ) : null}

              {block.kind === "video" ? (
                <div className="space-y-3">
                  <div className="space-y-1.5">
                    <label className="text-xs font-medium text-[#697584]">Video or media URL</label>
                    <Input
                      placeholder="https://www.youtube.com/watch?v=… or https://vimeo.com/… or .mp4"
                      data-body-focus
                      className="bg-white font-mono text-sm"
                      value={block.url}
                      onChange={(e) => updateAt(index, { url: e.target.value })}
                    />
                    <p className="text-xs leading-5 text-[#97a3b1]">
                      Saved as a body block. The Learning Hub detects this URL and shows an embedded player.
                    </p>
                  </div>
                  <VideoPreview url={block.url} />
                </div>
              ) : null}

              {block.kind === "preserved" ? (
                <div className="rounded-xl border border-[#e6e9ee] bg-white px-4 py-3 text-sm text-[#697584]">
                  This {block.label.toLowerCase()} was imported from the existing article and will be kept
                  as-is when you save. Remove it if you no longer need it, or leave it in place.
                </div>
              ) : null}
            </div>
          </div>
        ))}
      </div>

      <button
        type="button"
        onClick={() => add("paragraph")}
        className="flex w-full items-center justify-center gap-2 rounded-2xl border border-dashed border-[#cfd6df] bg-white px-4 py-3 text-sm font-medium text-[#697584] transition hover:border-[#aeb7c3] hover:bg-[#f8fafc] hover:text-[#2f3945]"
      >
        <Plus size={16} />
        Add another paragraph
      </button>
    </div>
  );
};

export default ArticleBodyEditor;
