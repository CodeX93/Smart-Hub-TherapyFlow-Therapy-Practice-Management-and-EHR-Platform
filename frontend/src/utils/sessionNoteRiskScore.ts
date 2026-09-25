/**
 * Overall risk scoring for the session-note Risk Assessment tab.
 *
 * Each item is answered by picking one option; the option's index is its score,
 * so index 0 is always the least concerning answer and the last index the most.
 * The worst possible total is derived from the items themselves rather than
 * hardcoded, so adding an item or an option keeps the denominator correct.
 */

export interface RiskScoreItem {
  id: string;
  options: string[];
}

export type RiskBand = "low" | "moderate" | "high" | "severe";

export interface RiskAssessmentSummary {
  /** Sum of the selected option indexes across answered items. */
  score: number;
  /** Worst total the current item set can produce. */
  maxScore: number;
  answeredCount: number;
  totalCount: number;
  isComplete: boolean;
  /** Highest single-item score, used to escalate the band. */
  highestItemScore: number;
  band: RiskBand;
}

/** Maps each risk item id to the API field that carries its score. */
export const RISK_ITEM_API_FIELDS = {
  suicidal_ideation: "riskSuicidalIdeation",
  self_harm: "riskSelfHarm",
  homicidal_ideation: "riskHomicidalIdeation",
  psychosis: "riskPsychosis",
  substance_abuse: "riskSubstanceUse",
  impulse_control: "riskImpulsivity",
  danger_to_others: "riskAggression",
  medication_compliance: "riskNonAdherence",
} as const;

export type RiskScorePayload = Partial<
  Record<(typeof RISK_ITEM_API_FIELDS)[keyof typeof RISK_ITEM_API_FIELDS], number>
>;

/**
 * Build the risk half of a session-note request body.
 *
 * An unanswered item is left out entirely rather than sent as `0`, so the
 * record keeps "not assessed" and "assessed as none" apart.
 */
export function buildRiskScorePayload(
  items: RiskScoreItem[],
  selections: Record<string, string | null | undefined>,
): RiskScorePayload {
  const payload: RiskScorePayload = {};

  for (const [itemId, field] of Object.entries(RISK_ITEM_API_FIELDS)) {
    const item = items.find((entry) => entry.id === itemId);
    if (!item) continue;

    const selected = selections[itemId];
    if (!selected) continue;

    const index = item.options.indexOf(selected);
    if (index < 0) continue;

    payload[field] = index;
  }

  return payload;
}

export const RISK_BAND_LABELS: Record<RiskBand, string> = {
  low: "Low",
  moderate: "Moderate",
  high: "High",
  severe: "Severe",
};

/** Share of the worst possible total at which each band starts. */
export const RISK_BAND_THRESHOLDS: { band: RiskBand; minRatio: number }[] = [
  { band: "severe", minRatio: 0.6 },
  { band: "high", minRatio: 0.35 },
  { band: "moderate", minRatio: 0.15 },
  { band: "low", minRatio: 0 },
];

/**
 * A single alarming answer must not be averaged away by seven calm ones, so the
 * worst individual item sets a floor on the overall band.
 */
const ITEM_SCORE_BAND_FLOOR: { minItemScore: number; band: RiskBand }[] = [
  { minItemScore: 4, band: "severe" },
  { minItemScore: 3, band: "high" },
];

const BAND_ORDER: RiskBand[] = ["low", "moderate", "high", "severe"];

function maxScoreForItem(item: RiskScoreItem): number {
  return Math.max(0, item.options.length - 1);
}

function bandFromRatio(ratio: number): RiskBand {
  for (const threshold of RISK_BAND_THRESHOLDS) {
    if (ratio >= threshold.minRatio) return threshold.band;
  }
  return "low";
}

function bandFromItemScore(highestItemScore: number): RiskBand | null {
  for (const floor of ITEM_SCORE_BAND_FLOOR) {
    if (highestItemScore >= floor.minItemScore) return floor.band;
  }
  return null;
}

function strongerBand(a: RiskBand, b: RiskBand): RiskBand {
  return BAND_ORDER.indexOf(a) >= BAND_ORDER.indexOf(b) ? a : b;
}

/**
 * Score the risk items against the currently selected options.
 *
 * `selections` maps item id to the selected option label. An item whose id is
 * missing, null, or holds an option that is not in its list counts as
 * unanswered: it adds nothing to the score and leaves the assessment incomplete.
 */
export function summarizeRiskAssessment(
  items: RiskScoreItem[],
  selections: Record<string, string | null | undefined>,
): RiskAssessmentSummary {
  let score = 0;
  let answeredCount = 0;
  let highestItemScore = 0;

  for (const item of items) {
    const selected = selections[item.id];
    if (!selected) continue;

    const index = item.options.indexOf(selected);
    if (index < 0) continue;

    score += index;
    answeredCount += 1;
    highestItemScore = Math.max(highestItemScore, index);
  }

  const maxScore = items.reduce((total, item) => total + maxScoreForItem(item), 0);
  const ratio = maxScore > 0 ? score / maxScore : 0;
  const itemFloorBand = bandFromItemScore(highestItemScore);
  const ratioBand = bandFromRatio(ratio);

  return {
    score,
    maxScore,
    answeredCount,
    totalCount: items.length,
    isComplete: items.length > 0 && answeredCount === items.length,
    highestItemScore,
    band: itemFloorBand ? strongerBand(ratioBand, itemFloorBand) : ratioBand,
  };
}
