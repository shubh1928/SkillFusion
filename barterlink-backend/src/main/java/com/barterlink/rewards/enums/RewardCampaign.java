package com.barterlink.rewards.enums;

/**
 * Contract codes must match the RewardCampaign enum in
 * BarterLinkTokenV2.sol. Keep the explicit codes stable.
 */
public enum RewardCampaign {

    DIRECT_BARTER_COMPLETION(0, 1, 6),
    DAILY_ACTIVITY_STREAK_30(1, 5, 1),
    REFERRAL_QUALIFIED(2, 3, 5);

    private final int contractCode;
    private final int tokenAmount;
    private final int lifetimeLimit;

    RewardCampaign(
            int contractCode,
            int tokenAmount,
            int lifetimeLimit
    ) {
        this.contractCode = contractCode;
        this.tokenAmount = tokenAmount;
        this.lifetimeLimit = lifetimeLimit;
    }

    public int getContractCode() {
        return contractCode;
    }

    public int getTokenAmount() {
        return tokenAmount;
    }

    public int getLifetimeLimit() {
        return lifetimeLimit;
    }
}