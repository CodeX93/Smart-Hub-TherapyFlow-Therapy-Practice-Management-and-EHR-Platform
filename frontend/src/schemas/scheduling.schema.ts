import z from "zod";

export const schedulingSchema = (role: "admin" | "therapist") =>
  z
    .object({
      sessionType: z.string().min(1, "Session type is required"),
      client: z.string().min(1, "Client is required"),
      service: z.string().min(1, "Service is required"),
      therapist: z.string().optional(),
      sessionMode: z.string().min(1, "Session mode is required"),
      date: z
        .date()
        .nullable()
        .refine((val) => val !== null, {
          message: "Date is required",
        }),
      selectedTimeSlot: z.string().min(1, "Time slot is required"),
      room: z.string().optional(),
      notes: z.string().optional(),
    })
    .superRefine((data, ctx) => {
      if (role === "admin" && !data.therapist?.trim()) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: "Therapist is required",
          path: ["therapist"],
        });
      }
    })
    .refine(
      (data) => {
        const mode = data.sessionMode.toLowerCase().replace(/[\s_-]+/g, "");
        const inPerson = mode.includes("person") || mode === "inperson";
        if (inPerson && !data.room) {
          return false;
        }
        return true;
      },
      {
        message: "Room is required for in-person sessions",
        path: ["room"],
      },
    );

export type SchedulingFormValues = z.infer<ReturnType<typeof schedulingSchema>>;

export type SchedulingSuccessData = SchedulingFormValues & {
  recurringSummary?: {
    createdCount: number;
    skippedCount: number;
    groupId: string;
  };
  zoomJoinUrl?: string;
  zoomPassword?: string;
};
