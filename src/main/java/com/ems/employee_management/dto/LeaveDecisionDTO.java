package com.ems.employee_management.dto;

import com.ems.employee_management.entity.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LeaveDecisionDTO {
    @NotNull
    private LeaveStatus status;
    @Size(max = 1000)
    private String note;
}
