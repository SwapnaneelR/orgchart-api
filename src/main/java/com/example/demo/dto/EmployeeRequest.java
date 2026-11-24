package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record EmployeeRequest(
    @NotBlank String name,
    @NotBlank String designation,
    @Positive double salary,
    Long managerId
) {}
