import { useCallback, useEffect, useRef, useState } from "react";

type SpeechRecognitionConstructor = new () => SpeechRecognition;

interface SpeechRecognitionResultLike {
  readonly isFinal: boolean;
  readonly 0: { transcript: string };
}

interface SpeechRecognitionEventLike extends Event {
  readonly resultIndex: number;
  readonly results: ArrayLike<SpeechRecognitionResultLike> & {
    length: number;
  };
}

interface SpeechRecognitionErrorEventLike extends Event {
  readonly error: string;
  readonly message?: string;
}

interface SpeechRecognition extends EventTarget {
  continuous: boolean;
  interimResults: boolean;
  lang: string;
  onresult: ((event: SpeechRecognitionEventLike) => void) | null;
  onerror: ((event: SpeechRecognitionErrorEventLike) => void) | null;
  onend: (() => void) | null;
  start: () => void;
  stop: () => void;
  abort: () => void;
}

declare global {
  interface Window {
    SpeechRecognition?: SpeechRecognitionConstructor;
    webkitSpeechRecognition?: SpeechRecognitionConstructor;
  }
}

export type SpeechRecognitionStatus =
  | "idle"
  | "listening"
  | "unsupported"
  | "error";

function getSpeechRecognitionConstructor(): SpeechRecognitionConstructor | null {
  if (typeof window === "undefined") return null;
  return window.SpeechRecognition ?? window.webkitSpeechRecognition ?? null;
}

export function isSpeechRecognitionSupported(): boolean {
  return getSpeechRecognitionConstructor() !== null;
}

interface UseSpeechRecognitionOptions {
  lang?: string;
  onTranscript?: (transcript: string, isFinal: boolean) => void;
  onError?: (message: string) => void;
}

export function useSpeechRecognition({
  lang = "en-US",
  onTranscript,
  onError,
}: UseSpeechRecognitionOptions = {}) {
  const [status, setStatus] = useState<SpeechRecognitionStatus>(() =>
    isSpeechRecognitionSupported() ? "idle" : "unsupported",
  );
  const recognitionRef = useRef<SpeechRecognition | null>(null);
  const finalTranscriptRef = useRef("");
  const shouldRestartRef = useRef(false);
  const onTranscriptRef = useRef(onTranscript);
  const onErrorRef = useRef(onError);
  const langRef = useRef(lang);

  useEffect(() => {
    onTranscriptRef.current = onTranscript;
  }, [onTranscript]);

  useEffect(() => {
    onErrorRef.current = onError;
  }, [onError]);

  useEffect(() => {
    langRef.current = lang;
    if (recognitionRef.current) {
      recognitionRef.current.lang = lang;
    }
  }, [lang]);

  const cleanupRecognition = useCallback(() => {
    const recognition = recognitionRef.current;
    if (!recognition) return;
    recognition.onresult = null;
    recognition.onerror = null;
    recognition.onend = null;
    try {
      recognition.abort();
    } catch {
      // ignore abort errors when already stopped
    }
    recognitionRef.current = null;
  }, []);

  useEffect(() => cleanupRecognition, [cleanupRecognition]);

  const stop = useCallback(() => {
    shouldRestartRef.current = false;
    const recognition = recognitionRef.current;
    if (!recognition) {
      setStatus((prev) => (prev === "unsupported" ? prev : "idle"));
      return;
    }
    try {
      recognition.stop();
    } catch {
      cleanupRecognition();
      setStatus("idle");
    }
  }, [cleanupRecognition]);

  const start = useCallback(() => {
    const SpeechRecognitionCtor = getSpeechRecognitionConstructor();
    if (!SpeechRecognitionCtor) {
      setStatus("unsupported");
      onErrorRef.current?.(
        "Speech recognition is not supported in this browser. Try Chrome or Edge.",
      );
      return;
    }

    cleanupRecognition();
    finalTranscriptRef.current = "";
    shouldRestartRef.current = true;

    const recognition = new SpeechRecognitionCtor();
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.lang = langRef.current;

    recognition.onresult = (event) => {
      let interim = "";
      let finals = finalTranscriptRef.current;

      for (let i = event.resultIndex; i < event.results.length; i += 1) {
        const result = event.results[i];
        const text = result?.[0]?.transcript?.trim() ?? "";
        if (!text) continue;
        if (result.isFinal) {
          finals = finals ? `${finals} ${text}` : text;
        } else {
          interim = interim ? `${interim} ${text}` : text;
        }
      }

      finalTranscriptRef.current = finals;
      const combined = [finals, interim].filter(Boolean).join(" ").trim();
      onTranscriptRef.current?.(combined, !interim);
    };

    recognition.onerror = (event) => {
      if (event.error === "aborted" || event.error === "no-speech") {
        return;
      }
      shouldRestartRef.current = false;
      setStatus("error");
      onErrorRef.current?.(
        event.message || `Speech recognition error: ${event.error}`,
      );
    };

    recognition.onend = () => {
      if (shouldRestartRef.current) {
        try {
          recognition.start();
          return;
        } catch {
          shouldRestartRef.current = false;
        }
      }
      recognitionRef.current = null;
      setStatus("idle");
    };

    recognitionRef.current = recognition;
    try {
      recognition.start();
      setStatus("listening");
    } catch (error) {
      shouldRestartRef.current = false;
      recognitionRef.current = null;
      setStatus("error");
      onErrorRef.current?.(
        error instanceof Error
          ? error.message
          : "Unable to start speech recognition.",
      );
    }
  }, [cleanupRecognition]);

  return {
    status,
    isListening: status === "listening",
    isSupported: status !== "unsupported",
    start,
    stop,
  };
}
