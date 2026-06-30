package com.barterlink.auth.services;

import com.barterlink.auth.payload.UserDto;

public interface UserService {

    //create user
    UserDto createUser(UserDto userDto);

    //Get User by Email
    UserDto getUserByEmail(String email);

    //Update User
    UserDto updateUser(UserDto userDto, String userId);

    //Delete User
    void deleteUser(String userId);

    //Get user by ID
    UserDto getUserById(String userId);

    //Get All User's
    Iterable<UserDto> getAllUsers();

}
