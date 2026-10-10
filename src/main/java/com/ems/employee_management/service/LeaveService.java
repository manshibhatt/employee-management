package com.ems.employee_management.service;

import com.ems.employee_management.dto.LeaveDecisionDTO;
import com.ems.employee_management.dto.LeaveRequestDTO;
import com.ems.employee_management.dto.LeaveTypeDTO;
import com.ems.employee_management.entity.*;
import com.ems.employee_management.exception.ResourceNotFoundException;
import com.ems.employee_management.repository.LeaveRequestRepository;
import com.ems.employee_management.repository.LeaveTypeRepository;
import com.ems.employee_management.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LeaveService {
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public LeaveService(LeaveRequestRepository leaveRequestRepository, LeaveTypeRepository leaveTypeRepository, UserRepository userRepository, EmailService emailService) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public List<LeaveTypeDTO> getTypes() {
        return leaveTypeRepository.findAll().stream().map(type -> {
            LeaveTypeDTO dto = new LeaveTypeDTO();
            dto.setId(type.getId());
            dto.setName(type.getName());
            dto.setDescription(type.getDescription());
            return dto;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDTO> getRequests(Authentication authentication, Long employeeId) {
        boolean approver = isApprover(authentication);
        List<LeaveRequest> requests;
        if (approver) {
            requests = employeeId == null ? leaveRequestRepository.findAllByOrderByCreatedAtDesc()
                    : leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
        } else {
            Employee employee = getCurrentEmployee(authentication);
            if (employeeId != null && !employeeId.equals(employee.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own leave requests");
            }
            requests = leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId());
        }
        return requests.stream().map(this::toDTO).toList();
    }

    @Transactional
    public LeaveRequestDTO create(LeaveRequestDTO dto, Authentication authentication) {
        if (dto.getStartDate() == null || dto.getEndDate() == null || dto.getStartDate().isBefore(LocalDate.now()) || dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose valid leave dates starting today or later");
        }
        if (ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) > 365) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A leave request cannot exceed 366 days");
        }
        Employee employee = getCurrentEmployee(authentication);
        LeaveType type = leaveTypeRepository.findById(dto.getLeaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with id: " + dto.getLeaveTypeId()));
        LeaveRequest request = new LeaveRequest();
        request.setEmployee(employee);
        request.setLeaveType(type);
        request.setStartDate(dto.getStartDate());
        request.setEndDate(dto.getEndDate());
        request.setReason(dto.getReason().trim());
        request.setStatus(LeaveStatus.PENDING);
        return toDTO(leaveRequestRepository.save(request));
    }

    @Transactional
    public LeaveRequestDTO decide(Long requestId, LeaveDecisionDTO decision) {
        if (decision.getStatus() != LeaveStatus.APPROVED && decision.getStatus() != LeaveStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A request can only be approved or rejected");
        }
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + requestId));
        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This leave request has already been decided");
        }
        request.setStatus(decision.getStatus());
        request.setDecisionNote(decision.getNote());
        request.setDecidedAt(java.time.LocalDateTime.now());
        LeaveRequest saved = leaveRequestRepository.save(request);
        if (saved.getStatus() == LeaveStatus.APPROVED && saved.getEmployee().getEmail() != null && !saved.getEmployee().getEmail().isBlank()) {
            String subject = "Your leave request was approved";
            String body = "Hello " + saved.getEmployee().getFirstName() + ",\n\n"
                    + "Your " + saved.getLeaveType().getName() + " leave request from " + saved.getStartDate()
                    + " to " + saved.getEndDate() + " has been approved."
                    + (saved.getDecisionNote() == null || saved.getDecisionNote().isBlank() ? "" : "\n\nApprover note: " + saved.getDecisionNote())
                    + "\n\nPeopleOS";
            emailService.sendEmail(saved.getEmployee().getEmail(), subject, body);
        }
        return toDTO(saved);
    }

    private Employee getCurrentEmployee(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getEmployee() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Your login is not linked to an employee profile. Ask the leave approver to link it.");
        }
        return user.getEmployee();
    }

    private boolean isApprover(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_LEAVE_APPROVER"));
    }

    private LeaveRequestDTO toDTO(LeaveRequest request) {
        LeaveRequestDTO dto = new LeaveRequestDTO();
        dto.setId(request.getId());
        dto.setEmployeeId(request.getEmployee().getId());
        dto.setEmployeeName(request.getEmployee().getFirstName() + " " + request.getEmployee().getLastName());
        dto.setEmployeeEmail(request.getEmployee().getEmail());
        dto.setLeaveTypeId(request.getLeaveType().getId());
        dto.setLeaveTypeName(request.getLeaveType().getName());
        dto.setStartDate(request.getStartDate());
        dto.setEndDate(request.getEndDate());
        dto.setReason(request.getReason());
        dto.setStatus(request.getStatus());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setDecidedAt(request.getDecidedAt());
        dto.setDecisionNote(request.getDecisionNote());
        return dto;
    }
}
