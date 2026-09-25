import type { SystemOptionValue } from "@/store/api/admin/systemOptions.api";

export const CUSTOM_TASK_TITLE_VALUE = "__custom__";

export function buildTaskTitlePayload(
  titleType: string,
  customTitle: string | undefined,
  titleOptions: SystemOptionValue[],
): { title: string; titleKey?: string } {
  if (titleType === CUSTOM_TASK_TITLE_VALUE) {
    return { title: customTitle?.trim() || "" };
  }

  const match = titleOptions.find((option) => option.optionKey === titleType);
  if (match) {
    return { title: match.optionLabel, titleKey: match.optionKey };
  }

  return { title: titleType };
}

export function resolveTaskTitleFormValue(
  title: string,
  titleKey: string | null | undefined,
  titleOptions: SystemOptionValue[],
): { titleType: string; customTitle: string } {
  if (titleKey) {
    const match = titleOptions.find((option) => option.optionKey === titleKey);
    if (match) {
      return { titleType: match.optionKey, customTitle: "" };
    }
  }

  const byLabel = titleOptions.find(
    (option) => option.optionLabel === title || option.optionKey === title,
  );
  if (byLabel) {
    return { titleType: byLabel.optionKey, customTitle: "" };
  }

  if (title.trim()) {
    return { titleType: CUSTOM_TASK_TITLE_VALUE, customTitle: title };
  }

  return { titleType: "", customTitle: "" };
}
