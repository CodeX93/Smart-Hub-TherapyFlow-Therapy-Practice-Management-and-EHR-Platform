/** Constants and helpers aligned with transcription-api-integration.md (v2). */

export const TRANSCRIPT_PLANNING_MAX_CHUNKS = 5000;
export const TRANSCRIPT_RETENTION_DAYS = 90;
export const TRANSCRIPT_SLICE_MS = 20_000;
export const TRANSCRIPT_MAX_CHUNK_INDEX = 500;
export const TRANSCRIPT_CHUNK_UPLOAD_MAX_ATTEMPTS = 3;
export const TRANSCRIPT_SILENCE_RMS_THRESHOLD = 0.0008;
export const TRANSCRIPT_LIVE_TIMESLICE_MS = 1000;

export const TRANSCRIPT_READY_STATUS = "ready";

export function toTranscriptionLanguageCode(value: string): string {
  const normalized = value.trim().toLowerCase().replace(/_/g, "-");
  if (!normalized) return "en-us";

  const mappedLocaleToApiCode: Record<string, string> = {
    auto: "auto",
    multi: "multi",
    "en": "en",
    "en-us": "en-us",
    "en-gb": "en-gb",
    "es": "es",
    "es-es": "es",
    "es-mx": "es",
    "fr": "fr",
    "fr-fr": "fr",
    "de": "de",
    "de-de": "de",
    "it": "it",
    "it-it": "it",
    "pt": "pt",
    "pt-br": "pt",
    "pt-pt": "pt",
    "nl": "nl",
    "nl-nl": "nl",
    "ru": "ru",
    "ru-ru": "ru",
    "hi": "hi",
    "hi-in": "hi",
    "zh": "zh",
    "zh-cn": "zh",
    "zh-tw": "zh",
    "ja": "ja",
    "ja-jp": "ja",
    "ko": "ko",
    "ko-kr": "ko",
    "tr": "tr",
    "tr-tr": "tr",
    "pl": "pl",
    "pl-pl": "pl",
    "ar": "ar",
    "ar-sa": "ar",
    "ar-ae": "ar",
    "ur": "ur",
    "ur-pk": "ur",
  };

  return mappedLocaleToApiCode[normalized] ?? normalized;
}

export function getTranscriptContent(payload: {
  content?: string | null;
  finalTranscript?: string | null;
}): string {
  const content = payload.content?.trim() ?? "";
  if (content) return content;
  return payload.finalTranscript?.trim() ?? "";
}

export function isTranscriptReady(status: string | null | undefined): boolean {
  return status?.toLowerCase() === TRANSCRIPT_READY_STATUS;
}

export class TranscriptChunkUploadError extends Error {
  readonly status: number;
  readonly retryAfterSec?: number;

  constructor(message: string, status: number, retryAfterSec?: number) {
    super(message);
    this.name = "TranscriptChunkUploadError";
    this.status = status;
    this.retryAfterSec = retryAfterSec;
  }
}

/** Only retry chunk uploads that may succeed on a later attempt. */
export function isRetryableTranscriptChunkUploadStatus(status: number): boolean {
  return status === 429 || status === 502 || status === 504;
}
