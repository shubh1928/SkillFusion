// SPDX-License-Identifier: MIT
pragma solidity ^0.8.34;

import {IERC20} from "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import {
    IERC20Permit
} from "@openzeppelin/contracts/token/ERC20/extensions/IERC20Permit.sol";
import {
    SafeERC20
} from "@openzeppelin/contracts/token/ERC20/utils/SafeERC20.sol";
import {
    ReentrancyGuard
} from "@openzeppelin/contracts/utils/ReentrancyGuard.sol";

interface IBarterLinkToken is IERC20, IERC20Permit {
    function protocolSigner() external view returns (address);
}

contract BarterLinkEscrow is ReentrancyGuard {

    using SafeERC20 for IERC20;

    IBarterLinkToken public immutable token;

    enum EscrowStatus {
        NONE,
        LOCKED,
        RELEASED,
        REFUNDED
    }

    struct Escrow {
        address requester;
        address provider;
        uint256 amount;
        bytes32 agreementId;
        bytes32 sessionId;
        EscrowStatus status;
        uint256 createdAt;
        uint256 resolvedAt;
    }

    mapping(bytes32 => Escrow) private escrows;

    error Unauthorized();
    error InvalidToken();
    error InvalidEscrowId();
    error InvalidRequester();
    error InvalidProvider();
    error InvalidAmount();
    error EscrowAlreadyExists();
    error EscrowNotLocked();
    error InvalidPermitDeadline();

    event EscrowCreated(
        bytes32 indexed escrowId,
        address indexed requester,
        address indexed provider,
        uint256 amount,
        bytes32 agreementId,
        bytes32 sessionId
    );

    event EscrowLocked(
        bytes32 indexed escrowId,
        uint256 amount
    );

    event EscrowReleased(
        bytes32 indexed escrowId,
        address indexed provider,
        uint256 amount
    );

    event EscrowRefunded(
        bytes32 indexed escrowId,
        address indexed requester,
        uint256 amount
    );

    modifier onlyProtocolSigner() {
        if (msg.sender != token.protocolSigner()) {
            revert Unauthorized();
        }

        _;
    }

    constructor(address tokenAddress) {
        if (tokenAddress == address(0)) {
            revert InvalidToken();
        }

        token = IBarterLinkToken(tokenAddress);
    }

    function lockWithPermit(
        bytes32 escrowId,
        address requester,
        address provider,
        uint256 amount,
        bytes32 agreementId,
        bytes32 sessionId,
        uint256 deadline,
        uint8 v,
        bytes32 r,
        bytes32 s
    )
        external
        onlyProtocolSigner
        nonReentrant
    {
        if (escrowId == bytes32(0)) {
            revert InvalidEscrowId();
        }

        if (requester == address(0)) {
            revert InvalidRequester();
        }

        if (provider == address(0)) {
            revert InvalidProvider();
        }

        if (amount == 0) {
            revert InvalidAmount();
        }

        if (deadline < block.timestamp) {
            revert InvalidPermitDeadline();
        }

        if (escrows[escrowId].status != EscrowStatus.NONE) {
            revert EscrowAlreadyExists();
        }

        token.permit(
            requester,
            address(this),
            amount,
            deadline,
            v,
            r,
            s
        );

        IERC20(address(token)).safeTransferFrom(
            requester,
            address(this),
            amount
        );

        escrows[escrowId] = Escrow({
            requester: requester,
            provider: provider,
            amount: amount,
            agreementId: agreementId,
            sessionId: sessionId,
            status: EscrowStatus.LOCKED,
            createdAt: block.timestamp,
            resolvedAt: 0
        });

        emit EscrowCreated(
            escrowId,
            requester,
            provider,
            amount,
            agreementId,
            sessionId
        );

        emit EscrowLocked(
            escrowId,
            amount
        );
    }

    function release(
        bytes32 escrowId
    )
        external
        onlyProtocolSigner
        nonReentrant
    {
        Escrow storage escrow = escrows[escrowId];

        if (escrow.status != EscrowStatus.LOCKED) {
            revert EscrowNotLocked();
        }

        escrow.status = EscrowStatus.RELEASED;
        escrow.resolvedAt = block.timestamp;

        IERC20(address(token)).safeTransfer(
            escrow.provider,
            escrow.amount
        );

        emit EscrowReleased(
            escrowId,
            escrow.provider,
            escrow.amount
        );
    }

    function refund(
        bytes32 escrowId
    )
        external
        onlyProtocolSigner
        nonReentrant
    {
        Escrow storage escrow = escrows[escrowId];

        if (escrow.status != EscrowStatus.LOCKED) {
            revert EscrowNotLocked();
        }

        escrow.status = EscrowStatus.REFUNDED;
        escrow.resolvedAt = block.timestamp;

        IERC20(address(token)).safeTransfer(
            escrow.requester,
            escrow.amount
        );

        emit EscrowRefunded(
            escrowId,
            escrow.requester,
            escrow.amount
        );
    }

    function getEscrow(
        bytes32 escrowId
    )
        external
        view
        returns (Escrow memory)
    {
        return escrows[escrowId];
    }
}