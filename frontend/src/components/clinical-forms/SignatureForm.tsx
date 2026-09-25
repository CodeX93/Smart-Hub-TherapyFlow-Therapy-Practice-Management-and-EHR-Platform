import { CalendarIcon } from "@/components/icons/commonIcons";
import {
  useState,
  useRef,
  useEffect,
  useLayoutEffect,
  useCallback,
  forwardRef,
  useImperativeHandle,
} from "react";
import { Type, Pencil } from "lucide-react";
import {
  FormControl,
  FormField,
  FormItem,
  FormMessage,
} from "../../components/ui/form";
import { Button } from "../../components/ui/button";
import type {
  Control,
  FieldValues,
  Path,
  UseFormReturn,
} from "react-hook-form";
import CustomDatePicker from "../form/CustomDatePicker";
import CustomInput from "../form/CustomInput";

interface SignatureFormProps<T extends FieldValues = FieldValues> {
  control: Control<T>;
  form: UseFormReturn<T>;
  onSubmit: (data: T) => void;
  isReadOnly?: boolean;
  isSubmitting?: boolean;
  signedAt?: Date;
}

export type SignaturePadHandle = {
  clear: () => void;
  toDataURL: () => string;
  isEmpty: () => boolean;
};

const SignaturePad = forwardRef<
  SignaturePadHandle,
  {
    onChange: (val: string) => void;
    value: string;
  }
