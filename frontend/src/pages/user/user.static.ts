export const APPOINTMENTS_STATIC_CONTENT = {
  serviceTypes: [
    {
      value: "psych-sc",
      label: "Psychotherapy (SC) session",
      duration: "60 Min",
      price: "$100.00",
    },
    {
      value: "psych-reg",
      label: "Psychotherapy session (Reg)",
      duration: "60 Min",
      price: "$170.00",
    },
    {
      value: "psych-pro",
      label: "Psychotherapy session (Pro Bono)",
      duration: "60 Min",
      price: "$0.00",
    },
    {
      value: "psych-pr",
      label: "Psychotherapy (PR) session 60 minutes",
      duration: "60 Min",
      price: "$170.00",
    },
  ],
};

export const INVOICES_STATIC_CONTENT = {
  overview: {
    totalInvoices: 6,
    totalBilled: "$450.00",
    totalPaid: "$300.00",
  },
  invoices: Array(100)
    .fill(null)
    .map((_, i) => ({
      id: (i + 1).toString(),
      date: "Dec 01, 2025",
      service:
        i % 2 === 0
          ? "Physiotherapy (PR) session 60 minutes PSY-01"
          : "Psychotherapy session (Reg)",
      amount: "$150.00",
      insurance: "Covered",
      copay: "--",
      status: i % 4 === 0 ? ("pending" as const) : ("paid" as const),
      paidDate: i % 4 === 0 ? undefined : "Nov 25, 2025",
      paymentMethod: i % 4 === 0 ? undefined : "Cash",
    })),
  totalInvoices: 100,
  itemsPerPage: 10,
  totalPages: 10,
};

export const DOCUMENTS_STATIC_CONTENT = {
  documents: Array(100)
    .fill(null)
    .map((_, i) => ({
      id: (i + 1).toString(),
      name:
        i % 3 === 0
          ? `medical-history-report-${i + 1}.doc`
          : i % 3 === 1
            ? `lab-results-${2025 - (i % 5)}.pdf`
            : `insurance-card-${2025 - (i % 3)}.png`,
      category:
        i % 4 === 0
          ? "Medical Records"
          : i % 4 === 1
            ? "Lab Results"
            : i % 4 === 2
              ? "Insurance"
              : "Prescriptions",
      size: `${(Math.random() * 5).toFixed(2)} MB`,
      uploadedDate: "Dec 07, 2025",
    })),
  totalDocuments: 100,
  itemsPerPage: 9,
  totalPages: 12,
};

export const CLINICAL_FORMS_STATIC_CONTENT = {
  forms: Array(100)
    .fill(null)
    .map((_, i) => ({
      id: (i + 1).toString(),
      title:
        i % 2 === 0
          ? "Informed Consent for Psychotherapy Services and treatment plan agreement"
          : "Release of Information Form for external healthcare providers",
      category: "Consent",
      status:
        i % 4 === 0
          ? ("pending" as const)
          : i % 4 === 1
            ? ("in-progress" as const)
            : ("completed" as const),
      assignedDate: "Dec 12, 2025",
      completedDate: i % 4 > 1 ? "Dec 14, 2025" : undefined,
    })),
  totalForms: 100,
  itemsPerPage: 9,
  totalPages: 12,
};

export const NOTIFICATIONS_STATIC_CONTENT = {
  notifications: [
    {
      id: "1",
      title: "Lorem ipsum dolor sit amet",
      description:
        "Consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
      timestamp: "12:13 PM",
      isRead: false,
    },
    {
      id: "2",
      title: "Duis aute irure dolor in reprehenderit in",
      description:
        "Consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
      timestamp: "12:13 PM",
      isRead: false,
      dateGroup: "Yesterday",
    },
    {
      id: "3",
      title: "Sed ut perspiciatis unde omnis",
      description:
        "Consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
      timestamp: "12:13 PM",
      isRead: true,
    },
    {
      id: "4",
      title: "Nemo enim ipsam voluptatem",
      description:
        "Consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
      timestamp: "12:13 PM",
      isRead: true,
      dateGroup: "Dec 09",
    },
    {
      id: "5",
      title: "Ut enim ad minima veniam",
      description:
        "Consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
      timestamp: "12:13 PM",
      isRead: true,
    },
  ],
};

export const PRIVACY_SETTINGS_STATIC_CONTENT = {
  dataProtectionCards: [
    {
      iconSrc: "/settings/shield.svg",
      title: "Data Security",
      description:
        "All data is encrypted in transit and at rest using industry-standard AES-256 encryption.",
    },
    {
      iconSrc: "/settings/access.svg",
      title: "Access Logging",
      description:
        "Every access to your records is logged with user identity, timestamp, and purpose.",
    },
    {
      iconSrc: "/settings/rights.svg",
      title: "Your Rights",
      description:
        "You have the right to access, correct, export, or delete your personal data. Contact us to exercise these rights.",
    },
    {
      iconSrc: "/settings/retention.svg",
      title: "Data Retention",
      description:
        "Clinical records are retained for 7 years as required by law. You may request earlier deletion with legal exceptions.",
    },
    {
      iconSrc: "/settings/questions.svg",
      title: "Questions?",
      description:
        "Contact our Privacy Officer at privacy@smarthub.com or speak with your therapist.",
    },
  ],
};

export const monthNames = [
  "January",
  "February",
  "March",
  "April",
  "May",
  "June",
  "July",
  "August",
  "September",
  "October",
  "November",
  "December",
];

export const dayLabels = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"];

export const timeSlots = [
  "09:00 AM",
  "10:00 AM",
  "10:30 AM",
  "11:30 AM",
  "12:00 PM",
  "02:00 PM",
  "02:30 PM",
  "03:00 PM",
  "04:00 PM",
  "04:30 PM",
  "05:15 PM",
  "05:45 PM",
];
