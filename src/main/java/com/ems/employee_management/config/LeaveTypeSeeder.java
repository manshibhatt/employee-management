package com.ems.employee_management.config;

import com.ems.employee_management.entity.LeaveType;
import com.ems.employee_management.repository.LeaveTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LeaveTypeSeeder {
    @Bean
    CommandLineRunner seedLeaveTypes(LeaveTypeRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                save(repository, "Annual leave", "Planned paid time off");
                save(repository, "Sick leave", "Time off for illness or medical care");
                save(repository, "Personal leave", "Personal or family commitments");
            }
        };
    }

    private void save(LeaveTypeRepository repository, String name, String description) {
        LeaveType type = new LeaveType();
        type.setName(name);
        type.setDescription(description);
        repository.save(type);
    }
}
