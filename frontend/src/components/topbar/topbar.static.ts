export const pageDescriptions: Record<
  string,
  string | { admin: string; user: string }
> = {
  appointments: "View and manage all your appointments here",
  "booked sessions": "View your booked therapy sessions and share session feedback",
  "my profile":
    "Update your profile picture and timezone. Other account details are managed by your clinic.",
  invoices: "View your billing history and payment status",
  documents:
    "Upload files your therapist needs and keep everything organized in one place",
  "clinical forms": {
    admin:
      "Manage form templates for client consent, intake, and documentation",
    user: "Complete forms assigned by your therapist",
  },
  scheduling: "Manage all the scheduling here",
  billings: "Track invoices, payments, and billing status",
  "Clinical Content Library":
    "Organize and access reusable clinical content for session notes",
  Assessments:
    "Manage assessment templates and track client assessment progress",
  "Healthcare Process Checklists":
    "Create and manage standardized healthcare process checklists for regulatory compliance",
  "Patient Consent Management":
    "View and monitor GDPR consent status for all clients",
  "User Profiles":
    "Manage user accounts, professional profiles, and role permissions",
  "HIPAA Audit Trail":
    "Complete audit log of all PHI access and system activities for compliance monitoring",
  "Role Management": "Create and manage custom roles with specific permissions",
  "Duplicate Detection":
    "Identify and manage potential duplicate client records",
  "System Settings": "Manage dropdown options and system configuration",
  "Payments & Subscription":
    "Connect Stripe for client invoice payments and manage your SmartHub plan",
  Security: "Manage multi-factor authentication and signed-in devices",
};
