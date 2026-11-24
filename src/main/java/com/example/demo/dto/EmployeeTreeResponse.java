package com.example.demo.dto;

import java.util.List;

public record EmployeeTreeResponse(
    Long id,
    String name,
    String designation,
    double salary,
    List<EmployeeTreeResponse> reports
) {}
