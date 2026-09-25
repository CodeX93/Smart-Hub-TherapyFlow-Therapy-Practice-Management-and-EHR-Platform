import CustomInput from "@/components/form/CustomInput";
import CustomSelect from "@/components/form/CustomSelect";
import CustomTextarea from "@/components/form/CustomTextarea";
import { Switch } from "@/components/ui/switch";
import type {
  AssessmentSection,
  AccessLevel,
  ReportSectionType,
} from "../../types/create-assessment";
import {
  ACCESS_LEVEL_OPTIONS,
  REPORT_SECTION_TYPE_OPTIONS,
} from "@/pages/admin/content/content.static";

interface SectionConfigurationProps {
  section: AssessmentSection;
  onUpdate: (updates: Partial<AssessmentSection>) => void;
}

const SectionConfiguration = ({
  section,
  onUpdate,
}: SectionConfigurationProps) => {
  return (
    <div className="flex flex-col gap-6">
      <div className="grid grid-cols-2 gap-4">
        <CustomInput
          label="Section Title"
          value={section.title}
          onChange={(e) => onUpdate({ title: e.target.value })}
          className="bg-white"
        />
        <CustomSelect
          label="Access Level"
          options={ACCESS_LEVEL_OPTIONS}
          value={section.accessLevel}
          onChange={(val) => onUpdate({ accessLevel: val as AccessLevel })}
          className="bg-white"
          isSearch={false}
        />
      </div>

      <CustomTextarea
        label="Description"
        value={section.description}
        onChange={(e) => onUpdate({ description: e.target.value })}
        className="bg-white min-h-25"
      />

      <div className="flex items-center justify-between gap-4">
        <div className="flex-1">
          <CustomSelect
            label="Report Section Type"
            options={REPORT_SECTION_TYPE_OPTIONS}
            value={section.reportSectionType}
            onChange={(val) =>
              onUpdate({ reportSectionType: val as ReportSectionType })
            }
            className="bg-white"
            isSearch={false}
          />
        </div>
        <div className="flex items-center gap-3">
          <Switch
            checked={section.enableScoring}
            onCheckedChange={(checked) => onUpdate({ enableScoring: checked })}
          />
          <div className="flex flex-col">
            <span className="text-sm text-(--text-neutral-800) font-medium">
              Enable Scoring
            </span>
            <span className="text-[0.6875rem] text-(--text-neutral-400)">
              (shows score values for all options)
            </span>
          </div>
        </div>
      </div>

      <div className="flex flex-col gap-1.5">
        <CustomTextarea
          label="AI Report Instructions"
          value={section.aiReportInstructions}
          onChange={(e) => onUpdate({ aiReportInstructions: e.target.value })}
          className="bg-white min-h-25"
        />
        <p className="text-[0.6875rem] text-(--text-neutral-400)">
          <span className="font-bold text-(--text-neutral-800)">
            How it works:
          </span>{" "}
          The AI will use these instructions to generate this section in
          reports.
        </p>
      </div>
    </div>
  );
};

export default SectionConfiguration;
