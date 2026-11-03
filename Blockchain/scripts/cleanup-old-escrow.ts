import { network } from "hardhat";

import {
  formatEther,
  getAddress,
} from "viem";

const connection = await network.create({
  network: "sepolia",
  chainType: "l1",
});

const { viem } = connection;

const publicClient =
  await viem.getPublicClient();

const [protocolWalletClient] =
  await viem.getWalletClients();

const protocolSigner =
  protocolWalletClient.account.address;

const escrowId =
  process.env.ESCROW_ID;

if (!escrowId) {
  throw new Error(
    "ESCROW_ID environment variable is required."
  );
}

const normalizedEscrowId =
  escrowId as `0x${string}`;

const tokenAddress =
  getAddress(
    "0x0F6eB87FA4743fa52fB34890947b6B4bFCBe32fa"
  );

const escrowAddress =
  getAddress(
    "0xA4F9ACB729cac5eD67D419395FE40B0660E0376a"
  );

const tokenAbi = [
  {
    type: "function",
    name: "balanceOf",
    stateMutability: "view",
    inputs: [
      {
        name: "account",
        type: "address",
      },
    ],
    outputs: [
      {
        type: "uint256",
      },
    ],
  },
] as const;

const escrowAbi = [
  {
    type: "function",
    name: "refund",
    stateMutability: "nonpayable",
    inputs: [
      {
        name: "escrowId",
        type: "bytes32",
      },
    ],
    outputs: [],
  },
  {
    type: "function",
    name: "getEscrow",
    stateMutability: "view",
    inputs: [
      {
        name: "escrowId",
        type: "bytes32",
      },
    ],
    outputs: [
      {
        name: "",
        type: "tuple",
        components: [
          {
            name: "requester",
            type: "address",
          },
          {
            name: "provider",
            type: "address",
          },
          {
            name: "amount",
            type: "uint256",
          },
          {
            name: "agreementId",
            type: "bytes32",
          },
          {
            name: "sessionId",
            type: "bytes32",
          },
          {
            name: "status",
            type: "uint8",
          },
          {
            name: "createdAt",
            type: "uint256",
          },
          {
            name: "resolvedAt",
            type: "uint256",
          },
        ],
      },
    ],
  },
] as const;

const sleep =
  (milliseconds: number) =>
    new Promise<void>((resolve) =>
      setTimeout(resolve, milliseconds)
    );

console.log("====================================");
console.log("BarterLink Old Escrow Cleanup");
console.log("====================================");

console.log(
  "Chain ID:",
  await publicClient.getChainId()
);

console.log(
  "Protocol Signer:",
  protocolSigner
);

console.log(
  "Escrow Contract:",
  escrowAddress
);

console.log(
  "Target Escrow:",
  normalizedEscrowId
);

console.log("------------------------------------");

let storedEscrow =
  await publicClient.readContract({
    address: escrowAddress,
    abi: escrowAbi,
    functionName: "getEscrow",
    args: [normalizedEscrowId],
  });

console.log(
  "Requester:",
  storedEscrow.requester
);

console.log(
  "Provider:",
  storedEscrow.provider
);

console.log(
  "Amount:",
  formatEther(storedEscrow.amount),
  "BLT"
);

console.log(
  "Current Escrow Status:",
  storedEscrow.status
);

console.log(
  "Agreement ID:",
  storedEscrow.agreementId
);

console.log(
  "Session ID:",
  storedEscrow.sessionId
);

console.log(
  "Resolved At:",
  storedEscrow.resolvedAt.toString()
);

const requesterBalanceBefore =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "balanceOf",
    args: [storedEscrow.requester],
  });

const escrowBalanceBefore =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "balanceOf",
    args: [escrowAddress],
  });

console.log(
  "Requester BLT Before:",
  formatEther(requesterBalanceBefore)
);

console.log(
  "Escrow BLT Before:",
  formatEther(escrowBalanceBefore)
);

console.log("------------------------------------");

/*
 * Status:
 * 0 = NONE
 * 1 = LOCKED
 * 2 = RELEASED
 * 3 = REFUNDED
 */

if (storedEscrow.status === 3) {
  console.log(
    "Escrow is already REFUNDED."
  );

  console.log(
    "No blockchain transaction will be submitted."
  );

  process.exit(0);
}

