export type BulkColumnRole = "domain" | "subdomain" | "title" | "content" | "skip";

export interface ImportReadiness {
  ok: boolean;
  message?: string;
  validCount: number;
  invalidCount: number;
}

export interface BulkImportRequirements {
  modeLabel: string;
  requiredColumns: string[];
  optionalColumns: string[];
  rules: string[];
}

export interface ParsedBulkRow {
  rowNumber: number;
  domain?: string;
  subdomain?: string;
  title: string;
  content: string;
  isValid: boolean;
  validationError?: string;
}

export interface BulkImportPayload {
  categoryId: number;
  entries: Array<{
    domain?: string;
    subdomain?: string;
    title: string;
    content: string;
  }>;
}

const COLUMN_ROLE_OPTIONS: { value: BulkColumnRole; label: string }[] = [
  { value: "domain", label: "Domain (connection)" },
  { value: "subdomain", label: "Subdomain (connection)" },
  { value: "title", label: "Composite (Title)" },
  { value: "content", label: "Content" },
  { value: "skip", label: "Skip" },
];

export function getBulkColumnRoleOptions() {
  return COLUMN_ROLE_OPTIONS;
}

export function detectDelimiter(firstLine: string): string {
  return firstLine.includes("\t") ? "\t" : ",";
}

function parseCsvRow(line: string): string[] {
  const cells: string[] = [];
  let current = "";
  let inQuotes = false;

  for (let index = 0; index < line.length; index += 1) {
    const char = line[index];
    const next = line[index + 1];

    if (char === '"') {
      if (inQuotes && next === '"') {
        current += '"';
        index += 1;
      } else {
        inQuotes = !inQuotes;
      }
      continue;
    }

    if (char === "," && !inQuotes) {
      cells.push(current.trim());
      current = "";
      continue;
    }

    current += char;
  }

  cells.push(current.trim());
  return cells;
}

export function parsePastedRows(pastedData: string): string[][] {
  const lines = pastedData
    .replace(/^\uFEFF/, "")
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
  if (lines.length === 0) return [];

  const separator = detectDelimiter(lines[0]);
  if (separator === "\t") {
    return lines.map((line) => line.split("\t").map((cell) => cell.trim()));
  }

  return lines.map((line) => parseCsvRow(line));
}

function normalizeHeader(header: string): string {
  return header
    .toLowerCase()
    .trim()
    .replace(/[_-]+/g, " ")
    .replace(/[^\w\s]/g, "")
    .replace(/\s+/g, " ");
}

export function autoMapHeaders(headers: string[]): BulkColumnRole[] {
  return headers.map((header) => {
    const normalized = normalizeHeader(header);
    if (!normalized) return "skip";

    if (
      normalized.includes("subdomain") ||
      normalized.includes("subcategory") ||
      normalized === "sub domain"
    ) {
      return "subdomain";
    }
    if (
      normalized === "domain" ||
      normalized === "category" ||
      normalized.startsWith("domain ") ||
      normalized.endsWith(" domain")
    ) {
      return "domain";
    }
    if (
      normalized.includes("composite") ||
      normalized === "title" ||
      normalized.includes("entry title") ||
      normalized === "name" ||
      normalized === "entry" ||
      normalized === "code" ||
      normalized === "label"
    ) {
      return "title";
    }
    if (
      normalized.includes("content") ||
      normalized.includes("description") ||
      normalized === "text" ||
      normalized.includes("definition") ||
      normalized === "body" ||
      normalized === "notes"
    ) {
      return "content";
    }
    return "skip";
  });
}

export function summarizeColumnMapping(
  columnMapping: BulkColumnRole[],
  headerCells: string[],
  firstRowIsHeader: boolean,
): Array<{ header: string; roleLabel: string; role: BulkColumnRole }> {
  return columnMapping
    .map((role, index) => {
      const header = firstRowIsHeader
        ? headerCells[index]?.trim() || `Column ${index + 1}`
        : `Column ${index + 1}`;
      const roleLabel =
        COLUMN_ROLE_OPTIONS.find((option) => option.value === role)?.label ?? role;
      return { header, roleLabel, role };
    })
    .filter((item) => item.role !== "skip");
}

export function buildInitialColumnMapping(
  columnCount: number,
  firstRowIsHeader: boolean,
  headerCells: string[],
): BulkColumnRole[] {
  if (firstRowIsHeader && headerCells.length > 0) {
    const mapped = autoMapHeaders(headerCells);
    return Array.from({ length: columnCount }, (_, index) => mapped[index] ?? "skip");
  }

  return Array.from({ length: columnCount }, (_, index) => {
    if (columnCount >= 4) {
      if (index === 0) return "domain";
      if (index === 1) return "subdomain";
      if (index === 2) return "title";
      if (index === 3) return "content";
    }
    if (columnCount === 3) {
      if (index === 0) return "title";
      if (index === 1) return "content";
      return "skip";
    }
    if (columnCount === 2) {
      return index === 0 ? "title" : "content";
    }
    return "skip";
  });
}

export function getBulkImportRequirements(categoryName?: string): BulkImportRequirements {
  return {
    modeLabel: "Active tab + optional connections",
    requiredColumns: ["Composite (Title)", "Content"],
    optionalColumns: ["Domain (connection)", "Subdomain (connection)"],
    rules: [
      categoryName
        ? `New entries are filed under the active tab: "${categoryName}".`
        : "Select a category tab before importing.",
      "Domain and Subdomain are optional connection targets — they do not change where the new entry is stored.",
      "When Domain is provided, the backend finds or creates that category and connects the new entry.",
      "When Subdomain is also provided, it resolves under Domain and connects via an entry named after the subdomain.",
      "Rows missing title or content are shown as invalid and are not sent.",
    ],
  };
}

