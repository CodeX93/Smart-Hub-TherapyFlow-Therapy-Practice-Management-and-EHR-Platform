
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useEffect, useRef, useState } from "react";
import { X, Mic, Check } from "lucide-react";
import { cn } from "../../../lib/utils";
import {
  useTranscribeSessionNoteAudioMutation,
  type SessionNoteTranscriptionResponse,
} from "@/store/api/admin/sessionNotes.api";
import { getApiErrorMessage } from "@/utils/apiError";

interface VoiceRecordingModalProps {
  isOpen: boolean;
  onClose: () => void;
  sessionData: {
    dateTime: string;
    clientName: string;
  };
  sessionNoteId?: number;
  onTranscriptionComplete: (data: SessionNoteTranscriptionResponse) => void;
}

type RecordingState = "idle" | "recording" | "transcribing";

  const WaveformBars = ({ recordingState }: { recordingState: RecordingState }) => (
    <div className="flex items-center justify-center gap-1 h-12">
      {[...Array(40)].map((_, i) => (
        <div
          key={i}
          className={cn(
            "w-1 bg-(--text-primary-dark) rounded-full transition-all duration-300",
            recordingState === "recording" ? "animate-pulse" : "",
          )}
          style={{
            height: recordingState === "recording" ? `${(i % 8 + 2) * 10}%` : "20%",
            animationDelay: `${i * 0.05}s`,
          }}
        />
      ))}
    </div>
  );

