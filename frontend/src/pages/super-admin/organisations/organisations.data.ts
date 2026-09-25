export type OrganisationStatus = "Active" | "Pending Activation" | "Suspended";
export type OrganisationPlan = "Enterprise" | "Pro" | "Trial" | "-";
export type OrganisationMemberRole =
  | "Admin"
  | "Supervisor"
  | "Therapist"
  | "Client";
export type OrganisationMemberStatus =
  | "Active"
  | "Pending"
  | "Inactive"
  | "Suspended";

export interface OrganisationRow {
  name: string;
  slug: string;
  status: OrganisationStatus;
  plan: OrganisationPlan;
  users: number;
  createdAt: string;
  rrCode: string;
  primaryAdmin: {
    initials: string;
    email: string;
  };
  /** The tenant's own support address; blank until someone sets one. */
  supportEmail?: string;
  region: string;
  timezone: string;
  dataResidency: string;
  customDomain: string;
  subscription: {
    plan: OrganisationPlan;
    status?: string;
    billingCycle: string;
    currentPeriodEnd: string;
    addOns: string;
    basePrice: string;
    userLimits?: {
      therapist: number | null;
      supervisor: number | null;
      client: number | null;
    };
    userUsage?: {
      therapist: number;
      supervisor: number;
      client: number;
      total: number;
    };
  };
}

export interface OrganisationMember {
  id: string;
  name: string;
  email: string;
  role: OrganisationMemberRole;
  status: OrganisationMemberStatus;
  lastLogin: string;
}

interface OrganisationMemberSeed {
  id: string;
  name: string;
  email: string;
  role: OrganisationMemberRole;
  status: OrganisationMemberStatus;
  lastLogin: string;
}

