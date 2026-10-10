package com.ems.employee_management.service;

import com.ems.employee_management.dto.EmployeeDTO;
import com.ems.employee_management.entity.Employee;
import com.ems.employee_management.exception.ResourceNotFoundException;
import com.ems.employee_management.repository.DepartmentRepository;
import com.ems.employee_management.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee employee;
    private EmployeeDTO employeeDTO;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setEmail("john@test.com");
        employee.setSalary(75000.0);
        employee.setDepartment(null);

        employeeDTO = new EmployeeDTO();
        employeeDTO.setFirstName("John");
        employeeDTO.setLastName("Doe");
        employeeDTO.setEmail("john@test.com");
        employeeDTO.setSalary(75000.0);
        employeeDTO.setDepartmentId(null);
    }

    // TEST 1 - Create employee
    @Test
    void createEmployee_ShouldReturnSavedEmployee() {
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        EmployeeDTO result = employeeService.createEmployee(employeeDTO);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("john@test.com", result.getEmail());
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    // TEST 2 - Get all employees
    @Test
    void getAllEmployees_ShouldReturnList() {
        Employee employee2 = new Employee();
        employee2.setId(2L);
        employee2.setFirstName("Mary");
        employee2.setLastName("Jane");
        employee2.setEmail("mary@test.com");
        employee2.setSalary(80000.0);

        when(employeeRepository.findAll()).thenReturn(Arrays.asList(employee, employee2));

        List<EmployeeDTO> result = employeeService.getAllEmployees();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("John", result.get(0).getFirstName());
        assertEquals("Mary", result.get(1).getFirstName());
        verify(employeeRepository, times(1)).findAll();
    }

    // TEST 3 - Get employee by ID - success
    @Test
    void getEmployeeById_WhenExists_ShouldReturnEmployee() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        EmployeeDTO result = employeeService.getEmployeeById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("John", result.getFirstName());
    }

    // TEST 4 - Get employee by ID - not found
    @Test
    void getEmployeeById_WhenNotExists_ShouldThrowException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            employeeService.getEmployeeById(99L);
        });

        verify(employeeRepository, times(1)).findById(99L);
    }

    // TEST 5 - Update employee
    @Test
    void updateEmployee_WhenExists_ShouldReturnUpdatedEmployee() {
        EmployeeDTO updateDTO = new EmployeeDTO();
        updateDTO.setFirstName("John Updated");
        updateDTO.setLastName("Doe");
        updateDTO.setEmail("johnupdated@test.com");
        updateDTO.setSalary(90000.0);

        Employee updatedEmployee = new Employee();
        updatedEmployee.setId(1L);
        updatedEmployee.setFirstName("John Updated");
        updatedEmployee.setLastName("Doe");
        updatedEmployee.setEmail("johnupdated@test.com");
        updatedEmployee.setSalary(90000.0);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(updatedEmployee);

        EmployeeDTO result = employeeService.updateEmployee(1L, updateDTO);

        assertNotNull(result);
        assertEquals("John Updated", result.getFirstName());
        assertEquals(90000.0, result.getSalary());
    }

    // TEST 6 - Delete employee
    @Test
    void deleteEmployee_WhenExists_ShouldDeleteSuccessfully() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        doNothing().when(employeeRepository).delete(employee);

        assertDoesNotThrow(() -> employeeService.deleteEmployee(1L));

        verify(employeeRepository, times(1)).delete(employee);
    }

    // TEST 7 - Delete employee not found
    @Test
    void deleteEmployee_WhenNotExists_ShouldThrowException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            employeeService.deleteEmployee(99L);
        });
    }
}