>(function SignaturePad({ onChange, value }, ref) {
  const containerRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const isDrawingRef = useRef(false);
  const hasStrokeRef = useRef(false);
  const lastEmittedRef = useRef<string>("");
  const valueRef = useRef(value);
  useLayoutEffect(() => {
    valueRef.current = value;
  }, [value]);

  const getContext = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return null;
    const ctx = canvas.getContext("2d");
    if (!ctx) return null;
    return { canvas, ctx };
  }, []);

  const configureContext = useCallback((ctx: CanvasRenderingContext2D) => {
    ctx.strokeStyle = "#000000";
    ctx.lineWidth = 2;
    ctx.lineCap = "round";
    ctx.lineJoin = "round";
  }, []);

  const emitCanvas = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    if (!hasStrokeRef.current) {
      lastEmittedRef.current = "";
      onChange("");
      return;
    }
    const dataUrl = canvas.toDataURL("image/png");
    lastEmittedRef.current = dataUrl;
    onChange(dataUrl);
  }, [onChange]);

  const clearCanvas = useCallback(() => {
    const pair = getContext();
    if (!pair) return;
    const { canvas, ctx } = pair;
    ctx.save();
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.restore();
    configureContext(ctx);
    hasStrokeRef.current = false;
    lastEmittedRef.current = "";
    onChange("");
  }, [configureContext, getContext, onChange]);

  const drawImageValue = useCallback(
    (dataUrl: string) => {
      const pair = getContext();
      if (!pair) return;
      const { canvas, ctx } = pair;
      const img = new Image();
      img.onload = () => {
        ctx.save();
        ctx.setTransform(1, 0, 0, 1, 0, 0);
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        ctx.restore();
        configureContext(ctx);
        // Draw in CSS pixel space (context is already scaled by DPR)
        const width = canvas.width / (window.devicePixelRatio || 1);
        const height = canvas.height / (window.devicePixelRatio || 1);
        ctx.drawImage(img, 0, 0, width, height);
        hasStrokeRef.current = true;
        lastEmittedRef.current = dataUrl;
      };
      img.src = dataUrl;
    },
    [configureContext, getContext],
  );

  const syncCanvasSize = useCallback(() => {
    const canvas = canvasRef.current;
    const container = containerRef.current;
    if (!canvas || !container) return;

    const rect = container.getBoundingClientRect();
    const width = Math.max(1, Math.floor(rect.width));
    const height = Math.max(1, Math.floor(rect.height));
    const dpr = window.devicePixelRatio || 1;

    const prevData =
      hasStrokeRef.current && canvas.width > 0
        ? canvas.toDataURL("image/png")
        : "";

    canvas.width = Math.floor(width * dpr);
    canvas.height = Math.floor(height * dpr);
    canvas.style.width = `${width}px`;
    canvas.style.height = `${height}px`;

    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.scale(dpr, dpr);
    configureContext(ctx);

    if (prevData) {
      drawImageValue(prevData);
    } else if (
      valueRef.current &&
      valueRef.current.startsWith("data:image")
    ) {
      drawImageValue(valueRef.current);
    }
  }, [configureContext, drawImageValue]);

  useImperativeHandle(
    ref,
    () => ({
      clear: clearCanvas,
      toDataURL: () => {
        const canvas = canvasRef.current;
        if (!canvas || !hasStrokeRef.current) return "";
        return canvas.toDataURL("image/png");
      },
      isEmpty: () => !hasStrokeRef.current,
    }),
    [clearCanvas],
  );

  useEffect(() => {
    syncCanvasSize();

    const container = containerRef.current;
    if (!container || typeof ResizeObserver === "undefined") return;

    const observer = new ResizeObserver(() => {
      syncCanvasSize();
    });
    observer.observe(container);
    return () => observer.disconnect();
  }, [syncCanvasSize]);

  // Restore an externally set value (saved signature / clear), skip our own emits
  useEffect(() => {
    if (value === lastEmittedRef.current) return;

    if (!value) {
      const pair = getContext();
      if (!pair) return;
      const { canvas, ctx } = pair;
      ctx.save();
      ctx.setTransform(1, 0, 0, 1, 0, 0);
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      ctx.restore();
      configureContext(ctx);
      hasStrokeRef.current = false;
      lastEmittedRef.current = "";
      return;
    }

    if (value.startsWith("data:image")) {
      drawImageValue(value);
    }
  }, [value, configureContext, drawImageValue, getContext]);

  const getPoint = (e: React.MouseEvent | React.TouchEvent) => {
    const canvas = canvasRef.current;
    if (!canvas) return null;
    const rect = canvas.getBoundingClientRect();
    const clientX = "touches" in e ? e.touches[0]?.clientX : e.clientX;
    const clientY = "touches" in e ? e.touches[0]?.clientY : e.clientY;
    if (clientX == null || clientY == null) return null;
    return {
      x: clientX - rect.left,
      y: clientY - rect.top,
    };
  };

  const startDrawing = (e: React.MouseEvent | React.TouchEvent) => {
    const pair = getContext();
    const point = getPoint(e);
    if (!pair || !point) return;
    e.preventDefault();
    configureContext(pair.ctx);
    pair.ctx.beginPath();
    pair.ctx.moveTo(point.x, point.y);
    isDrawingRef.current = true;
  };

  const draw = (e: React.MouseEvent | React.TouchEvent) => {
    if (!isDrawingRef.current) return;
    const pair = getContext();
    const point = getPoint(e);
    if (!pair || !point) return;
    e.preventDefault();
    pair.ctx.lineTo(point.x, point.y);
    pair.ctx.stroke();
    hasStrokeRef.current = true;
  };

  const stopDrawing = () => {
    if (!isDrawingRef.current) return;
    isDrawingRef.current = false;
    emitCanvas();
  };

  return (
    <div ref={containerRef} className="h-full w-full">
      <canvas
        ref={canvasRef}
        className="h-full w-full cursor-crosshair touch-none"
        onMouseDown={startDrawing}
        onMouseMove={draw}
        onMouseUp={stopDrawing}
        onMouseLeave={stopDrawing}
        onTouchStart={startDrawing}
        onTouchMove={draw}
        onTouchEnd={stopDrawing}
        onTouchCancel={stopDrawing}
      />
    </div>
  );
});

