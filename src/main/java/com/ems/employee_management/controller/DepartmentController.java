package com.ems.employee_management.controller;

import com.ems.employee_management.entity.Department;
import com.ems.employee_management.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    @Autowired
    private DepartmentRepository departmentRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('LEAVE_APPROVER', 'ADMIN')")
    public Department createDepartment(@RequestBody Department department) {
        return departmentRepository.save(department);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LEAVE_APPROVER', 'ADMIN')")
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }
}
