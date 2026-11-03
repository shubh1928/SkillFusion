import type FraudReason from "./FraudReason";

export type RiskLevel =
  | "LOW"
  | "MEDIUM"
  | "HIGH";

export default interface TrustAssessment {

  riskLevel: RiskLevel;

  trustScore: number;

  riskScore: number;

  evaluatedAt: string;

  reasons: FraudReason[];

}