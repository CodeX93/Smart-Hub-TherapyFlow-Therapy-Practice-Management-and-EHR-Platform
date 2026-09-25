import type { LibraryEntry } from "@/store/api/admin/libraryEntries.api";

export type SessionNoteLibraryField =
  | "sessionFocus"
  | "symptoms"
  | "shortTermGoals"
  | "intervention"
  | "progress";

export const SESSION_NOTE_LIBRARY_FIELDS: SessionNoteLibraryField[] = [
  "sessionFocus",
  "symptoms",
  "shortTermGoals",
  "intervention",
  "progress",
];

export const SESSION_NOTE_FIELD_LABELS: Record<SessionNoteLibraryField, string> = {
  sessionFocus: "Session Focus",
  symptoms: "Symptoms",
  shortTermGoals: "Short-term Goals",
  intervention: "Intervention",
  progress: "Progress",
};

const CATEGORY_NAME_BY_FIELD: Record<SessionNoteLibraryField, string> = {
  sessionFocus: "Session Focus",
  symptoms: "Symptoms",
  shortTermGoals: "Short-term Goals",
  intervention: "Intervention",
  progress: "Progress",
};

const FALLBACK_CATEGORY_ID_BY_FIELD: Record<SessionNoteLibraryField, number> = {
  sessionFocus: 1,
  symptoms: 2,
  shortTermGoals: 3,
  intervention: 4,
  progress: 5,
};

export function isSessionNoteLibraryField(field: string): field is SessionNoteLibraryField {
  return SESSION_NOTE_LIBRARY_FIELDS.includes(field as SessionNoteLibraryField);
}

export function buildSessionNoteCategoryIdMap(
  categories: Array<{ id: number; name: string }>,
): Record<SessionNoteLibraryField, number> {
  const map = { ...FALLBACK_CATEGORY_ID_BY_FIELD };

  for (const field of SESSION_NOTE_LIBRARY_FIELDS) {
    const targetName = CATEGORY_NAME_BY_FIELD[field].toLowerCase();
    const match = categories.find(
      (category) => category.name.trim().toLowerCase() === targetName,
    );
    if (match) {
      map[field] = match.id;
    }
  }

  return map;
}

export function filterLibraryEntriesForField(
  entries: LibraryEntry[],
  categoryId: number,
  searchQuery: string,
): LibraryEntry[] {
  const query = searchQuery.trim().toLowerCase();

  return entries.filter((entry) => {
    if (!entry.isActive) return false;
    if (entry.categoryId !== categoryId) return false;
    if (!query) return true;

    return (
      entry.title.toLowerCase().includes(query) ||
      entry.content.toLowerCase().includes(query) ||
      entry.tags.some((tag) => tag.toLowerCase().includes(query))
    );
  });
}

export function appendLibraryContent(currentValue: string, content: string): string {
  const trimmed = currentValue.trim();
  if (!trimmed) return content;
  return `${trimmed}\n\n${content}`;
}
