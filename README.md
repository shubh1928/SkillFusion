# SkillFusion - AI-Powered Skill Exchange Platform

**Learn. Teach. Grow Together.**

SkillFusion is an AI-powered skill exchange platform that enables people to exchange knowledge, skills, and services without relying entirely on monetary transactions. It provides a collaborative environment where users can discover learning opportunities, connect with skilled individuals, negotiate services, create digital agreements, and build their professional reputation.

The platform combines Artificial Intelligence, Blockchain technology, and modern full-stack development to make peer-to-peer skill exchange more transparent, secure, and accessible.

## Live Application

**Live Website:** https://skillfusion-frontend.vercel.app/

**Backend API Documentation:** https://skillfusion-backend-35dh.onrender.com/swagger-ui/index.html

**AI Service Health:** https://skillfusion-ai.onrender.com/

**GitHub Repository:** https://github.com/shubh1928/SkillFusion

## Key Features

### 1. Skill Marketplace
- Browse and discover available skills and service requests.
- Create service requests and connect with other participants.
- Manage personal skills and professional profiles.
- Support peer-to-peer skill exchange.

### 2. AI-Powered Recommendation System
- Semantic matching between user skills and marketplace requests.
- Context-aware recommendations using Sentence Embeddings.
- Similarity-based ranking of relevant opportunities.

### 3. AI-Based Fraud Detection
- Behavioural risk assessment using Isolation Forest.
- Risk scoring and classification.
- Feature-based explanations for potentially suspicious activities.

### 4. Negotiation and Digital Agreements
- Interactive negotiation management.
- Version-controlled digital agreements.
- Mutual agreement acceptance.
- Milestone-based service execution and token settlement.

### 5. Blockchain and Token Economy
- SkillFusion Token (SFT) wallet.
- Blockchain-based transaction tracking.
- Smart contract-based escrow and settlement.
- Secure and transparent token management.

### 6. Collaboration and Communication
- Real-time notifications.
- WebSocket-based collaboration.
- Chat and interactive service sessions.
- WebRTC-based audio and video communication.

### 7. Trust, Reputation and Governance
- Participant ratings and reputation management.
- Proof-of-contribution tracking.
- Dispute management and resolution workflows.
- DAO-inspired governance mechanisms.

### 8. Referral and Rewards
- Referral-based rewards.
- Direct barter rewards.
- Daily activity streak rewards.
- Reward progress tracking.

## Technology Stack

| Layer | Technologies |
|---|---|
| Frontend | React 19, TypeScript, Vite, Tailwind CSS |
| Backend | Java 21, Spring Boot 3.5.7 |
| Security | Spring Security, JWT, Google OAuth2, GitHub OAuth2 |
| AI Service | Python, FastAPI, FastEmbed, ONNX Runtime |
| Machine Learning | Sentence Transformers (all-MiniLM-L6-v2), Isolation Forest |
| Database | MySQL |
| Blockchain | Solidity, Hardhat, Ethereum Sepolia |
| Real-Time Communication | WebSocket, STOMP, WebRTC |
| Decentralized Storage | IPFS, Pinata |
| Deployment | Vercel, Render, Aiven |

## System Architecture

The platform follows a modular architecture with independently maintained frontend, backend, AI, and blockchain components.

```text
                   USERS
                     |
                     v
             REACT FRONTEND
                  VERCEL
                     |
                     v
           SPRING BOOT BACKEND
                  RENDER
                     |
          +----------+----------+
          |          |          |
          v          v          v
       MYSQL       AI API    BLOCKCHAIN
       AIVEN      FASTAPI     ETHEREUM
                    RENDER     SEPOLIA
          |          |
          v          v
       USER DATA   RECOMMENDATION
       AND SKILLS   AND FRAUD
                    DETECTION

          IPFS / PINATA
          DECENTRALIZED
             STORAGE
```

## Repository Structure

```text
SkillFusion/
|
├── Blockchain/
|   └── Smart contracts and blockchain integration
|
├── barterlink-backend/
|   └── Spring Boot backend source code
|
├── barterlink-frontend/
|   └── React frontend source code
|
├── skillfusion-ai/
|   └── FastAPI AI and machine learning service
|
└── README.md
```

The consolidated repository contains the major project codebases. Individual deployment repositories are also maintained for the hosted services.

## Deployment

| Component | Hosting Platform |
|---|---|
| Frontend | Vercel |
| Backend | Render |
| AI Microservice | Render |
| MySQL Database | Aiven |
| Smart Contracts | Ethereum Sepolia Test Network |
| Decentralized Storage | Pinata / IPFS |

## Project Objectives

- Encourage collaborative learning through skill exchange.
- Reduce dependency on traditional monetary payments for knowledge sharing.
- Improve skill discovery through AI-based recommendations.
- Increase trust using blockchain-based agreements and transparent transactions.
- Provide secure collaboration and dispute management.
- Create a community-driven ecosystem for sharing expertise.

## Future Enhancements

- Improve recommendation accuracy using additional user interaction data.
- Expand AI-based fraud detection and behavioural analytics.
- Enhance mobile responsiveness and accessibility.
- Extend platform scalability and performance.
- Introduce additional integrations for professional learning and collaboration.

## Author

**Shubham Gopale**

MCA Student | Full Stack Java Developer

**GitHub:** https://github.com/shubh1928

---

*SkillFusion – Learn. Teach. Grow Together.*
