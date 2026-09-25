const SESSION_NOTE_AI_TEMPLATE_NAME_MAX = 50;

/** Truncate for UI display; full name stays available via title/tooltip. */
function truncateSessionNoteAiTemplateName(
  name: string,
  max = SESSION_NOTE_AI_TEMPLATE_NAME_MAX,
): string {
  const trimmed = name.trim();
  if (trimmed.length <= max) return trimmed;
  return `${trimmed.slice(0, Math.max(0, max - 1))}…`;
}

export {
  SESSION_NOTE_AI_TEMPLATE_NAME_MAX,
  truncateSessionNoteAiTemplateName,
};
