package com.example.demo.mapper.util;

import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.Period;

public final class AgeMapper {

    @Named("calculateAge")
    public static int calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}