import assert from "node:assert/strict";
import { describe, it, beforeEach } from "node:test";
import { parseEther } from "viem";
import { network } from "hardhat";

describe("BarterLinkToken", async function () {

  let viem: any;
  let publicClient: any;

  let protocolSigner: any;
  let newProtocolSigner: any;
  let user1: any;
  let user2: any;
  let attacker: any;

  let token: any;

  beforeEach(async function () {

    const connection = await network.create();

    viem = connection.viem;
    publicClient = await viem.getPublicClient();

    const walletClients =
      await viem.getWalletClients();

    [
      protocolSigner,
      newProtocolSigner,
      user1,
      user2,
      attacker,
    ] = walletClients;

    token = await viem.deployContract(
      "BarterLinkToken",
      [
        protocolSigner.account.address,
      ]
    );
  });

  it("should have the correct token metadata", async function () {

    assert.equal(
      await token.read.name(),
      "BarterLink Token"
    );

    assert.equal(
      await token.read.symbol(),
      "BLT"
    );

    assert.equal(
      await token.read.decimals(),
      18
    );
  });

  it("should start with zero total supply", async function () {

    const totalSupply =
      await token.read.totalSupply();

    assert.equal(
      totalSupply,
      0n
    );
  });

  it("should allow the protocol signer to mint the signup bonus", async function () {

    const txHash =
      await token.write.mintSignupBonus(
        [
          user1.account.address,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    await publicClient.waitForTransactionReceipt({
      hash: txHash,
    });

    const balance =
      await token.read.balanceOf([
        user1.account.address,
      ]);

    assert.equal(
      balance,
      parseEther("5")
    );
  });

  it("should record that the signup bonus was claimed", async function () {

    const txHash =
      await token.write.mintSignupBonus(
        [
          user1.account.address,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    await publicClient.waitForTransactionReceipt({
      hash: txHash,
    });

    const claimed =
      await token.read.signupBonusClaimed([
        user1.account.address,
      ]);

    assert.equal(
      claimed,
      true
    );
  });

  it("should reject a second signup bonus claim", async function () {

    const firstTx =
      await token.write.mintSignupBonus(
        [
          user1.account.address,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    await publicClient.waitForTransactionReceipt({
      hash: firstTx,
    });

    await assert.rejects(
      async () => {

        await token.write.mintSignupBonus(
          [
            user1.account.address,
          ],
          {
            account:
              protocolSigner.account,
          }
        );

      }
    );
  });

  it("should reject unauthorized minting", async function () {

    await assert.rejects(
      async () => {

        await token.write.mintSignupBonus(
          [
            user1.account.address,
          ],
          {
            account:
              attacker.account,
          }
        );

      }
    );
  });

  it("should reject minting to the zero address", async function () {

    const zeroAddress =
      "0x0000000000000000000000000000000000000000";

    await assert.rejects(
      async () => {

        await token.write.mintSignupBonus(
          [
            zeroAddress,
          ],
          {
            account:
              protocolSigner.account,
          }
        );

      }
    );
  });

  it("should support normal ERC-20 transfers", async function () {

    const mintTx =
      await token.write.mintSignupBonus(
        [
          user1.account.address,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    await publicClient.waitForTransactionReceipt({
      hash: mintTx,
    });

    const transferTx =
      await token.write.transfer(
        [
          user2.account.address,
          parseEther("2"),
        ],
        {
          account:
            user1.account,
        }
      );

    await publicClient.waitForTransactionReceipt({
      hash: transferTx,
    });

    const user1Balance =
      await token.read.balanceOf([
        user1.account.address,
      ]);

    const user2Balance =
      await token.read.balanceOf([
        user2.account.address,
      ]);

    assert.equal(
      user1Balance,
      parseEther("3")
    );

    assert.equal(
      user2Balance,
      parseEther("2")
    );
  });

  it("should expose ERC-2612 permit nonce functionality", async function () {

    const nonce =
      await token.read.nonces([
        user1.account.address,
      ]);

    assert.equal(
      nonce,
      0n
    );
  });

  it("should allow the protocol signer to propose a new protocol signer", async function () {

  const txHash =
    await token.write.transferProtocolSigner(
      [
        newProtocolSigner.account.address,
      ],
      {
        account:
          protocolSigner.account,
      }
    );

  await publicClient.waitForTransactionReceipt({
    hash: txHash,
  });

  const pendingSigner =
    await token.read.pendingProtocolSigner();

  assert.equal(
    pendingSigner.toLowerCase(),
    newProtocolSigner.account.address.toLowerCase()
  );
});

it("should allow the pending signer to accept protocol signer authority", async function () {

  const transferTx =
    await token.write.transferProtocolSigner(
      [
        newProtocolSigner.account.address,
      ],
      {
        account:
          protocolSigner.account,
      }
    );

  await publicClient.waitForTransactionReceipt({
    hash: transferTx,
  });

  const acceptTx =
    await token.write.acceptProtocolSigner({
      account:
        newProtocolSigner.account,
    });

  await publicClient.waitForTransactionReceipt({
    hash: acceptTx,
  });

  const currentSigner =
    await token.read.protocolSigner();

  const pendingSigner =
    await token.read.pendingProtocolSigner();

  assert.equal(
    currentSigner.toLowerCase(),
    newProtocolSigner.account.address.toLowerCase()
  );

  assert.equal(
    pendingSigner,
    "0x0000000000000000000000000000000000000000"
  );
});

it("should prevent the old protocol signer from minting after authority transfer", async function () {

  const transferTx =
    await token.write.transferProtocolSigner(
      [
        newProtocolSigner.account.address,
      ],
      {
        account:
          protocolSigner.account,
      }
    );

  await publicClient.waitForTransactionReceipt({
    hash: transferTx,
  });

  const acceptTx =
    await token.write.acceptProtocolSigner({
      account:
        newProtocolSigner.account,
    });

  await publicClient.waitForTransactionReceipt({
    hash: acceptTx,
  });

  await assert.rejects(
    async () => {

      await token.write.mintSignupBonus(
        [
          user1.account.address,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    }
  );

  const mintTx =
    await token.write.mintSignupBonus(
      [
        user1.account.address,
      ],
      {
        account:
          newProtocolSigner.account,
      }
    );

  await publicClient.waitForTransactionReceipt({
    hash: mintTx,
  });

  const balance =
    await token.read.balanceOf([
      user1.account.address,
    ]);

  assert.equal(
    balance,
    parseEther("5")
  );
});

});