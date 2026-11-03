# BarterLink Blockchain Deployment Record

This document records the verified deployment and real-network testing state of the BarterLink blockchain layer.

Research paper: **SkillFusion**

Blockchain component: **Blockchain-Based Skill Wallet**

---

## 1. Network

| Property | Value |
|---|---|
| Network | Ethereum Sepolia Testnet |
| Chain ID | `11155111` |
| Token Standard | ERC-20 |
| Token Symbol | `BLT` |
| Decimals | `18` |

---

## 2. Protocol Signer

The protocol signer is the technical transaction-sending account used by the protocol to submit blockchain transactions and pay Sepolia gas.

```text
Protocol Signer:
0x1F2D5bB2Cec2dde10cCB971FE9fa88A26C1fc9b5
```

The protocol signer is not an application administrator.

---

## 3. Deployed Contracts

### BarterLinkToken

```text
Contract:
BarterLinkToken

Address:
0x0F6eB87FA4743fa52fB34890947b6B4bFCBe32fa

Deployment Transaction:
0x08a60953d8543502153051cc20357e9259347d3c7d78244de8e5ac18418e15a9

Deployment Block:
11796183
```

### BarterLinkEscrow

```text
Contract:
BarterLinkEscrow

Address:
0xA4F9ACB729cac5eD67D419395FE40B0660E0376a

Deployment Transaction:
0x7c26f1810c5f655f4b98775f1a36c74aa435fbcc49c156ebb709e30f2d9e4eff

Deployment Block:
11796189
```

The Hardhat Ignition deployment journal confirms the BarterLinkToken deployment and its transaction record, followed by the BarterLinkEscrow deployment using the deployed BLT token address as its constructor argument. :contentReference[oaicite:0]{index=0} :contentReference[oaicite:1]{index=1}

---

## 4. Deployment Configuration

Deployment module:

```text
ignition/modules/BarterLink.ts
```

Deployment command:

```powershell
npx hardhat ignition deploy ./ignition/modules/BarterLink.ts --network sepolia
```

The deployment uses protected Hardhat configuration variables:

```text
SEPOLIA_RPC_URL
SEPOLIA_PRIVATE_KEY
```

Secrets must never be committed to source control.

---

## 5. Deployment Verification

The deployed contracts were verified using:

```powershell
npx hardhat run scripts/verify-sepolia-deployment.ts
```

Verified properties:

```text
Chain ID = 11155111
BLT contract exists
Escrow contract exists
BLT name = BarterLink Token
BLT symbol = BLT
BLT decimals = 18
BLT total supply = 5 BLT
BLT signup bonus = 5 BLT
BLT protocol signer matches expected protocol signer
Escrow uses deployed BLT contract
```

Final result:

```text
Deployment Verification: PASSED
```

---

## 6. Signup Mint Test

A real signup bonus mint was successfully executed on Sepolia.

```text
Signup Bonus:
5 BLT
```

Result:

```text
Initial BLT supply: 0 BLT
Test user balance before: 0 BLT
Test user balance after: 5 BLT
Final BLT supply: 5 BLT
Signup claim: true
```

Transaction:

```text
0xd60aeb6b5d968d507760ba8379c61461ff495aba86bd414af375993652b3201b
```

Block:

```text
11796244
```

Result:

```text
Sepolia Signup Bonus Test: PASSED
```

---

## 7. Test User and Provider

### Requester / Test User

```text
0xB6f48178BCDa2f807675948eB6675e363eEA46BC
```

### Provider / Test Provider

```text
0xD0a8EBf20FAE75DB5A4D25d4043980624A2e8eCc
```

The requester and provider are distinct Ethereum addresses.

---

## 8. Real Escrow Lock Test

The first real escrow lock test used:

```text
Escrow Amount:
2 BLT
```

The requester signed an ERC-2612 Permit off-chain and the protocol signer submitted `lockWithPermit()`.

Result:

```text
Requester: 5 → 3 BLT
Provider:  0 → 0 BLT
Escrow:    0 → 2 BLT
Status:    LOCKED
```

Transaction:

```text
0xec0192e639b86c52fddf55f10bf10ebaffaa1a6e02e755a77624da3c63a5d848
```

Block:

```text
11800952
```

Result:

```text
Real Sepolia Escrow Lock Test: PASSED
```

