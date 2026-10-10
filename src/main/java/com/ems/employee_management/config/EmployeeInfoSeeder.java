package com.ems.employee_management.config;

import com.ems.employee_management.entity.EmployeeInfo;
import com.ems.employee_management.repository.EmployeeInfoRepository;
import com.ems.employee_management.repository.EmployeeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmployeeInfoSeeder {
    @Bean
    CommandLineRunner ensureEmployeeInfo(EmployeeRepository employees, EmployeeInfoRepository infos) {
        return args -> employees.findAll().forEach(employee -> {
            if (infos.findByEmployeeId(employee.getId()).isEmpty()) {
                EmployeeInfo info = new EmployeeInfo();
                info.setEmployee(employee);
                infos.save(info);
            }
        });
    }
}
