import { network } from "hardhat";
import {
  formatEther,
  getAddress,
  keccak256,
  parseEther,
  stringToHex,
  type Hex,
} from "viem";

const connection = await network.create({
  network: "sepolia",
  chainType: "l1",
});

const { viem } = connection;

const publicClient =
  await viem.getPublicClient();

const [protocolWalletClient, requesterWalletClient] =
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

const agreementId =
  keccak256(
    stringToHex(
      "barterlink-sepolia-agreement-test-2"
    )
  );

const sessionId =
  keccak256(
    stringToHex(
      "barterlink-sepolia-session-test-2"
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
  {
    type: "function",
    name: "nonces",
    stateMutability: "view",
    inputs: [
      {
        name: "owner",
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
    name: "lockWithPermit",
    stateMutability: "nonpayable",
    inputs: [
      {
        name: "escrowId",
        type: "bytes32",
      },
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
        name: "deadline",
        type: "uint256",
      },
      {
        name: "v",
        type: "uint8",
      },
      {
        name: "r",
        type: "bytes32",
      },
      {
        name: "s",
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
console.log("BarterLink Real Sepolia Escrow Lock Test");
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

const nonce =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "nonces",
    args: [requester],
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
  "Requester Permit Nonce:",
  nonce.toString()
);

if (requesterBalanceBefore < amount) {
  throw new Error(
    "Requester does not have enough BLT."
  );
}

if (provider === requester) {
  throw new Error(
    "Requester and provider must be different addresses."
  );
}

if (escrowBalanceBefore !== 0n) {
  throw new Error(
    "Escrow contract already holds BLT. Refusing to continue."
  );
}

console.log("------------------------------------");

const chainId =
  await publicClient.getChainId();

const deadline =
  BigInt(
    Math.floor(
      Date.now() / 1000
    ) + 3600
  );

console.log(
  "Permit Deadline:",
  deadline.toString()
);

console.log(
  "Creating ERC-2612 Permit signature..."
);

const signature =
  await requesterWalletClient.signTypedData({
    account:
      requesterWalletClient.account,

    domain: {
      name: "BarterLink Token",
      version: "1",
      chainId,
      verifyingContract: tokenAddress,
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
      owner: requester,
      spender: escrowAddress,
      value: amount,
      nonce,
      deadline,
    },
  });

const r =
  `0x${signature.slice(2, 66)}` as Hex;

const s =
  `0x${signature.slice(66, 130)}` as Hex;

const v =
  parseInt(
    signature.slice(130, 132),
    16
  );

console.log(
  "Permit signature created successfully."
);

console.log("------------------------------------");
console.log(
  "Submitting lockWithPermit transaction..."
);

const txHash =
  await protocolWalletClient.writeContract({
    address: escrowAddress,
    abi: escrowAbi,
    functionName: "lockWithPermit",
    args: [
      escrowId,
      requester,
      provider,
      amount,
      agreementId,
      sessionId,
      deadline,
      v,
      r,
      s,
    ],
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

const storedEscrow =
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
  storedEscrow.requester
);

console.log(
  "Stored Escrow Provider:",
  storedEscrow.provider
);

console.log(
  "Stored Escrow Amount:",
  formatEther(storedEscrow.amount),
  "BLT"
);

console.log(
  "Stored Agreement ID:",
  storedEscrow.agreementId
);

console.log(
  "Stored Session ID:",
  storedEscrow.sessionId
);

console.log(
  "Stored Escrow Status:",
  storedEscrow.status
);

console.log(
  "Created At:",
  storedEscrow.createdAt.toString()
);

console.log(
  "Resolved At:",
  storedEscrow.resolvedAt.toString()
);

console.log("------------------------------------");

const requesterCorrect =
  requesterBalanceAfter ===
  requesterBalanceBefore - amount;

const providerCorrect =
  providerBalanceAfter ===
  providerBalanceBefore;

const escrowCorrect =
  escrowBalanceAfter === amount;

const metadataCorrect =
  storedEscrow.requester.toLowerCase() ===
    requester.toLowerCase() &&
  storedEscrow.provider.toLowerCase() ===
    provider.toLowerCase() &&
  storedEscrow.amount === amount &&
  storedEscrow.agreementId ===
    agreementId &&
  storedEscrow.sessionId ===
    sessionId &&
  storedEscrow.status === 1;

if (
  receipt.status === "success" &&
  requesterCorrect &&
  providerCorrect &&
  escrowCorrect &&
  metadataCorrect
) {
  console.log(
    "Real Sepolia Escrow Lock Test: PASSED"
  );
} else {
  console.log(
    "Real Sepolia Escrow Lock Test: FAILED"
  );
}

console.log("====================================");