package com.ems.employee_management.service;

import com.ems.employee_management.dto.AttendanceDTO;
import com.ems.employee_management.entity.Attendance;
import com.ems.employee_management.entity.AttendanceStatus;
import com.ems.employee_management.entity.Employee;
import com.ems.employee_management.exception.ResourceNotFoundException;
import com.ems.employee_management.repository.AttendanceRepository;
import com.ems.employee_management.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private AttendanceDTO convertToDTO(Attendance a) {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setId(a.getId());
        dto.setEmployeeId(a.getEmployee().getId());
        dto.setEmployeeName(a.getEmployee().getFirstName() + " " + a.getEmployee().getLastName());
        dto.setDate(a.getDate());
        dto.setCheckInTime(a.getCheckInTime());
        dto.setCheckOutTime(a.getCheckOutTime());
        dto.setStatus(a.getStatus());
        return dto;
    }

    // CHECK IN — called when employee starts their day
    public AttendanceDTO checkIn(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        LocalDate today = LocalDate.now();

        attendanceRepository.findByEmployeeIdAndDate(employeeId, today).ifPresent(a -> {
            throw new IllegalStateException("Already checked in today");
        });

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setDate(today);
        attendance.setCheckInTime(LocalTime.now());
        attendance.setStatus(AttendanceStatus.PRESENT);

        Attendance saved = attendanceRepository.save(attendance);
        return convertToDTO(saved);
    }

    // CHECK OUT — called when employee ends their day
    public AttendanceDTO checkOut(Long employeeId) {
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
                .orElseThrow(() -> new IllegalStateException("No check-in record found for today"));

        attendance.setCheckOutTime(LocalTime.now());
        Attendance updated = attendanceRepository.save(attendance);
        return convertToDTO(updated);
    }

    public List<AttendanceDTO> getMyAttendance(Long employeeId) {
        return attendanceRepository.findByEmployeeId(employeeId)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<AttendanceDTO> getAttendanceByDate(LocalDate date) {
        return attendanceRepository.findByDate(date)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<AttendanceDTO> getAttendanceInRange(Long employeeId, LocalDate start, LocalDate end) {
        return attendanceRepository.findByEmployeeIdAndDateBetween(employeeId, start, end)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }
}