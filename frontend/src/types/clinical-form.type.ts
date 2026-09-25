export type FormStatus = "pending" | "in-progress" | "completed";

export interface ClinicalForm {
  id: string;
  title: string;
  category: string;
  status: FormStatus;
  assignedDate?: string;
  completedDate?: string;
}

export interface ClinicalFormsPageProps {
  clinicalForms: ClinicalForm[];
  totalClinicalForms: number;
  itemsPerPage: number;
  totalPages: number;
}