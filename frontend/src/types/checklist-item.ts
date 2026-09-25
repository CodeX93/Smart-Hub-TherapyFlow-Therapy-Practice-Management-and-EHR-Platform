export interface ChecklistItem {
  id: string;
  title: string;
  category: string;
  description: string;
  templates?: string[];
  required: boolean;
}
