import { network } from "hardhat";
import { formatEther, getAddress } from "viem";

const connection = await network.create({
  network: "sepolia",
  chainType: "l1",
});

const { viem } = connection;

const publicClient =
  await viem.getPublicClient();

const [walletClient] =
  await viem.getWalletClients();

const protocolSigner =
  walletClient.account.address;

const tokenAddress =
  getAddress(
    "0x0F6eB87FA4743fa52fB34890947b6B4bFCBe32fa"
  );

const testUser =
  getAddress(
    "0xB6f48178BCDa2f807675948eB6675e363eEA46BC"
  );

const tokenAbi = [
  {
    type: "function",
    name: "mintSignupBonus",
    stateMutability: "nonpayable",
    inputs: [
      {
        name: "user",
        type: "address",
      },
    ],
    outputs: [],
  },
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
    name: "totalSupply",
    stateMutability: "view",
    inputs: [],
    outputs: [
      {
        type: "uint256",
      },
    ],
  },
  {
    type: "function",
    name: "signupBonusClaimed",
    stateMutability: "view",
    inputs: [
      {
        name: "user",
        type: "address",
      },
    ],
    outputs: [
      {
        type: "bool",
      },
    ],
  },
  {
    type: "function",
    name: "SIGNUP_BONUS",
    stateMutability: "view",
    inputs: [],
    outputs: [
      {
        type: "uint256",
      },
    ],
  },
] as const;

console.log("====================================");
console.log("BarterLink Sepolia Signup Bonus Test");
console.log("====================================");

const chainId =
  await publicClient.getChainId();

console.log(
  "Chain ID:",
  chainId
);

console.log(
  "Protocol Signer:",
  protocolSigner
);

console.log(
  "Test User:",
  testUser
);

console.log(
  "BLT Contract:",
  tokenAddress
);

console.log("------------------------------------");

const bonusAmount =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "SIGNUP_BONUS",
  });

const alreadyClaimed =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "signupBonusClaimed",
    args: [testUser],
  });

const balanceBefore =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "balanceOf",
    args: [testUser],
  });

const supplyBefore =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "totalSupply",
  });

console.log(
  "Signup Bonus:",
  formatEther(bonusAmount),
  "BLT"
);

console.log(
  "Already Claimed:",
  alreadyClaimed
);

console.log(
  "Test User Balance Before:",
  formatEther(balanceBefore),
  "BLT"
);

console.log(
  "Total Supply Before:",
  formatEther(supplyBefore),
  "BLT"
);

if (alreadyClaimed) {
  console.log("------------------------------------");
  console.log(
    "Signup bonus has already been claimed for this address."
  );
  console.log(
    "No transaction was sent."
  );
  console.log("====================================");
  process.exit(0);
}

console.log("------------------------------------");
console.log("Sending mint transaction...");

const txHash =
  await walletClient.writeContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "mintSignupBonus",
    args: [testUser],
    account: walletClient.account,
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

const balanceAfter =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "balanceOf",
    args: [testUser],
  });

const supplyAfter =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "totalSupply",
  });

const claimedAfter =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "signupBonusClaimed",
    args: [testUser],
  });

console.log(
  "Test User Balance After:",
  formatEther(balanceAfter),
  "BLT"
);

console.log(
  "Total Supply After:",
  formatEther(supplyAfter),
  "BLT"
);

console.log(
  "Signup Bonus Claimed:",
  claimedAfter
);

console.log("------------------------------------");

const balanceCorrect =
  balanceAfter === bonusAmount;

const supplyCorrect =
  supplyAfter === bonusAmount;

const claimedCorrect =
  claimedAfter === true;

if (
  receipt.status === "success" &&
  balanceCorrect &&
  supplyCorrect &&
  claimedCorrect
) {
  console.log(
    "Sepolia Signup Bonus Test: PASSED"
  );
} else {
  console.log(
    "Sepolia Signup Bonus Test: FAILED"
  );
}

console.log("====================================");