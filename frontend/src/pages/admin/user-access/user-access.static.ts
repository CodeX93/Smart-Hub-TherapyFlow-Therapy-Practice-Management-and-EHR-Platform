import type { DuplicateGroup } from "@/types/duplicate-detection.types";

const BASE_ROLES = [
  {
    roleName: "administrator",
    displayName: "Administrator",
    description: "Full system administrator with all permissions",
    permissionsAssigned: 15,
  },
  {
    roleName: "supervisor",
    displayName: "Clinical Supervisor",
    description: "Supervisory role with oversight capabilities",
    permissionsAssigned: 12,
  },
  {
    roleName: "therapist",
    displayName: "Therapist",
    description: "Provides client care and manages assigned caseloads",
    permissionsAssigned: 8,
  },
  {
    roleName: "associate_therapist",
    displayName: "Associate Therapist",
    description: "Delivers care under clinical supervision",
    permissionsAssigned: 6,
  },
  {
    roleName: "intake_coordinator",
    displayName: "Intake Coordinator",
    description: "Manages client intake, onboarding, and initial assessments",
    permissionsAssigned: 5,
  },
  {
    roleName: "billing_specialist",
    displayName: "Billing Specialist",
    description: "Handles invoicing, payments, and insurance workflows",
    permissionsAssigned: 6,
  },
];

export const MOCK_ROLES = Array.from({ length: 100 }, (_, i) => {
  const base = BASE_ROLES[i % BASE_ROLES.length];
  return {
    ...base,
    id: (i + 1).toString(),
    // Slightly vary some names for better sorting representation
    displayName:
      i > 5 ? `${base.displayName} ${Math.floor(i / 6) + 1}` : base.displayName,
  };
});

export interface Permission {
  id: string;
  label: string;
}

export interface PermissionGroup {
  id: string;
  category: string;
  permissions: Permission[];
}

export const PERMISSIONS_GROUPS: PermissionGroup[] = [
  {
    id: "assessment",
    category: "ASSESSMENT MANAGEMENT",
    permissions: [{ id: "manage_assessments", label: "Manage Assessments" }],
  },
  {
    id: "client",
    category: "CLIENT MANAGEMENT",
    permissions: [
      { id: "delete_clients", label: "Delete Clients" },
      { id: "edit_clients", label: "Edit Clients" },
      { id: "view_clients", label: "View Clients" },
    ],
  },
  {
    id: "financial",
    category: "FINANCIAL MANAGEMENT",
    permissions: [
      { id: "manage_billing", label: "Manage Billing" },
      { id: "view_billing", label: "View Billing" },
    ],
  },
  {
    id: "resource",
    category: "RESOURCE MANAGEMENT",
    permissions: [
      { id: "edit_library", label: "Edit Library" },
      { id: "view_library", label: "View Library" },
    ],
  },
];

export const mockDuplicateGroups: DuplicateGroup[] = [
  {
    id: "group-1",
    groupNumber: 1,
    matchReasons: ["Email Match", "Phone Match"],
    recommendation: {
      keepName: "Badredoine Soufan",
      keepId: "CL-2025-0001",
      reason: "Older profile",
    },
    records: [
      {
        id: "rec-1",
        clientId: "CL-2025-0001",
        name: "Badredoine Soufan",
        phone: "5197020230",
        email: "2020signsprinting@gmail.com",
        dob: "Feb 1, 1970",
        status: "Pending",
        created: "Jul 30, 2025",
        stats: {
          sessions: 2,
          documents: 5,
          billing: 2,
        },
        isRecommendedToKeep: true,
      },
      {
        id: "rec-2",
        clientId: "CL-2025-0899",
        name: "Dean Soufan",
        phone: "5197020230",
        email: "2020signsprinting@gmail.com",
        dob: "Jan 13, 2022",
        status: "Pending",
        created: "Aug 02, 2025",
        stats: {
          sessions: 3,
          documents: 4,
          billing: 3,
        },
        isRecommendedToKeep: false,
      },
    ],
  },
  {
    id: "group-2",
    groupNumber: 2,
    matchReasons: ["Phone Match"],
    recommendation: {
      keepName: "Ahmed Wael Aboleila",
      keepId: "CL-2025-0005",
      reason: "Older profile",
    },
    records: [
      {
        id: "rec-3",
        clientId: "CL-2025-0005",
        name: "Ahmed Wael Aboleila",
        phone: "6475204778",
        email: "aboleila@gmail.com",
        dob: "Oct 3, 2004",
        status: "Pending",
        created: "Jul 30, 2025",
        stats: {
          sessions: 0,
          documents: 0,
          billing: 0,
        },
        isRecommendedToKeep: true,
      },
      {
        id: "rec-4",
        clientId: "CL-2025-0007",
        name: "WAEL MOHAM ABOLEILA",
        phone: "6475204778",
        email: "aboleila@gmail.com",
        dob: "Jul 10, 1968",
        status: "Pending",
        created: "Aug 02, 2025",
        stats: {
          sessions: 0,
          documents: 0,
          billing: 0,
        },
        isRecommendedToKeep: false,
      },
    ],
  },
];
