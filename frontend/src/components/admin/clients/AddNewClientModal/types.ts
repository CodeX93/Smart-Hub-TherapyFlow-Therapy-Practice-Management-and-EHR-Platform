import type { Control } from "react-hook-form";
import type { AddClientFormValues } from "../../../../types/add-client.type";
import type { CustomSelectOption } from "@/components/form/CustomSelect";
import type { ClientSystemOptionsContext } from "@/hooks/useClientSystemOptions";

export type TabType =
  | "Personal"
  | "Address"
  | "Referral"
  | "Employment"
  | "Clinical"
  | "Consents";

export interface TabComponentProps {
  control: Control<AddClientFormValues>;
  therapistOptions?: CustomSelectOption[];
  assignedTherapistId?: string;
  onAssignedTherapistChange?: (value: string) => void;
  hideAssignedTherapist?: boolean;
  mode?: "create" | "edit";
  systemOptions?: ClientSystemOptionsContext;
}