Stored escrow metadata was verified for:

```text
Requester
Provider
Amount
Agreement ID
Session ID
Status
Created At
Resolved At
```

---

## 9. Real Escrow Release Test

The first locked escrow was used to test the release path.

Flow:

```text
LOCKED
   ↓
release()
   ↓
RELEASED
```

Result:

```text
Provider: 0 → 2 BLT
Escrow:   2 → 0 BLT
Status:   LOCKED → RELEASED
```

Transaction:

```text
0x0e31658dc675955343485678c5a4a9fca8e45d41d7f693354a4228816b2750ee
```

Block:

```text
11800975
```

Result:

```text
Real Sepolia Escrow Release Test: PASSED
```

---

## 10. Real Escrow Refund Test

A separate escrow was created for the refund path.

Flow:

```text
LOCKED
   ↓
refund()
   ↓
REFUNDED
```

Result:

```text
Requester: 1 → 3 BLT
Provider:  2 → 2 BLT
Escrow:    2 → 0 BLT
Status:    LOCKED → REFUNDED
```

Transaction:

```text
0xd1f2cd087f58a67daf9a9a8969f8c38022a39fc1c8d32a7ac05b5ca5c8384742
```

Block:

```text
11801043
```

Result:

```text
Real Sepolia Escrow Refund Test: PASSED
```

---

## 11. Escrow Test Summary

The real Sepolia escrow lifecycle has been demonstrated successfully:

```text
Requester owns BLT
       ↓
ERC-2612 Permit
       ↓
lockWithPermit()
       ↓
LOCKED
       ├── release() → RELEASED
       │
       └── refund()  → REFUNDED
```

Both terminal paths were executed successfully using the deployed contracts.

---

## 12. Local Regression Tests

The local Node.js/Viem test suite was executed after the Sepolia testing and cleanup changes.

Command:

```powershell
npx hardhat test nodejs
```

Result:

```text
31 passing
```

The suite covers both:

```text
BarterLinkToken
```

and:

```text
BarterLinkEscrow
```

The deprecated `network.connect()` usage was also replaced with `network.create()` in the test files, and the final test run completed without the previous deprecation warning.

---

## 13. TypeScript Validation

The project was type-checked successfully using:

```powershell
npx tsc --noEmit
```

Result:

```text
No TypeScript errors
```

---

## 14. Source of Deployment Records

Hardhat Ignition deployment records are stored under:

```text
ignition/deployments/chain-11155111/
```

Relevant deployment records include:

```text
deployed_addresses.json
journal.jsonl
```

The deployment journal records the on-chain deployment transactions, contract addresses, constructor information, and successful deployment receipts.

---

## 15. Current Verified State

```text
Ethereum Sepolia deployment        ✅
BLT contract                       ✅
Escrow contract                    ✅
Signup mint                        ✅
ERC-2612 Permit                    ✅
Real escrow lock                   ✅
LOCKED → RELEASED                  ✅
LOCKED → REFUNDED                  ✅
Deployment verification            ✅
Local tests                        31/31 ✅
TypeScript validation              ✅
Deprecated API cleanup             ✅
Hardhat starter cleanup            ✅
```

---

## 16. Current Blockchain Phase

The blockchain contracts and real Sepolia transaction flows have been verified.

The next development phase is **Spring Boot blockchain integration**.

The backend integration will eventually connect the existing BarterLink application workflow with:

```text
Blockchain wallet creation
Encrypted private-key storage
Blockchain address association
BLT balance retrieval
Signup mint
ERC-2612 Permit generation
Escrow lock
Escrow release
Escrow refund
Blockchain transaction tracking
Retry and reconciliation
```

The existing application-level negotiation, agreement, and session workflow is intended to remain in place.

---

## 17. Security

Never commit:

```text
Private keys
Seed phrases
Recovery phrases
Passwords
RPC secrets
Hardhat keystore secrets
```

Blockchain credentials must remain protected through the configured secure secret-management mechanism.

---

## 18. Important Note

This deployment record describes the **currently deployed Sepolia contracts** and the real-network tests performed against them.

The deployed contracts are authoritative for:

```text
BLT ownership
Actual BLT movement
Escrowed BLT
Escrow state
```

Application SQL remains responsible for application-level state and metadata.
