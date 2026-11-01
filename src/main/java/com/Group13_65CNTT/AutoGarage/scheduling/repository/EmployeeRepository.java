package com.Group13_65CNTT.AutoGarage.scheduling.repository;

import com.Group13_65CNTT.AutoGarage.scheduling.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByUserId(Long userId);

    Optional<Employee> findByEmployeeCode(String employeeCode);

    boolean existsByEmployeeCode(String employeeCode);

    java.util.List<Employee> findByIsActiveTrue();

    boolean existsByUserId(Long userId);

    Page<Employee> findByIsActive(Boolean isActive, Pageable pageable);
}
