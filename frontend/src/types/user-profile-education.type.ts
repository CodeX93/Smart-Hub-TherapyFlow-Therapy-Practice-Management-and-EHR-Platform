export interface UserProfileEducationEntry {
  id?: number;
  degreeType?: string;
  fieldOfStudy?: string;
  institution?: string;
  graduationYear?: number;
  graduationDate?: string;
  isAccredited?: boolean;
  accreditationBody?: string;
  notes?: string;
}

export interface UserProfileEducationFormEntry {
  degreeType: string;
  fieldOfStudy: string;
  institution: string;
  graduationYear: string;
  graduationDate: Date | null;
  isAccredited: boolean;
  accreditationBody: string;
  notes: string;
}

export type UserProfileEducationPayload = Omit<
  UserProfileEducationEntry,
  "id"
>;