const VoiceRecordingModalContent = ({
  isOpen,
  onClose,
  sessionData,
  sessionNoteId,
  onTranscriptionComplete,
}: VoiceRecordingModalProps) => {
  const [recordingState, setRecordingState] = useState<RecordingState>("idle");
  const [recordingError, setRecordingError] = useState<string | null>(null);
  const [recordingSeconds, setRecordingSeconds] = useState(0);
  const mountedRef = useRef(false);
  const startingRef = useRef(false);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const mediaStreamRef = useRef<MediaStream | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const [transcribeSessionNoteAudio, { isLoading: isTranscribing }] =
    useTranscribeSessionNoteAudioMutation();





  useEffect(() => {
    let interval: ReturnType<typeof setInterval> | null = null;
    if (recordingState === "recording") {
      interval = setInterval(() => {
        setRecordingSeconds((prev) => prev + 1);
      }, 1000);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [recordingState]);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      const recorder = mediaRecorderRef.current;
      if (recorder?.state === "recording") recorder.stop();
      mediaStreamRef.current?.getTracks().forEach(track => track.stop());
    };
  }, []);

  const handleStartRecording = async () => {
    if (startingRef.current || mediaRecorderRef.current?.state === "recording") return;
    startingRef.current = true;
    try {
      setRecordingError(null);
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      if (!mountedRef.current) {
        stream.getTracks().forEach(track => track.stop());
        return;
      }
      mediaStreamRef.current = stream;
      const mediaRecorder = new MediaRecorder(stream);
      audioChunksRef.current = [];
      mediaRecorderRef.current = mediaRecorder;
      mediaStreamRef.current = stream;
      mediaRecorder.ondataavailable = (event: BlobEvent) => {
        if (event.data.size > 0) audioChunksRef.current.push(event.data);
      };
      mediaRecorder.start();
      setRecordingSeconds(0);
      setRecordingState("recording");
    } catch (error) {
      mediaStreamRef.current?.getTracks().forEach(track => track.stop());
      if (mountedRef.current) {
        setRecordingError(getApiErrorMessage(error));
        setRecordingState("idle");
      }
    } finally {
      startingRef.current = false;
    }
  };

  const handleStopRecording = async () => {
    if (!mediaRecorderRef.current) return;
    setRecordingState("transcribing");

    const audioBlob = await new Promise<Blob>((resolve) => {
      const recorder = mediaRecorderRef.current!;
      recorder.onstop = () => {
        const blob = new Blob(audioChunksRef.current, {
          type: recorder.mimeType || "audio/webm",
        });
        resolve(blob);
      };
      recorder.stop();
      mediaStreamRef.current?.getTracks().forEach((track) => track.stop());
    });

    if (!mountedRef.current) return;
    try {
      const file = new File([audioBlob], "session-note-recording.webm", {
        type: audioBlob.type || "audio/webm",
      });
      const response = await transcribeSessionNoteAudio({
        audioFile: file,
        sessionNoteId,
      }).unwrap();
      if (mountedRef.current) onTranscriptionComplete(response);
    } catch (error) {
      setRecordingError(getApiErrorMessage(error));
      setRecordingState("idle");
    }
  };

  if (!isOpen) return null;



    return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-[10000] p-4 font-sans">
      <div className="bg-white rounded-2xl w-full max-w-lg overflow-hidden shadow-xl">
                {/* Header */}
                <div className="flex items-center justify-between p-6 border-b border-gray-100">
                    <h2 className="text-xl font-semibold text-(--text-primary-dark)">Voice Recording</h2>
                    <button
                        onClick={onClose}
                        className="p-1 hover:bg-gray-100 rounded-full transition-colors text-gray-500"
                    >
                        <X size={20} />
                    </button>
                </div>

                <div className="p-8 flex flex-col items-center">
                    <p className="text-gray-500 text-sm mb-6 text-center">
                        Record your session notes and AI will structure them automatically
                    </p>

                    {/* Session Info Box */}
                    <div className="mb-8 flex w-full flex-col gap-2 rounded-xl bg-[#f0f4ff] p-4">
                        <div className="flex min-w-0 items-start justify-between gap-3 text-sm">
                            <span className="shrink-0 text-gray-500">Session:</span>
                            <span className="min-w-0 truncate text-right font-semibold text-(--text-primary-dark)">
                              {sessionData.dateTime}
                            </span>
                        </div>
                        <div className="flex min-w-0 items-start justify-between gap-3 text-sm">
                            <span className="shrink-0 text-gray-500">Client:</span>
                            <span
                              className="min-w-0 truncate text-right font-semibold text-(--text-primary-dark)"
                              title={sessionData.clientName}
                            >
                              {sessionData.clientName}
                            </span>
                        </div>
                    </div>

                    {/* State Content */}
                    {recordingState === "idle" && (
                        <div className="flex flex-col items-center animate-in fade-in zoom-in duration-300">
                            <button
                                onClick={handleStartRecording}
                                className="w-20 h-20 bg-(--bg-primary-dark) rounded-full flex items-center justify-center mb-6 hover:bg-[#1f2937] transition-all shadow-lg hover:scale-105 active:scale-95 group"
                            >
                                <Mic size={32} className="text-white group-hover:scale-110 transition-transform" />
                            </button>
                            <p className="text-gray-500 text-xs text-center max-w-xs leading-relaxed">
                                Click on the mic and speak naturally. AI will organize your notes into the appropriate fields.
                            </p>
                        </div>
                    )}

                    {recordingState === "recording" && (
                        <div className="w-full flex flex-col items-center animate-in fade-in duration-300">
                            <div className="w-full h-16 flex items-center justify-center mb-8 overflow-hidden">
                                <WaveformBars recordingState={recordingState} />
                            </div>
                            <p className="mb-4 text-sm text-(--text-neutral-600)">
                              Recording: {Math.floor(recordingSeconds / 60).toString().padStart(2, "0")}:
                              {(recordingSeconds % 60).toString().padStart(2, "0")}
                            </p>

                            <div className="flex items-center gap-6">
                                <button
                                    onClick={() => {
                                      if (mediaRecorderRef.current?.state === "recording") mediaRecorderRef.current.stop();
                                      mediaStreamRef.current?.getTracks().forEach(track => track.stop());
                                      audioChunksRef.current = [];
                                      setRecordingState("idle");
                                    }}
                                    className="p-3 rounded-full hover:bg-gray-100 transition-colors text-gray-500"
                                >
                                    <X size={24} />
                                </button>
                                <button
                                    onClick={handleStopRecording}
                                    className="p-4 bg-(--bg-primary-dark) rounded-full hover:bg-(--bg-primary-dark)/90 transition-all shadow-lg text-white hover:scale-105 active:scale-95"
                                >
                                    <Check size={28} />
                                </button>
                            </div>
                        </div>
                    )}

                    {recordingState === "transcribing" && (
                        <div className="w-full flex flex-col items-center animate-in fade-in duration-300">
                            <div className="w-full h-16 flex items-center justify-center mb-6 opacity-50">
                                <WaveformBars recordingState={recordingState} />
                            </div>
                            <div className="flex items-center gap-3 text-gray-600">
                                <span className="text-sm font-medium">
                                  {isTranscribing ? "Transcribing audio" : "Finishing..."}
                                </span>
                                <ContentLoader variant="inline" size="md" />
                            </div>
                        </div>
                    )}
                    {recordingError ? (
                      <p className="mt-6 w-full rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                        {recordingError}
                      </p>
                    ) : null}
                </div>
            </div>
        </div>
    );
};

const VoiceRecordingModal = (props: VoiceRecordingModalProps) => props.isOpen ? <VoiceRecordingModalContent key={props.sessionNoteId} {...props} /> : null;

export default VoiceRecordingModal;
