package com.ems.employee_management.controller;

import com.ems.employee_management.dto.EmployeeInfoDTO;
import com.ems.employee_management.service.EmployeeInfoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/employees/{employeeId}/info")
public class EmployeeInfoController {
    private final EmployeeInfoService employeeInfoService;

    public EmployeeInfoController(EmployeeInfoService employeeInfoService) {
        this.employeeInfoService = employeeInfoService;
    }

    @GetMapping
    public ResponseEntity<EmployeeInfoDTO> get(@PathVariable Long employeeId, Authentication authentication) {
        return ResponseEntity.ok(employeeInfoService.getOrCreate(employeeId, authentication));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('LEAVE_APPROVER', 'ADMIN')")
    public ResponseEntity<EmployeeInfoDTO> save(@PathVariable Long employeeId, @Valid @RequestBody EmployeeInfoDTO dto) {
        return ResponseEntity.ok(employeeInfoService.save(employeeId, dto));
    }
}
