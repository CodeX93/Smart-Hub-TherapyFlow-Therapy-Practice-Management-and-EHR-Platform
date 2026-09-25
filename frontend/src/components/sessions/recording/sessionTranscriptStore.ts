const SESSION_TRANSCRIPTS_STORAGE_KEY = "session-transcripts";

export interface StoredSessionTranscript {
  transcriptId?: number;
  sessionId: number;
  uploadId: string;
  clientId: number | null;
  therapistId: number | null;
  clientName: string;
  sessionType: string;
  sessionDateTime: string;
  status: string;
  expectedChunks: number;
  receivedChunks: number;
  rawTranscription: string;
  /** AI speaker-labeled version; null/undefined until Identify Speakers is run */
  diarizedTranscript?: string | null;
  formattedTranscript: string;
  mappedFields: Record<string, string>;
  durationSeconds: number;
  wordCount: number;
  createdAt: string;
  updatedAt: string;
  finalizedAt?: string;
  failureReason?: string;
}

/**
 * Transcripts are PHI, so they live only as long as the tab. Earlier builds kept
 * them in localStorage, where they outlived the session on shared machines.
 */
function isBrowser() {
  return typeof window !== "undefined" && typeof window.sessionStorage !== "undefined";
}

export function clearStoredSessionTranscripts() {
  if (!isBrowser()) return;
  window.sessionStorage.removeItem(SESSION_TRANSCRIPTS_STORAGE_KEY);
  window.localStorage.removeItem(SESSION_TRANSCRIPTS_STORAGE_KEY);
}

if (isBrowser()) {
  window.localStorage.removeItem(SESSION_TRANSCRIPTS_STORAGE_KEY);
}

function readTranscriptMap(): Record<string, StoredSessionTranscript> {
  if (!isBrowser()) return {};

  try {
    const raw = window.sessionStorage.getItem(SESSION_TRANSCRIPTS_STORAGE_KEY);
    if (!raw) return {};
    const parsed = JSON.parse(raw);
    return typeof parsed === "object" && parsed !== null ? parsed as Record<string, StoredSessionTranscript> : {};
  } catch {
    return {};
  }
}

function writeTranscriptMap(value: Record<string, StoredSessionTranscript>) {
  if (!isBrowser()) return;
  window.sessionStorage.setItem(SESSION_TRANSCRIPTS_STORAGE_KEY, JSON.stringify(value));
}

export function buildStoredSessionTranscript(args: {
  transcriptId?: number;
  sessionId: number;
  uploadId: string;
  clientId: number | null;
  therapistId: number | null;
  clientName: string;
  sessionType: string;
  sessionDateTime: string;
  durationSeconds: number;
  status: string;
  expectedChunks: number;
  receivedChunks: number;
  rawTranscription: string;
  diarizedTranscript?: string | null;
  mappedFields?: Record<string, string>;
  wordCount?: number;
  finalizedAt?: string;
  failureReason?: string;
}): StoredSessionTranscript {
  const now = new Date().toISOString();
  const diarized = args.diarizedTranscript?.trim() || null;
  const transcriptText = (diarized || args.rawTranscription).trim();
  const formattedTranscript = transcriptText
    ? transcriptText
    : "[00:00:00]\n[GAP IN RECORDING — no transcript available]";
  const wordCount =
    args.wordCount ??
    (transcriptText ? transcriptText.split(/\s+/).filter(Boolean).length : 0);

  return {
    transcriptId: args.transcriptId,
    sessionId: args.sessionId,
    uploadId: args.uploadId,
    clientId: args.clientId,
    therapistId: args.therapistId,
    clientName: args.clientName,
    sessionType: args.sessionType,
    sessionDateTime: args.sessionDateTime,
    status: args.status,
    expectedChunks: args.expectedChunks,
    receivedChunks: args.receivedChunks,
    rawTranscription: args.rawTranscription,
    diarizedTranscript: diarized,
    formattedTranscript,
    mappedFields: args.mappedFields ?? {},
    durationSeconds: args.durationSeconds,
    wordCount,
    createdAt: now,
    updatedAt: now,
    finalizedAt: args.finalizedAt,
    failureReason: args.failureReason,
  };
}

export function saveStoredSessionTranscript(transcript: StoredSessionTranscript) {
  const existing = readTranscriptMap();
  writeTranscriptMap({
    ...existing,
    [String(transcript.sessionId)]: transcript,
  });
}

export function getStoredSessionTranscript(sessionId: number) {
  const existing = readTranscriptMap();
  return existing[String(sessionId)] ?? null;
}

export function hasSessionTranscriptContent(sessionId: number) {
  const transcript = getStoredSessionTranscript(sessionId);
  return Boolean(transcript?.rawTranscription?.trim());
}

export function removeStoredSessionTranscript(sessionId: number) {
  const existing = readTranscriptMap();
  if (!(String(sessionId) in existing)) return;
  const next = { ...existing };
  delete next[String(sessionId)];
  writeTranscriptMap(next);
}
