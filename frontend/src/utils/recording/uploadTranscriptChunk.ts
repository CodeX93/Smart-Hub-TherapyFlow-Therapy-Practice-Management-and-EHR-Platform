import { fetchWithAuth } from "@/utils/fetchWithAuth";
import type { AppendSessionTranscriptChunkResponse } from "@/store/api/admin/sessionTranscripts.api";
import { TranscriptChunkUploadError } from "@/utils/recording/transcriptionApi";

export interface UploadTranscriptChunkArgs {
  sessionId: number;
  uploadId: string;
  chunkIndex: number;
  chunkDurationSeconds: number;
  language?: string;
  audio: Blob;
}

export async function uploadTranscriptChunk(
  args: UploadTranscriptChunkArgs,
): Promise<AppendSessionTranscriptChunkResponse> {
  const mimeType = args.audio.type || "audio/webm";
  const ext = mimeType.includes("mp4") ? "mp4" : "webm";

  const formData = new FormData();
  formData.append("uploadId", args.uploadId);
  formData.append("chunkIndex", String(args.chunkIndex));
  formData.append("chunkDurationSeconds", String(args.chunkDurationSeconds));
  formData.append("audio", args.audio, `chunk-${args.chunkIndex}.${ext}`);
  if (args.language) {
    formData.append("language", args.language);
  }

  // FormData can be sent again as-is, so a 401 → refresh → retry is safe here.
  const response = await fetchWithAuth(
    `/api/v1/sessions/${args.sessionId}/transcribe-chunk`,
    {
      method: "POST",
      body: formData,
    },
  );

  const payload = (await response.json().catch(() => ({}))) as Record<string, unknown>;
  if (!response.ok) {
    const apiMessage =
      typeof payload.message === "string" ? payload.message.trim() : "";
    const apiError =
      typeof payload.error === "string" ? payload.error.trim() : "";
    const message =
      apiMessage ||
      apiError ||
      `Chunk upload failed (${response.status})`;
    const retryAfterHeader = response.headers.get("Retry-After");
    const retryAfterSec = retryAfterHeader
      ? Number.parseInt(retryAfterHeader, 10)
      : undefined;

    throw new TranscriptChunkUploadError(
      message,
      response.status,
      Number.isFinite(retryAfterSec) ? retryAfterSec : undefined,
    );
  }

  return payload as unknown as AppendSessionTranscriptChunkResponse;
}