function validateParsedRow(
  title: string,
  content: string,
  domain: string | undefined,
  subdomain: string | undefined,
): { isValid: boolean; validationError?: string } {
  const hasTitle = title.length > 0;
  const hasContent = content.length > 0;
  const hasDomain = Boolean(domain);
  const hasSubdomain = Boolean(subdomain);

  if (!hasTitle && !hasContent) {
    return { isValid: false, validationError: "Missing title and content" };
  }
  if (!hasTitle) {
    return { isValid: false, validationError: "Missing Composite (title)" };
  }
  if (!hasContent) {
    return { isValid: false, validationError: "Missing content" };
  }
  if (hasSubdomain && !hasDomain) {
    return { isValid: false, validationError: "Subdomain requires domain on this row" };
  }

  return { isValid: true };
}

export function buildParsedRows(
  allRows: string[][],
  columnMapping: BulkColumnRole[],
  firstRowIsHeader: boolean,
): ParsedBulkRow[] {
  const dataRows = firstRowIsHeader ? allRows.slice(1) : allRows;

  return dataRows.map((row, index) => {
    let domain: string | undefined;
    let subdomain: string | undefined;
    let title = "";
    let content = "";

    columnMapping.forEach((role, columnIndex) => {
      const cell = row[columnIndex]?.trim() ?? "";
      if (!cell) return;
      if (role === "domain") domain = cell;
      else if (role === "subdomain") subdomain = cell;
      else if (role === "title") title = cell;
      else if (role === "content") content = cell;
    });

    const validation = validateParsedRow(title, content, domain, subdomain);

    return {
      rowNumber: firstRowIsHeader ? index + 2 : index + 1,
      domain,
      subdomain,
      title,
      content,
      isValid: validation.isValid,
      validationError: validation.validationError,
    };
  });
}

export function hasColumnRole(
  columnMapping: BulkColumnRole[],
  role: BulkColumnRole,
): boolean {
  return columnMapping.includes(role);
}

export function canImportBulkRows(
  columnMapping: BulkColumnRole[],
  categoryId?: number | null,
): { ok: boolean; message?: string } {
  const hasTitle = hasColumnRole(columnMapping, "title");
  const hasContent = hasColumnRole(columnMapping, "content");
  const hasSubdomain = hasColumnRole(columnMapping, "subdomain");
  const hasDomain = hasColumnRole(columnMapping, "domain");

  if (!categoryId) {
    return { ok: false, message: "Select a category tab before importing." };
  }
  if (!hasTitle || !hasContent) {
    return {
      ok: false,
      message: "Map Composite (Title) and Content columns before importing.",
    };
  }
  if (hasSubdomain && !hasDomain) {
    return {
      ok: false,
      message: "Subdomain cannot be mapped without Domain.",
    };
  }
  return { ok: true };
}

export function getImportReadiness(
  columnMapping: BulkColumnRole[],
  parsedRows: ParsedBulkRow[],
  categoryId?: number | null,
): ImportReadiness {
  const mappingCheck = canImportBulkRows(columnMapping, categoryId);
  const validRows = parsedRows.filter((row) => row.isValid);
  const invalidCount = parsedRows.length - validRows.length;

  if (!mappingCheck.ok) {
    return {
      ok: false,
      message: mappingCheck.message,
      validCount: validRows.length,
      invalidCount,
    };
  }

  if (parsedRows.length === 0) {
    return {
      ok: false,
      message: "No data rows found. Paste at least one row of entry data below the header.",
      validCount: 0,
      invalidCount: 0,
    };
  }

  if (validRows.length === 0) {
    return {
      ok: false,
      message: "No valid rows to import. Each row needs Composite (title) and Content.",
      validCount: 0,
      invalidCount,
    };
  }

  return {
    ok: true,
    validCount: validRows.length,
    invalidCount,
  };
}

export function validateBulkImportPayload(
  categoryId: number | null | undefined,
  columnMapping: BulkColumnRole[],
  validRows: ParsedBulkRow[],
): { ok: boolean; message?: string } {
  if (!categoryId) {
    return { ok: false, message: "Select a category tab before importing." };
  }
  if (validRows.length === 0) {
    return { ok: false, message: "No valid rows to import." };
  }

  const mappingCheck = canImportBulkRows(columnMapping, categoryId);
  if (!mappingCheck.ok) {
    return { ok: false, message: mappingCheck.message };
  }

  const subdomainWithoutDomain = validRows.some(
    (row) => row.subdomain?.trim() && !row.domain?.trim(),
  );
  if (subdomainWithoutDomain) {
    return {
      ok: false,
      message: "One or more rows have Subdomain without Domain. Fix those rows before importing.",
    };
  }

  return { ok: true };
}

export function buildBulkImportPayload(
  categoryId: number,
  validRows: ParsedBulkRow[],
): BulkImportPayload {
  return {
    categoryId,
    entries: validRows.map((row) => ({
      title: row.title,
      content: row.content,
      ...(row.domain ? { domain: row.domain } : {}),
      ...(row.subdomain ? { subdomain: row.subdomain } : {}),
    })),
  };
}
