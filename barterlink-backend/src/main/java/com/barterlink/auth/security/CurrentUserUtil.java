package com.barterlink.auth.security;

import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CurrentUserUtil {

    private final UserRepository userRepository;

    public User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || authentication.getName() == null) {

            throw new UnauthorizedException(
                    "User not authenticated");
        }

        String email =
                authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UnauthorizedException(
                                "User not found"));
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
