package com.example.demo.dto;

public record EmployeeResponse(
    Long id,
    String name,
    String designation,
    double salary,
    Long managerId,
    String managerName
) {}
