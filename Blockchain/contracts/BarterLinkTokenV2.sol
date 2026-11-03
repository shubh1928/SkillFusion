// SPDX-License-Identifier: MIT
pragma solidity ^0.8.34;

import {ERC20} from "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import {
    ERC20Permit
} from "@openzeppelin/contracts/token/ERC20/extensions/ERC20Permit.sol";

/**
 * BarterLinkTokenV2
 *
 * Adds fixed, protocol-authorized BLT reward campaigns while preserving
 * the existing token metadata, ERC-20 behavior, ERC-2612 permit support,
 * and one-time signup bonus.
 *
 * IMPORTANT:
 * - This is a new contract, not an upgrade to the deployed v1 contract.
 * - All reward campaigns start paused.
 * - Signup minting and campaign rewards remain disabled until legacy
 *   migration is explicitly closed.
 * - The protocol signer is trusted to validate off-chain eligibility.
 *   The contract independently enforces fixed amounts, caps and replay
 *   protection.
 */
contract BarterLinkTokenV2 is ERC20, ERC20Permit {

    uint256 public constant SIGNUP_BONUS = 5 ether;

    uint256 public constant DIRECT_BARTER_REWARD = 1 ether;
    uint256 public constant DAILY_STREAK_REWARD = 5 ether;
    uint256 public constant REFERRAL_REWARD = 3 ether;

    uint256 public constant MAX_DIRECT_BARTER_REWARDS = 6;
    uint256 public constant MAX_DAILY_STREAK_REWARDS = 1;
    uint256 public constant MAX_REFERRAL_REWARDS = 5;

    enum RewardCampaign {
        DIRECT_BARTER_COMPLETION,
        DAILY_ACTIVITY_STREAK_30,
        REFERRAL_QUALIFIED
    }

    // Existing signup-bonus protection.
    mapping(address => bool) public signupBonusClaimed;

    // Global protection against replaying an on-chain reward claim.
    mapping(bytes32 => bool) public rewardClaimKeyProcessed;

    // Independent lifetime caps for each recipient wallet.
    mapping(address => uint256) public directBarterRewardsClaimed;
    mapping(address => bool) public dailyStreakRewardClaimed;
    mapping(address => uint256) public referralRewardsClaimed;

    // Emergency campaign controls.
    mapping(RewardCampaign => bool) public rewardCampaignPaused;

    // One-time legacy migration controls.
    bool public legacyMigrationOpen = true;

    mapping(address => bool) public legacyWalletMigrated;
    mapping(bytes32 => bool) public legacyMigrationKeyProcessed;

    uint256 public legacyWalletMigrationCount;

    address public protocolSigner;
    address public pendingProtocolSigner;

    error SignupBonusAlreadyClaimed();
    error InvalidRecipient();
    error InvalidClaimKey();
    error InvalidMigrationKey();
    error Unauthorized();

    error LegacyMigrationStillOpen();
    error LegacyMigrationClosed();
    error LegacyWalletAlreadyMigrated();
    error LegacyMigrationKeyAlreadyUsed();
    error LegacyMigrationCountMismatch(
        uint256 expected,
        uint256 actual
    );

    error RewardClaimAlreadyProcessed();
    error RewardCampaignIsPaused();
    error DirectBarterRewardCapReached();
    error DailyStreakRewardAlreadyClaimed();
    error ReferralRewardCapReached();

    event SignupBonusMinted(
        address indexed user,
        uint256 amount
    );

    event RewardMinted(
        address indexed recipient,
        RewardCampaign indexed campaign,
        bytes32 indexed claimKey,
        uint256 amount
    );

    event RewardCampaignPauseChanged(
        RewardCampaign indexed campaign,
        bool paused
    );

    event LegacyWalletMigrated(
        address indexed legacyWallet,
        uint256 legacyBalanceMinted,
        bool signupBonusWasClaimed,
        bytes32 indexed migrationKey
    );

    event LegacyMigrationClosedEvent(
        uint256 migratedWalletCount
    );

    event ProtocolSignerTransferStarted(
        address indexed currentSigner,
        address indexed pendingSigner
    );

    event ProtocolSignerTransferred(
        address indexed previousSigner,
        address indexed newSigner
    );

    modifier onlyProtocolSigner() {
        if (msg.sender != protocolSigner) {
            revert Unauthorized();
        }

        _;
    }

    constructor(address _protocolSigner)
        ERC20("BarterLink Token", "BLT")
        ERC20Permit("BarterLink Token")
    {
        if (_protocolSigner == address(0)) {
            revert InvalidRecipient();
        }

        protocolSigner = _protocolSigner;

        // Rewards are deliberately disabled until migration is complete
        // and the backend reward processing is ready.
        rewardCampaignPaused[
            RewardCampaign.DIRECT_BARTER_COMPLETION
        ] = true;

        rewardCampaignPaused[
            RewardCampaign.DAILY_ACTIVITY_STREAK_30
        ] = true;

        rewardCampaignPaused[
            RewardCampaign.REFERRAL_QUALIFIED
        ] = true;
    }

    /**
     * Existing signup-bonus behavior.
     *
     * Signup minting is unavailable while the legacy snapshot/migration
     * window remains open, preventing signup claims from racing against
     * migration of previous signupBonusClaimed states.
     */
    function mintSignupBonus(
        address user
    )
        external
        onlyProtocolSigner
    {
        if (legacyMigrationOpen) {
            revert LegacyMigrationStillOpen();
        }

        if (user == address(0)) {
            revert InvalidRecipient();
        }

        if (signupBonusClaimed[user]) {
            revert SignupBonusAlreadyClaimed();
        }

        signupBonusClaimed[user] = true;

        _mint(user, SIGNUP_BONUS);

        emit SignupBonusMinted(
            user,
            SIGNUP_BONUS
        );
    }

    /**
     * Mint a fixed campaign reward.
     *
     * The caller cannot choose the amount.
     * The claim key must be unique across all campaigns.
     *
     * The backend must validate the source event before calling this
     * function. Examples include a successfully completed Direct Barter
     * session, a verified 30-day streak, or a qualified referral.
     */
    function mintReward(
        address recipient,
        RewardCampaign campaign,
        bytes32 claimKey
    )
        external
        onlyProtocolSigner
    {
        if (legacyMigrationOpen) {
            revert LegacyMigrationStillOpen();
        }

        if (recipient == address(0)) {
            revert InvalidRecipient();
        }

        if (claimKey == bytes32(0)) {
            revert InvalidClaimKey();
        }

        if (rewardClaimKeyProcessed[claimKey]) {
            revert RewardClaimAlreadyProcessed();
        }

        if (rewardCampaignPaused[campaign]) {
            revert RewardCampaignIsPaused();
        }

        uint256 amount;

        if (
            campaign
                == RewardCampaign.DIRECT_BARTER_COMPLETION
        ) {
            if (
                directBarterRewardsClaimed[recipient]
                    >= MAX_DIRECT_BARTER_REWARDS
            ) {
                revert DirectBarterRewardCapReached();
            }

            directBarterRewardsClaimed[recipient] += 1;

            amount = DIRECT_BARTER_REWARD;

        } else if (
            campaign
                == RewardCampaign.DAILY_ACTIVITY_STREAK_30
        ) {
            if (dailyStreakRewardClaimed[recipient]) {
                revert DailyStreakRewardAlreadyClaimed();
            }

            dailyStreakRewardClaimed[recipient] = true;

            amount = DAILY_STREAK_REWARD;

        } else if (
            campaign
                == RewardCampaign.REFERRAL_QUALIFIED
        ) {
            if (
                referralRewardsClaimed[recipient]
                    >= MAX_REFERRAL_REWARDS
            ) {
                revert ReferralRewardCapReached();
            }

            referralRewardsClaimed[recipient] += 1;

            amount = REFERRAL_REWARD;

        } else {
            // Defensive fallback for a future enum/code change.
            revert RewardCampaignIsPaused();
        }

        // Mark the claim before minting. If minting reverts, the entire
        // transaction also reverts and the claim remains unused.
        rewardClaimKeyProcessed[claimKey] = true;

        _mint(recipient, amount);

        emit RewardMinted(
            recipient,
            campaign,
            claimKey,
            amount
        );
    }

    /**
     * Pause or resume an individual reward campaign.
     *
     * All campaigns start paused. Activate them only after the matching
     * backend implementation and reconciliation path are ready.
     */
    function setRewardCampaignPaused(
        RewardCampaign campaign,
        bool paused
    )
        external
        onlyProtocolSigner
    {
        rewardCampaignPaused[campaign] = paused;

        emit RewardCampaignPauseChanged(
            campaign,
            paused
        );
    }

    /**
     * Migrate one legacy wallet's verified v1 balance and signup-bonus
     * claim state.
     *
     * legacyBalance must be the exact balance recorded in the approved
     * v1 snapshot, expressed in token base units (18 decimals).
     *
     * migrationKey must be unique and reproducible from the approved
     * migration manifest.
     *
     * This function is separate from reward minting. It does not consume
     * a reward-campaign cap or reward claim key.
     */
    function migrateLegacyWallet(
        address legacyWallet,
        uint256 legacyBalance,
        bool legacySignupBonusWasClaimed,
        bytes32 migrationKey
    )
        external
        onlyProtocolSigner
    {
        if (!legacyMigrationOpen) {
            revert LegacyMigrationClosed();
        }

        if (legacyWallet == address(0)) {
            revert InvalidRecipient();
        }

        if (migrationKey == bytes32(0)) {
            revert InvalidMigrationKey();
        }

        if (legacyWalletMigrated[legacyWallet]) {
            revert LegacyWalletAlreadyMigrated();
        }

        if (
            legacyMigrationKeyProcessed[migrationKey]
        ) {
            revert LegacyMigrationKeyAlreadyUsed();
        }

        legacyWalletMigrated[legacyWallet] = true;
        legacyMigrationKeyProcessed[migrationKey] = true;

        legacyWalletMigrationCount += 1;

        // Preserve the historical signup-bonus entitlement state.
        signupBonusClaimed[legacyWallet] =
            legacySignupBonusWasClaimed;

        // Migrate the exact snapshot balance, including zero balances.
        if (legacyBalance > 0) {
            _mint(legacyWallet, legacyBalance);
        }

        emit LegacyWalletMigrated(
            legacyWallet,
            legacyBalance,
            legacySignupBonusWasClaimed,
            migrationKey
        );
    }

    /**
     * Permanently close migration after independently verifying the
     * approved manifest count.
     *
     * This function cannot validate the manifest's contents; address,
     * balance and claim-state reconciliation must be completed off-chain
     * before the signer confirms closure.
     */
    function closeLegacyMigration(
        uint256 expectedWalletCount
    )
        external
        onlyProtocolSigner
    {
        if (!legacyMigrationOpen) {
            revert LegacyMigrationClosed();
        }

        if (
            legacyWalletMigrationCount
                != expectedWalletCount
        ) {
            revert LegacyMigrationCountMismatch(
                expectedWalletCount,
                legacyWalletMigrationCount
            );
        }

        legacyMigrationOpen = false;

        emit LegacyMigrationClosedEvent(
            legacyWalletMigrationCount
        );
    }

    /**
     * Two-step protocol signer transfer.
     */
    function transferProtocolSigner(
        address newSigner
    )
        external
        onlyProtocolSigner
    {
        if (newSigner == address(0)) {
            revert InvalidRecipient();
        }

        pendingProtocolSigner = newSigner;

        emit ProtocolSignerTransferStarted(
            protocolSigner,
            newSigner
        );
    }

    function acceptProtocolSigner()
        external
    {
        if (msg.sender != pendingProtocolSigner) {
            revert Unauthorized();
        }

        address previousSigner = protocolSigner;

        protocolSigner = msg.sender;
        pendingProtocolSigner = address(0);

        emit ProtocolSignerTransferred(
            previousSigner,
            msg.sender
        );
    }
}