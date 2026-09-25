import { toTranscriptionLanguageCode } from "@/utils/recording/transcriptionApi";

export { toTranscriptionLanguageCode as toRecordingLanguageCode };

/** Locate the first WebM Cluster element (EBML ID 0x1F43B675) in a buffer. */
export function findClusterStart(bytes: Uint8Array): number {
  for (let i = 0; i + 3 < bytes.length; i += 1) {
    if (
      bytes[i] === 0x1f &&
      bytes[i + 1] === 0x43 &&
      bytes[i + 2] === 0xb6 &&
      bytes[i + 3] === 0x75
    ) {
      return i;
    }
  }
  return -1;
}

export function pickRecorderMimeType(): string {
  if (typeof MediaRecorder === "undefined") return "audio/webm";
  if (MediaRecorder.isTypeSupported("audio/webm;codecs=opus")) {
    return "audio/webm;codecs=opus";
  }
  if (MediaRecorder.isTypeSupported("audio/webm")) return "audio/webm";
  if (MediaRecorder.isTypeSupported("audio/mp4")) return "audio/mp4";
  return "audio/webm";
}
