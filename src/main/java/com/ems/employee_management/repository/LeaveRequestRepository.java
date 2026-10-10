package com.ems.employee_management.repository;

import com.ems.employee_management.entity.LeaveRequest;
import com.ems.employee_management.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
    List<LeaveRequest> findAllByOrderByCreatedAtDesc();
    List<LeaveRequest> findByStatusOrderByCreatedAtAsc(LeaveStatus status);
}
