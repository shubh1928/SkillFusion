import { getWallet, getWalletSummary } from "./WalletService";
import { getMyRequests } from "./MarketplaceService";
import {
  getRequesterAgreements,
  getProviderAgreements,
} from "./AgreementService";
import {
  getRequesterSessions,
  getProviderSessions,
} from "./SessionService";
import { getUserSkills } from "./SkillService";

import useAuth from "@/auth/store";

import type Wallet from "@/models/wallet/Wallet";
import type WalletSummary from "@/models/wallet/WalletSummary";
import type UserSkill from "@/models/skills/UserSkill";
import type Agreement from "@/models/agreement/Agreement";
import type Session from "@/models/sessions/Session";
import type ServiceRequest from "@/models/marketplace/ServiceRequest";
import type User from "@/models/User";


export interface DashboardData {

  user: User;

  wallet: Wallet;

  walletSummary: WalletSummary;

  skills: UserSkill[];

  marketplaceRequests: ServiceRequest[];

  requesterAgreements: Agreement[];

  providerAgreements: Agreement[];

  requesterSessions: Session[];

  providerSessions: Session[];

}


export async function loadDashboardData(): Promise<DashboardData> {

  try {

    const user = useAuth.getState().user;

if (!user) {
  throw new Error("User not found");
}

const [

  wallet,

  walletSummary,

  skills,

  marketplaceRequests,

  requesterAgreements,

  providerAgreements,

  requesterSessions,

  providerSessions,

] = await Promise.all([

  getWallet(),

  getWalletSummary(),

  getUserSkills(user.id),

  getMyRequests(),

  getRequesterAgreements(),

  getProviderAgreements(),

  getRequesterSessions(),

  getProviderSessions(),

]);

return {

  user,

  wallet,

  walletSummary,

  skills,

  marketplaceRequests,

  requesterAgreements,

  providerAgreements,

  requesterSessions,

  providerSessions,

};

  } catch (error) {

    console.error("Failed to load dashboard:", error);

    throw error;

  }

}