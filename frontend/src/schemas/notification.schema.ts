import { z } from "zod";

export const NOTIFICATION_TRIGGER_LIMITS = {
  name: 120,
  description: 500,
  jsonRules: 5000,
  numericMax: 99999,
} as const;

export const NOTIFICATION_ENTITY_TYPES = [
  "GENERAL",
  "CLIENT",
  "SESSION",
  "TASK",
  "CHECKLIST",
  "DOCUMENT",
  "BILLING",
  "FORM",
  "ASSESSMENT",
  "USER",
  "SUPERVISOR_ASSIGNMENT",
] as const;

export const NOTIFICATION_TRIGGER_PRIORITIES = [
  "LOW",
  "MEDIUM",
  "HIGH",
  "URGENT",
] as const;

export const NOTIFICATION_TEMPLATE_LIMITS = {
  name: 100,
  subject: 200,
  body: 5000,
  eventType: 120,
  type: 20,
} as const;

const TRIGGER_NAME_MAX_LENGTH = NOTIFICATION_TRIGGER_LIMITS.name;
const TRIGGER_DESCRIPTION_MAX_LENGTH = NOTIFICATION_TRIGGER_LIMITS.description;
const JSON_RULES_MAX_LENGTH = NOTIFICATION_TRIGGER_LIMITS.jsonRules;
const TEMPLATE_NAME_MAX_LENGTH = NOTIFICATION_TEMPLATE_LIMITS.name;
const TEMPLATE_SUBJECT_MAX_LENGTH = NOTIFICATION_TEMPLATE_LIMITS.subject;
const TEMPLATE_MESSAGE_MAX_LENGTH = NOTIFICATION_TEMPLATE_LIMITS.body;

export const notificationTriggerSchema = z
  .object({
    name: z
      .string()
      .trim()
      .min(1, "Trigger name is required")
      .max(
        TRIGGER_NAME_MAX_LENGTH,
        `Trigger name cannot exceed ${TRIGGER_NAME_MAX_LENGTH} characters`,
      ),
    description: z
      .string()
      .max(
        TRIGGER_DESCRIPTION_MAX_LENGTH,
        `Description cannot exceed ${TRIGGER_DESCRIPTION_MAX_LENGTH} characters`,
      )
      .optional(),
    eventType: z.string().trim().min(1, "Event type is required"),
    entityType: z.string().trim().min(1, "Entity type is required"),
    conditionRules: z
      .string()
      .min(1, "Condition rules are required")
      .max(
        JSON_RULES_MAX_LENGTH,
        `Condition rules cannot exceed ${JSON_RULES_MAX_LENGTH} characters`,
      ),
    recipientRules: z
      .string()
      .min(1, "Recipient rules are required")
      .max(
        JSON_RULES_MAX_LENGTH,
        `Recipient rules cannot exceed ${JSON_RULES_MAX_LENGTH} characters`,
      ),
    priority: z.string().trim().min(1, "Priority is required"),
    isScheduled: z.boolean(),
    scheduleOffsetMinutes: z.string(),
    batchWindowMinutes: z.string(),
    maxBatchSize: z.string(),
    isActive: z.boolean(),
  })
  .superRefine((data, ctx) => {
    if (
      !(NOTIFICATION_ENTITY_TYPES as readonly string[]).includes(data.entityType)
    ) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["entityType"],
        message: "Select a valid entity type",
      });
    }

    const normalizedPriority = data.priority.toUpperCase();
    if (
      !(NOTIFICATION_TRIGGER_PRIORITIES as readonly string[]).includes(
        normalizedPriority,
      )
    ) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["priority"],
        message: "Select a valid priority",
      });
    }

    const numericFields = [
      ["scheduleOffsetMinutes", data.scheduleOffsetMinutes],
      ["batchWindowMinutes", data.batchWindowMinutes],
      ["maxBatchSize", data.maxBatchSize],
    ] as const;

    numericFields.forEach(([field, value]) => {
      const trimmed = value.trim();
      if (!/^\d+$/.test(trimmed)) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: [field],
          message: "Enter a valid non-negative number",
        });
        return;
      }

      const numericValue = Number.parseInt(trimmed, 10);
      if (numericValue > NOTIFICATION_TRIGGER_LIMITS.numericMax) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: [field],
          message: `Value must be ${NOTIFICATION_TRIGGER_LIMITS.numericMax.toLocaleString()} or less`,
        });
      }
    });

    const jsonFields = [
      ["conditionRules", data.conditionRules],
      ["recipientRules", data.recipientRules],
    ] as const;

    jsonFields.forEach(([field, value]) => {
      try {
        JSON.parse(value);
      } catch {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: [field],
          message: "Enter valid JSON",
        });
      }
    });
  });

export type NotificationTriggerSchema = z.infer<
  typeof notificationTriggerSchema
>;

export const notificationTemplateSchema = z.object({
  templateName: z
    .string()
    .trim()
    .min(1, "Template name is required")
    .max(
      TEMPLATE_NAME_MAX_LENGTH,
      `Template name cannot exceed ${TEMPLATE_NAME_MAX_LENGTH} characters`,
    ),
  subject: z
    .string()
    .max(
      TEMPLATE_SUBJECT_MAX_LENGTH,
      `Subject cannot exceed ${TEMPLATE_SUBJECT_MAX_LENGTH} characters`,
    )
    .optional(),
  message: z
    .string()
    .max(
      TEMPLATE_MESSAGE_MAX_LENGTH,
      `Message cannot exceed ${TEMPLATE_MESSAGE_MAX_LENGTH} characters`,
    )
    .optional(),
  status: z.string().optional(),
});

export type NotificationTemplateSchema = z.infer<
  typeof notificationTemplateSchema
>;
