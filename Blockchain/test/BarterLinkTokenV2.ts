import assert from "node:assert/strict";
import { describe, it, beforeEach } from "node:test";

import {
  keccak256,
  parseEther,
  stringToHex,
} from "viem";

import { network } from "hardhat";

describe("BarterLinkTokenV2", async function () {
  let viem: any;
  let publicClient: any;

  let protocolSigner: any;
  let newProtocolSigner: any;
  let user1: any;
  let user2: any;
  let attacker: any;

  let token: any;

  const ZERO_ADDRESS =
    "0x0000000000000000000000000000000000000000";

  const DIRECT_BARTER = 0;
  const DAILY_STREAK = 1;
  const REFERRAL = 2;

  function makeKey(value: string): `0x${string}` {
    return keccak256(stringToHex(value));
  }

  async function waitForTransaction(
    hash: `0x${string}`,
  ) {
    return publicClient.waitForTransactionReceipt({ hash });
  }

  async function closeMigration(
    expectedCount: bigint = 0n,
  ) {
    const hash = await token.write.closeLegacyMigration(
      [expectedCount],
      { account: protocolSigner.account },
    );

    await waitForTransaction(hash);
  }

  async function pauseCampaign(
    campaign: number,
    paused: boolean,
  ) {
    const hash = await token.write.setRewardCampaignPaused(
      [campaign, paused],
      { account: protocolSigner.account },
    );

    await waitForTransaction(hash);
  }

  async function mintReward(
    recipient: string,
    campaign: number,
    claimKey: `0x${string}`,
  ) {
    const hash = await token.write.mintReward(
      [recipient, campaign, claimKey],
      { account: protocolSigner.account },
    );

    await waitForTransaction(hash);
  }

  beforeEach(async function () {
    const connection = await network.create();

    viem = connection.viem;
    publicClient = await viem.getPublicClient();

    const walletClients = await viem.getWalletClients();

    [
      protocolSigner,
      newProtocolSigner,
      user1,
      user2,
      attacker,
    ] = walletClients;

    token = await viem.deployContract(
      "BarterLinkTokenV2",
      [protocolSigner.account.address],
    );
  });

  it("preserves the BLT token metadata", async function () {
    assert.equal(await token.read.name(), "BarterLink Token");
    assert.equal(await token.read.symbol(), "BLT");
    assert.equal(await token.read.decimals(), 18);
    assert.equal(await token.read.totalSupply(), 0n);
  });

  it("starts migration open and reward campaigns paused", async function () {
    assert.equal(await token.read.legacyMigrationOpen(), true);

    assert.equal(
      await token.read.rewardCampaignPaused([DIRECT_BARTER]),
      true,
    );

    assert.equal(
      await token.read.rewardCampaignPaused([DAILY_STREAK]),
      true,
    );

    assert.equal(
      await token.read.rewardCampaignPaused([REFERRAL]),
      true,
    );
  });

  it("prevents signup and campaign reward minting until migration closes", async function () {
    await assert.rejects(async () => {
      await token.write.mintSignupBonus(
        [user1.account.address],
        { account: protocolSigner.account },
      );
    });

    await assert.rejects(async () => {
      await token.write.mintReward(
        [
          user1.account.address,
          DIRECT_BARTER,
          makeKey("premature-direct-reward"),
        ],
        { account: protocolSigner.account },
      );
    });
  });

  it("migrates legacy balances and signup claim state exactly once", async function () {
    const migrationKey1 = makeKey("legacy-wallet-user-1");
    const migrationKey2 = makeKey("legacy-wallet-user-2");

    let hash = await token.write.migrateLegacyWallet(
      [
        user1.account.address,
        parseEther("3"),
        true,
        migrationKey1,
      ],
      { account: protocolSigner.account },
    );

    await waitForTransaction(hash);

    hash = await token.write.migrateLegacyWallet(
      [
        user2.account.address,
        0n,
        false,
        migrationKey2,
      ],
      { account: protocolSigner.account },
    );

    await waitForTransaction(hash);

    assert.equal(
      await token.read.balanceOf([user1.account.address]),
      parseEther("3"),
    );

    assert.equal(
      await token.read.signupBonusClaimed([user1.account.address]),
      true,
    );

    assert.equal(
      await token.read.signupBonusClaimed([user2.account.address]),
      false,
    );

    assert.equal(
      await token.read.legacyWalletMigrated([user1.account.address]),
      true,
    );

    assert.equal(
      await token.read.legacyWalletMigrated([user2.account.address]),
      true,
    );

    assert.equal(
      await token.read.legacyWalletMigrationCount(),
      2n,
    );

    // The contract must reject a closure count which disagrees with
    // the actual number of migrated wallet records.
    await assert.rejects(async () => {
      await token.write.closeLegacyMigration(
        [1n],
        { account: protocolSigner.account },
      );
    });

    await closeMigration(2n);

    assert.equal(await token.read.legacyMigrationOpen(), false);

    // A migrated wallet with a prior claim cannot claim signup again.
    await assert.rejects(async () => {
      await token.write.mintSignupBonus(
        [user1.account.address],
        { account: protocolSigner.account },
      );
    });

    // A migrated wallet whose old signup bonus was unclaimed may
    // claim it after migration closes.
    hash = await token.write.mintSignupBonus(
      [user2.account.address],
      { account: protocolSigner.account },
    );

    await waitForTransaction(hash);

    assert.equal(
      await token.read.balanceOf([user2.account.address]),
      parseEther("5"),
    );

    assert.equal(
      await token.read.signupBonusClaimed([user2.account.address]),
      true,
    );
  });

  it("rejects a migrated wallet and migration key being reused", async function () {
    const key = makeKey("legacy-migration-unique-key");

    let hash = await token.write.migrateLegacyWallet(
      [
        user1.account.address,
        parseEther("1"),
        false,
        key,
      ],
      { account: protocolSigner.account },
    );

    await waitForTransaction(hash);

    await assert.rejects(async () => {
      await token.write.migrateLegacyWallet(
        [
          user1.account.address,
          parseEther("1"),
          false,
          makeKey("different-key-same-wallet"),
        ],
        { account: protocolSigner.account },
      );
    });

    await assert.rejects(async () => {
      await token.write.migrateLegacyWallet(
        [
          user2.account.address,
          parseEther("1"),
          false,
          key,
        ],
        { account: protocolSigner.account },
      );
    });
  });

  it("rejects legacy migration after migration is closed", async function () {
    await closeMigration(0n);

    await assert.rejects(async () => {
      await token.write.migrateLegacyWallet(
        [
          user1.account.address,
          parseEther("1"),
          false,
          makeKey("late-legacy-migration"),
        ],
        { account: protocolSigner.account },
      );
    });
  });

  it("mints one BLT per Direct Barter claim, up to six lifetime claims", async function () {
    await closeMigration(0n);
    await pauseCampaign(DIRECT_BARTER, false);

    for (let index = 1; index <= 6; index += 1) {
      await mintReward(
        user1.account.address,
        DIRECT_BARTER,
        makeKey(`direct-barter-claim-${index}`),
      );
    }

    assert.equal(
      await token.read.directBarterRewardsClaimed([
        user1.account.address,
      ]),
      6n,
    );

    assert.equal(
      await token.read.balanceOf([user1.account.address]),
      parseEther("6"),
    );

    await assert.rejects(async () => {
      await mintReward(
        user1.account.address,
        DIRECT_BARTER,
        makeKey("direct-barter-claim-7"),
      );
    });
  });

  it("mints the daily streak reward once per wallet lifetime", async function () {
    await closeMigration(0n);
    await pauseCampaign(DAILY_STREAK, false);

    await mintReward(
      user1.account.address,
      DAILY_STREAK,
      makeKey("streak-claim-first"),
    );

    assert.equal(
      await token.read.dailyStreakRewardClaimed([
        user1.account.address,
      ]),
      true,
    );

    assert.equal(
      await token.read.balanceOf([user1.account.address]),
      parseEther("5"),
    );

    await assert.rejects(async () => {
      await mintReward(
        user1.account.address,
        DAILY_STREAK,
        makeKey("streak-claim-second"),
      );
    });
  });

  it("mints three BLT per qualified referral, up to five rewards", async function () {
    await closeMigration(0n);
    await pauseCampaign(REFERRAL, false);

    for (let index = 1; index <= 5; index += 1) {
      await mintReward(
        user1.account.address,
        REFERRAL,
        makeKey(`referral-claim-${index}`),
      );
    }

    assert.equal(
      await token.read.referralRewardsClaimed([
        user1.account.address,
      ]),
      5n,
    );

    assert.equal(
      await token.read.balanceOf([user1.account.address]),
      parseEther("15"),
    );

    await assert.rejects(async () => {
      await mintReward(
        user1.account.address,
        REFERRAL,
        makeKey("referral-claim-6"),
      );
    });
  });

  it("rejects replaying a reward claim key across campaigns", async function () {
    await closeMigration(0n);

    await pauseCampaign(DIRECT_BARTER, false);
    await pauseCampaign(DAILY_STREAK, false);

    const sharedKey = makeKey("globally-unique-reward-claim");

    await mintReward(
      user1.account.address,
      DIRECT_BARTER,
      sharedKey,
    );

    await assert.rejects(async () => {
      await mintReward(
        user2.account.address,
        DAILY_STREAK,
        sharedKey,
      );
    });
  });

  it("rejects unauthorized campaign reward minting", async function () {
    await closeMigration(0n);
    await pauseCampaign(DIRECT_BARTER, false);

    await assert.rejects(async () => {
      await token.write.mintReward(
        [
          user1.account.address,
          DIRECT_BARTER,
          makeKey("attacker-claim"),
        ],
        { account: attacker.account },
      );
    });
  });

  it("rejects a zero recipient and a zero claim key", async function () {
    await closeMigration(0n);
    await pauseCampaign(DIRECT_BARTER, false);

    await assert.rejects(async () => {
      await mintReward(
        ZERO_ADDRESS,
        DIRECT_BARTER,
        makeKey("zero-recipient-claim"),
      );
    });

    await assert.rejects(async () => {
      await mintReward(
        user1.account.address,
        DIRECT_BARTER,
        "0x0000000000000000000000000000000000000000000000000000000000000000",
      );
    });
  });

  it("allows only the protocol signer to pause a reward campaign", async function () {
    await assert.rejects(async () => {
      await token.write.setRewardCampaignPaused(
        [DIRECT_BARTER, false],
        { account: attacker.account },
      );
    });

    await pauseCampaign(DIRECT_BARTER, false);

    assert.equal(
      await token.read.rewardCampaignPaused([DIRECT_BARTER]),
      false,
    );
  });

  it("preserves the two-step protocol signer transfer", async function () {
    let hash = await token.write.transferProtocolSigner(
      [newProtocolSigner.account.address],
      { account: protocolSigner.account },
    );

    await waitForTransaction(hash);

    assert.equal(
      (
        await token.read.pendingProtocolSigner()
      ).toLowerCase(),
      newProtocolSigner.account.address.toLowerCase(),
    );

    hash = await token.write.acceptProtocolSigner({
      account: newProtocolSigner.account,
    });

    await waitForTransaction(hash);

    assert.equal(
      (
        await token.read.protocolSigner()
      ).toLowerCase(),
      newProtocolSigner.account.address.toLowerCase(),
    );

    assert.equal(
      await token.read.pendingProtocolSigner(),
      ZERO_ADDRESS,
    );
  });
});