package com.ems.employee_management.repository;

import com.ems.employee_management.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Page<Employee> findByFirstNameContainingIgnoreCase(String firstName, Pageable pageable);

    @Query("SELECT e FROM Employee e WHERE e.department.id = :departmentId")
    Page<Employee> findByDepartmentId(@Param("departmentId") Long departmentId, Pageable pageable);
}