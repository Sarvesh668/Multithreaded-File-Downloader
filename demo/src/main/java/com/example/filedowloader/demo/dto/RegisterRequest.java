package com.example.filedowloader.demo.dto;

import com.example.filedowloader.demo.model.Role;

public record RegisterRequest(

    String username,
    String password,
    Role role
) {}
