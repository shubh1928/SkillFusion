import apiClient from "@/config/ApiClient";
import type ApiResponse from "@/models/ApiResponse";

export interface ReferralCodeResponseDto {
  referralCode: string;
}

/**
 * Retrieves the authenticated user's referral code.
 * The backend creates a code for existing accounts when needed.
 */
export const getMyReferralCode = async (): Promise<string> => {
  const response = await apiClient.get<
    ApiResponse<ReferralCodeResponseDto>
  >("/referrals/me/code");

  const code = response.data?.data?.referralCode;

  if (!code) {
    throw new Error("The server did not return a referral code.");
  }

  return code;
};