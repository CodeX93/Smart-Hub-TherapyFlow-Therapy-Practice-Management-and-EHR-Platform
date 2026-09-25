import { z } from "zod";
import {
  PRACTICE_CONFIG_FIELD_LIMITS,
  isValidPracticePhone,
} from "@/utils/practiceConfigInput";
import {
  ROOM_FIELD_LIMITS,
  ROOM_NUMBER_PATTERN,
  isValidRoomCapacity,
} from "@/utils/roomInput";
import { INVOICE_POLICY_FIELD_LIMITS } from "@/utils/invoicePolicyForm";
import {
  SERVICE_CODE_PATTERN,
  SERVICE_FIELD_LIMITS,
  isValidServiceBaseRate,
  isValidServiceDuration,
} from "@/utils/serviceCodeInput";

export const categorySchema = z.object({
  key: z
    .string()
    .min(1, "Category key is required")
    .max(50, "Category key must be 50 characters or fewer"),
  name: z
    .string()
    .min(1, "Category name is required")
    .max(50, "Category name must be 50 characters or fewer"),
  description: z.string(),
  isSystem: z.boolean(),
  isActive: z.boolean(),
});

export type CategoryFormData = z.infer<typeof categorySchema>;

export const optionSchema = z.object({
  value: z
    .string()
    .min(1, "Option key is required")
    .max(50, "Option key must be 50 characters or fewer"),
  label: z
    .string()
    .min(1, "Option label is required")
    .max(50, "Option label must be 50 characters or fewer"),
  isDefault: z.boolean(),
  isSystem: z.boolean(),
  isActive: z.boolean(),
});

export type OptionFormData = z.infer<typeof optionSchema>;

export const serviceCodeSchema = z.object({
  code: z
    .string()
    .trim()
    .min(1, "Service code is required")
    .max(
      SERVICE_FIELD_LIMITS.serviceCode,
      `Service code cannot exceed ${SERVICE_FIELD_LIMITS.serviceCode} characters`,
    )
    .regex(
      SERVICE_CODE_PATTERN,
      "Service code may only contain letters, numbers, hyphens, and underscores",
    ),
  name: z
    .string()
    .trim()
    .min(1, "Service name is required")
    .max(
      SERVICE_FIELD_LIMITS.serviceName,
      `Service name cannot exceed ${SERVICE_FIELD_LIMITS.serviceName} characters`,
    ),
  description: z
    .string()
    .max(
      SERVICE_FIELD_LIMITS.description,
      `Description cannot exceed ${SERVICE_FIELD_LIMITS.description} characters`,
    )
    .optional(),
  duration: z
    .string()
    .min(1, "Session duration is required")
    .refine((value) => isValidServiceDuration(value), {
      message: `Session duration must be a whole number between ${SERVICE_FIELD_LIMITS.durationMin} and ${SERVICE_FIELD_LIMITS.durationMax}.`,
    }),
  price: z
    .string()
    .min(1, "Base rate is required")
    .refine((value) => isValidServiceBaseRate(value), {
      message: "Base rate must be a valid number with up to 8 digits and 2 decimal places.",
    }),
});

export type ServiceCodeFormData = z.infer<typeof serviceCodeSchema>;

export const roomSchema = z.object({
  roomNumber: z
    .string()
    .trim()
    .min(1, "Room number is required")
    .max(
      ROOM_FIELD_LIMITS.roomNumber,
      `Room number cannot exceed ${ROOM_FIELD_LIMITS.roomNumber} characters`,
    )
    .regex(
      ROOM_NUMBER_PATTERN,
      "Room number may only contain letters, numbers, spaces, hyphens, and underscores",
    ),
  roomName: z
    .string()
    .trim()
    .min(1, "Room name is required")
    .max(
      ROOM_FIELD_LIMITS.roomName,
      `Room name cannot exceed ${ROOM_FIELD_LIMITS.roomName} characters`,
    ),
  capacity: z
    .string()
    .optional()
    .refine((value) => isValidRoomCapacity(value), {
      message: `Capacity must be a whole number between ${ROOM_FIELD_LIMITS.capacityMin} and ${ROOM_FIELD_LIMITS.capacityMax}.`,
    }),
  equipment: z
    .string()
    .max(
      ROOM_FIELD_LIMITS.equipment,
      `Equipment cannot exceed ${ROOM_FIELD_LIMITS.equipment} characters`,
    )
    .optional(),
  roomType: z.enum(["PHYSICAL", "VIRTUAL"], {
    message: "Room type is required",
  }),
  isActive: z.boolean(),
});

export type RoomFormData = z.infer<typeof roomSchema>;

function isValidPracticeWebsite(value: string): boolean {
  const trimmed = value.trim();
  if (!trimmed) return true;
  try {
    const candidate = /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;

    new URL(candidate);
    return true;
  } catch {
    return false;
  }
}

function isValidOptionalEmail(value: string): boolean {
  const trimmed = value.trim();
  if (!trimmed) return true;
  return z.string().email().safeParse(trimmed).success;
}

