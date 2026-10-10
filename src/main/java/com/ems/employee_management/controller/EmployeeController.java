//package com.ems.employee_management.controller;
//
//import com.ems.employee_management.entity.Employee;
//import com.ems.employee_management.service.EmployeeService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/employees")
//public class EmployeeController {
//
//    @Autowired
//    private EmployeeService employeeService;
//
//    // CREATE — POST /api/employees
//    @PostMapping
//    public ResponseEntity<Employee> createEmployee(@RequestBody Employee employee) {
//        Employee savedEmployee = employeeService.createEmployee(employee);
//        return new ResponseEntity<>(savedEmployee, HttpStatus.CREATED);
//    }
//
//    // READ ALL — GET /api/employees
//    @GetMapping
//    public ResponseEntity<List<Employee>> getAllEmployees() {
//        List<Employee> employees = employeeService.getAllEmployees();
//        return new ResponseEntity<>(employees, HttpStatus.OK);
//    }
//
//    // READ ONE — GET /api/employees/1
//    @GetMapping("/{id}")
//    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
//        Employee employee = employeeService.getEmployeeById(id);
//        return new ResponseEntity<>(employee, HttpStatus.OK);
//    }
//
//    // UPDATE — PUT /api/employees/1
//    @PutMapping("/{id}")
//    public ResponseEntity<Employee> updateEmployee(@PathVariable Long id, @RequestBody Employee employee) {
//        Employee updatedEmployee = employeeService.updateEmployee(id, employee);
//        return new ResponseEntity<>(updatedEmployee, HttpStatus.OK);
//    }
//
//    // DELETE — DELETE /api/employees/1
//    @DeleteMapping("/{id}")
//    public ResponseEntity<String> deleteEmployee(@PathVariable Long id) {
//        employeeService.deleteEmployee(id);
//        return new ResponseEntity<>("Employee deleted successfully", HttpStatus.OK);
//    }
//}


package com.ems.employee_management.controller;

import com.ems.employee_management.dto.EmployeeDTO;
import com.ems.employee_management.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @PostMapping
    @PreAuthorize("hasRole('LEAVE_APPROVER')")
    public ResponseEntity<EmployeeDTO> createEmployee(@Valid @RequestBody EmployeeDTO employeeDTO) {
        EmployeeDTO saved = employeeService.createEmployee(employeeDTO);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('LEAVE_APPROVER')")
    public ResponseEntity<List<EmployeeDTO>> getAllEmployees() {
        return new ResponseEntity<>(employeeService.getAllEmployees(), HttpStatus.OK);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<EmployeeDTO> getMyEmployee(Authentication authentication) {
        return ResponseEntity.ok(employeeService.getMyEmployee(authentication.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('LEAVE_APPROVER')")
    public ResponseEntity<EmployeeDTO> getEmployeeById(@PathVariable Long id) {
        return new ResponseEntity<>(employeeService.getEmployeeById(id), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LEAVE_APPROVER')")
    public ResponseEntity<EmployeeDTO> updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeDTO employeeDTO) {
        return new ResponseEntity<>(employeeService.updateEmployee(id, employeeDTO), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('LEAVE_APPROVER')")
    public ResponseEntity<String> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return new ResponseEntity<>("Employee deleted successfully", HttpStatus.OK);
    }

    @GetMapping("/page")
    @PreAuthorize("hasRole('LEAVE_APPROVER')")
    public ResponseEntity<Page<EmployeeDTO>> getEmployeesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return new ResponseEntity<>(employeeService.getAllEmployeesPaginated(pageable), HttpStatus.OK);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('LEAVE_APPROVER')")
    public ResponseEntity<Page<EmployeeDTO>> searchEmployees(
            @RequestParam String firstName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return new ResponseEntity<>(employeeService.searchByFirstName(firstName, pageable), HttpStatus.OK);
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasRole('LEAVE_APPROVER')")
    public ResponseEntity<Page<EmployeeDTO>> getByDepartment(
            @PathVariable Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return new ResponseEntity<>(employeeService.getByDepartment(departmentId, pageable), HttpStatus.OK);
    }
}
