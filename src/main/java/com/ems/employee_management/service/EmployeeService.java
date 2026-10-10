//package com.ems.employee_management.service;
//
//import com.ems.employee_management.entity.Employee;
//import com.ems.employee_management.repository.EmployeeRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import com.ems.employee_management.exception.ResourceNotFoundException;
//
//import java.util.List;

//@Service
//public class EmployeeService {
//
//    @Autowired
//    private EmployeeRepository employeeRepository;
//
//    // CREATE
//    public Employee createEmployee(Employee employee) {
//        return employeeRepository.save(employee);
//    }
//
//    // READ ALL
//    public List<Employee> getAllEmployees() {
//        return employeeRepository.findAll();
//    }
//
//    // READ ONE
//    public Employee getEmployeeById(Long id) {
//        return employeeRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
//    }
//
//    // UPDATE
//    public Employee updateEmployee(Long id, Employee employeeDetails) {
//        Employee employee = getEmployeeById(id);
//        employee.setFirstName(employeeDetails.getFirstName());
//        employee.setLastName(employeeDetails.getLastName());
//        employee.setEmail(employeeDetails.getEmail());
//        employee.setDepartment(employeeDetails.getDepartment());
//        employee.setSalary(employeeDetails.getSalary());
//        return employeeRepository.save(employee);
//    }
//
//    // DELETE
//    public void deleteEmployee(Long id) {
//        Employee employee = getEmployeeById(id);
//        employeeRepository.delete(employee);
//    }
//}


package com.ems.employee_management.service;

import com.ems.employee_management.dto.EmployeeDTO;
import com.ems.employee_management.entity.Employee;
import com.ems.employee_management.entity.EmployeeInfo;
import com.ems.employee_management.exception.ResourceNotFoundException;
import com.ems.employee_management.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import com.ems.employee_management.entity.Department;
import com.ems.employee_management.repository.DepartmentRepository;
import com.ems.employee_management.repository.UserRepository;
import com.ems.employee_management.entity.User;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;



    private EmployeeDTO convertToDTO(Employee employee) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(employee.getId());
        dto.setFirstName(employee.getFirstName());
        dto.setLastName(employee.getLastName());
        dto.setEmail(employee.getEmail());
        dto.setSalary(employee.getSalary());
        if (employee.getDepartment() != null) {
            dto.setDepartmentId(employee.getDepartment().getId());
            dto.setDepartmentName(employee.getDepartment().getName());
        }
        if (employee.getUser() != null) dto.setUserId(employee.getUser().getId());
        return dto;
    }

    private Employee convertToEntity(EmployeeDTO dto) {
        Employee employee = new Employee();
        employee.setFirstName(dto.getFirstName());
        employee.setLastName(dto.getLastName());
        employee.setEmail(dto.getEmail());
        employee.setSalary(dto.getSalary());
        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + dto.getDepartmentId()));
            employee.setDepartment(department);
        }
        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getUserId()));
            if (user.getEmployee() != null) throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "This login is already linked to an employee");
            employee.setUser(user);
        }
        EmployeeInfo info = new EmployeeInfo();
        info.setEmployee(employee);
        employee.setEmployeeInfo(info);
        return employee;
    }

    @Transactional
    public EmployeeDTO createEmployee(EmployeeDTO dto) {
        Employee employee = convertToEntity(dto);
        Employee saved = employeeRepository.save(employee);

        emailService.sendEmail(
                saved.getEmail(),
                "Welcome to the company!",
                "Hi " + saved.getFirstName() + ",\n\nYour employee profile has been created successfully.\n\nRegards,\nHR Team"
        );

        return convertToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<EmployeeDTO> getAllEmployees() {
        return employeeRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmployeeDTO getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return convertToDTO(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeDTO getMyEmployee(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getEmployee() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "No employee profile is linked to this login");
        }
        return convertToDTO(user.getEmployee());
    }

    @Transactional
    public EmployeeDTO updateEmployee(Long id, EmployeeDTO dto) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        employee.setFirstName(dto.getFirstName());
        employee.setLastName(dto.getLastName());
        employee.setEmail(dto.getEmail());
        employee.setSalary(dto.getSalary());
        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + dto.getDepartmentId()));
            employee.setDepartment(department);
        } else employee.setDepartment(null);
        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getUserId()));
            if (user.getEmployee() != null && !user.getEmployee().getId().equals(employee.getId())) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT, "This login is already linked to another employee");
            }
            employee.setUser(user);
        } else employee.setUser(null);
        if (employee.getEmployeeInfo() == null) {
            EmployeeInfo info = new EmployeeInfo();
            info.setEmployee(employee);
            employee.setEmployeeInfo(info);
        }
        Employee updated = employeeRepository.save(employee);
        return convertToDTO(updated);
    }

    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        employeeRepository.delete(employee);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<EmployeeDTO> getAllEmployeesPaginated(
            org.springframework.data.domain.Pageable pageable) {
        return employeeRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<EmployeeDTO> searchByFirstName(
            String firstName, org.springframework.data.domain.Pageable pageable) {
        return employeeRepository.findByFirstNameContainingIgnoreCase(firstName, pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<EmployeeDTO> getByDepartment(
            Long departmentId, org.springframework.data.domain.Pageable pageable) {
        return employeeRepository.findByDepartmentId(departmentId, pageable)
                .map(this::convertToDTO);
    }


}
