package com.ems.employee_management.service;

import com.ems.employee_management.dto.EmployeeInfoDTO;
import com.ems.employee_management.entity.Employee;
import com.ems.employee_management.entity.EmployeeInfo;
import com.ems.employee_management.exception.ResourceNotFoundException;
import com.ems.employee_management.repository.EmployeeInfoRepository;
import com.ems.employee_management.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.ems.employee_management.entity.User;
import com.ems.employee_management.repository.UserRepository;

@Service
public class EmployeeInfoService {
    private final EmployeeRepository employeeRepository;
    private final EmployeeInfoRepository employeeInfoRepository;
    private final UserRepository userRepository;

    public EmployeeInfoService(EmployeeRepository employeeRepository, EmployeeInfoRepository employeeInfoRepository, UserRepository userRepository) {
        this.employeeRepository = employeeRepository;
        this.employeeInfoRepository = employeeInfoRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EmployeeInfoDTO getOrCreate(Long employeeId, Authentication authentication) {
        Employee employee = getEmployee(employeeId);
        boolean approver = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_LEAVE_APPROVER"));
        if (!approver) {
            User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
            if (user.getEmployee() == null || !user.getEmployee().getId().equals(employeeId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own employee information");
            }
        }
        EmployeeInfo info = employeeInfoRepository.findByEmployeeId(employeeId).orElseGet(() -> {
            EmployeeInfo created = new EmployeeInfo();
            created.setEmployee(employee);
            return employeeInfoRepository.save(created);
        });
        return toDTO(info, employeeId);
    }

    @Transactional
    public EmployeeInfoDTO save(Long employeeId, EmployeeInfoDTO dto) {
        Employee employee = getEmployee(employeeId);
        EmployeeInfo info = employeeInfoRepository.findByEmployeeId(employeeId).orElseGet(EmployeeInfo::new);
        info.setEmployee(employee);
        info.setJobTitle(dto.getJobTitle());
        info.setPhone(dto.getPhone());
        info.setAddress(dto.getAddress());
        info.setDateOfBirth(dto.getDateOfBirth());
        info.setHireDate(dto.getHireDate());
        info.setEmergencyContact(dto.getEmergencyContact());
        return toDTO(employeeInfoRepository.save(info), employeeId);
    }

    private Employee getEmployee(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
    }

    private EmployeeInfoDTO toDTO(EmployeeInfo info, Long employeeId) {
        EmployeeInfoDTO dto = new EmployeeInfoDTO();
        dto.setId(info.getId());
        dto.setEmployeeId(employeeId);
        dto.setJobTitle(info.getJobTitle());
        dto.setPhone(info.getPhone());
        dto.setAddress(info.getAddress());
        dto.setDateOfBirth(info.getDateOfBirth());
        dto.setHireDate(info.getHireDate());
        dto.setEmergencyContact(info.getEmergencyContact());
        return dto;
    }
}
