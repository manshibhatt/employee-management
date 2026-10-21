package com.ems.employee_management.controller;

import com.ems.employee_management.dto.AttendanceDTO;
import com.ems.employee_management.service.AttendanceService;
import com.ems.employee_management.entity.User;
import com.ems.employee_management.exception.ResourceNotFoundException;
import com.ems.employee_management.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/check-in/{employeeId}")
    public ResponseEntity<AttendanceDTO> checkIn(@PathVariable Long employeeId, Authentication authentication) {
        requireOwnEmployeeOrApprover(employeeId, authentication);
        return new ResponseEntity<>(attendanceService.checkIn(employeeId), HttpStatus.CREATED);
    }

    @PutMapping("/check-out/{employeeId}")
    public ResponseEntity<AttendanceDTO> checkOut(@PathVariable Long employeeId, Authentication authentication) {
        requireOwnEmployeeOrApprover(employeeId, authentication);
        return new ResponseEntity<>(attendanceService.checkOut(employeeId), HttpStatus.OK);
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<AttendanceDTO>> getMyAttendance(@PathVariable Long employeeId, Authentication authentication) {
        requireOwnEmployeeOrApprover(employeeId, authentication);
        return ResponseEntity.ok(attendanceService.getMyAttendance(employeeId));
    }

    @GetMapping("/date/{date}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('LEAVE_APPROVER', 'ADMIN')")
    public ResponseEntity<List<AttendanceDTO>> getAttendanceByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.getAttendanceByDate(date));
    }

    @GetMapping("/employee/{employeeId}/range")
    public ResponseEntity<List<AttendanceDTO>> getAttendanceInRange(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {
        requireOwnEmployeeOrApprover(employeeId, authentication);
        if (end.isBefore(start)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must be on or after start date");
        return ResponseEntity.ok(attendanceService.getAttendanceInRange(employeeId, start, end));
    }

    private void requireOwnEmployeeOrApprover(Long employeeId, Authentication authentication) {
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_LEAVE_APPROVER") || a.getAuthority().equals("ROLE_ADMIN"))) return;
        User user = userRepository.findWithEmployeeByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getEmployee() == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Your login is not linked to an employee profile yet.");
        if (!user.getEmployee().getId().equals(employeeId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own attendance");
    }
}
