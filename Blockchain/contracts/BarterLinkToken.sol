// SPDX-License-Identifier: MIT
pragma solidity ^0.8.34;

import {ERC20} from "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import {
    ERC20Permit
} from "@openzeppelin/contracts/token/ERC20/extensions/ERC20Permit.sol";

contract BarterLinkToken is ERC20, ERC20Permit {

    uint256 public constant SIGNUP_BONUS = 5 ether;

    mapping(address => bool) public signupBonusClaimed;

    address public protocolSigner;
    address public pendingProtocolSigner;

    error SignupBonusAlreadyClaimed();
    error InvalidRecipient();
    error Unauthorized();

    event SignupBonusMinted(
        address indexed user,
        uint256 amount
    );

    event ProtocolSignerTransferStarted(
        address indexed currentSigner,
        address indexed pendingSigner
    );

    event ProtocolSignerTransferred(
        address indexed previousSigner,
        address indexed newSigner
    );

    constructor(address _protocolSigner)
        ERC20("BarterLink Token", "BLT")
        ERC20Permit("BarterLink Token")
    {
        if (_protocolSigner == address(0)) {
            revert InvalidRecipient();
        }

        protocolSigner = _protocolSigner;
    }

    modifier onlyProtocolSigner() {
        if (msg.sender != protocolSigner) {
            revert Unauthorized();
        }

        _;
    }

    function mintSignupBonus(
        address user
    )
        external
        onlyProtocolSigner
    {
        if (user == address(0)) {
            revert InvalidRecipient();
        }

        if (signupBonusClaimed[user]) {
            revert SignupBonusAlreadyClaimed();
        }

        signupBonusClaimed[user] = true;

        _mint(user, SIGNUP_BONUS);

        emit SignupBonusMinted(
            user,
            SIGNUP_BONUS
        );
    }

    function transferProtocolSigner(
        address newSigner
    )
        external
        onlyProtocolSigner
    {
        if (newSigner == address(0)) {
            revert InvalidRecipient();
        }

        pendingProtocolSigner = newSigner;

        emit ProtocolSignerTransferStarted(
            protocolSigner,
            newSigner
        );
    }

    function acceptProtocolSigner()
        external
    {
        if (msg.sender != pendingProtocolSigner) {
            revert Unauthorized();
        }

        address previousSigner = protocolSigner;

        protocolSigner = msg.sender;
        pendingProtocolSigner = address(0);

        emit ProtocolSignerTransferred(
            previousSigner,
            msg.sender
        );
    }
}