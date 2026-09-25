import type { AuditRecord } from "@/types/hipaa.types";
import { Shield, Eye, AlertTriangle, AlertCircle } from "lucide-react";
import React from "react";

export interface FilterType {
  label: string;
  value: string;
}

export interface ConsentRecord {
  id: string;
  clientId: string;
  fullName: string;
  email: string;
  portalAccess: "Enabled" | "Disabled";
  aiProcessing: "Not Set" | "Granted" | "Denied";
  dataSharing: "Not Set" | "Granted" | "Denied";
  research: "Not Set" | "Granted" | "Denied";
  marketing: "Not Set" | "Granted" | "Denied";
}

export const consentTypes: FilterType[] = [
  { label: "All Consent Types", value: "ALL" },
  { label: "AI Processing", value: "AI_PROCESSING" },
  { label: "Data Sharing", value: "DATA_SHARING" },
  {
    label: "Research Participation",
    value: "RESEARCH",
  },
  {
    label: "Marketing Communications",
    value: "MARKETING",
  },
];

export const consentStatuses: FilterType[] = [
  { label: "All Consent Statuses", value: "ALL" },
  { label: "Granted", value: "GRANTED" },
  { label: "Denied / Withdrawn", value: "DENIED_WITHDRAWN" },
];

export const mockConsentData: ConsentRecord[] = Array(100)
  .fill(null)
  .map((_, i) => ({
    id: (i + 1).toString(),
    clientId: `CL-2025-${(i + 1).toString().padStart(4, "0")}`,
    fullName: i % 2 === 0 ? "Aaron Ansah" : "Aaron Nava",
    email: i % 2 === 0 ? "aaronansah@ymail.com" : "aaron.j.nava@gmail.com",
    portalAccess: i % 3 === 0 ? "Enabled" : "Disabled",
    aiProcessing: ["Not Set", "Granted", "Denied"][i % 3] as
      | "Not Set"
      | "Granted"
      | "Denied",
    dataSharing: ["Not Set", "Granted", "Denied"][i % 3] as
      | "Not Set"
      | "Granted"
      | "Denied",
    research: ["Not Set", "Granted", "Denied"][i % 3] as
      | "Not Set"
      | "Granted"
      | "Denied",
    marketing: ["Not Set", "Granted", "Denied"][i % 3] as
      | "Not Set"
      | "Granted"
      | "Denied",
  }));

export const hipaaOverviewData = [
  {
    label: "Total Activities",
    value: "24921",
    icon: React.createElement(Shield, { size: 20 }),
    iconClassName:
      "bg-(--bg-primary-50) text-(--bg-primary-dark) !h-12 !w-12 !flex-none rounded-xl items-center justify-center p-3.25",
  },
  {
    label: "PHI Access Events",
    value: "20206",
    icon: React.createElement(Eye, { size: 20 }),
    iconClassName:
      "bg-(--bg-primary-50) text-(--bg-primary-dark) !h-12 !w-12 !flex-none rounded-xl items-center justify-center p-3.25",
  },
  {
    label: "High Risk Events",
    value: "2157",
    icon: React.createElement(AlertTriangle, { size: 20 }),
    iconClassName:
      "bg-[#fff9e6] text-[#b45309] !h-12 !w-12 !flex-none rounded-xl items-center justify-center p-3.25",
  },
  {
    label: "Failed Attempts",
    value: "506",
    icon: React.createElement(AlertCircle, { size: 20 }),
    iconClassName:
      "bg-[#fef2f2] text-[#b91c1c] !h-12 !w-12 !flex-none rounded-xl items-center justify-center p-3.25",
  },
];

export const mockAuditTrailData: AuditRecord[] = Array.from(
  { length: 100 },
  (_, i) => ({
    id: (i + 1).toString(),
    timestamp: "Jan 18, 2026 22:47:34",
    user: i % 3 === 0 ? "eman.halles" : "amjed.abojedi",
    action: i % 2 === 0 ? "Client viewed" : "Session created",
    client: i % 2 === 0 ? "Alaa Diba" : "Faidhah Azeez Fala Al-Jashaam",
    details:
      i % 2 === 0 ? "Viewed client profile" : "Session type: psychotherapy",
    riskLevel: ["Low", "Medium", "High"][i % 3] as "Low" | "Medium" | "High",
    result: i % 5 === 0 ? "Failure" : "Success",
    ipAddress: "127.0.0.1",
  }),
);

export const actionTypes: FilterType[] = [
  { label: "All Actions", value: "all" },
  { label: "Client Viewed", value: "Client Viewed" },
  { label: "Document Accessed", value: "Document Accessed" },
  { label: "Log Events", value: "Log Events" },
  { label: "Data Exported", value: "Data Exported" },
];

export const riskLevels: FilterType[] = [
  { label: "All Levels", value: "all" },
  { label: "Critical", value: "critical" },
  { label: "High", value: "high" },
  { label: "Medium", value: "medium" },
  { label: "Low", value: "low" },
];

export const userActivityData = [
  { name: "John Doe", count: 12150 },
  { name: "shaher.awaw...", count: 4500 },
  { name: "admin.user", count: 2000 },
  { name: "fatima.hasso...", count: 1500 },
  { name: "Williams hales", count: 1000 },
];

export const riskDistributionData = {
  total: "23,921",
  breakdown: [
    { label: "Low Risk", count: "18,607", percentage: 78, color: "#009B65" },
    { label: "Medium Risk", count: "3,157", percentage: 13, color: "#FBAC00" },
    { label: "High Risk", count: "2,157", percentage: 9, color: "#EF4444" },
  ],
};
