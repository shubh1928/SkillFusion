import { network } from "hardhat";

const connection = await network.create({
  network: "sepolia",
  chainType: "l1",
});

const { viem } = connection;

const publicClient =
  await viem.getPublicClient();

const walletClients =
  await viem.getWalletClients();

console.log("====================================");
console.log("BarterLink Sepolia Account Check");
console.log("====================================");

console.log(
  "Chain ID:",
  await publicClient.getChainId()
);

console.log("------------------------------------");

console.log(
  "Configured wallet count:",
  walletClients.length
);

for (let i = 0; i < walletClients.length; i++) {

  const address =
    walletClients[i].account.address;

  console.log(
    `Account ${i}:`,
    address
  );
}

console.log("------------------------------------");

const expectedProtocolSigner =
  "0x1F2D5bB2Cec2dde10cCB971FE9fa88A26C1fc9b5";

const expectedTestUser =
  "0xB6f48178BCDa2f807675948eB6675e363eEA46BC";

const protocolMatches =
  walletClients[0]?.account.address.toLowerCase() ===
  expectedProtocolSigner.toLowerCase();

const testUserMatches =
  walletClients[1]?.account.address.toLowerCase() ===
  expectedTestUser.toLowerCase();

console.log(
  "Protocol signer matches:",
  protocolMatches
);

console.log(
  "Test user matches:",
  testUserMatches
);

console.log("------------------------------------");

if (
  walletClients.length >= 2 &&
  protocolMatches &&
  testUserMatches
) {
  console.log(
    "Sepolia Account Verification: PASSED"
  );
} else {
  console.log(
    "Sepolia Account Verification: FAILED"
  );
}

console.log("====================================");