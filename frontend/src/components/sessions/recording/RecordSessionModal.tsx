
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Check, Copy, Download, X } from "lucide-react";
import { AltArrowDown } from "@solar-icons/react-perf/category/arrows/Linear/AltArrowDown";
import { Microphone3 } from "@solar-icons/react-perf/category/video/Linear/Microphone3";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Switch } from "@/components/ui/switch";
import {
  buildStoredSessionTranscript,
  saveStoredSessionTranscript,
  type StoredSessionTranscript,
} from "./sessionTranscriptStore";
import {
  useFinalizeSessionTranscriptionMutation,
  useStartSessionTranscriptionMutation,
} from "@/store/api/admin/sessionTranscripts.api";
import type { SessionNoteTranscriptionResponse } from "@/store/api/admin/sessionNotes.api";
import { getApiErrorMessage } from "@/utils/apiError";
import { getAuthSession } from "@/utils/authStorage";
import { resolveApiBaseForSession } from "@/utils/tenantUrls";
import {
  clearFailedChunksForUpload,
  deleteFailedChunk,
  putFailedChunk,
} from "@/utils/recording/recordingBlobStore";
import {
  TRANSCRIPT_CHUNK_UPLOAD_MAX_ATTEMPTS,
  TRANSCRIPT_LIVE_TIMESLICE_MS,
  TRANSCRIPT_SILENCE_RMS_THRESHOLD,
  TRANSCRIPT_SLICE_MS,
  TranscriptChunkUploadError,
  isRetryableTranscriptChunkUploadStatus,
  toTranscriptionLanguageCode,
} from "@/utils/recording/transcriptionApi";
import { uploadTranscriptChunk } from "@/utils/recording/uploadTranscriptChunk";
import { findClusterStart, pickRecorderMimeType } from "@/utils/recording/webmChunkUtils";

interface RecordSessionModalProps {
  isOpen: boolean;
  onClose: () => void;
  sessionData: {
    sessionId: number;
    clientId: number | null;
    therapistId: number | null;
    dateTime: string;
    sessionType: string;
    clientName: string;
  } | null;
  onTranscriptSaved: (
    transcript: StoredSessionTranscript,
    transcriptionResponse: SessionNoteTranscriptionResponse,
  ) => void;
  onToast: (message: string, type: "success" | "error" | "info") => void;
}

type RecordingStage = "setup" | "recording" | "paused" | "finalizing";

const WAVEFORM_BAR_COUNT = 47;

const LANGUAGE_OPTIONS = [
  { label: "Auto detect", value: "auto" },
  { label: "English (US)", value: "en-US" },
  { label: "English (UK)", value: "en-GB" },
  { label: "Spanish", value: "es-ES" },
  { label: "French", value: "fr-FR" },
  { label: "German", value: "de-DE" },
  { label: "Italian", value: "it-IT" },
  { label: "Portuguese", value: "pt-BR" },
  { label: "Dutch", value: "nl-NL" },
  { label: "Russian", value: "ru-RU" },
  { label: "Turkish", value: "tr-TR" },
  { label: "Polish", value: "pl-PL" },
  { label: "Arabic", value: "ar-SA" },
  { label: "Urdu", value: "ur-PK" },
  { label: "Hindi", value: "hi-IN" },
  { label: "Chinese", value: "zh-CN" },
  { label: "Japanese", value: "ja-JP" },
  { label: "Korean", value: "ko-KR" },
  { label: "Multiple languages", value: "multi" },
];

function normalizeTranscriptText(value: string) {
  return value.replace(/\s+/g, " ").trim();
}

function buildDownloadFileName(clientName: string) {
  return `${clientName.toLowerCase().replace(/[^a-z0-9]+/g, "-") || "session"}-transcript.txt`;
}

function getLiveTranscriptText(payload: Record<string, unknown>) {
  if (typeof payload.text === "string") {
    return payload.text.trim();
  }

  const channel = payload.channel;
  if (!channel || typeof channel !== "object") return "";
  const alternatives = (channel as { alternatives?: unknown }).alternatives;
  if (!Array.isArray(alternatives) || alternatives.length === 0) return "";
  const firstAlternative = alternatives[0];
  if (!firstAlternative || typeof firstAlternative !== "object") return "";
  const transcript = (firstAlternative as { transcript?: unknown }).transcript;
  return typeof transcript === "string" ? transcript.trim() : "";
}

function getChunkResponseCount(
  response: Partial<SessionNoteTranscriptionResponse> & {
    chunksReceived?: unknown;
    receivedChunks?: unknown;
  },
) {
  if (typeof response.chunksReceived === "number") {
    return response.chunksReceived;
  }
  if (typeof response.receivedChunks === "number") {
    return response.receivedChunks;
  }
  return null;
}

function getWebSocketUrl(uploadId: string, language: string) {
  const session = getAuthSession();
  const base = resolveApiBaseForSession(session);
  const url = new URL(base);
  url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
  url.pathname = "/ws/transcribe-live";
  url.searchParams.set("uploadId", uploadId);
  url.searchParams.set("language", language);
  if (session?.accessToken) {
    url.searchParams.set("token", session.accessToken);
  }
  return url.toString();
}

