package com.ems.employee_management.controller;

import com.ems.employee_management.dto.LeaveDecisionDTO;
import com.ems.employee_management.dto.LeaveRequestDTO;
import com.ems.employee_management.dto.LeaveTypeDTO;
import com.ems.employee_management.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {
    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping("/types")
    public ResponseEntity<List<LeaveTypeDTO>> getTypes() {
        return ResponseEntity.ok(leaveService.getTypes());
    }

    @GetMapping
    public ResponseEntity<List<LeaveRequestDTO>> getRequests(Authentication authentication,
            @RequestParam(required = false) Long employeeId) {
        return ResponseEntity.ok(leaveService.getRequests(authentication, employeeId));
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LeaveRequestDTO> create(@Valid @RequestBody LeaveRequestDTO request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.create(request, authentication));
    }

    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasRole('LEAVE_APPROVER')")
    public ResponseEntity<LeaveRequestDTO> decide(@PathVariable Long id, @Valid @RequestBody LeaveDecisionDTO decision) {
        return ResponseEntity.ok(leaveService.decide(id, decision));
    }
}
