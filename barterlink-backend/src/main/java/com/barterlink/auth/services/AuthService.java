package com.barterlink.auth.services;

import com.barterlink.auth.payload.UserDto;

public interface AuthService {

    UserDto registerUser(UserDto userDto);

}
