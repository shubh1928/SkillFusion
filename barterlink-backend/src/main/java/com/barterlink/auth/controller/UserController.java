package com.barterlink.auth.controller;

import com.barterlink.auth.payload.UserDto;
import com.barterlink.auth.services.UserService;
import com.barterlink.auth.config.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    //Create user api
    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto){
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(userDto));
    }

    //Get all users api
    @GetMapping
    public ResponseEntity<Iterable<UserDto>> getAllUsers(){
        return ResponseEntity.ok(userService.getAllUsers());
    }

    //Get user by email
    @GetMapping("/email/{email}")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email){
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }


    //Delete User
    @DeleteMapping("/{userId}")
    public void deleteUser(@PathVariable String userId){
        userService.deleteUser(userId);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserDto> updateUser(@RequestBody UserDto userDto, @PathVariable String userId){
        return ResponseEntity.ok(userService.updateUser(userDto, userId));
    }

    //Get user by id
    @PreAuthorize("hasRole('"+ AppConstants.ADMIN_ROLE +"')")
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable String userId){
        return ResponseEntity.ok(userService.getUserById(userId));
    }

}