export const practiceConfigSchema = z.object({
  name: z
    .string()
    .max(
      PRACTICE_CONFIG_FIELD_LIMITS.name,
      `Practice name cannot exceed ${PRACTICE_CONFIG_FIELD_LIMITS.name} characters`,
    ),
  subtitle: z
    .string()
    .max(
      PRACTICE_CONFIG_FIELD_LIMITS.subtitle,
      `Practice subtitle cannot exceed ${PRACTICE_CONFIG_FIELD_LIMITS.subtitle} characters`,
    ),
  description: z
    .string()
    .max(
      PRACTICE_CONFIG_FIELD_LIMITS.description,
      `Practice description cannot exceed ${PRACTICE_CONFIG_FIELD_LIMITS.description} characters`,
    ),
  address: z
    .string()
    .max(
      PRACTICE_CONFIG_FIELD_LIMITS.address,
      `Practice address cannot exceed ${PRACTICE_CONFIG_FIELD_LIMITS.address} characters`,
    ),
  phone: z
    .string()
    .max(
      PRACTICE_CONFIG_FIELD_LIMITS.phone,
      `Phone number cannot exceed ${PRACTICE_CONFIG_FIELD_LIMITS.phone} characters`,
    )
    .refine((value) => isValidPracticePhone(value), {
      message: "Use an optional leading + followed by digits only.",
    }),
  email: z
    .string()
    .max(
      PRACTICE_CONFIG_FIELD_LIMITS.email,
      `Email cannot exceed ${PRACTICE_CONFIG_FIELD_LIMITS.email} characters`,
    )
    .refine((value) => isValidOptionalEmail(value), {
      message: "Invalid email address",
    }),
  website: z
    .string()
    .max(
      PRACTICE_CONFIG_FIELD_LIMITS.website,
      `Website URL cannot exceed ${PRACTICE_CONFIG_FIELD_LIMITS.website} characters`,
    )
    .refine((value) => isValidPracticeWebsite(value), {
      message: "Invalid URL",
    }),
  timezone: z.string(),
  taxId: z
    .string()
    .max(200)
    .optional()
    .refine((val) => !val || /^(\d{2}-\d{7}|\d{9})$/.test(val), {
      message: "Tax ID must be in format XX-XXXXXXX or 9 digits",
    }),
  licenseNumber: z.string().max(200).optional(),
  licenseState: z.string().max(200).optional(),
  npiNumber: z.string()
    .max(200)
    .optional()
    .refine((val) => !val || /^\d{10}$/.test(val), {
      message: "NPI number must be exactly 10 digits",
    }),
});

export type PracticeConfigFormData = z.infer<typeof practiceConfigSchema>;

function compareDateOnlyStrings(left: string, right: string): number {
  const parse = (value: string) => {
    const match = value.trim().match(/^(\d{4})-(\d{2})-(\d{2})$/);
    if (!match) return null;
    return {
      year: Number.parseInt(match[1], 10),
      month: Number.parseInt(match[2], 10),
      day: Number.parseInt(match[3], 10),
    };
  };

  const leftDate = parse(left);
  const rightDate = parse(right);
  if (!leftDate || !rightDate) return 0;

  if (leftDate.year !== rightDate.year) return leftDate.year - rightDate.year;
  if (leftDate.month !== rightDate.month) return leftDate.month - rightDate.month;
  return leftDate.day - rightDate.day;
}

export const invoicePolicySchema = z
  .object({
    clientTypeKey: z.string().min(1, "Client type is required"),
    appointmentStatusKey: z.string().min(1, "Session status is required"),
    priceType: z.enum(["FIXED", "PERCENTAGE"]),
    invoicePrice: z
      .string()
      .min(1, "Invoice price is required")
      .refine((value) => {
        const parsed = Number.parseFloat(value);
        return Number.isFinite(parsed) && parsed >= 0;
      }, "Invoice price must be a valid number"),
    policyName: z
      .string()
      .max(
        INVOICE_POLICY_FIELD_LIMITS.policyName,
        `Policy name cannot exceed ${INVOICE_POLICY_FIELD_LIMITS.policyName} characters`,
      )
      .optional(),
    serviceId: z
      .string()
      .min(1, "Service is required")
      .refine((value) => {
        if (value.trim().toLowerCase() === "all") return true;
        const parsed = Number.parseInt(value, 10);
        return Number.isFinite(parsed) && parsed > 0;
      }, "Select a valid service"),
    priority: z
      .string()
      .optional()
      .refine((value) => {
        if (!value?.trim()) return true;
        const parsed = Number.parseInt(value, 10);
        return Number.isFinite(parsed) && parsed >= 0;
      }, "Priority must be a non-negative whole number"),
    effectiveFrom: z.string().optional(),
    effectiveTo: z.string().optional(),
    enabled: z.boolean(),
  })
  .refine(
    (data) => {
      if (data.priceType !== "PERCENTAGE") return true;
      const parsed = Number.parseFloat(data.invoicePrice);
      return parsed <= 100;
    },
    {
      message: "Percentage cannot exceed 100",
      path: ["invoicePrice"],
    },
  )
  .refine(
    (data) => {
      const effectiveFrom = data.effectiveFrom?.trim();
      const effectiveTo = data.effectiveTo?.trim();
      if (!effectiveFrom || !effectiveTo) return true;
      return compareDateOnlyStrings(effectiveTo, effectiveFrom) >= 0;
    },
    {
      message: "Effective to date cannot be before effective from date",
      path: ["effectiveTo"],
    },
  );

export type InvoicePolicyFormData = z.infer<typeof invoicePolicySchema>;
