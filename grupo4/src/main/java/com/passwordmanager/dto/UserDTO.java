package com.passwordmanager.dto;

public record UserDTO(
        Long id,
        String name,
        String email,
        byte[] image,
        String imageContentType) {}
