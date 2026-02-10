package com.pos.repository;

import com.pos.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    
    Optional<Employee> findByEmployeeId(String employeeId);
    
    Optional<Employee> findByBadgeId(String badgeId);
    
    Optional<Employee> findByEmployeeIdAndPin(String employeeId, String pin);
    
    List<Employee> findByActiveTrue();
    
    List<Employee> findByActiveTrueOrderByNameAsc();
    
    boolean existsByEmployeeId(String employeeId);
    
    boolean existsByBadgeId(String badgeId);
}
