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

const deployer =
  walletClient.account.address;

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
    name: "name",
    stateMutability: "view",
    inputs: [],
    outputs: [
      {
        type: "string",
      },
    ],
  },
  {
    type: "function",
    name: "symbol",
    stateMutability: "view",
    inputs: [],
    outputs: [
      {
        type: "string",
      },
    ],
  },
  {
    type: "function",
    name: "decimals",
    stateMutability: "view",
    inputs: [],
    outputs: [
      {
        type: "uint8",
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
    name: "protocolSigner",
    stateMutability: "view",
    inputs: [],
    outputs: [
      {
        type: "address",
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

const escrowAbi = [
  {
    type: "function",
    name: "token",
    stateMutability: "view",
    inputs: [],
    outputs: [
      {
        type: "address",
      },
    ],
  },
] as const;

const chainId =
  await publicClient.getChainId();

const balance =
  await publicClient.getBalance({
    address: deployer,
  });

const tokenCode =
  await publicClient.getCode({
    address: tokenAddress,
  });

const escrowCode =
  await publicClient.getCode({
    address: escrowAddress,
  });

const tokenName =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "name",
  });

const tokenSymbol =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "symbol",
  });

const tokenDecimals =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "decimals",
  });

const totalSupply =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "totalSupply",
  });

const protocolSigner =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "protocolSigner",
  });

const signupBonus =
  await publicClient.readContract({
    address: tokenAddress,
    abi: tokenAbi,
    functionName: "SIGNUP_BONUS",
  });

const escrowToken =
  await publicClient.readContract({
    address: escrowAddress,
    abi: escrowAbi,
    functionName: "token",
  });

console.log("====================================");
console.log("BarterLink Sepolia Deployment Check");
console.log("====================================");

console.log("Chain ID:", chainId);

console.log(
  "Protocol Account:",
  deployer
);

console.log(
  "Sepolia ETH Balance:",
  formatEther(balance),
  "ETH"
);

console.log("------------------------------------");

console.log(
  "BLT Address:",
  tokenAddress
);

console.log(
  "BLT Contract Exists:",
  tokenCode !== undefined &&
  tokenCode !== "0x"
);

console.log(
  "BLT Name:",
  tokenName
);

console.log(
  "BLT Symbol:",
  tokenSymbol
);

console.log(
  "BLT Decimals:",
  tokenDecimals
);

console.log(
  "BLT Total Supply:",
  formatEther(totalSupply),
  "BLT"
);

console.log(
  "BLT Signup Bonus:",
  formatEther(signupBonus),
  "BLT"
);

console.log(
  "BLT Protocol Signer:",
  protocolSigner
);

console.log("------------------------------------");

console.log(
  "Escrow Address:",
  escrowAddress
);

console.log(
  "Escrow Contract Exists:",
  escrowCode !== undefined &&
  escrowCode !== "0x"
);

console.log(
  "Escrow Token Address:",
  escrowToken
);

console.log("------------------------------------");

const signerMatches =
  protocolSigner.toLowerCase() ===
  deployer.toLowerCase();

const escrowTokenMatches =
  escrowToken.toLowerCase() ===
  tokenAddress.toLowerCase();

console.log(
  "Protocol Signer Matches:",
  signerMatches
);

console.log(
  "Escrow Uses Deployed BLT:",
  escrowTokenMatches
);

console.log("------------------------------------");

if (
  chainId === 11155111 &&
  tokenCode !== undefined &&
  tokenCode !== "0x" &&
  escrowCode !== undefined &&
  escrowCode !== "0x" &&
  tokenName === "BarterLink Token" &&
  tokenSymbol === "BLT" &&
  tokenDecimals === 18 &&
  totalSupply === 5000000000000000000n &&
  signerMatches &&
  escrowTokenMatches
) {
  console.log(
    "Deployment Verification: PASSED"
  );
} else {
  console.log(
    "Deployment Verification: FAILED"
  );
}

console.log("====================================");