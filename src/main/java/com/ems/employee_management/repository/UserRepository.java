package com.ems.employee_management.repository;

import com.ems.employee_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = "employee")
    List<User> findAllByOrderByIdDesc();

    long countByRole(String role);

    @EntityGraph(attributePaths = "employee")
    Optional<User> findWithEmployeeByUsername(String username);
}