if (storedEscrow.status !== 1) {
  throw new Error(
    "Escrow is not LOCKED. Refusing cleanup."
  );
}

if (escrowBalanceBefore !== storedEscrow.amount) {
  throw new Error(
    "Escrow contract balance does not match stored escrow amount. Refusing cleanup."
  );
}

console.log(
  "Escrow is LOCKED. Refund is required."
);

console.log(
  "Submitting refund transaction..."
);

let txHash:
  | `0x${string}`
  | undefined;

try {
  txHash =
    await protocolWalletClient.writeContract({
      address: escrowAddress,
      abi: escrowAbi,
      functionName: "refund",
      args: [normalizedEscrowId],
      account:
        protocolWalletClient.account,
    });

  console.log(
    "Transaction Hash:",
    txHash
  );

} catch (error) {

  const message =
    error instanceof Error
      ? error.message
      : String(error);

  if (
    message
      .toLowerCase()
      .includes("already known")
  ) {

    console.log("------------------------------------");

    console.log(
      "RPC reports: already known."
    );

    console.log(
      "A previously submitted transaction may still be pending."
    );

    console.log(
      "No second transaction will be submitted."
    );

    console.log(
      "Checking escrow state for up to 2 minutes..."
    );

    for (
      let attempt = 1;
      attempt <= 12;
      attempt++
    ) {

      await sleep(10000);

      storedEscrow =
        await publicClient.readContract({
          address: escrowAddress,
          abi: escrowAbi,
          functionName: "getEscrow",
          args: [normalizedEscrowId],
        });

      console.log(
        `Check ${attempt}/12 - escrow status:`,
        storedEscrow.status
      );

      if (storedEscrow.status === 3) {

        console.log("------------------------------------");

        console.log(
          "Existing refund transaction has completed."
        );

        console.log(
          "Escrow Status: REFUNDED"
        );

        process.exit(0);
      }
    }

    throw new Error(
      "Escrow is still LOCKED after waiting 2 minutes. A previously submitted transaction may still be pending. No additional refund was submitted."
    );
  }

  throw error;
}

if (!txHash) {
  throw new Error(
    "Refund transaction hash was not obtained."
  );
}

console.log(
  "Waiting for confirmation..."
);

const receipt =
  await publicClient.waitForTransactionReceipt({
    hash: txHash,
  });

console.log(
  "Transaction Status:",
  receipt.status
);

console.log(
  "Block Number:",
  receipt.blockNumber
);

if (receipt.status !== "success") {
  throw new Error(
    "Refund transaction was mined but did not succeed."
  );
}

console.log("------------------------------------");

const requesterBalanceAfter =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "balanceOf",
    args: [storedEscrow.requester],
  });

const escrowBalanceAfter =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "balanceOf",
    args: [escrowAddress],
  });

const storedEscrowAfter =
  await publicClient.readContract({
    address: escrowAddress,
    abi: escrowAbi,
    functionName: "getEscrow",
    args: [normalizedEscrowId],
  });

console.log(
  "Requester BLT After:",
  formatEther(requesterBalanceAfter)
);

console.log(
  "Escrow BLT After:",
  formatEther(escrowBalanceAfter)
);

console.log(
  "Escrow Status After:",
  storedEscrowAfter.status
);

console.log(
  "Resolved At:",
  storedEscrowAfter.resolvedAt.toString()
);

console.log("------------------------------------");

const requesterCorrect =
  requesterBalanceAfter ===
  requesterBalanceBefore +
    storedEscrow.amount;

const escrowCorrect =
  escrowBalanceAfter === 0n;

const statusCorrect =
  storedEscrowAfter.status === 3;

const resolvedCorrect =
  storedEscrowAfter.resolvedAt > 0n;

if (
  requesterCorrect &&
  escrowCorrect &&
  statusCorrect &&
  resolvedCorrect
) {

  console.log(
    "OLD ESCROW CLEANUP: PASSED"
  );

  console.log(
    "Blockchain escrow is now REFUNDED."
  );

} else {

  console.log(
    "OLD ESCROW CLEANUP: FAILED"
  );

  throw new Error(
    "Post-refund blockchain verification failed."
  );
}

console.log("====================================");