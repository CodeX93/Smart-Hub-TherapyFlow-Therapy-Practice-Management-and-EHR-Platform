import z from "zod";

export const recordPaymentSchema = z.object({
  paymentSide: z.enum(["client", "insurance"], {
    message: "Paid by is required",
  }),
  paymentAmount: z
    .union([z.string(), z.number()])
    .refine((val) => !isNaN(Number(val)) && Number(val) > 0, {
      message: "Amount must be a positive number",
    }),
  paymentDate: z
    .string()
    .min(1, "Date received is required")
    .refine((dateStr) => {
      const selectedDate = new Date(dateStr);
      const today = new Date();
      today.setHours(23, 59, 59, 999); // End of today
      return selectedDate <= today;
    }, {
      message: "Payment date cannot be in the future",
    })
    .refine((dateStr) => {
      const selectedDate = new Date(dateStr);
      const maxPastDate = new Date();
      maxPastDate.setFullYear(maxPastDate.getFullYear() - 2); // 2 years ago
      return selectedDate >= maxPastDate;
    }, {
      message: "Payment date cannot be more than 2 years in the past",
    }),
  paymentMethod: z.string().min(1, "Payment method is required"),
  referenceNumber: z.string().optional(),
  notes: z.string().optional(),
});

export type RecordPaymentFormValues = z.infer<typeof recordPaymentSchema>;

/** Payload after converting delta → cumulative for the API. */
export type RecordPaymentSubmitValues = RecordPaymentFormValues & {
  expectedPreviousForSource: number;
};

export const applyDiscountSchema = z
  .object({
    discountType: z.string().min(1, "Discount type is required"),
    discountValue: z.union([z.string(), z.number()]).optional(),
  })
  .refine(
    (data) =>
      data.discountType === "no_discount" ||
      (!!data.discountValue &&
        !Number.isNaN(Number(data.discountValue)) &&
        Number(data.discountValue) > 0),
    {
      message: "A valid positive discount value is required",
      path: ["discountValue"],
    },
  )
  .refine(
    (data) =>
      data.discountType !== "percentage" ||
      !data.discountValue ||
      Number(data.discountValue) <= 100,
    {
      message: "Percentage discount cannot exceed 100",
      path: ["discountValue"],
    },
  );

export type ApplyDiscountFormValues = z.infer<typeof applyDiscountSchema>;

export const changeBillingStatusSchema = z.object({
  billingStatus: z.string().min(1, "Billing status is required"),
  notes: z.string().optional(),
});

export type ChangeBillingStatusFormValues = z.infer<typeof changeBillingStatusSchema>;
