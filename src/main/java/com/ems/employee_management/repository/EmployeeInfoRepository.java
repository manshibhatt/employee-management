package com.ems.employee_management.repository;

import com.ems.employee_management.entity.EmployeeInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeInfoRepository extends JpaRepository<EmployeeInfo, Long> {
    Optional<EmployeeInfo> findByEmployeeId(Long employeeId);
}
