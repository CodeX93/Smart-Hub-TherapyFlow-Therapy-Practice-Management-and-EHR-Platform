import { useMemo, useState } from "react";
import { AlertCircle, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import CustomTextarea from "@/components/form/CustomTextarea";
import { cn } from "@/lib/utils";
import type { BulkLibraryEntriesImportResponse } from "@/store/api/admin/libraryEntries.api";
import {
  buildBulkImportPayload,
  buildInitialColumnMapping,
  buildParsedRows,
  getBulkColumnRoleOptions,
  getBulkImportRequirements,
  getImportReadiness,
  parsePastedRows,
  summarizeColumnMapping,
  validateBulkImportPayload,
  type BulkColumnRole,
  type BulkImportRequirements,
  type ParsedBulkRow,
} from "@/utils/libraryBulkImport";

interface BulkAddModalProps {
  isOpen: boolean;
  onClose: () => void;
  categoryId?: number;
  categoryName?: string;
  onImport: (payload: {
    categoryId: number;
    entries: Array<{
      title: string;
      content: string;
      domain?: string;
      subdomain?: string;
    }>;
  }) => Promise<BulkLibraryEntriesImportResponse>;
  isSubmitting?: boolean;
}

const BulkAddModalContent = ({
  isOpen,
  onClose,
  categoryId,
  categoryName,
  onImport,
  isSubmitting = false,
}: BulkAddModalProps) => {
  const [step, setStep] = useState(1);
  const [pastedData, setPastedData] = useState("");
  const [firstRowIsHeader, setFirstRowIsHeader] = useState(true);
  const [columnMapping, setColumnMapping] = useState<BulkColumnRole[]>([]);
  const [importResult, setImportResult] = useState<BulkLibraryEntriesImportResponse | null>(
    null,
  );
  const [importError, setImportError] = useState<string | null>(null);

  const columnRoleOptions = getBulkColumnRoleOptions();
  const allRows = useMemo(() => parsePastedRows(pastedData), [pastedData]);
  const headerCells = useMemo(() => allRows[0] ?? [], [allRows]);
  const columnCount = useMemo(() => {
    if (!allRows.length) return 0;
    return Math.max(...allRows.map((row) => row.length));
  }, [allRows]);

  const columnLabels = useMemo(() => {
    if (!columnCount) return [];
    if (firstRowIsHeader && headerCells.length > 0) {
      return Array.from({ length: columnCount }, (_, index) => {
        const header = headerCells[index]?.trim();
        return header ? `Column ${index + 1}: ${header}` : `Column ${index + 1}`;
      });
    }
    return Array.from({ length: columnCount }, (_, index) => `Column ${index + 1}`);
  }, [columnCount, firstRowIsHeader, headerCells]);



  const mappingKey = JSON.stringify([pastedData, firstRowIsHeader]);
  const [previousMappingKey, setPreviousMappingKey] = useState(mappingKey);
  if (previousMappingKey !== mappingKey) {
    setPreviousMappingKey(mappingKey);
    setColumnMapping(buildInitialColumnMapping(columnCount, firstRowIsHeader, headerCells));
  }

  const detectedMappings = useMemo(
    () => summarizeColumnMapping(columnMapping, headerCells, firstRowIsHeader),
    [columnMapping, firstRowIsHeader, headerCells],
  );

  const parsedRows = useMemo(
    () => buildParsedRows(allRows, columnMapping, firstRowIsHeader),
    [allRows, columnMapping, firstRowIsHeader],
  );

  const validRows = useMemo(
    () => parsedRows.filter((row) => row.isValid),
    [parsedRows],
  );

  const importReadiness = useMemo(
    () => getImportReadiness(columnMapping, parsedRows, categoryId),
    [columnMapping, parsedRows, categoryId],
  );

  const importRequirements = useMemo(
    () => getBulkImportRequirements(categoryName),
    [categoryName],
  );

  const hasDataRows = firstRowIsHeader ? allRows.length > 1 : allRows.length > 0;
  const pasteValidationMessage = !pastedData.trim()
    ? "Paste at least one row of data to continue."
    : !hasDataRows
      ? firstRowIsHeader
        ? "Only a header row was detected. Add at least one data row below the header."
        : "No data rows found in the pasted content."
      : null;

  const hasConnectionColumns =
    columnMapping.includes("domain") || columnMapping.includes("subdomain");

  const canProceedFromPaste = hasDataRows && !!categoryId;
  const canProceedFromMapping = importReadiness.ok;

  const handleClose = () => {
    if (isSubmitting) return;
    onClose();
  };

  const handleImport = async () => {
    setImportError(null);

    const payloadCheck = validateBulkImportPayload(categoryId, columnMapping, validRows);
    if (!payloadCheck.ok) {
      setImportError(payloadCheck.message ?? "Import validation failed.");
      return;
    }

    if (!categoryId) {
      setImportError("Select a category tab before importing.");
      return;
    }

    try {
      const payload = buildBulkImportPayload(categoryId, validRows);
      const result = await onImport(payload);
      setImportResult({
        ...result,
        errors: result.errors.map((entry) => ({
          ...entry,
          row: validRows[entry.row - 1]?.rowNumber ?? entry.row,
        })),
      });
      setStep(4);
    } catch {
      // API errors are shown via toast in the parent handler.
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-60 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
      <div className="flex max-h-[90vh] w-full max-w-2xl flex-col overflow-hidden rounded-2xl bg-white shadow-xl">
        <div className="flex items-start justify-between gap-3 border-b border-(--neutral-100) px-5 py-4">
          <div className="min-w-0">
            <h2 className="text-lg font-bold text-(--neutral-950)">Bulk Add</h2>
            <p className="mt-0.5 truncate text-xs text-(--text-neutral-600)">
              {categoryName ? `Filing to ${categoryName}` : "Select a tab first"}
              {" · "}Title + Content required
            </p>
          </div>
          <button
            type="button"
            onClick={handleClose}
            disabled={isSubmitting}
            className="rounded-full p-1 text-(--text-neutral-600) transition-colors hover:bg-(--neutral-50) hover:text-(--neutral-950) cursor-pointer"
          >
            <X size={24} />
          </button>
        </div>

        <div className="flex items-center gap-1.5 px-5 pt-3">
          {[1, 2, 3].map((stepIndex) => (
            <div
              key={stepIndex}
              className={cn(
                "flex h-6 w-6 items-center justify-center rounded-full text-[0.6875rem] font-semibold",
                step >= stepIndex
                  ? "bg-(--bg-primary-dark) text-white"
                  : "bg-(--neutral-100) text-(--text-neutral-600)",
              )}
            >
              {stepIndex}
            </div>
          ))}
        </div>

        <div className="flex-1 overflow-y-auto px-5 py-4 custom-scrollbar">
          {step === 1 ? (
            <div className="space-y-3">
              {!categoryId ? (
                <ValidationAlert message="Select a category tab before importing." />
              ) : null}
              <CustomTextarea
                label="Paste data"
                value={pastedData}
                onChange={(event) => setPastedData(event.target.value)}
                placeholder={"Composite\tContent\nANXS10\tClient reports excessive worry..."}
                className="min-h-0"
                textareaClassName="field-sizing-content min-h-20 max-h-52 overflow-y-auto text-sm"
                required
              />
              {pasteValidationMessage ? (
                <ValidationAlert message={pasteValidationMessage} />
              ) : null}
              <div className="flex items-center gap-2">
                <Checkbox
                  id="first-row-header"
                  checked={firstRowIsHeader}
                  onChange={(event) => setFirstRowIsHeader(event.target.checked)}
                />
                <label
                  htmlFor="first-row-header"
                  className="cursor-pointer text-sm text-(--neutral-950)"
                >
                  First row is header
                </label>
              </div>
              <ImportRequirementsPanel requirements={importRequirements} />
            </div>
          ) : null}

          {step === 2 ? (
            <div className="space-y-3">
              {detectedMappings.length > 0 ? (
                <div className="flex flex-wrap gap-1.5">
                  {detectedMappings.map((mapping) => (
                    <span
                      key={`${mapping.header}-${mapping.role}`}
                      className="rounded-full border border-(--neutral-200) bg-(--neutral-50) px-2 py-0.5 text-[0.6875rem] text-(--neutral-950)"
                    >
                      {mapping.header} → {mapping.roleLabel}
                    </span>
                  ))}
                </div>
              ) : null}
              <div className="space-y-2">
                {columnLabels.map((label, index) => (
                  <div
                    key={`${label}-${index}`}
                    className="grid grid-cols-1 gap-1.5 sm:grid-cols-[1fr_11.25rem] sm:items-center"
                  >
                    <span className="truncate text-sm text-(--neutral-950)">{label}</span>
                    <select
                      value={columnMapping[index] ?? "skip"}
                      onChange={(event) => {
                        const nextValue = event.target.value as BulkColumnRole;
                        setColumnMapping((previous) => {
                          const next = [...previous];
                          next[index] = nextValue;
                          return next;
                        });
                      }}
                      className="h-10 rounded-lg border border-(--neutral-100) bg-white px-2.5 text-sm text-(--neutral-950)"
                    >
                      {columnRoleOptions.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </div>
                ))}
              </div>
              {!importReadiness.ok ? (
                <ValidationAlert message={importReadiness.message ?? "Fix column mapping."} />
              ) : (
                <p className="text-xs text-(--text-neutral-600)">
                  {importReadiness.validCount} valid
                  {importReadiness.invalidCount > 0
                    ? ` · ${importReadiness.invalidCount} invalid`
                    : ""}
                </p>
              )}
            </div>
          ) : null}

          {step === 3 ? (
            <div className="space-y-3">
              <div className="flex items-center justify-between gap-2 text-xs">
                <span className="font-medium text-(--neutral-950)">
                  Preview · {parsedRows.length} rows
                </span>
                <div className="flex items-center gap-2">
                  <span className="text-(--border-success)">{importReadiness.validCount} valid</span>
                  {importReadiness.invalidCount > 0 ? (
                    <span className="text-(--status-denied)">
                      {importReadiness.invalidCount} invalid
                    </span>
                  ) : null}
                </div>
              </div>
              {!importReadiness.ok ? (
                <ValidationAlert message={importReadiness.message ?? "Fix invalid rows."} />
              ) : null}
              {importError ? <ValidationAlert message={importError} /> : null}
              <div className="overflow-x-auto rounded-xl border border-(--neutral-100)">
                <table className="w-max min-w-full text-left text-sm">
                  <thead className="bg-(--bg-primary-50) text-xs text-(--text-neutral-600)">
                    <tr>
                      <th className="px-3 py-2 whitespace-nowrap">Row</th>
                      {hasConnectionColumns ? (
                        <>
                          <th className="px-3 py-2 whitespace-nowrap">Domain (connection)</th>
                          <th className="px-3 py-2 whitespace-nowrap">Subdomain (connection)</th>
                        </>
                      ) : null}
                      <th className="px-3 py-2 whitespace-nowrap">Title</th>
                      <th className="px-3 py-2 whitespace-nowrap">Content</th>
                      <th className="px-3 py-2 whitespace-nowrap">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {parsedRows.map((row) => (
                      <PreviewRow
                        key={row.rowNumber}
                        row={row}
                        showConnectionColumns={hasConnectionColumns}
                      />
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ) : null}

          {step === 4 && importResult ? (
            <div className="space-y-3">
              <h3 className="text-base font-semibold text-(--neutral-950)">Results</h3>
              <div className="grid grid-cols-3 gap-2">
                <ResultCard label="Total" value={importResult.total} />
                <ResultCard label="Successful" value={importResult.successful} tone="success" />
                <ResultCard label="Skipped" value={importResult.skipped} />
                <ResultCard label="Failed" value={importResult.failed} tone="danger" />
                <ResultCard label="Categories" value={importResult.categoriesCreated} />
                <ResultCard label="Connections" value={importResult.connectionsCreated} />
              </div>
              {importResult.errors.length > 0 ? (
                <div className="rounded-lg border border-red-100 bg-red-50 p-3">
                  <p className="mb-1.5 text-xs font-semibold text-red-700">
                    Errors ({importResult.errors.length})
                  </p>
                  <div className="max-h-36 space-y-1 overflow-y-auto custom-scrollbar">
                    {importResult.errors.map((entry, index) => (
                      <p key={`${entry.row}-${index}`} className="text-xs text-red-700">
                        Row {entry.row}
                        {entry.title ? ` (${entry.title})` : ""}: {entry.error}
                      </p>
                    ))}
                  </div>
                </div>
              ) : null}
              {importResult.successful === 0 && importResult.skipped === 0 && importResult.failed === 0 ? (
                <ValidationAlert message="No entries were imported. Review the pasted data and try again." />
              ) : null}
            </div>
          ) : null}
        </div>

        <div className="flex items-center justify-between gap-2 border-t border-(--neutral-100) px-5 py-4">
          <div>
            {step > 1 && step < 4 ? (
              <Button
                type="button"
                variant="outline"
                onClick={() => setStep((previous) => previous - 1)}
                disabled={isSubmitting}
                className="h-10 rounded-full px-5"
              >
                Back
              </Button>
            ) : null}
          </div>
          <div className="flex items-center gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={handleClose}
              disabled={isSubmitting}
              className="h-10 rounded-full px-5"
            >
              {step === 4 ? "Close" : "Cancel"}
            </Button>
            {step === 1 ? (
              <Button
                type="button"
                onClick={() => setStep(2)}
                disabled={!canProceedFromPaste}
                className="h-10 rounded-full px-5"
              >
                Next
              </Button>
            ) : null}
            {step === 2 ? (
              <Button
                type="button"
                onClick={() => setStep(3)}
                disabled={!canProceedFromMapping}
                className="h-10 rounded-full px-5"
              >
                Preview
              </Button>
            ) : null}
            {step === 3 ? (
              <Button
                type="button"
                onClick={() => void handleImport()}
                disabled={!canProceedFromMapping || isSubmitting}
                className="h-10 rounded-full px-5"
                loading={isSubmitting}
                loadingLabel="Importing..."
              >
                {`Import ${importReadiness.validCount} Entr${importReadiness.validCount === 1 ? "y" : "ies"}`}
              </Button>
            ) : null}
          </div>
        </div>
      </div>
    </div>
  );
};

function ValidationAlert({ message }: { message: string }) {
  return (
    <div className="flex items-start gap-2 rounded-lg border border-red-100 bg-red-50 px-3 py-2 text-xs text-red-700">
      <AlertCircle size={14} className="mt-0.5 shrink-0" />
      <p>{message}</p>
    </div>
  );
}

function ImportRequirementsPanel({
  requirements,
}: {
  requirements: BulkImportRequirements;
}) {
  return (
    <div className="rounded-lg border border-(--neutral-100) bg-(--neutral-50) px-3 py-2.5 text-xs text-(--text-neutral-600)">
      <p className="font-semibold text-(--neutral-950)">{requirements.modeLabel}</p>
      <ul className="mt-1.5 list-disc space-y-1 pl-4">
        <li>
          <span className="font-medium text-(--neutral-950)">Required:</span>{" "}
          {requirements.requiredColumns.join(", ")}
        </li>
        <li>
          <span className="font-medium text-(--neutral-950)">Optional:</span>{" "}
          {requirements.optionalColumns.join(", ")}
        </li>
        {requirements.rules.map((rule) => (
          <li key={rule}>{rule}</li>
        ))}
      </ul>
    </div>
  );
}

function PreviewRow({
  row,
  showConnectionColumns,
}: {
  row: ParsedBulkRow;
  showConnectionColumns: boolean;
}) {
  return (
    <tr
      className={cn(
        "border-t border-(--neutral-100)",
        !row.isValid && "bg-red-50/60",
      )}
    >
      <td className="px-3 py-2 whitespace-nowrap text-(--text-neutral-600)">{row.rowNumber}</td>
      {showConnectionColumns ? (
        <>
          <td className="px-3 py-2 whitespace-nowrap">{row.domain || "-"}</td>
          <td className="px-3 py-2 whitespace-nowrap">{row.subdomain || "-"}</td>
        </>
      ) : null}
      <td className="px-3 py-2 whitespace-nowrap">{row.title || "-"}</td>
      <td className="max-w-xs px-3 py-2 whitespace-nowrap truncate" title={row.content}>
        {row.content || "-"}
      </td>
      <td className="px-3 py-2 whitespace-nowrap">
        <span
          className={cn(
            "text-xs font-medium",
            row.isValid ? "text-(--border-success)" : "text-(--status-denied)",
          )}
        >
          {row.isValid ? "Valid" : "Invalid"}
        </span>
        {!row.isValid && row.validationError ? (
          <p className="mt-0.5 max-w-xs text-xs text-(--status-denied)">{row.validationError}</p>
        ) : null}
      </td>
    </tr>
  );
}

function ResultCard({
  label,
  value,
  tone = "default",
}: {
  label: string;
  value: number;
  tone?: "default" | "success" | "danger";
}) {
  return (
    <div
      className={cn(
        "rounded-lg border p-3",
        tone === "success" && "border-emerald-100 bg-emerald-50",
        tone === "danger" && "border-red-100 bg-red-50",
        tone === "default" && "border-(--neutral-100)",
      )}
    >
      <p
        className={cn(
          "text-xs",
          tone === "success" && "text-emerald-700",
          tone === "danger" && "text-red-700",
          tone === "default" && "text-(--text-neutral-600)",
        )}
      >
        {label}
      </p>
      <p
        className={cn(
          "text-xl font-semibold",
          tone === "success" && "text-emerald-700",
          tone === "danger" && "text-red-700",
          tone === "default" && "text-(--text-primary-dark)",
        )}
      >
        {value}
      </p>
    </div>
  );
}

const BulkAddModal = (props: BulkAddModalProps) => props.isOpen ? <BulkAddModalContent key={props.categoryId} {...props} /> : null;

export default BulkAddModal;
