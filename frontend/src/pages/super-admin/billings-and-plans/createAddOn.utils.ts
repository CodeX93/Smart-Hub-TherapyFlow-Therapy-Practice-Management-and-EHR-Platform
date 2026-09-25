export const CREATE_ADD_ON_LIMITS = {
  codeMax: 30,
  nameMax: 80,
  descriptionMax: 500,
  priceMaxChars: 10,
  priceMax: 9_999_999.99,
} as const;

export function sanitizeAddOnCode(value: string): string {
  return value
    .replace(/[^A-Za-z0-9_-]/g, "")
    .slice(0, CREATE_ADD_ON_LIMITS.codeMax);
}

export function sanitizeAddOnName(value: string): string {
  return value.slice(0, CREATE_ADD_ON_LIMITS.nameMax);
}

export function sanitizeAddOnDescription(value: string): string {
  return value.slice(0, CREATE_ADD_ON_LIMITS.descriptionMax);
}

export function sanitizeAddOnPrice(value: string): string {
  const sanitized = value.replace(/[^\d.]/g, "");
  const firstDot = sanitized.indexOf(".");
  if (firstDot === -1) {
    return sanitized.slice(0, CREATE_ADD_ON_LIMITS.priceMaxChars);
  }

  const whole = sanitized.slice(0, firstDot);
  const decimal = sanitized
    .slice(firstDot + 1)
    .replace(/\./g, "")
    .slice(0, 2);
  const combined =
    decimal.length > 0 || sanitized.endsWith(".")
      ? `${whole}.${decimal}`
      : whole;

  return combined.slice(0, CREATE_ADD_ON_LIMITS.priceMaxChars);
}

export function parseAddOnPriceUsd(input: string): number {
  const trimmed = input.trim();
  if (!trimmed) return 0;
  const parsed = Number.parseFloat(trimmed);
  return Number.isFinite(parsed) ? parsed : 0;
}

export function validateCreateAddOnFields(input: {
  code: string;
  name: string;
  description: string;
  priceUsd: string;
}): string | null {
  const code = input.code.trim();
  const name = input.name.trim();
  const description = input.description.trim();
  const price = input.priceUsd.trim();

  if (!code) return "Code is required.";
  if (code.length > CREATE_ADD_ON_LIMITS.codeMax) {
    return `Code must be ${CREATE_ADD_ON_LIMITS.codeMax} characters or less.`;
  }
  if (!/^[A-Za-z0-9]+(?:[_-][A-Za-z0-9]+)*$/.test(code)) {
    return "Code can only use letters, numbers, underscores, or hyphens.";
  }

  if (!name) return "Name is required.";
  if (name.length > CREATE_ADD_ON_LIMITS.nameMax) {
    return `Name must be ${CREATE_ADD_ON_LIMITS.nameMax} characters or less.`;
  }

  if (description.length > CREATE_ADD_ON_LIMITS.descriptionMax) {
    return `Description must be ${CREATE_ADD_ON_LIMITS.descriptionMax} characters or less.`;
  }

  if (!price) return "Price is required.";
  if (price.length > CREATE_ADD_ON_LIMITS.priceMaxChars) {
    return `Price must be ${CREATE_ADD_ON_LIMITS.priceMaxChars} characters or less.`;
  }
  if (!/^\d+(\.\d{1,2})?$/.test(price)) {
    return "Enter a valid price (up to 2 decimals).";
  }

  const parsedPrice = parseAddOnPriceUsd(price);
  if (parsedPrice < 0) return "Price cannot be negative.";
  if (parsedPrice > CREATE_ADD_ON_LIMITS.priceMax) {
    return `Price cannot exceed $${CREATE_ADD_ON_LIMITS.priceMax.toLocaleString()}.`;
  }

  return null;
}
