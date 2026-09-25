import { TrashIcon } from "@/components/icons/commonIcons";
import { ArrowDown, ArrowUp, Plus } from "lucide-react";
import type { ReactNode } from "react";
import { Button } from "@/components/ui/button";

type Props<T> = {
  label: string;
  items: T[];
  onChange: (items: T[]) => void;
  createItem: () => T;
  renderItem: (item: T, index: number, update: (patch: Partial<T>) => void) => ReactNode;
  max?: number;
  addLabel?: string;
};

export default function RepeatableCardList<T extends object>({
  label,
  items,
  onChange,
  createItem,
  renderItem,
  max,
  addLabel = "Add item",
}: Props<T>) {
  const list = items || [];
  const atMax = max != null && list.length >= max;

  const update = (index: number, patch: Partial<T>) => {
    onChange(list.map((item, i) => (i === index ? { ...item, ...patch } : item)));
  };

  const move = (index: number, dir: -1 | 1) => {
    const next = [...list];
    const target = index + dir;
    if (target < 0 || target >= next.length) return;
    [next[index], next[target]] = [next[target], next[index]];
    onChange(next);
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between gap-2">
        <label className="text-sm font-medium text-[#2f3945]">
          {label}
          {max != null ? (
            <span className="ml-2 font-normal text-[#97a3b1]">
              ({list.length}/{max})
            </span>
          ) : null}
        </label>
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={atMax}
          onClick={() => onChange([...list, createItem()])}
        >
          <Plus size={14} className="mr-1" />
          {addLabel}
        </Button>
      </div>
      {list.length === 0 ? (
        <p className="rounded-lg border border-dashed border-[#d7dde5] px-3 py-4 text-sm text-[#97a3b1]">
          No items yet.
        </p>
      ) : (
        <div className="space-y-3">
          {list.map((item, index) => (
            <div key={index} className="space-y-3 rounded-xl border border-[#e6e9ee] bg-white p-4">
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium uppercase tracking-wide text-[#97a3b1]">
                  Item {index + 1}
                </span>
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
                    onClick={() => onChange(list.filter((_, i) => i !== index))}
                  >
                    <TrashIcon size={14} />
                  </Button>
                </div>
              </div>
              {renderItem(item, index, (patch) => update(index, patch))}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
