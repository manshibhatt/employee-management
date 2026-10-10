package com.ems.employee_management.dto;

import com.ems.employee_management.entity.LeaveStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LeaveRequestDTO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeEmail;
    @NotNull
    private Long leaveTypeId;
    private String leaveTypeName;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
    @NotBlank
    @Size(max = 1000)
    private String reason;
    private LeaveStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;
    private String decisionNote;
}