export const ORGANISATIONS: OrganisationRow[] = [
  {
    name: "Harbor Wellness",
    slug: "harbor-wellness",
    status: "Active",
    plan: "Pro",
    users: 86,
    createdAt: "Jan 12, 2026",
    rrCode: "RR: org_1092",
    primaryAdmin: { initials: "HW", email: "admin@harborwellness.com" },
    region: "us-east-1",
    timezone: "America/New_York",
    dataResidency: "United States (US)",
    customDomain: "harbor.smarthub.com",
    subscription: {
      plan: "Pro",
      billingCycle: "Monthly",
      currentPeriodEnd: "Apr 30, 2026",
      addOns: "Audit Logs, Advanced Analytics",
      basePrice: "$499.00 / mo",
    },
  },
  {
    name: "Stellar Solutions",
    slug: "stellar",
    status: "Active",
    plan: "Enterprise",
    users: 86,
    createdAt: "Dec 07, 2025",
    rrCode: "Ref: 987654",
    primaryAdmin: { initials: "SS", email: "ops@stellar.com" },
    region: "us-east-1",
    timezone: "America/New_York",
    dataResidency: "United States (US)",
    customDomain: "stellar.smarthub.com",
    subscription: {
      plan: "Enterprise",
      billingCycle: "Monthly",
      currentPeriodEnd: "Mar 31, 2026",
      addOns: "Priority Support",
      basePrice: "$1,200.00 / mo",
    },
  },
  {
    name: "Orion Technologies",
    slug: "orion-tech",
    status: "Pending Activation",
    plan: "Pro",
    users: 86,
    createdAt: "Dec 07, 2025",
    rrCode: "Ref: 951753",
    primaryAdmin: { initials: "OT", email: "admin@oriontech.com" },
    region: "us-east-1",
    timezone: "America/New_York",
    dataResidency: "United States (US)",
    customDomain: "orion-tech.smarthub.com",
    subscription: {
      plan: "Pro",
      billingCycle: "Monthly",
      currentPeriodEnd: "Mar 31, 2026",
      addOns: "None",
      basePrice: "$499.00 / mo",
    },
  },
  {
    name: "Orion Technologies",
    slug: "orion-tech-west",
    status: "Pending Activation",
    plan: "Pro",
    users: 86,
    createdAt: "Dec 07, 2025",
    rrCode: "Ref: 789456",
    primaryAdmin: { initials: "OW", email: "west@oriontech.com" },
    region: "us-east-1",
    timezone: "America/New_York",
    dataResidency: "United States (US)",
    customDomain: "orion-west.smarthub.com",
    subscription: {
      plan: "Pro",
      billingCycle: "Monthly",
      currentPeriodEnd: "Mar 31, 2026",
      addOns: "Audit Logs",
      basePrice: "$499.00 / mo",
    },
  },
  {
    name: "Apex Innovations",
    slug: "apex",
    status: "Suspended",
    plan: "Enterprise",
    users: 86,
    createdAt: "Dec 07, 2025",
    rrCode: "Ref: 789456",
    primaryAdmin: { initials: "AI", email: "ops@apex.io" },
    region: "us-east-1",
    timezone: "America/New_York",
    dataResidency: "United States (US)",
    customDomain: "apex.smarthub.com",
    subscription: {
      plan: "Enterprise",
      billingCycle: "Monthly",
      currentPeriodEnd: "Mar 31, 2026",
      addOns: "Audit Logs",
      basePrice: "$1,200.00 / mo",
    },
  },
  {
    name: "Global Dynamics",
    slug: "global-dynamics",
    status: "Active",
    plan: "Pro",
    users: 86,
    createdAt: "Dec 07, 2025",
    rrCode: "Ref: 456789",
    primaryAdmin: { initials: "GD", email: "admin@globaldynamics.com" },
    region: "us-east-1",
    timezone: "America/New_York",
    dataResidency: "United States (US)",
    customDomain: "global-dynamics.smarthub.com",
    subscription: {
      plan: "Pro",
      billingCycle: "Monthly",
      currentPeriodEnd: "Mar 31, 2026",
      addOns: "None",
      basePrice: "$499.00 / mo",
    },
  },
  {
    name: "Global Dynamics",
    slug: "global-dynamics-east",
    status: "Active",
    plan: "Pro",
    users: 86,
    createdAt: "Dec 07, 2025",
    rrCode: "Ref: 159753",
    primaryAdmin: { initials: "GE", email: "east@globaldynamics.com" },
    region: "us-east-1",
    timezone: "America/New_York",
    dataResidency: "United States (US)",
    customDomain: "global-east.smarthub.com",
    subscription: {
      plan: "Pro",
      billingCycle: "Monthly",
      currentPeriodEnd: "Mar 31, 2026",
      addOns: "Priority Support",
      basePrice: "$499.00 / mo",
    },
  },
  {
    name: "Zenith Systems",
    slug: "zenith",
    status: "Active",
    plan: "Enterprise",
    users: 86,
    createdAt: "Dec 07, 2025",
    rrCode: "Ref: 159753",
    primaryAdmin: { initials: "ZS", email: "admin@zenithsystems.com" },
    region: "us-east-1",
    timezone: "America/New_York",
    dataResidency: "United States (US)",
    customDomain: "zenith.smarthub.com",
    subscription: {
      plan: "Enterprise",
      billingCycle: "Monthly",
      currentPeriodEnd: "Mar 31, 2026",
      addOns: "Priority Support",
      basePrice: "$1,200.00 / mo",
    },
  },
  {
    name: "Pinnacle Group",
    slug: "pinnacle",
    status: "Active",
    plan: "Trial",
    users: 86,
    createdAt: "Dec 07, 2025",
    rrCode: "Ref: 321654",
    primaryAdmin: { initials: "PG", email: "hello@pinnacle.com" },
    region: "us-east-1",
    timezone: "America/New_York",
    dataResidency: "United States (US)",
    customDomain: "pinnacle.smarthub.com",
    subscription: {
      plan: "Trial",
      billingCycle: "Monthly",
      currentPeriodEnd: "Mar 31, 2026",
      addOns: "None",
      basePrice: "$0.00 / mo",
    },
  },
  {
    name: "Nova Enterprises",
    slug: "nova-enterprises",
    status: "Pending Activation",
    plan: "Enterprise",
    users: 86,
    createdAt: "Dec 07, 2025",
    rrCode: "Ref: 751359",
    primaryAdmin: { initials: "NE", email: "billing@novaenterprises.com" },
    region: "us-east-1",
    timezone: "America/New_York",
    dataResidency: "United States (US)",
    customDomain: "nova-enterprises.smarthub.com",
    subscription: {
      plan: "Enterprise",
      billingCycle: "Monthly",
      currentPeriodEnd: "Mar 31, 2026",
      addOns: "Custom Domain",
      basePrice: "$1,200.00 / mo",
    },
  },
  {
    name: "Vanguard Industries",
    slug: "vanguard",
    status: "Active",
    plan: "Enterprise",
    users: 86,
    createdAt: "Dec 07, 2025",
    rrCode: "Ref: 654321",
    primaryAdmin: { initials: "VI", email: "admin@vanguard.com" },
    region: "us-east-1",
    timezone: "America/New_York",
    dataResidency: "United States (US)",
    customDomain: "vanguard.smarthub.com",
    subscription: {
      plan: "Enterprise",
      billingCycle: "Monthly",
      currentPeriodEnd: "Mar 31, 2026",
      addOns: "Priority Support",
      basePrice: "$1,200.00 / mo",
    },
  },
];

