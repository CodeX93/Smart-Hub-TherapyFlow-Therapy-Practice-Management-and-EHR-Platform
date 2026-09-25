import { TrashIcon } from "@/components/icons/commonIcons";
import { ArrowDown, ArrowUp, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { CmsLinkItem } from "@/store/api/superAdminApi";

type Props = {
  label: string;
  value: CmsLinkItem[];
  onChange: (links: CmsLinkItem[]) => void;
  labelPlaceholder?: string;
  urlPlaceholder?: string;
};

export default function LinkTableEditor({
  label,
  value,
  onChange,
  labelPlaceholder = "Label",
  urlPlaceholder = "https://…",
}: Props) {
  const links = value || [];

  const update = (index: number, patch: Partial<CmsLinkItem>) => {
    onChange(links.map((item, i) => (i === index ? { ...item, ...patch } : item)));
  };

  const move = (index: number, dir: -1 | 1) => {
    const next = [...links];
    const target = index + dir;
    if (target < 0 || target >= next.length) return;
    [next[index], next[target]] = [next[target], next[index]];
    onChange(next);
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <label className="text-sm font-medium text-[#2f3945]">{label}</label>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => onChange([...links, { Label: "", URL: "" }])}
        >
          <Plus size={14} className="mr-1" />
          Add link
        </Button>
      </div>
      {links.length === 0 ? (
        <p className="rounded-lg border border-dashed border-[#d7dde5] px-3 py-4 text-sm text-[#97a3b1]">
          No links yet. Click “Add link” to create one.
        </p>
      ) : (
        <div className="space-y-2">
          {links.map((link, index) => (
            <div
              key={index}
              className="grid gap-2 rounded-xl border border-[#e6e9ee] bg-white p-3 md:grid-cols-[1fr_1.4fr_auto]"
            >
              <Input
                value={link.Label || ""}
                placeholder={labelPlaceholder}
                onChange={(e) => update(index, { Label: e.target.value })}
              />
              <Input
                value={link.URL || ""}
                placeholder={urlPlaceholder}
                onChange={(e) => update(index, { URL: e.target.value })}
              />
              <div className="flex items-center gap-1">
                <Button type="button" variant="ghost" size="icon" onClick={() => move(index, -1)}>
                  <ArrowUp size={14} />
                </Button>
                <Button type="button" variant="ghost" size="icon" onClick={() => move(index, 1)}>
                  <ArrowDown size={14} />
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  onClick={() => onChange(links.filter((_, i) => i !== index))}
                >
                  <TrashIcon size={14} />
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
