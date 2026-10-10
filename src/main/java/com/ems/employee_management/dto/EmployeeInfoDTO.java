package com.ems.employee_management.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeeInfoDTO {
    private Long id;
    private Long employeeId;
    @Size(max = 120)
    private String jobTitle;
    @Size(max = 40)
    private String phone;
    @Size(max = 500)
    private String address;
    private LocalDate dateOfBirth;
    private LocalDate hireDate;
    @Size(max = 200)
    private String emergencyContact;
}
