package com.ems.employee_management.controller;

import com.ems.employee_management.dto.EmployeeDTO;
import com.ems.employee_management.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private EmployeeController employeeController;

    private EmployeeDTO employeeDTO;

    @BeforeEach
    void setUp() {
        employeeDTO = new EmployeeDTO();
        employeeDTO.setId(1L);
        employeeDTO.setFirstName("John");
        employeeDTO.setLastName("Doe");
        employeeDTO.setEmail("john@test.com");
        employeeDTO.setSalary(75000.0);
    }

    @Test
    void getAllEmployees_ShouldReturn200WithList() {
        when(employeeService.getAllEmployees())
                .thenReturn(Arrays.asList(employeeDTO));

        ResponseEntity<List<EmployeeDTO>> response = employeeController.getAllEmployees();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("John", response.getBody().get(0).getFirstName());
        verify(employeeService, times(1)).getAllEmployees();
    }

    @Test
    void createEmployee_ShouldReturn201() {
        when(employeeService.createEmployee(any(EmployeeDTO.class)))
                .thenReturn(employeeDTO);

        ResponseEntity<EmployeeDTO> response = employeeController.createEmployee(employeeDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("John", response.getBody().getFirstName());
        assertEquals("john@test.com", response.getBody().getEmail());
        verify(employeeService, times(1)).createEmployee(any(EmployeeDTO.class));
    }

    @Test
    void getEmployeeById_ShouldReturn200() {
        when(employeeService.getEmployeeById(1L)).thenReturn(employeeDTO);

        ResponseEntity<EmployeeDTO> response = employeeController.getEmployeeById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("John", response.getBody().getFirstName());
        verify(employeeService, times(1)).getEmployeeById(1L);
    }

    @Test
    void deleteEmployee_ShouldReturn200() {
        doNothing().when(employeeService).deleteEmployee(1L);

        ResponseEntity<String> response = employeeController.deleteEmployee(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Employee deleted successfully", response.getBody());
        verify(employeeService, times(1)).deleteEmployee(1L);
    }

    @Test
    void updateEmployee_ShouldReturn200WithUpdatedData() {
        EmployeeDTO updatedDTO = new EmployeeDTO();
        updatedDTO.setId(1L);
        updatedDTO.setFirstName("John Updated");
        updatedDTO.setLastName("Doe");
        updatedDTO.setEmail("johnupdated@test.com");
        updatedDTO.setSalary(90000.0);

        when(employeeService.updateEmployee(any(Long.class), any(EmployeeDTO.class)))
                .thenReturn(updatedDTO);

        ResponseEntity<EmployeeDTO> response = employeeController.updateEmployee(1L, updatedDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("John Updated", response.getBody().getFirstName());
        assertEquals(90000.0, response.getBody().getSalary());
        verify(employeeService, times(1)).updateEmployee(any(Long.class), any(EmployeeDTO.class));
    }
}