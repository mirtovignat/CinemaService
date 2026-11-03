package com.example.demo.mapper.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.Period;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AgeMapper {

    @Named("calculateAge")
    public static int calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}