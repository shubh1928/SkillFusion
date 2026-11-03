import { buildModule } from "@nomicfoundation/hardhat-ignition/modules";

/**
 * Separate deployment for the new BLT token and matching escrow.
 *
 * The existing BarterLinkModule and its deployed addresses are left
 * untouched. This module deploys a new escrow instance wired to V2.
 *
 * Do not run this against Sepolia until the V2 contract tests and
 * migration plan have been reviewed.
 */
export default buildModule("BarterLinkV2Module", (m) => {
  const deployer = m.getAccount(0);

  const protocolSigner = deployer;

  const tokenV2 = m.contract(
    "BarterLinkTokenV2",
    [protocolSigner],
    {
      from: deployer,
    },
  );

  // Reuse the existing escrow source as a separate deployment.
  // Its constructor accepts an ERC-20 + ERC-2612 token address.
  const escrowV2 = m.contract(
    "BarterLinkEscrow",
    [tokenV2],
    {
      from: deployer,
    },
  );

  return {
    tokenV2,
    escrowV2,
  };
});