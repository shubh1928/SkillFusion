package com.barterlink.auth.services.impl;

import com.barterlink.auth.repositories.RoleRepository;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.auth.config.AppConstants;
import com.barterlink.auth.payload.UserDto;
import com.barterlink.auth.entities.Provider;
import com.barterlink.auth.entities.Role;
import com.barterlink.auth.entities.User;
import com.barterlink.exceptions.ResourceNotFoundException;
import com.barterlink.auth.helpers.UserHelper;
import com.barterlink.auth.services.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final RoleRepository roleRepository;


    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {

        if (userDto.getEmail() == null || userDto.getEmail().isBlank()){
            throw new IllegalArgumentException("Email is required!!!");
        }

        if (userRepository.existsByEmail(userDto.getEmail())){
            throw new IllegalArgumentException("User with given email already exists!!!");
        }

        User user = modelMapper.map(userDto, User.class);
        user.setProvider(userDto.getProvider() != null ? userDto.getProvider() : Provider.LOCAL);

        Role role = roleRepository.findByName("ROLE_"+ AppConstants.GUEST_ROLE).orElse(null);
        user.getRoles().add(role);

        //role assign here to new user - for authorization
        User savedUser = userRepository.save(user);

        return modelMapper.map(savedUser, UserDto.class);
    }

    @Override
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found with the given id!!!"));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    @Transactional
    public UserDto updateUser(UserDto userDto, String userId) {
        UUID uID = UserHelper.parseUUID(userId);
        User existingUser = userRepository.findById(uID).orElseThrow(() -> new ResourceNotFoundException("User not found with the given id!!!"));

        if(userDto.getName() != null) existingUser.setName(userDto.getName());
        if(userDto.getImage() != null) existingUser.setImage(userDto.getImage());
        if(userDto.getProvider() != null) existingUser.setProvider(userDto.getProvider());
        if(userDto.getPassword() != null) existingUser.setPassword(userDto.getPassword());
        existingUser.setEnable(userDto.isEnable());
        existingUser.setUpdatedAt(Instant.now());
        User updatedUser = userRepository.save(existingUser);
        return modelMapper.map(updatedUser, UserDto.class);
    }

    @Override
    public void deleteUser(String userId) {
        UUID uID = UserHelper.parseUUID(userId);
        User user = userRepository.findById(uID).orElseThrow(() -> new ResourceNotFoundException("User not found with the given id!!!"));
        userRepository.delete(user);
    }

    @Override
    public UserDto getUserById(String userId) {
        User user = userRepository.findById(UserHelper.parseUUID(userId)).orElseThrow(() -> new ResourceNotFoundException("User not found with the given id!!!"));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    @Transactional
    public Iterable<UserDto> getAllUsers() {
        return userRepository.findAll()
                             .stream()
                             .map(user -> modelMapper.map(user, UserDto.class))
                             .toList();
    }
}
