import assert from "node:assert/strict";
import { describe, it, beforeEach } from "node:test";
import {
  keccak256,
  parseEther,
  stringToHex,
} from "viem";
import { network } from "hardhat";

describe("BarterLinkEscrow", async function () {

  let viem: any;
  let publicClient: any;

    let protocolSigner: any;
    let newProtocolSigner: any;
    let user1: any;
    let user2: any;
    let attacker: any;

  let token: any;
  let escrow: any;

  const ZERO_ADDRESS =
    "0x0000000000000000000000000000000000000000";

  const ESCROW_ID =
    keccak256(stringToHex("escrow-1"));

  const AGREEMENT_ID =
    keccak256(stringToHex("agreement-1"));

  const SESSION_ID =
    keccak256(stringToHex("session-1"));

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

    escrow = await viem.deployContract(
      "BarterLinkEscrow",
      [
        token.address,
      ]
    );

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
  });

  async function createPermitSignature(
    owner: any,
    spender: string,
    amount: bigint,
    deadline: bigint,
  ) {

    const nonce =
      await token.read.nonces([
        owner.account.address,
      ]);

    const chainId =
      await publicClient.getChainId();

    const signature =
      await owner.signTypedData({
        account: owner.account,

        domain: {
          name: "BarterLink Token",
          version: "1",
          chainId,
          verifyingContract: token.address,
        },

        types: {
          Permit: [
            {
              name: "owner",
              type: "address",
            },
            {
              name: "spender",
              type: "address",
            },
            {
              name: "value",
              type: "uint256",
            },
            {
              name: "nonce",
              type: "uint256",
            },
            {
              name: "deadline",
              type: "uint256",
            },
          ],
        },

        primaryType: "Permit",

        message: {
          owner:
            owner.account.address,

          spender,

          value:
            amount,

          nonce,

          deadline,
        },
      });

    const r =
      `0x${signature.slice(2, 66)}`;

    const s =
      `0x${signature.slice(66, 130)}`;

    const v =
      parseInt(
        signature.slice(130, 132),
        16
      );

    return {
      v,
      r,
      s,
      deadline,
    };
  }

  it("should reject deployment with a zero token address", async function () {

    await assert.rejects(
      async () => {

        await viem.deployContract(
          "BarterLinkEscrow",
          [
            ZERO_ADDRESS,
          ]
        );

      }
    );
  });

  it("should lock BLT using ERC-2612 permit", async function () {

    const amount =
      parseEther("2");

    const deadline =
      BigInt(
        Math.floor(
          Date.now() / 1000
        ) + 3600
      );

    const permit =
      await createPermitSignature(
        user1,
        escrow.address,
        amount,
        deadline
      );

    const txHash =
      await escrow.write.lockWithPermit(
        [
          ESCROW_ID,
          user1.account.address,
          user2.account.address,
          amount,
          AGREEMENT_ID,
          SESSION_ID,
          permit.deadline,
          permit.v,
          permit.r,
          permit.s,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    await publicClient.waitForTransactionReceipt({
      hash: txHash,
    });

    const userBalance =
      await token.read.balanceOf([
        user1.account.address,
      ]);

    const escrowBalance =
      await token.read.balanceOf([
        escrow.address,
      ]);

    const storedEscrow =
      await escrow.read.getEscrow([
        ESCROW_ID,
      ]);

    assert.equal(
      userBalance,
      parseEther("3")
    );

    assert.equal(
      escrowBalance,
      parseEther("2")
    );

    assert.equal(
      storedEscrow.requester.toLowerCase(),
      user1.account.address.toLowerCase()
    );

    assert.equal(
      storedEscrow.provider.toLowerCase(),
      user2.account.address.toLowerCase()
    );

    assert.equal(
      storedEscrow.amount,
      amount
    );

    assert.equal(
      storedEscrow.agreementId,
      AGREEMENT_ID
    );

    assert.equal(
      storedEscrow.sessionId,
      SESSION_ID
    );

    assert.equal(
      storedEscrow.status,
      1
    );
  });

  it("should reject duplicate escrow creation", async function () {

    const amount =
      parseEther("2");

    const deadline =
      BigInt(
        Math.floor(
          Date.now() / 1000
        ) + 3600
      );

    const firstPermit =
      await createPermitSignature(
        user1,
        escrow.address,
        amount,
        deadline
      );

    const firstTx =
      await escrow.write.lockWithPermit(
        [
          ESCROW_ID,
          user1.account.address,
          user2.account.address,
          amount,
          AGREEMENT_ID,
          SESSION_ID,
          firstPermit.deadline,
          firstPermit.v,
          firstPermit.r,
          firstPermit.s,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    await publicClient.waitForTransactionReceipt({
      hash: firstTx,
    });

    const secondDeadline =
      BigInt(
        Math.floor(
          Date.now() / 1000
        ) + 3600
      );

    const secondPermit =
      await createPermitSignature(
        user1,
        escrow.address,
        amount,
        secondDeadline
      );

    await assert.rejects(
      async () => {

        await escrow.write.lockWithPermit(
          [
            ESCROW_ID,
            user1.account.address,
            user2.account.address,
            amount,
            AGREEMENT_ID,
            SESSION_ID,
            secondPermit.deadline,
            secondPermit.v,
            secondPermit.r,
            secondPermit.s,
          ],
          {
            account:
              protocolSigner.account,
          }
        );

      }
    );
  });

  it("should reject unauthorized escrow locking", async function () {

    const amount =
      parseEther("2");

    const deadline =
      BigInt(
        Math.floor(
          Date.now() / 1000
        ) + 3600
      );

    const permit =
      await createPermitSignature(
        user1,
        escrow.address,
        amount,
        deadline
      );

    await assert.rejects(
      async () => {

        await escrow.write.lockWithPermit(
          [
            ESCROW_ID,
            user1.account.address,
            user2.account.address,
            amount,
            AGREEMENT_ID,
            SESSION_ID,
            permit.deadline,
            permit.v,
            permit.r,
            permit.s,
          ],
          {
            account:
              attacker.account,
          }
        );

      }
    );
  });

  it("should release locked BLT to the provider", async function () {

    const amount =
      parseEther("2");

    const deadline =
      BigInt(
        Math.floor(
          Date.now() / 1000
        ) + 3600
      );

    const permit =
      await createPermitSignature(
        user1,
        escrow.address,
        amount,
        deadline
      );

    const lockTx =
      await escrow.write.lockWithPermit(
        [
          ESCROW_ID,
          user1.account.address,
          user2.account.address,
          amount,
          AGREEMENT_ID,
          SESSION_ID,
          permit.deadline,
          permit.v,
          permit.r,
          permit.s,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    await publicClient.waitForTransactionReceipt({
      hash: lockTx,
    });

    const releaseTx =
      await escrow.write.release(
        [
          ESCROW_ID,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    await publicClient.waitForTransactionReceipt({
      hash: releaseTx,
    });

    const providerBalance =
      await token.read.balanceOf([
        user2.account.address,
      ]);

    const escrowBalance =
      await token.read.balanceOf([
        escrow.address,
      ]);

    const storedEscrow =
      await escrow.read.getEscrow([
        ESCROW_ID,
      ]);

    assert.equal(
      providerBalance,
      amount
    );

    assert.equal(
      escrowBalance,
      0n
    );

    assert.equal(
      storedEscrow.status,
      2
    );
  });

  it("should refund locked BLT to the requester", async function () {

    const amount =
      parseEther("2");

    const deadline =
      BigInt(
        Math.floor(
          Date.now() / 1000
        ) + 3600
      );

    const permit =
      await createPermitSignature(
        user1,
        escrow.address,
        amount,
        deadline
      );

    const lockTx =
      await escrow.write.lockWithPermit(
        [
          ESCROW_ID,
          user1.account.address,
          user2.account.address,
          amount,
          AGREEMENT_ID,
          SESSION_ID,
          permit.deadline,
          permit.v,
          permit.r,
          permit.s,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    await publicClient.waitForTransactionReceipt({
      hash: lockTx,
    });

    const refundTx =
      await escrow.write.refund(
        [
          ESCROW_ID,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    await publicClient.waitForTransactionReceipt({
      hash: refundTx,
    });

    const requesterBalance =
      await token.read.balanceOf([
        user1.account.address,
      ]);

    const escrowBalance =
      await token.read.balanceOf([
        escrow.address,
      ]);

    const storedEscrow =
      await escrow.read.getEscrow([
        ESCROW_ID,
      ]);

    assert.equal(
      requesterBalance,
      parseEther("5")
    );

    assert.equal(
      escrowBalance,
      0n
    );

    assert.equal(
      storedEscrow.status,
      3
    );
  });

  it("should reject releasing an already released escrow", async function () {

    const amount =
      parseEther("2");

    const deadline =
      BigInt(
        Math.floor(
          Date.now() / 1000
        ) + 3600
      );

    const permit =
      await createPermitSignature(
        user1,
        escrow.address,
        amount,
        deadline
      );

    const lockTx =
      await escrow.write.lockWithPermit(
        [
          ESCROW_ID,
          user1.account.address,
          user2.account.address,
          amount,
          AGREEMENT_ID,
          SESSION_ID,
          permit.deadline,
          permit.v,
          permit.r,
          permit.s,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    await publicClient.waitForTransactionReceipt({
      hash: lockTx,
    });

    const releaseTx =
      await escrow.write.release(
        [
          ESCROW_ID,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    await publicClient.waitForTransactionReceipt({
      hash: releaseTx,
    });

    await assert.rejects(
      async () => {

        await escrow.write.release(
          [
            ESCROW_ID,
          ],
          {
            account:
              protocolSigner.account,
          }
        );

      }
    );
  });

  it("should reject refunding an already released escrow", async function () {

    const amount =
      parseEther("2");

    const deadline =
      BigInt(
        Math.floor(
          Date.now() / 1000
        ) + 3600
      );

    const permit =
      await createPermitSignature(
        user1,
        escrow.address,
        amount,
        deadline
      );

    const lockTx =
      await escrow.write.lockWithPermit(
        [
          ESCROW_ID,
          user1.account.address,
          user2.account.address,
          amount,
          AGREEMENT_ID,
          SESSION_ID,
          permit.deadline,
          permit.v,
          permit.r,
          permit.s,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    await publicClient.waitForTransactionReceipt({
      hash: lockTx,
    });

    const releaseTx =
      await escrow.write.release(
        [
          ESCROW_ID,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    await publicClient.waitForTransactionReceipt({
      hash: releaseTx,
    });

    await assert.rejects(
      async () => {

        await escrow.write.refund(
          [
            ESCROW_ID,
          ],
          {
            account:
              protocolSigner.account,
          }
        );

      }
    );
  });

  it("should reject release and refund by an unauthorized caller", async function () {

    const amount =
      parseEther("2");

    const deadline =
      BigInt(
        Math.floor(
          Date.now() / 1000
        ) + 3600
      );

    const permit =
      await createPermitSignature(
        user1,
        escrow.address,
        amount,
        deadline
      );

    const lockTx =
      await escrow.write.lockWithPermit(
        [
          ESCROW_ID,
          user1.account.address,
          user2.account.address,
          amount,
          AGREEMENT_ID,
          SESSION_ID,
          permit.deadline,
          permit.v,
          permit.r,
          permit.s,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    await publicClient.waitForTransactionReceipt({
      hash: lockTx,
    });

    await assert.rejects(
      async () => {

        await escrow.write.release(
          [
            ESCROW_ID,
          ],
          {
            account:
              attacker.account,
          }
        );

      }
    );

    await assert.rejects(
      async () => {

        await escrow.write.refund(
          [
            ESCROW_ID,
          ],
          {
            account:
              attacker.account,
          }
        );

      }
    );
  });

  it("should reject a zero escrow ID", async function () {

  const amount =
    parseEther("2");

  const deadline =
    BigInt(
      Math.floor(
        Date.now() / 1000
      ) + 3600
    );

  const permit =
    await createPermitSignature(
      user1,
      escrow.address,
      amount,
      deadline
    );

  const zeroEscrowId =
    "0x0000000000000000000000000000000000000000000000000000000000000000";

  await assert.rejects(
    async () => {

      await escrow.write.lockWithPermit(
        [
          zeroEscrowId,
          user1.account.address,
          user2.account.address,
          amount,
          AGREEMENT_ID,
          SESSION_ID,
          permit.deadline,
          permit.v,
          permit.r,
          permit.s,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    }
  );
});

it("should reject a zero requester", async function () {

  const amount =
    parseEther("2");

  const deadline =
    BigInt(
      Math.floor(
        Date.now() / 1000
      ) + 3600
    );

  const permit =
    await createPermitSignature(
      user1,
      escrow.address,
      amount,
      deadline
    );

  await assert.rejects(
    async () => {

      await escrow.write.lockWithPermit(
        [
          ESCROW_ID,
          "0x0000000000000000000000000000000000000000",
          user2.account.address,
          amount,
          AGREEMENT_ID,
          SESSION_ID,
          permit.deadline,
          permit.v,
          permit.r,
          permit.s,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    }
  );
});

it("should reject a zero provider", async function () {

  const amount =
    parseEther("2");

  const deadline =
    BigInt(
      Math.floor(
        Date.now() / 1000
      ) + 3600
    );

  const permit =
    await createPermitSignature(
      user1,
      escrow.address,
      amount,
      deadline
    );

  await assert.rejects(
    async () => {

      await escrow.write.lockWithPermit(
        [
          ESCROW_ID,
          user1.account.address,
          "0x0000000000000000000000000000000000000000",
          amount,
          AGREEMENT_ID,
          SESSION_ID,
          permit.deadline,
          permit.v,
          permit.r,
          permit.s,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    }
  );
});

it("should reject a zero escrow amount", async function () {

  const amount =
    0n;

  const deadline =
    BigInt(
      Math.floor(
        Date.now() / 1000
      ) + 3600
    );

  const permit =
    await createPermitSignature(
      user1,
      escrow.address,
      amount,
      deadline
    );

  await assert.rejects(
    async () => {

      await escrow.write.lockWithPermit(
        [
          ESCROW_ID,
          user1.account.address,
          user2.account.address,
          amount,
          AGREEMENT_ID,
          SESSION_ID,
          permit.deadline,
          permit.v,
          permit.r,
          permit.s,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    }
  );
});

it("should reject an expired permit deadline", async function () {

  const amount =
    parseEther("2");

  const expiredDeadline =
    1n;

  const permit =
    await createPermitSignature(
      user1,
      escrow.address,
      amount,
      expiredDeadline
    );

  await assert.rejects(
    async () => {

      await escrow.write.lockWithPermit(
        [
          ESCROW_ID,
          user1.account.address,
          user2.account.address,
          amount,
          AGREEMENT_ID,
          SESSION_ID,
          permit.deadline,
          permit.v,
          permit.r,
          permit.s,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    }
  );
});

it("should reject escrow locking when the requester has insufficient BLT", async function () {

  const amount =
    parseEther("6");

  const deadline =
    BigInt(
      Math.floor(
        Date.now() / 1000
      ) + 3600
    );

  const permit =
    await createPermitSignature(
      user1,
      escrow.address,
      amount,
      deadline
    );

  await assert.rejects(
    async () => {

      await escrow.write.lockWithPermit(
        [
          ESCROW_ID,
          user1.account.address,
          user2.account.address,
          amount,
          AGREEMENT_ID,
          SESSION_ID,
          permit.deadline,
          permit.v,
          permit.r,
          permit.s,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    }
  );
});

it("should reject an invalid permit signature", async function () {

  const amount =
    parseEther("2");

  const deadline =
    BigInt(
      Math.floor(
        Date.now() / 1000
      ) + 3600
    );

  const invalidPermit =
    await createPermitSignature(
      attacker,
      escrow.address,
      amount,
      deadline
    );

  await assert.rejects(
    async () => {

      await escrow.write.lockWithPermit(
        [
          ESCROW_ID,
          user1.account.address,
          user2.account.address,
          amount,
          AGREEMENT_ID,
          SESSION_ID,
          invalidPermit.deadline,
          invalidPermit.v,
          invalidPermit.r,
          invalidPermit.s,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    }
  );
});

it("should reject refunding an already refunded escrow", async function () {

  const amount =
    parseEther("2");

  const deadline =
    BigInt(
      Math.floor(
        Date.now() / 1000
      ) + 3600
    );

  const permit =
    await createPermitSignature(
      user1,
      escrow.address,
      amount,
      deadline
    );

  const lockTx =
    await escrow.write.lockWithPermit(
      [
        ESCROW_ID,
        user1.account.address,
        user2.account.address,
        amount,
        AGREEMENT_ID,
        SESSION_ID,
        permit.deadline,
        permit.v,
        permit.r,
        permit.s,
      ],
      {
        account:
          protocolSigner.account,
      }
    );

  await publicClient.waitForTransactionReceipt({
    hash: lockTx,
  });

  const refundTx =
    await escrow.write.refund(
      [
        ESCROW_ID,
      ],
      {
        account:
          protocolSigner.account,
      }
    );

  await publicClient.waitForTransactionReceipt({
    hash: refundTx,
  });

  await assert.rejects(
    async () => {

      await escrow.write.refund(
        [
          ESCROW_ID,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    }
  );
});

it("should reject releasing an already refunded escrow", async function () {

  const amount =
    parseEther("2");

  const deadline =
    BigInt(
      Math.floor(
        Date.now() / 1000
      ) + 3600
    );

  const permit =
    await createPermitSignature(
      user1,
      escrow.address,
      amount,
      deadline
    );

  const lockTx =
    await escrow.write.lockWithPermit(
      [
        ESCROW_ID,
        user1.account.address,
        user2.account.address,
        amount,
        AGREEMENT_ID,
        SESSION_ID,
        permit.deadline,
        permit.v,
        permit.r,
        permit.s,
      ],
      {
        account:
          protocolSigner.account,
      }
    );

  await publicClient.waitForTransactionReceipt({
    hash: lockTx,
  });

  const refundTx =
    await escrow.write.refund(
      [
        ESCROW_ID,
      ],
      {
        account:
          protocolSigner.account,
      }
    );

  await publicClient.waitForTransactionReceipt({
    hash: refundTx,
  });

  await assert.rejects(
    async () => {

      await escrow.write.release(
        [
          ESCROW_ID,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    }
  );
});

it("should follow the protocol signer defined by the BLT token", async function () {

  const amount =
    parseEther("2");

  const deadline =
    BigInt(
      Math.floor(
        Date.now() / 1000
      ) + 3600
    );

  const permit =
    await createPermitSignature(
      user1,
      escrow.address,
      amount,
      deadline
    );

  const lockTx =
    await escrow.write.lockWithPermit(
      [
        ESCROW_ID,
        user1.account.address,
        user2.account.address,
        amount,
        AGREEMENT_ID,
        SESSION_ID,
        permit.deadline,
        permit.v,
        permit.r,
        permit.s,
      ],
      {
        account:
          protocolSigner.account,
      }
    );

  await publicClient.waitForTransactionReceipt({
    hash: lockTx,
  });

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

      await escrow.write.release(
        [
          ESCROW_ID,
        ],
        {
          account:
            protocolSigner.account,
        }
      );

    }
  );

  const releaseTx =
    await escrow.write.release(
      [
        ESCROW_ID,
      ],
      {
        account:
          newProtocolSigner.account,
      }
    );

  await publicClient.waitForTransactionReceipt({
    hash: releaseTx,
  });

  const providerBalance =
    await token.read.balanceOf([
      user2.account.address,
    ]);

  assert.equal(
    providerBalance,
    amount
  );
});



});