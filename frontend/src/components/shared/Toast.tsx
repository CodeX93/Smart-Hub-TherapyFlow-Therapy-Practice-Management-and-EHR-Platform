import { useEffect, useEffectEvent } from "react";
import { AlertCircle, CheckCircle, X } from "lucide-react";
import { cn } from "@/lib/utils";

interface ToastProps {
  message: string;
  type?: "success" | "error" | "info";
  onClose: () => void;
  duration?: number;
}

const Toast = ({
  message,
  type = "info",
  onClose,
  duration,
}: ToastProps) => {
  const resolvedDuration =
    duration ??
    (type === "error" && message.length > 120 ? 12000 : 3000);
  const close = useEffectEvent(onClose);

  useEffect(() => {
    const timer = setTimeout(() => {
      close();
    }, resolvedDuration);
    return () => clearTimeout(timer);
  }, [resolvedDuration, message]);

  const isError = type === "error";
  const isSuccess = type === "success";

  return (
    <div
      className={cn(
        "fixed right-6 top-6 z-[10050] flex items-center gap-3 rounded-xl border px-4 py-3 shadow-lg animate-in fade-in slide-in-from-top-4 duration-300",
        isError && "border-red-200 bg-red-50 text-red-700",
        isSuccess && "border-emerald-200 bg-emerald-50 text-emerald-700",
        !isError && !isSuccess && "border-blue-200 bg-blue-50 text-blue-700",
      )}
    >
      {isError && <AlertCircle size={18} />}
      {isSuccess && <CheckCircle size={18} />}
      {!isError && !isSuccess && (
        <AlertCircle size={18} className="text-blue-500" />
      )}

      <span className="max-w-md text-sm font-medium break-words">{message}</span>

      <button
        aria-label="Close notification"
        onClick={onClose}
        className="ml-2 hover:opacity-70 transition-opacity cursor-pointer"
      >
        <X size={16} />
      </button>
    </div>
  );
};

export default Toast;