const RecordSessionModal = ({
  isOpen,
  onClose,
  sessionData,
  onTranscriptSaved,
  onToast,
}: RecordSessionModalProps) => {
  const [stage, setStage] = useState<RecordingStage>("setup");
  const [recordingSeconds, setRecordingSeconds] = useState(0);
  const [micLevel, setMicLevel] = useState(0);
  const [waveformLevels, setWaveformLevels] = useState<number[]>(
    () => Array(WAVEFORM_BAR_COUNT).fill(0) as number[],
  );
  const [selectedLanguage, setSelectedLanguage] = useState("auto");
  const [translateToEnglish, setTranslateToEnglish] = useState(false);
  const [availableMics, setAvailableMics] = useState<MediaDeviceInfo[]>([]);
  const [selectedMicId, setSelectedMicId] = useState("");
  const [finalizedPreview, setFinalizedPreview] = useState("");
  const [interimPreview, setInterimPreview] = useState("");
  const [hidePreview, setHidePreview] = useState(false);
  const [chunksSent, setChunksSent] = useState(0);
  const [chunksUploaded, setChunksUploaded] = useState(0);
  const [totalSlices, setTotalSlices] = useState(0);
  const [uploadId, setUploadId] = useState<string | null>(null);

  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const liveRecorderRef = useRef<MediaRecorder | null>(null);
  const mediaStreamRef = useRef<MediaStream | null>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const analyserRef = useRef<AnalyserNode | null>(null);
  const micLevelRef = useRef(0);
  const animationFrameRef = useRef<number | null>(null);
  const meterIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const socketRef = useRef<WebSocket | null>(null);
  const liveSocketFrameQueueRef = useRef<ArrayBuffer[]>([]);
  const liveSocketQueuedBytesRef = useRef(0);
  const livePreviewRef = useRef<HTMLDivElement | null>(null);
  const uploadQueueRef = useRef<Promise<void>>(Promise.resolve());
  const chunksUploadedRef = useRef(0);
  const chunkIndexRef = useRef(0);
  const webmInitRef = useRef<Uint8Array | null>(null);
  const mimeTypeRef = useRef("audio/webm");
  const uploadIdRef = useRef("");
  const segmentStartRef = useRef(0);
  const segmentMaxRmsRef = useRef(0);
  const silentChunksRef = useRef<Map<number, number>>(new Map());
  const failedChunksRef = useRef<
    Map<number, { blob: Blob; durationSec: number; mime: string }>
  >(new Map());
  const isPausedRef = useRef(false);
  const stoppingRef = useRef(false);
  const liveActiveRef = useRef(false);
  const liveStoppedRef = useRef(false);
  const stopFlushedRef = useRef<Promise<void>>(Promise.resolve());
  const chunkErrorToastShownRef = useRef(false);
  const recordingAbortedRef = useRef(false);
  const [hasFatalChunkError, setHasFatalChunkError] = useState(false);

  const [startSessionTranscription, { isLoading: isStarting }] =
    useStartSessionTranscriptionMutation();
  const [finalizeSessionTranscription, { isLoading: isFinalizing }] =
    useFinalizeSessionTranscriptionMutation();

  const livePreview = useMemo(
    () => normalizeTranscriptText([finalizedPreview, interimPreview].filter(Boolean).join(" ")),
    [finalizedPreview, interimPreview],
  );

  const displayedChunkTotal = useMemo(() => {
    if (stage === "recording" || stage === "paused") {
      return Math.max(totalSlices + 1, 1);
    }
    return Math.max(totalSlices, 1);
  }, [stage, totalSlices]);

  const progressPercentage = useMemo(() => {
    if (displayedChunkTotal <= 0) return 0;
    return Math.min(100, Math.round((chunksUploaded / displayedChunkTotal) * 100));
  }, [chunksUploaded, displayedChunkTotal]);

  const syncUploadedChunks = useCallback((nextCount: number) => {
    chunksUploadedRef.current = Math.max(chunksUploadedRef.current, nextCount);
    setChunksUploaded(chunksUploadedRef.current);
  }, []);

  const hasMediaRecorder = useMemo(
    () => typeof window !== "undefined" && typeof MediaRecorder !== "undefined",
    [],
  );

  const loadMicrophones = useCallback(async () => {
    if (!navigator.mediaDevices?.enumerateDevices) return;
    try {
      const devices = await navigator.mediaDevices.enumerateDevices();
      const microphones = devices.filter((device) => device.kind === "audioinput");
      setAvailableMics(microphones);
      if (!selectedMicId && microphones[0]?.deviceId) {
        setSelectedMicId(microphones[0].deviceId);
      }
    } catch {
      setAvailableMics([]);
    }
  }, [selectedMicId]);

  const stopLiveSocket = useCallback(() => {
    liveStoppedRef.current = true;
    if (liveRecorderRef.current && liveRecorderRef.current.state !== "inactive") {
      try {
        liveRecorderRef.current.stop();
      } catch {
        // no-op
      }
    }
    liveRecorderRef.current = null;
    if (socketRef.current) {
      try {
        if (socketRef.current.readyState === WebSocket.OPEN) {
          socketRef.current.send("finalize");
        }
        socketRef.current.close();
      } catch {
        // no-op
      }
      socketRef.current = null;
    }
    liveActiveRef.current = false;
    liveSocketFrameQueueRef.current = [];
    liveSocketQueuedBytesRef.current = 0;
  }, []);

  const appendFinalizedPreviewText = useCallback((text: string) => {
    const normalized = normalizeTranscriptText(text);
    if (!normalized) return;
    setFinalizedPreview((previous) => {
      const next = normalizeTranscriptText(
        previous ? `${previous} ${normalized}` : normalized,
      );
      return next;
    });
  }, []);

  const cleanupMedia = useCallback(() => {
    if (animationFrameRef.current !== null) {
      cancelAnimationFrame(animationFrameRef.current);
      animationFrameRef.current = null;
    }
    if (meterIntervalRef.current !== null) {
      clearInterval(meterIntervalRef.current);
      meterIntervalRef.current = null;
    }
    stopLiveSocket();
    if (audioContextRef.current) {
      void audioContextRef.current.close();
      audioContextRef.current = null;
    }
    analyserRef.current = null;
    mediaRecorderRef.current = null;
    mediaStreamRef.current?.getTracks().forEach((track) => track.stop());
    mediaStreamRef.current = null;
    webmInitRef.current = null;
    segmentMaxRmsRef.current = 0;
  }, [stopLiveSocket]);

  const resetRecordingState = useCallback(() => {
    setStage("setup");
    setRecordingSeconds(0);
    setMicLevel(0);
    micLevelRef.current = 0;
    setWaveformLevels(Array(WAVEFORM_BAR_COUNT).fill(0) as number[]);
    setSelectedLanguage("auto");
    setTranslateToEnglish(false);
    setFinalizedPreview("");
    setInterimPreview("");
    setHidePreview(false);
    setChunksSent(0);
    setChunksUploaded(0);
    setTotalSlices(0);
    setUploadId(null);
    chunkIndexRef.current = 0;
    chunksUploadedRef.current = 0;
    uploadIdRef.current = "";
    silentChunksRef.current = new Map();
    failedChunksRef.current = new Map();
    isPausedRef.current = false;
    stoppingRef.current = false;
    liveStoppedRef.current = false;
    liveActiveRef.current = false;
    uploadQueueRef.current = Promise.resolve();
    stopFlushedRef.current = Promise.resolve();
    chunkErrorToastShownRef.current = false;
    recordingAbortedRef.current = false;
    setHasFatalChunkError(false);
  }, []);

  useEffect(() => {
    if (!isOpen) return;
    const timeoutId = window.setTimeout(() => {
      resetRecordingState();
      void loadMicrophones();
    }, 0);
    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [isOpen, loadMicrophones, resetRecordingState]);

  useEffect(() => {
    let interval: ReturnType<typeof setInterval> | null = null;
    if (stage === "recording") {
      interval = setInterval(() => {
        setRecordingSeconds((prev) => prev + 1);
      }, 1000);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [stage]);

  useEffect(() => {
    if (stage !== "recording") return;
    const interval = window.setInterval(() => {
      setWaveformLevels((levels) => [
        ...levels.slice(-(WAVEFORM_BAR_COUNT - 1)),
        micLevelRef.current,
      ]);
    }, 100);
    return () => window.clearInterval(interval);
  }, [stage]);

  useEffect(() => {
    if (isOpen) return;
    cleanupMedia();
  }, [cleanupMedia, isOpen]);

  useEffect(() => {
    return () => {
      cleanupMedia();
    };
  }, [cleanupMedia]);

  useEffect(() => {
    if (!livePreviewRef.current || hidePreview) return;
    livePreviewRef.current.scrollTop = livePreviewRef.current.scrollHeight;
  }, [hidePreview, livePreview]);

  const connectLiveSocket = useCallback(
    (nextUploadId: string, language: string) => {
      liveStoppedRef.current = false;
      try {
        const socket = new WebSocket(getWebSocketUrl(nextUploadId, language));
        socket.binaryType = "arraybuffer";
        socket.onopen = () => {
          if (liveSocketFrameQueueRef.current.length > 0) {
            for (const frame of liveSocketFrameQueueRef.current) {
              socket.send(frame);
            }
            liveSocketFrameQueueRef.current = [];
            liveSocketQueuedBytesRef.current = 0;
          }
        };
        socket.onmessage = (event) => {
          if (typeof event.data !== "string") return;
          try {
            const parsed = JSON.parse(event.data) as Record<string, unknown>;
            if (parsed.type === "error") {
              return;
            }
            if (parsed.type === "connected" || parsed.type === "ping") {
              return;
            }
            if (parsed.type !== "transcript") {
              return;
            }

            const transcript = getLiveTranscriptText(parsed);
            if (transcript) {
              liveActiveRef.current = true;
              const isFinal = Boolean(
                parsed.isFinal || parsed.is_final || parsed.speechFinal || parsed.speech_final,
              );
              if (isFinal) {
                appendFinalizedPreviewText(transcript);
                setInterimPreview("");
              } else {
                setInterimPreview(transcript);
              }
            }
            const acknowledgedCount = getChunkResponseCount(parsed);
            if (acknowledgedCount !== null) {
              syncUploadedChunks(acknowledgedCount);
            }
          } catch {
            // Deepgram websocket messages are expected to be JSON.
          }
        };
        socket.onerror = () => {};
        socket.onclose = () => {};
        socketRef.current = socket;
      } catch {
        socketRef.current = null;
      }
    },
    [appendFinalizedPreviewText, syncUploadedChunks],
  );

  const startLiveRecorder = useCallback((stream: MediaStream, language: string) => {
    const mimeType = mimeTypeRef.current;
    try {
      connectLiveSocket(uploadIdRef.current, language);
      const recorder = new MediaRecorder(stream, {
        mimeType,
        audioBitsPerSecond: 128_000,
      });
      recorder.ondataavailable = (event) => {
        if (!event.data || event.data.size === 0 || liveStoppedRef.current) return;
        void event.data.arrayBuffer().then((buffer) => {
          if (liveStoppedRef.current) return;
          if (socketRef.current?.readyState === WebSocket.OPEN) {
            socketRef.current.send(buffer);
            return;
          }

          if (socketRef.current?.readyState === WebSocket.CONNECTING) {
            liveSocketFrameQueueRef.current.push(buffer);
            liveSocketQueuedBytesRef.current += buffer.byteLength;

            while (liveSocketQueuedBytesRef.current > 1_048_576 && liveSocketFrameQueueRef.current.length > 0) {
              const dropped = liveSocketFrameQueueRef.current.shift();
              if (dropped) {
                liveSocketQueuedBytesRef.current = Math.max(
                  0,
                  liveSocketQueuedBytesRef.current - dropped.byteLength,
                );
              }
            }
          }
        });
      };
      recorder.start(TRANSCRIPT_LIVE_TIMESLICE_MS);
      liveRecorderRef.current = recorder;
    } catch {
      liveRecorderRef.current = null;
    }
  }, [connectLiveSocket]);

  const pauseLiveRecorder = useCallback(() => {
    if (liveRecorderRef.current?.state === "recording") {
      try {
        liveRecorderRef.current.pause();
      } catch {
        // no-op
      }
    }
  }, []);

  const resumeLiveRecorder = useCallback(() => {
    if (liveRecorderRef.current?.state === "paused") {
      try {
        liveRecorderRef.current.resume();
      } catch {
        // no-op
      }
    }
  }, []);

  const startMicMeter = useCallback((stream: MediaStream) => {
    const audioContext = new window.AudioContext();
    const analyser = audioContext.createAnalyser();
    analyser.fftSize = 1024;
    const source = audioContext.createMediaStreamSource(stream);
    source.connect(analyser);
    audioContextRef.current = audioContext;
    analyserRef.current = analyser;

    const data = new Uint8Array(analyser.fftSize);
    const sample = () => {
      if (!analyserRef.current) return;
      analyserRef.current.getByteTimeDomainData(data);
      let sum = 0;
      for (let i = 0; i < data.length; i += 1) {
        const value = (data[i] - 128) / 128;
        sum += value * value;
      }
      const rms = Math.sqrt(sum / data.length);
      if (rms > segmentMaxRmsRef.current) {
        segmentMaxRmsRef.current = rms;
      }
      const nextLevel = Math.min(100, Math.round(rms * 400));
      micLevelRef.current = nextLevel;
      setMicLevel(nextLevel);
      return rms;
    };

    const updateLevel = () => {
      sample();
      animationFrameRef.current = requestAnimationFrame(updateLevel);
    };
    updateLevel();
    meterIntervalRef.current = setInterval(sample, 100);
  }, []);

  const abortRecordingOnChunkFailure = useCallback(() => {
    if (recordingAbortedRef.current) return;
    recordingAbortedRef.current = true;
    stoppingRef.current = true;
    isPausedRef.current = true;
    setHasFatalChunkError(true);
    setStage("paused");

    const recorder = mediaRecorderRef.current;
    if (recorder && recorder.state !== "inactive") {
      try {
        recorder.stop();
      } catch {
        // no-op
      }
    }

    stopLiveSocket();

    if (animationFrameRef.current !== null) {
      cancelAnimationFrame(animationFrameRef.current);
      animationFrameRef.current = null;
    }
    if (meterIntervalRef.current !== null) {
      clearInterval(meterIntervalRef.current);
      meterIntervalRef.current = null;
    }
    micLevelRef.current = 0;
    setMicLevel(0);

    mediaStreamRef.current?.getTracks().forEach((track) => track.stop());
    mediaStreamRef.current = null;

    if (audioContextRef.current) {
      void audioContextRef.current.close();
      audioContextRef.current = null;
    }
    analyserRef.current = null;
    mediaRecorderRef.current = null;
  }, [stopLiveSocket]);

  const markChunkUploadFailed = useCallback(
    (
      index: number,
      blob: Blob,
      durationSec: number,
      mime: string,
      error: unknown,
    ) => {
      if (!uploadIdRef.current || !sessionData) return;

      failedChunksRef.current.set(index, { blob, durationSec, mime });
      void putFailedChunk({
        uploadId: uploadIdRef.current,
        sessionId: sessionData.sessionId,
        index,
        durationSec,
        mime,
        blob,
      });

      if (!chunkErrorToastShownRef.current) {
        chunkErrorToastShownRef.current = true;
        onToast(getApiErrorMessage(error), "error");
      }

      abortRecordingOnChunkFailure();
    },
    [abortRecordingOnChunkFailure, onToast, sessionData],
  );

  const uploadChunk = useCallback(
    async (blob: Blob, index: number, durationSec: number, mime: string) => {
      if (!sessionData || !uploadIdRef.current || recordingAbortedRef.current) return;
      if (blob.size < 128) {
        onToast(
          `Chunk ${index} had no usable audio (${blob.size} bytes). Check microphone permissions.`,
          "error",
        );
        return;
      }

      for (let attempt = 1; attempt <= TRANSCRIPT_CHUNK_UPLOAD_MAX_ATTEMPTS; attempt += 1) {
        try {
          const response = await uploadTranscriptChunk({
            sessionId: sessionData.sessionId,
            uploadId: uploadIdRef.current,
            chunkIndex: index,
            chunkDurationSeconds: durationSec,
            language: toTranscriptionLanguageCode(selectedLanguage),
            audio: blob,
          });

          failedChunksRef.current.delete(index);
          void deleteFailedChunk(uploadIdRef.current, index);
          const acknowledgedCount =
            getChunkResponseCount(response) ?? chunksUploadedRef.current + 1;
          syncUploadedChunks(acknowledgedCount);
          return;
        } catch (error) {
          const chunkError =
            error instanceof TranscriptChunkUploadError ? error : null;
          const shouldRetry =
            chunkError !== null &&
            isRetryableTranscriptChunkUploadStatus(chunkError.status) &&
            attempt < TRANSCRIPT_CHUNK_UPLOAD_MAX_ATTEMPTS;

          if (!shouldRetry) {
            markChunkUploadFailed(index, blob, durationSec, mime, error);
            return;
          }

          const waitMs =
            chunkError.status === 429
              ? (chunkError.retryAfterSec ?? attempt) * 1000
              : 1000 * attempt;
          await new Promise((resolve) => {
            setTimeout(resolve, waitMs);
          });
        }
      }
    },
    [
      markChunkUploadFailed,
      onToast,
      selectedLanguage,
      sessionData,
      syncUploadedChunks,
    ],
  );

  const startSegmentRecorder = useCallback(() => {
    const stream = mediaStreamRef.current;
    if (!stream) return;

    const mimeType = pickRecorderMimeType();
    mimeTypeRef.current = mimeType;
    webmInitRef.current = null;
    segmentStartRef.current = Date.now();

    let resolveStopped = () => {};
    stopFlushedRef.current = new Promise<void>((resolve) => {
      resolveStopped = resolve;
    });

    const recorder = new MediaRecorder(stream, {
      mimeType,
      audioBitsPerSecond: 128_000,
    });

    recorder.ondataavailable = (event) => {
      if (!event.data || event.data.size === 0 || recordingAbortedRef.current) return;

      const segmentDurationSec = (Date.now() - segmentStartRef.current) / 1000;
      segmentStartRef.current = Date.now();
      const segmentPeakRms = segmentMaxRmsRef.current;
      segmentMaxRmsRef.current = 0;
      const wasSilent = segmentPeakRms < TRANSCRIPT_SILENCE_RMS_THRESHOLD;
      const index = chunkIndexRef.current;
      chunkIndexRef.current += 1;
      setTotalSlices(index + 1);

      if (wasSilent) {
        silentChunksRef.current.set(index, segmentDurationSec);
        return;
      }

      setChunksSent((count) => count + 1);
      uploadQueueRef.current = uploadQueueRef.current.then(async () => {
        try {
          const chunkBytes = new Uint8Array(await event.data.arrayBuffer());
          if (chunkBytes.byteLength < 128) {
            onToast(
              `Chunk ${index} had no usable audio (${chunkBytes.byteLength} bytes).`,
              "error",
            );
            return;
          }

          let blobToSend: Blob;
          if (!webmInitRef.current && mimeType.includes("webm")) {
            const clusterStart = findClusterStart(chunkBytes);
            if (clusterStart > 0) {
              webmInitRef.current = chunkBytes.slice(0, clusterStart);
            }
            blobToSend = new Blob([chunkBytes], { type: mimeType });
          } else if (webmInitRef.current && mimeType.includes("webm")) {
            blobToSend = new Blob(
              [new Uint8Array(webmInitRef.current), chunkBytes],
              { type: mimeType },
            );
          } else {
            blobToSend = new Blob([chunkBytes], { type: mimeType });
          }

          await uploadChunk(blobToSend, index, segmentDurationSec, mimeType);
        } catch (error) {
          onToast(getApiErrorMessage(error), "error");
        }
      });
    };

    recorder.onstop = () => {
      resolveStopped();
    };

    recorder.start(TRANSCRIPT_SLICE_MS);
    mediaRecorderRef.current = recorder;
  }, [onToast, uploadChunk]);

  const handleStartRecording = async () => {
    if (!sessionData) return;
    if (!hasMediaRecorder) {
      onToast("This browser does not support audio recording.", "error");
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: selectedMicId
          ? {
              deviceId: { exact: selectedMicId },
              echoCancellation: true,
              noiseSuppression: true,
              autoGainControl: true,
            }
          : {
              echoCancellation: true,
              noiseSuppression: true,
              autoGainControl: true,
            },
      });

      const startResponse = await startSessionTranscription({
        sessionId: sessionData.sessionId,
        language: toTranscriptionLanguageCode(selectedLanguage),
        translateToEnglish,
      }).unwrap();

      mediaStreamRef.current = stream;
      uploadIdRef.current = startResponse.uploadId;
      setUploadId(startResponse.uploadId);
      await loadMicrophones();

      startMicMeter(stream);
      startSegmentRecorder();
      startLiveRecorder(stream, toTranscriptionLanguageCode(selectedLanguage));
      setStage("recording");
    } catch (error) {
      onToast(getApiErrorMessage(error), "error");
      setStage("setup");
      cleanupMedia();
    }
  };

  const handleDismissAfterFailure = useCallback(() => {
    cleanupMedia();
    resetRecordingState();
  }, [cleanupMedia, resetRecordingState]);

  const handlePauseResume = () => {
    if (hasFatalChunkError) return;

    const recorder = mediaRecorderRef.current;
    if (!recorder) return;

    if (stage === "recording" && recorder.state === "recording") {
      recorder.pause();
      pauseLiveRecorder();
      isPausedRef.current = true;
      setStage("paused");
      return;
    }

    if (stage === "paused" && recorder.state === "paused") {
      segmentMaxRmsRef.current = 0;
      segmentStartRef.current = Date.now();
      recorder.resume();
      resumeLiveRecorder();
      isPausedRef.current = false;
      setStage("recording");
    }
  };

  const handleCopyPreview = () => {
    void navigator.clipboard.writeText(livePreview);
  };

  const handleDownloadPreview = () => {
    const blob = new Blob([livePreview], { type: "text/plain;charset=utf-8" });
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = buildDownloadFileName(sessionData?.clientName ?? "session");
    anchor.click();
    window.URL.revokeObjectURL(url);
  };

  const handleStopAndSave = async () => {
    if (hasFatalChunkError) {
      handleDismissAfterFailure();
      return;
    }

    if (!mediaRecorderRef.current || !sessionData || !uploadId) return;

    if (failedChunksRef.current.size > 0) {
      onToast(
        "Some chunks failed to upload. Resolve failed chunks before saving.",
        "error",
      );
      return;
    }

    setStage("finalizing");
    stoppingRef.current = true;

    try {
      const recorder = mediaRecorderRef.current;
      if (recorder.state === "recording" || recorder.state === "paused") {
        const flushed = stopFlushedRef.current;
        recorder.stop();
        await flushed;
      }

      stopLiveSocket();
      mediaStreamRef.current?.getTracks().forEach((track) => track.stop());
      mediaStreamRef.current = null;

      if (animationFrameRef.current !== null) {
        cancelAnimationFrame(animationFrameRef.current);
        animationFrameRef.current = null;
      }
      if (meterIntervalRef.current !== null) {
        clearInterval(meterIntervalRef.current);
        meterIntervalRef.current = null;
      }
      if (audioContextRef.current) {
        await audioContextRef.current.close();
        audioContextRef.current = null;
      }

      await uploadQueueRef.current;

      const totalChunks = chunkIndexRef.current;
      const finalizeResponse = await finalizeSessionTranscription({
        sessionId: sessionData.sessionId,
        uploadId,
        expectedChunks: totalChunks,
        totalChunks,
        silentChunks: Array.from(silentChunksRef.current.entries()).map(
          ([index, durationSeconds]) => ({ index, durationSeconds }),
        ),
      }).unwrap();

      const transcript = buildStoredSessionTranscript({
        transcriptId: finalizeResponse.id,
        sessionId: sessionData.sessionId,
        uploadId: finalizeResponse.uploadId,
        clientId: sessionData.clientId ?? finalizeResponse.clientId,
        therapistId: sessionData.therapistId,
        clientName: sessionData.clientName,
        sessionType: sessionData.sessionType,
        sessionDateTime: sessionData.dateTime,
        durationSeconds: finalizeResponse.durationSeconds || recordingSeconds,
        status: finalizeResponse.status,
        expectedChunks: totalChunks,
        receivedChunks: chunksUploadedRef.current,
        rawTranscription: finalizeResponse.content,
        diarizedTranscript: finalizeResponse.diarizedTranscript ?? null,
        wordCount: finalizeResponse.wordCount,
        mappedFields: {},
      });

      void clearFailedChunksForUpload(uploadId);
      saveStoredSessionTranscript(transcript);
      cleanupMedia();
      onTranscriptSaved(transcript, {
        success: true,
        rawTranscription:
          finalizeResponse.diarizedTranscript?.trim() || finalizeResponse.content,
        mappedFields: {},
      });
    } catch (error) {
      onToast(getApiErrorMessage(error), "error");
      setStage(chunksUploadedRef.current > 0 ? "paused" : "setup");
      isPausedRef.current = true;
      stoppingRef.current = false;
    }
  };

  if (!isOpen || !sessionData) return null;

  return createPortal(
    <div
      data-scheduling-nested-modal
      className="fixed inset-0 z-[10000] flex items-center justify-center bg-[#111727]/30 p-4"
      onMouseDown={(event) => event.stopPropagation()}
    >
      <div className="flex max-h-[calc(100dvh-2rem)] w-full max-w-[36.6875rem] flex-col overflow-hidden rounded-3xl bg-white shadow-[0_12px_28px_rgba(0,0,0,0.14),0_6px_12px_-6px_rgba(0,0,0,0.12)]">
        <div className="flex shrink-0 items-start justify-between gap-4 px-5 py-5 sm:px-6">
          <div className="min-w-0 space-y-[0.5625rem]">
            <h2 className="text-xl font-semibold leading-7 text-[#1B1C20]">
              AI Note Taker
            </h2>
            <p className="text-sm leading-[1.375rem] text-[#5B616E]">
              Record the session and AI turns it into a transcript you can use to fill your note
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close voice recording"
            className="flex size-11 shrink-0 cursor-pointer items-center justify-center rounded-full text-[#1B1C20] transition-colors hover:bg-[#F6F6F6] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#3C4D58]/30"
          >
            <X size={20} strokeWidth={2} />
          </button>
        </div>

        <div className="min-h-0 overflow-y-auto px-5 pt-3 pb-10 sm:px-6">
          <div className="space-y-8">
            <div className="rounded-xl border border-[#E0E7FF] bg-[#F5F7FF] p-3">
              <div className="space-y-2">
                <div className="flex min-w-0 flex-col gap-0.5 text-sm leading-[1.375rem] min-[430px]:flex-row min-[430px]:items-start min-[430px]:justify-between min-[430px]:gap-4">
                  <span className="shrink-0 text-[#5B616E]">Session:</span>
                  <span
                    className="min-w-0 font-semibold text-[#1B1C20] min-[430px]:truncate min-[430px]:text-right"
                    title={`${sessionData.dateTime} | ${sessionData.sessionType}`}
                  >
                    {sessionData.dateTime} | {sessionData.sessionType}
                  </span>
                </div>
                <div className="flex min-w-0 flex-col gap-0.5 text-sm leading-[1.375rem] min-[430px]:flex-row min-[430px]:items-start min-[430px]:justify-between min-[430px]:gap-4">
                  <span className="shrink-0 text-[#5B616E]">Client:</span>
                  <span
                    className="min-w-0 font-semibold text-[#1B1C20] min-[430px]:truncate min-[430px]:text-right"
                    title={sessionData.clientName}
                  >
                    {sessionData.clientName}
                  </span>
                </div>
              </div>
            </div>

            {stage === "setup" ? (
              <div className="space-y-5">
                <div className="space-y-1.5">
                  <label className="text-sm font-medium leading-[1.375rem] text-[#1B1C20]">
                    Microphone
                  </label>
                  <div className="relative">
                    <select
                      value={selectedMicId}
                      onChange={(event) => setSelectedMicId(event.target.value)}
                      className="h-12 w-full appearance-none rounded-xl border border-[#EDEEF1] bg-white px-4 pr-11 text-base text-[#1B1C20] outline-none transition-colors hover:border-[#D8DBDF] focus:border-[#3C4D58] focus:ring-2 focus:ring-[#3C4D58]/10 sm:text-sm"
                    >
                      {availableMics.length > 0 ? (
                        availableMics.map((device, index) => (
                          <option key={device.deviceId || index} value={device.deviceId}>
                            {device.label || `Microphone ${index + 1}`}
                          </option>
                        ))
                      ) : (
                        <option value="">Default microphone</option>
                      )}
                    </select>
                    <AltArrowDown
                      aria-hidden="true"
                      size={20}
                      color="#5B616E"
                      className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2"
                    />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="text-sm font-medium leading-[1.375rem] text-[#1B1C20]">
                    Spoken language
                  </label>
                  <div className="relative">
                    <select
                      value={selectedLanguage}
                      onChange={(event) => setSelectedLanguage(event.target.value)}
                      className="h-12 w-full appearance-none rounded-xl border border-[#EDEEF1] bg-white px-4 pr-11 text-base text-[#1B1C20] outline-none transition-colors hover:border-[#D8DBDF] focus:border-[#3C4D58] focus:ring-2 focus:ring-[#3C4D58]/10 sm:text-sm"
                    >
                      {LANGUAGE_OPTIONS.map((language) => (
                        <option key={language.value} value={language.value}>
                          {language.label}
                        </option>
                      ))}
                    </select>
                    <AltArrowDown
                      aria-hidden="true"
                      size={20}
                      color="#5B616E"
                      className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2"
                    />
                  </div>
                  <p className="text-xs leading-[1.125rem] text-[#5B616E]">
                    Used for live preview and the saved transcript. Cannot be changed once recording starts.
                  </p>
                </div>

                <div className="flex items-center justify-between gap-4 rounded-xl border border-[#E0E7FF] bg-[#F5F7FF] p-3">
                  <div className="min-w-0 space-y-0.5">
                    <h4 className="text-sm font-semibold leading-[1.375rem] text-[#1B1C20]">
                      Translate to English
                    </h4>
                    <p className="text-xs leading-[1.125rem] text-[#5B616E]">
                      When enabled, the server translates the finalized transcript to English.
                    </p>
                  </div>
                  <Switch
                    checked={translateToEnglish}
                    onCheckedChange={setTranslateToEnglish}
                    className="shrink-0"
                  />
                </div>

                <div className="flex flex-col items-center gap-4 pt-3">
                  <Button
                    type="button"
                    onClick={() => void handleStartRecording()}
                    disabled={isStarting}
                    aria-label={isStarting ? "Starting recording" : "Start recording"}
                    className="size-[3.625rem] rounded-full bg-[#3C4D58] p-[0.9375rem] text-white shadow-[0_16px_8px_rgba(30,40,46,0.05),0_32px_16px_rgba(30,40,46,0.05)] hover:bg-[#323E47] active:bg-[#1E282E]"
                  >
                    {isStarting ? (
                      <ContentLoader size="xl" />
                    ) : (
                      <Microphone3
                        size={28}
                        color="#FFFFFF"
                        className="size-7"
                      />
                    )}
                  </Button>
                  <p className="max-w-[21.375rem] text-center text-sm leading-[1.375rem] text-[#5B616E]">
                    Click the mic to start recording the session. When you stop, AI creates the transcript, and Smart Fill can suggest your note fields for you to review.
                  </p>
                </div>

                {!hasMediaRecorder ? (
                  <div className="rounded-[1rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-sm text-(--status-denied)">
                    Audio recording is not supported in this browser.
                  </div>
                ) : null}
              </div>
            ) : null}

            {stage === "recording" || stage === "paused" ? (
              <div className="space-y-6">
                <div className="flex min-h-24 items-center">
                  <div className="flex h-9 w-full items-center justify-between gap-[0.4375rem]">
                    <button
                      type="button"
                      onClick={handlePauseResume}
                      disabled={hasFatalChunkError}
                      aria-label={stage === "paused" ? "Resume recording" : "Pause recording"}
                      aria-pressed={stage === "paused"}
                      title={stage === "paused" ? "Resume recording" : "Pause recording"}
                      className="flex h-9 min-w-0 flex-1 cursor-pointer items-center rounded-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#3C4D58]/30 disabled:cursor-not-allowed"
                    >
                      <span
                        role="img"
                        aria-label={`Microphone level ${micLevel}%`}
                        className={`flex h-5 w-full max-w-[28.75rem] items-center justify-between overflow-hidden transition-opacity ${
                          stage === "paused" ? "opacity-45" : "opacity-100"
                        }`}
                      >
                        {waveformLevels.map((level, index) => (
                          <span
                            key={index}
                            aria-hidden="true"
                            className="w-0.5 shrink-0 rounded-full bg-[#1B1C20] transition-[height] duration-100"
                            style={{
                              height: `${Math.max(
                                2,
                                Math.min(
                                  20,
                                  Math.round(
                                    2 +
                                      level * 0.18 *
                                        (0.6 + ((index * 7) % 5) * 0.1),
                                  ),
                                ),
                              )}px`,
                            }}
                          />
                        ))}
                      </span>
                    </button>

                    <div className="flex h-9 shrink-0 items-center">
                      <button
                        type="button"
                        onClick={onClose}
                        aria-label="Cancel recording"
                        title="Cancel recording"
                        className="flex size-9 cursor-pointer items-center justify-center rounded-md text-[#1B1C20] transition-colors hover:bg-[#F6F6F6] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#3C4D58]/30"
                      >
                        <X size={20} strokeWidth={2} />
                      </button>
                      <button
                        type="button"
                        onClick={() => void handleStopAndSave()}
                        aria-label={hasFatalChunkError ? "Dismiss recording error" : "Save recording"}
                        title={hasFatalChunkError ? "Dismiss recording error" : "Save recording"}
                        className="flex size-9 cursor-pointer items-center justify-center rounded-md text-[#1B1C20] transition-colors hover:bg-[#F6F6F6] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#3C4D58]/30"
                      >
                        <Check size={20} strokeWidth={2} />
                      </button>
                    </div>
                  </div>
                </div>

                <div className="rounded-xl border border-[#E0E7FF] bg-[#F5F7FF] p-3">
                  <div className="mb-2 flex flex-wrap items-center justify-between gap-2 text-xs leading-[1.125rem] text-[#5B616E]">
                    <span>
                      Transcript progress · {chunksUploaded}/{displayedChunkTotal} chunks
                    </span>
                    <span className="font-semibold text-[#1B1C20]">
                      {progressPercentage}%
                    </span>
                  </div>
                  <div className="h-1 overflow-hidden rounded-full bg-[#E0E7FF]">
                    <div
                      className="h-full rounded-full bg-[#3C4D58] transition-all duration-300"
                      style={{ width: `${progressPercentage}%` }}
                    />
                  </div>
                  <span className="sr-only">{chunksSent} chunks contain audio</span>
                </div>

                <div className="flex items-center justify-end gap-3">
                  <label className="flex items-center gap-2 text-xs leading-[1.125rem] text-[#5B616E]">
                    Hide live preview (screen-share safe)
                    <Checkbox
                      id="hide-session-live-preview"
                      checked={hidePreview}
                      onCheckedChange={(checked) => setHidePreview(checked)}
                    />
                  </label>
                </div>

                {!hidePreview ? (
                  <div className="rounded-xl border border-[#EDEEF1] bg-white p-4">
                    <div className="mb-3 flex items-center justify-between gap-3">
                      <div className="min-w-0">
                        <div className="text-sm font-semibold leading-[1.375rem] text-[#1B1C20]">
                          Live transcript
                        </div>
                        <div className="text-xs leading-[1.125rem] text-[#5B616E]">
                          {stage === "paused" ? "Recording paused" : "Listening and transcribing"}
                        </div>
                      </div>
                      <div className="flex shrink-0 items-center">
                        <button
                          type="button"
                          onClick={handleCopyPreview}
                          aria-label="Copy live transcript"
                          className="flex size-9 cursor-pointer items-center justify-center rounded-md text-[#1B1C20] transition-colors hover:bg-[#F6F6F6]"
                        >
                          <Copy size={20} />
                        </button>
                        <button
                          type="button"
                          onClick={handleDownloadPreview}
                          aria-label="Download live transcript"
                          className="flex size-9 cursor-pointer items-center justify-center rounded-md text-[#1B1C20] transition-colors hover:bg-[#F6F6F6]"
                        >
                          <Download size={20} />
                        </button>
                      </div>
                    </div>
                    <div
                      ref={livePreviewRef}
                      className="max-h-60 min-h-24 overflow-y-auto rounded-lg bg-[#FAFAFB] p-3 text-sm leading-[1.375rem] text-[#1B1C20]"
                    >
                      {finalizedPreview || interimPreview ? (
                        <>
                          {finalizedPreview ? <span>{finalizedPreview}</span> : null}
                          {finalizedPreview && interimPreview ? " " : null}
                          {interimPreview ? (
                            <span className="text-(--text-neutral-500)">{interimPreview}</span>
                          ) : null}
                        </>
                      ) : (
                        "Listening..."
                      )}
                    </div>
                  </div>
                ) : null}
              </div>
            ) : null}

            {stage === "finalizing" ? (
              <div
                className="flex min-h-48 flex-col items-center justify-center gap-4 text-center"
                role="status"
                aria-live="polite"
              >
                <span className="flex size-12 items-center justify-center rounded-full bg-[#F5F7FF] text-[#3C4D58]">
                  <ContentLoader size="lg" />
                </span>
                <div className="space-y-1">
                  <h3 className="text-base font-semibold leading-6 text-[#1B1C20]">
                    {isFinalizing
                      ? "Saving your recording"
                      : "Finishing recording"}
                  </h3>
                  <p className="mx-auto max-w-[21.375rem] text-sm leading-[1.375rem] text-[#5B616E]">
                    {isFinalizing
                      ? "Finalizing the transcript and preparing it for review."
                      : "Securing the last audio segment before transcription."}
                  </p>
                </div>
                <div className="h-1 w-40 overflow-hidden rounded-full bg-[#E0E7FF]">
                  <div className="h-full w-1/2 animate-pulse rounded-full bg-[#3C4D58]" />
                </div>
              </div>
            ) : null}
          </div>
        </div>
      </div>
    </div>,
    document.body,
  );
};

export default RecordSessionModal;
