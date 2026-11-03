import { network } from "hardhat";
import { formatEther, getAddress, keccak256, stringToHex } from "viem";

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

const provider =
  getAddress(
    "0xD0a8EBf20FAE75DB5A4D25d4043980624A2e8eCc"
  );

const amount =
  2000000000000000000n;

const escrowId =
  keccak256(
    stringToHex(
      "barterlink-sepolia-escrow-test-1"
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
    name: "release",
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
console.log("BarterLink Real Sepolia Escrow Release Test");
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

console.log(
  "Submitting release transaction..."
);

const txHash =
  await protocolWalletClient.writeContract({
    address: escrowAddress,
    abi: escrowAbi,
    functionName: "release",
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
  "Provider BLT After:",
  formatEther(providerBalanceAfter)
);

console.log(
  "Escrow BLT After:",
  formatEther(escrowBalanceAfter)
);

console.log("------------------------------------");

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

const providerCorrect =
  providerBalanceAfter ===
  providerBalanceBefore + amount;

const escrowCorrect =
  escrowBalanceAfter === 0n;

const metadataCorrect =
  storedEscrowAfter.provider.toLowerCase() ===
    provider.toLowerCase() &&
  storedEscrowAfter.amount === amount &&
  storedEscrowAfter.status === 2 &&
  storedEscrowAfter.resolvedAt > 0n;

if (
  receipt.status === "success" &&
  providerCorrect &&
  escrowCorrect &&
  metadataCorrect
) {
  console.log(
    "Real Sepolia Escrow Release Test: PASSED"
  );
} else {
  console.log(
    "Real Sepolia Escrow Release Test: FAILED"
  );
}

console.log("====================================");