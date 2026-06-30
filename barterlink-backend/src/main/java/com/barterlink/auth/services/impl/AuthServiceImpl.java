package com.barterlink.auth.services.impl;

import com.barterlink.auth.payload.UserDto;
import com.barterlink.auth.services.AuthService;
import com.barterlink.auth.services.UserService;
import com.barterlink.wallet.service.WalletService;
import com.barterlink.common.constants.WalletConstants;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;

    @Override
    public UserDto registerUser(UserDto userDto) {

        userDto.setPassword(
                passwordEncoder.encode(userDto.getPassword()));

        UserDto createdUser =
                userService.createUser(userDto);

        walletService.initializeWallet(
                createdUser.getId());

        walletService.addBonusTokens(
                createdUser.getId(),
                WalletConstants.SIGNUP_BONUS,
                "Signup Bonus");

        return createdUser;
    }
}
