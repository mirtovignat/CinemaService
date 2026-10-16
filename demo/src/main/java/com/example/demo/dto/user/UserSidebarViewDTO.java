package com.example.demo.dto.user;

import lombok.Builder;

@Builder
public record UserSidebarViewDTO(
        String username,
        String name,
        String surname,
        String patronymic,
        String email,
        String phone
) {}