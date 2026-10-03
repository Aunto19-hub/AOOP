package com.seamline.repository;

import com.seamline.domain.Employee;
import com.seamline.domain.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Employee> findByPhone(String phone);

    boolean existsByPhone(String phone);

    boolean existsByRole(Role role);

    List<Employee> findByEnabledFalseOrderByCreatedAtAsc();

    Optional<Employee> findByEmployeeIdIgnoreCase(String employeeId);
}
