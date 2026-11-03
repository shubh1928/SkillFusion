import { network } from "hardhat";
import {
  formatEther,
  getAddress,
  keccak256,
  parseEther,
  stringToHex,
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

const tokenAddress =
  getAddress(
    "0x0F6eB87FA4743fa52fB34890947b6B4bFCBe32fa"
  );

const escrowAddress =
  getAddress(
    "0xA4F9ACB729cac5eD67D419395FE40B0660E0376a"
  );

const requester =
  getAddress(
    "0xB6f48178BCDa2f807675948eB6675e363eEA46BC"
  );

const provider =
  getAddress(
    "0xD0a8EBf20FAE75DB5A4D25d4043980624A2e8eCc"
  );

const amount =
  parseEther("2");

const escrowId =
  keccak256(
    stringToHex(
      "barterlink-sepolia-escrow-test-2"
    )
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

console.log("====================================");
console.log("BarterLink Real Sepolia Escrow Refund Test");
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
  "Requester:",
  requester
);

console.log(
  "Provider:",
  provider
);

console.log(
  "BLT Contract:",
  tokenAddress
);

console.log(
  "Escrow Contract:",
  escrowAddress
);

console.log(
  "Escrow Amount:",
  formatEther(amount),
  "BLT"
);

console.log("------------------------------------");

const requesterBalanceBefore =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "balanceOf",
    args: [requester],
  });

const providerBalanceBefore =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "balanceOf",
    args: [provider],
  });

const escrowBalanceBefore =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "balanceOf",
    args: [escrowAddress],
  });

const storedEscrowBefore =
  await publicClient.readContract({
    address: escrowAddress,
    abi: escrowAbi,
    functionName: "getEscrow",
    args: [escrowId],
  });

console.log(
  "Requester BLT Before:",
  formatEther(requesterBalanceBefore)
);

console.log(
  "Provider BLT Before:",
  formatEther(providerBalanceBefore)
);

console.log(
  "Escrow BLT Before:",
  formatEther(escrowBalanceBefore)
);

console.log(
  "Stored Escrow Status Before:",
  storedEscrowBefore.status
);

console.log("------------------------------------");

if (storedEscrowBefore.status !== 1) {
  throw new Error(
    "Escrow is not in LOCKED status. Refusing to continue."
  );
}

if (escrowBalanceBefore !== amount) {
  throw new Error(
    "Escrow does not hold the expected 2 BLT. Refusing to continue."
  );
}

if (
  storedEscrowBefore.requester.toLowerCase() !==
  requester.toLowerCase()
) {
  throw new Error(
    "Stored escrow requester does not match expected requester."
  );
}

console.log(
  "Submitting refund transaction..."
);

const txHash =
  await protocolWalletClient.writeContract({
    address: escrowAddress,
    abi: escrowAbi,
    functionName: "refund",
    args: [escrowId],
    account:
      protocolWalletClient.account,
  });

console.log(
  "Transaction Hash:",
  txHash
);

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

console.log("------------------------------------");

const requesterBalanceAfter =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "balanceOf",
    args: [requester],
  });

const providerBalanceAfter =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "balanceOf",
    args: [provider],
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
    args: [escrowId],
  });

console.log(
  "Requester BLT After:",
  formatEther(requesterBalanceAfter)
);

console.log(
  "Provider BLT After:",
  formatEther(providerBalanceAfter)
);

console.log(
  "Escrow BLT After:",
  formatEther(escrowBalanceAfter)
);

console.log("------------------------------------");

console.log(
  "Stored Escrow Requester:",
  storedEscrowAfter.requester
);

console.log(
  "Stored Escrow Provider:",
  storedEscrowAfter.provider
);

console.log(
  "Stored Escrow Amount:",
  formatEther(storedEscrowAfter.amount),
  "BLT"
);

console.log(
  "Stored Escrow Status:",
  storedEscrowAfter.status
);

console.log(
  "Resolved At:",
  storedEscrowAfter.resolvedAt.toString()
);

console.log("------------------------------------");

const requesterCorrect =
  requesterBalanceAfter ===
  requesterBalanceBefore + amount;

const providerCorrect =
  providerBalanceAfter ===
  providerBalanceBefore;

const escrowCorrect =
  escrowBalanceAfter === 0n;

const metadataCorrect =
  storedEscrowAfter.requester.toLowerCase() ===
    requester.toLowerCase() &&
  storedEscrowAfter.provider.toLowerCase() ===
    provider.toLowerCase() &&
  storedEscrowAfter.amount === amount &&
  storedEscrowAfter.status === 3 &&
  storedEscrowAfter.resolvedAt > 0n;

if (
  receipt.status === "success" &&
  requesterCorrect &&
  providerCorrect &&
  escrowCorrect &&
  metadataCorrect
) {
  console.log(
    "Real Sepolia Escrow Refund Test: PASSED"
  );
} else {
  console.log(
    "Real Sepolia Escrow Refund Test: FAILED"
  );
}

console.log("====================================");