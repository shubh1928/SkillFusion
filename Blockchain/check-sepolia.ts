import { network } from "hardhat";
import { formatEther } from "viem";

const connection = await network.create({
  network: "sepolia",
  chainType: "l1",
});

const { viem } = connection;

const publicClient = await viem.getPublicClient();

const [walletClient] =
  await viem.getWalletClients();

const address =
  walletClient.account.address;

const chainId =
  await publicClient.getChainId();

const balance =
  await publicClient.getBalance({
    address,
  });

console.log("====================================");
console.log("BarterLink Sepolia Verification");
console.log("====================================");

console.log(
  "Network Chain ID:",
  chainId
);

console.log(
  "Protocol Account:",
  address
);

console.log(
  "Sepolia ETH Balance:",
  formatEther(balance),
  "ETH"
);

console.log("====================================");