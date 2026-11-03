import { buildModule } from "@nomicfoundation/hardhat-ignition/modules";

export default buildModule("BarterLinkModule", (m) => {

  const deployer = m.getAccount(0);

  const protocolSigner = deployer;

  const token = m.contract(
    "BarterLinkToken",
    [
      protocolSigner,
    ],
    {
      from: deployer,
    }
  );

  const escrow = m.contract(
    "BarterLinkEscrow",
    [
      token,
    ],
    {
      from: deployer,
    }
  );

  return {
    token,
    escrow,
  };
});