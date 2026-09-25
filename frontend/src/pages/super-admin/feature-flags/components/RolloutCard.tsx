import { useState } from "react";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";

interface RolloutCardProps {
  className?: string;
}

interface FieldShellProps {
  label: string;
  helperText?: string;
  children: React.ReactNode;
}

function FieldShell(props: FieldShellProps) {
  return (
    <div className="w-full">
      <div className="rounded-[0.875rem] border border-[#d9e2ec] bg-white px-4 py-2.5">
        <div className="text-[0.75rem] font-medium leading-4 text-[#7c8a97]">
          {props.label}
        </div>
        <div className="mt-1">{props.children}</div>
      </div>
      {props.helperText ? (
        <div className="mt-1.5 text-[0.75rem] font-normal leading-4.5 text-[#9aa5b1]">
          {props.helperText}
        </div>
      ) : null}
    </div>
  );
}

function TargetChip(props: { label: string; onRemove: () => void }) {
  return (
    <div className="inline-flex h-[1.875rem] items-center gap-2 rounded-full bg-[#eef2f6] px-4">
      <span className="text-[0.75rem] font-medium leading-4 text-[#5f6f7f]">
        {props.label}
      </span>
      <button
        type="button"
        onClick={props.onRemove}
        className="grid h-4 w-4 place-items-center rounded-full text-[#5f6f7f] transition-colors hover:bg-white/60"
        aria-label={"Remove " + props.label}
      >
        <X size={12} aria-hidden="true" />
      </button>
    </div>
  );
}

function getMutedInputClassName(): string {
  return cn(
    "h-auto border-0 bg-transparent p-0 shadow-none",
    "text-[0.875rem] font-normal text-[#2b3946] placeholder:text-[#97a4b0]",
    "focus-visible:border-0 focus-visible:ring-0 disabled:opacity-100"
  );
}

function getSelectTriggerClassName(): string {
  return cn(
    "h-auto w-full border-0 bg-transparent p-0 shadow-none",
    "text-[0.875rem] font-normal text-[#2b3946] [&_svg]:text-[#7c8a97]",
    "focus-visible:border-0 focus-visible:ring-0"
  );
}

function RolloutCard(props: RolloutCardProps) {
  const [strategy, setStrategy] = useState("Include Specific Organizations");
  const [targets, setTargets] = useState(["Harbor Wellness", "Blue Shore Therapy"]);

  return (
    <div
      className={cn(
        "flex min-h-[36.8125rem] w-full flex-col rounded-[0.875rem] border border-[#EDEEF1] bg-white px-4 py-4 shadow-[0_1px_2px_rgba(15,23,42,0.04)]",
        props.className
      )}
    >
      <div className="flex h-full w-full flex-col gap-7">
        <div className="flex min-h-[3.75rem] w-full items-center justify-between rounded-[0.625rem] bg-[#f6f7fa] px-4 py-3">
          <div>
            <div className="text-[0.75rem] font-medium leading-4 text-[#7c8a97]">
              Feature Target
            </div>
            <div className="mt-1 text-[0.875rem] font-semibold leading-5 text-[#1f2d38]">
              SSO Enabled
            </div>
          </div>
          <div className="inline-flex h-7 items-center rounded-[0.375rem] bg-white px-3">
            <span className="text-[0.6875rem] font-medium leading-4 text-[#8a96a3]">
              SSO_ENABLED
            </span>
          </div>
        </div>

        <FieldShell
          helperText="This strategy overrides the global default and plan entitlements for the chosen targets."
          label="Rollout Strategy"
        >
          <Select value={strategy} onValueChange={setStrategy}>
            <SelectTrigger className={getSelectTriggerClassName()}>
              <SelectValue placeholder="Include Specific Organizations" />
            </SelectTrigger>
            <SelectContent className="rounded-[0.875rem] border border-[#dce5ee] bg-white shadow-[0_12px_28px_rgba(15,23,42,0.08)]">
              {[
                "Include Specific Organizations",
                "Exclude Specific Organizations",
                "Specific Therapists Only",
              ].map(function (option) {
                return (
                  <SelectItem
                    key={option}
                    value={option}
                    className="text-[#2b3946] focus:bg-[#f4f7fa] focus:text-[#2b3946]"
                  >
                    {option}
                  </SelectItem>
                );
              })}
            </SelectContent>
          </Select>
        </FieldShell>

        <FieldShell
          label="Target Organizations"
          helperText="Search and select organizations; Type to search for an organization by name or slug. You can add multiple targets to this rollout."
        >
          <div className="flex min-h-[1.875rem] flex-wrap items-center gap-2">
            {targets.map(function (target) {
              return (
                <TargetChip
                  key={target}
                  label={target}
                  onRemove={function () {
                    setTargets(function (currentTargets) {
                      return currentTargets.filter(function (item) {
                        return item !== target;
                      });
                    });
                  }}
                />
              );
            })}
          </div>
        </FieldShell>

        <FieldShell
          label="Usage Limit Override (Optional)"
          helperText="This feature is a toggle (ON/OFF) type and does not support numeric usage limits. (Optional)"
        >
          <Input
            aria-label="Usage limit override"
            disabled
            value="N/A for Toggle Features"
            className={getMutedInputClassName()}
          />
        </FieldShell>

        <div className="mt-auto flex items-center justify-end gap-3 pt-7">
          <Button type="button" variant="secondary" size="md">
            Cancel
          </Button>
          <Button type="button" variant="primary" size="md">
            Save Rollout
          </Button>
        </div>
      </div>
    </div>
  );
}

export default RolloutCard;
