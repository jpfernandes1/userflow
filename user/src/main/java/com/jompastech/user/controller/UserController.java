package com.jompastech.user.controller;

import com.jompastech.user.model.dto.UserDTO;
import com.jompastech.user.model.entity.User;
import com.jompastech.user.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService service){
        this.userService = service;
    }

    @PostMapping
    public User create(@RequestBody UserDTO dto){
        return userService.createUser(dto);
    }
}
