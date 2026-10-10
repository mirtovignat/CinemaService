package com.example.demo.dto.user;

import com.example.demo.dto.authorize.Identifier;
import com.example.demo.validation.annotation.ValidProfileField;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

@Builder
@ValidProfileField
public record ProfileFieldUpdateDTO(
        @NotNull Identifier identifier,
        String field
) {
    public ProfileFieldUpdateDTO {
        if (field == null) {
            field = "";
        }
    }
}