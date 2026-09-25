/**
 * Risk assessment items for the session-note Risk Assessment tab.
 *
 * Option order carries meaning: index 0 is the least concerning answer and the
 * last index the most, which is what `summarizeRiskAssessment` scores against.
 * Kept free of path aliases so it can be imported by plain Node test runs.
 */

export interface RiskItem {
  id: string;
  title: string;
  description: string;
  options: string[];
}

export const RISK_ASSESSMENT_ITEMS: RiskItem[] = [
  {
    id: "suicidal_ideation",
    title: "Suicidal Ideation",
    description:
      "Assessment of thoughts or plans related to self-harm or suicide",
    options: [
      "None",
      "Passive",
      "Active without plan",
      "Active with plan",
      "Active with intent",
    ],
  },
  {
    id: "homicidal_ideation",
    title: "Homicidal Ideation",
    description: "Assessment of thoughts or plans related to harming others",
    options: [
      "None",
      "Passive",
      "Active without plan",
      "Active with plan",
      "Active with intent",
    ],
  },
  {
    id: "substance_abuse",
    title: "Substance Abuse",
    description: "Current substance use patterns and risk level",
    options: ["None", "Minimal", "Moderate", "Severe", "Critical"],
  },
  {
    id: "self_harm",
    title: "Self-Harm Behaviors",
    description: "Non-suicidal self-injury or destructive behaviors",
    options: [
      "None",
      "Past only",
      "Occasional thoughts",
      "Recent behavior",
      "Frequent/severe",
    ],
  },
  {
    id: "danger_to_others",
    title: "Danger to Others",
    description: "Risk of harm to others through action or neglect",
    options: ["None", "Low", "Moderate", "High", "Imminent"],
  },
  {
    id: "psychosis",
    title: "Psychotic Symptoms",
    description:
      "Presence of hallucinations, delusions, or disorganized thinking",
    options: ["None", "Minimal", "Moderate", "Severe", "Acute"],
  },
  {
    id: "impulse_control",
    title: "Impulse Control",
    description: "Ability to manage impulsive or aggressive behaviors",
    options: ["Good", "Fair", "Poor", "Very poor", "Absent"],
  },
  {
    id: "medication_compliance",
    title: "Medication Compliance",
    description: "Adherence to prescribed medication regimen",
    options: ["Excellent", "Good", "Fair", "Poor", "Non-compliant"],
  },
];