const SignatureForm = <T extends FieldValues = FieldValues>({
  control,
  form,
  onSubmit,
  isReadOnly = false,
  isSubmitting = false,
  signedAt = new Date(),
}: SignatureFormProps<T>) => {
  const [signatureMethod, setSignatureMethod] = useState<"type" | "draw">(
    "draw",
  );
  const signaturePadRef = useRef<SignaturePadHandle>(null);

  const signatureValue = form.watch("signature" as Path<T>);
  const clientName = form.watch("clientName" as Path<T>);

  useEffect(() => {
    if (isReadOnly) {
      setSignatureMethod(
        typeof signatureValue === "string" &&
          signatureValue.startsWith("data:image")
          ? "draw"
          : "type",
      );
    }
  }, [isReadOnly, signatureValue]);

  const clearSignature = () => {
    form.setValue("signature" as Path<T>, "" as T[Path<T>], {
      shouldDirty: true,
      shouldValidate: true,
    });
    signaturePadRef.current?.clear();
  };

  const handleSubmitClick = form.handleSubmit((data) => {
    if (signatureMethod === "draw" && !isReadOnly) {
      const drawn = signaturePadRef.current?.toDataURL() ?? "";
      if (!drawn || signaturePadRef.current?.isEmpty()) {
        form.setValue("signature" as Path<T>, "" as T[Path<T>], {
          shouldDirty: true,
          shouldValidate: true,
        });
        onSubmit({ ...data, signature: "" } as T);
        return;
      }
      form.setValue("signature" as Path<T>, drawn as T[Path<T>], {
        shouldDirty: true,
        shouldValidate: true,
      });
      onSubmit({ ...data, signature: drawn } as T);
      return;
    }

    onSubmit(data);
  });

  const isDrawnImage =
    typeof signatureValue === "string" &&
    signatureValue.startsWith("data:image");

  return (
    <div>
      {/* Input Fields */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
        {/* Client Full Name */}
        <CustomInput
          control={control}
          name={"clientName" as Path<T>}
          label="Client full name"
          required
          disabled={true}
        />

        {/* Date */}
        <FormField
          control={control}
          name={"date" as Path<T>}
          render={({ field }) => (
            <FormItem>
              <FormControl>
                <CustomDatePicker
                  label="Date"
                  date={(() => {
                    const d = field.value
                      ? typeof field.value === "string"
                        ? new Date(field.value)
                        : field.value
                      : null;
                    return d instanceof Date && !isNaN(d.getTime()) ? d : null;
                  })()}
                  onDateChange={field.onChange}
                  disabled={isReadOnly}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </div>

      {/* Signature Section */}
      <div className="space-y-4">
        <h3 className="text-lg font-semibold text-(--text-primary-dark)">
          Electronic Signature
        </h3>
        <p className="text-sm text-(--text-neutral-600)">
          Draw your signature below. It will be saved as a PNG image when you
          submit the form.
        </p>

        {/* Signature Method Tabs */}
        <div className="flex gap-1 p-1 mb-4 border border-gray-200 rounded-full bg-gray-50">
          <button
            type="button"
            disabled={isReadOnly}
            onClick={() => {
              setSignatureMethod("type");
              clearSignature();
            }}
            className={`flex-1 flex items-center justify-center gap-2 py-3 px-4 transition-all ${
              isReadOnly ? "cursor-default" : "cursor-pointer"
            } ${
              signatureMethod === "type"
                ? "bg-white text-gray-900 shadow-sm rounded-full"
                : "bg-transparent text-gray-600 hover:text-gray-900"
            }`}
          >
            <Type size={20} />
            <span className="text-sm font-medium">Type Name</span>
          </button>
          <button
            type="button"
            disabled={isReadOnly}
            onClick={() => {
              setSignatureMethod("draw");
              clearSignature();
            }}
            className={`flex-1 flex items-center justify-center gap-2 py-3 px-4 transition-all ${
              isReadOnly ? "cursor-default" : "cursor-pointer"
            } ${
              signatureMethod === "draw"
                ? "bg-white text-gray-900 shadow-sm rounded-full"
                : "bg-transparent text-gray-600 hover:text-gray-900"
            }`}
          >
            <Pencil size={20} />
            <span className="text-sm font-medium">Draw Signature</span>
          </button>
        </div>

        {/* Signature Input / Preview Box */}
        <div className="">
          {isReadOnly ? (
            <div className="border border-(--neutral-100) rounded-xl h-48 flex items-center justify-center bg-white overflow-hidden relative">
              {isDrawnImage ? (
                <img
                  src={signatureValue}
                  alt="Signature"
                  className="max-h-32 max-w-full object-contain"
                />
              ) : signatureValue ? (
                <p className="text-3xl font-script text-black italic">
                  {String(signatureValue)}
                </p>
              ) : null}
            </div>
          ) : signatureMethod === "type" ? (
            <div className="w-full">
              <CustomInput
                control={control}
                name={"signature" as Path<T>}
                label="Type your full name"
                disabled={isReadOnly || isSubmitting}
                hint="Your typed name will be used as your electronic signature"
                maxLength={50}
              />
            </div>
          ) : (
            <div className="border border-(--neutral-100) rounded-xl h-48 flex items-center justify-center bg-white overflow-hidden relative">
              <FormField
                control={control}
                name={"signature" as Path<T>}
                render={({ field }) => (
                  <FormItem className="h-full w-full space-y-0">
                    <SignaturePad
                      ref={signaturePadRef}
                      onChange={field.onChange}
                      value={
                        typeof field.value === "string" ? field.value : ""
                      }
                    />
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
          )}
        </div>

        {!isReadOnly && signatureMethod === "draw" && (
          <div className="flex items-center justify-between mt-4">
            <p className="text-sm text-(--text-primary-dark)">
              Sign above using your mouse or touch screen
            </p>
            <Button
              variant="outline"
              type="button"
              onClick={clearSignature}
              className="px-8 h-11 rounded-full cursor-pointer"
            >
              Clear
            </Button>
          </div>
        )}

        {/* Signature Preview for Type Mode */}
        {signatureMethod === "type" && !isReadOnly && (
          <div className="border border-(--neutral-100) rounded-xl p-6 h-44 bg-white mt-4 flex flex-col justify-between">
            <div className="flex items-center justify-center flex-1">
              <p className="text-3xl font-script text-black italic">
                {typeof signatureValue === "string" &&
                !signatureValue.startsWith("data:image")
                  ? signatureValue
                  : ""}
              </p>
            </div>
          </div>
        )}

        {isReadOnly && (
          <div className="space-y-4 mt-4">
            <p className="text-center text-sm text-(--text-neutral-600)">
              Electronic signature (read-only)
            </p>
            <div className="flex items-center justify-between p-4 bg-gray-50 rounded-xl border border-(--neutral-100)">
              <div className="flex items-center gap-2">
                <span className="text-sm font-medium text-gray-900">
                  Signed by: {clientName}
                </span>
              </div>
              <div className="flex items-center gap-2 text-gray-500 text-sm">
                <CalendarIcon className="w-4 h-4 text-(--text-neutral-400)" />
                <span>
                  {signedAt.toLocaleDateString("en-GB")}{" "}
                  {signedAt.toLocaleTimeString([], {
                    hour: "2-digit",
                    minute: "2-digit",
                    second: "2-digit",
                    hour12: true,
                  })}
                </span>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Submit Button */}
      {!isReadOnly && (
        <div className="flex justify-end mt-6">
          <Button
            type="button"
            disabled={isSubmitting}
            loading={isSubmitting}
            loadingLabel="Submitting..."
            onClick={handleSubmitClick}
            className="px-6 h-11 rounded-full cursor-pointer disabled:cursor-not-allowed disabled:opacity-60"
          >
            Submit Form
          </Button>
        </div>
      )}
    </div>
  );
};

export default SignatureForm;