const HARBOR_WELLNESS_USERS: OrganisationMember[] = [
  {
    id: "samantha-lee",
    name: "Samantha Lee",
    email: "samantha.lee@example.com",
    role: "Supervisor",
    status: "Pending",
    lastLogin: "11-15-2025 08:45",
  },
  {
    id: "david-kim",
    name: "David Kim",
    email: "david.kim@testmail.com",
    role: "Therapist",
    status: "Active",
    lastLogin: "03-22-2024 14:20",
  },
  {
    id: "maria-rodriguez",
    name: "Maria Rodriguez",
    email: "maria.rodriguez@domain.com",
    role: "Client",
    status: "Inactive",
    lastLogin: "06-09-2023 12:10",
  },
  {
    id: "james-carter",
    name: "James Carter",
    email: "james.carter@webmail.org",
    role: "Admin",
    status: "Active",
    lastLogin: "01-30-2026 11:05",
  },
  {
    id: "nina-patel",
    name: "Nina Patel",
    email: "nina.patel@company.net",
    role: "Therapist",
    status: "Suspended",
    lastLogin: "09-18-2025 17:55",
  },
  {
    id: "liam-oconnor",
    name: "Liam O'Connor",
    email: "liam.oconnor@mailservice.com",
    role: "Therapist",
    status: "Active",
    lastLogin: "07-07-2024 09:33",
  },
  {
    id: "chen-wei",
    name: "Chen Wei",
    email: "chen.wei@inbox.com",
    role: "Therapist",
    status: "Active",
    lastLogin: "12-01-2025 22:41",
  },
];

const GENERIC_MEMBER_SEEDS: OrganisationMemberSeed[] = [
  {
    id: "ops-admin",
    name: "Jordan Blake",
    email: "jordan.blake",
    role: "Admin",
    status: "Active",
    lastLogin: "02-12-2026 09:14",
  },
  {
    id: "clinical-supervisor",
    name: "Avery Collins",
    email: "avery.collins",
    role: "Supervisor",
    status: "Active",
    lastLogin: "02-10-2026 13:40",
  },
  {
    id: "lead-therapist",
    name: "Priya Shah",
    email: "priya.shah",
    role: "Therapist",
    status: "Active",
    lastLogin: "02-11-2026 10:05",
  },
  {
    id: "therapist-1",
    name: "Marcus Reed",
    email: "marcus.reed",
    role: "Therapist",
    status: "Active",
    lastLogin: "02-09-2026 16:22",
  },
  {
    id: "therapist-2",
    name: "Elena Torres",
    email: "elena.torres",
    role: "Therapist",
    status: "Pending",
    lastLogin: "02-04-2026 08:18",
  },
  {
    id: "client-1",
    name: "Noah Bennett",
    email: "noah.bennett",
    role: "Client",
    status: "Active",
    lastLogin: "01-29-2026 19:12",
  },
  {
    id: "client-2",
    name: "Sophia Nguyen",
    email: "sophia.nguyen",
    role: "Client",
    status: "Inactive",
    lastLogin: "12-18-2025 11:47",
  },
  {
    id: "client-3",
    name: "Mateo Alvarez",
    email: "mateo.alvarez",
    role: "Client",
    status: "Suspended",
    lastLogin: "11-03-2025 15:30",
  },
];

function getSeedEmailDomain(org: OrganisationRow): string {
  const segments = org.customDomain.split(".");

  if (segments.length >= 2) {
    return segments.slice(-2).join(".");
  }

  return org.slug + ".com";
}

function getStatusForOrganisation(
  org: OrganisationRow,
  member: OrganisationMemberSeed
): OrganisationMemberStatus {
  if (org.status === "Suspended") {
    if (member.role === "Admin") {
      return "Active";
    }

    return member.role === "Client" ? "Inactive" : "Suspended";
  }

  if (org.status === "Pending Activation") {
    if (member.role === "Admin") {
      return "Active";
    }

    return member.role === "Client" ? "Inactive" : "Pending";
  }

  return member.status;
}

function createOrganisationUsers(org: OrganisationRow): OrganisationMember[] {
  const emailDomain = getSeedEmailDomain(org);

  return GENERIC_MEMBER_SEEDS.map(function (member) {
    return {
      id: org.slug + "-" + member.id,
      name: member.name,
      email: member.email + "@" + emailDomain,
      role: member.role,
      status: getStatusForOrganisation(org, member),
      lastLogin: member.lastLogin,
    };
  });
}

export const ORGANISATION_USERS: Record<string, OrganisationMember[]> =
  Object.fromEntries(
    ORGANISATIONS.map(function (org) {
      return [
        org.slug,
        org.slug === "harbor-wellness"
          ? HARBOR_WELLNESS_USERS
          : createOrganisationUsers(org),
      ];
    })
  ) as Record<string, OrganisationMember[]>;
