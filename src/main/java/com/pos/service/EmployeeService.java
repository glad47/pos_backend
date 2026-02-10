package com.pos.service;

import com.pos.dto.EmployeeDTO;
import com.pos.dto.EmployeeLoginDTO;
import com.pos.model.Employee;
import com.pos.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * Authenticate employee with barcode (employeeId) and PIN
     */
    public Map<String, Object> authenticate(EmployeeLoginDTO loginDTO) {
        Map<String, Object> response = new HashMap<>();
        
        Optional<Employee> employeeOpt = employeeRepository.findByEmployeeIdAndPin(
            loginDTO.getEmployeeId(), 
            loginDTO.getPin()
        );
        
        if (employeeOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "معرف الموظف أو الرقم السري غير صحيح");
            return response;
        }
        
        Employee employee = employeeOpt.get();
        
        if (!employee.getActive()) {
            response.put("success", false);
            response.put("message", "هذا الموظف غير نشط");
            return response;
        }
        
        response.put("success", true);
        response.put("message", "تم تسجيل الدخول بنجاح");
        response.put("employee", convertToDTO(employee));
        
        return response;
    }

    /**
     * Get all active employees
     */
    public List<Employee> getAllActiveEmployees() {
        return employeeRepository.findByActiveTrueOrderByNameAsc();
    }

    /**
     * Get all employees (for management)
     */
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    /**
     * Get employee by ID
     */
    public Optional<Employee> getEmployeeById(Long id) {
        return employeeRepository.findById(id);
    }

    /**
     * Get employee by employee ID
     */
    public Optional<Employee> getEmployeeByEmployeeId(String employeeId) {
        return employeeRepository.findByEmployeeId(employeeId);
    }

    /**
     * Create new employee
     */
    @Transactional
    public Employee createEmployee(EmployeeDTO dto) {
        // Validate unique employee ID
        if (employeeRepository.existsByEmployeeId(dto.getEmployeeId())) {
            throw new RuntimeException("معرف الموظف موجود بالفعل");
        }
        
        // Validate unique badge ID if provided
        if (dto.getBadgeId() != null && !dto.getBadgeId().trim().isEmpty()) {
            if (employeeRepository.existsByBadgeId(dto.getBadgeId())) {
                throw new RuntimeException("رقم الشارة موجود بالفعل");
            }
        }
        
        Employee employee = new Employee();
        employee.setEmployeeId(dto.getEmployeeId());
        employee.setBadgeId(dto.getBadgeId());
        employee.setName(dto.getName());
        employee.setPin(dto.getPin());
        employee.setSaleUser(dto.getSaleUser() != null ? dto.getSaleUser() : false);
        employee.setReturnUser(dto.getReturnUser() != null ? dto.getReturnUser() : false);
        employee.setManagerUser(dto.getManagerUser() != null ? dto.getManagerUser() : false);
        employee.setActive(dto.getActive() != null ? dto.getActive() : true);
        
        return employeeRepository.save(employee);
    }

    /**
     * Update employee
     */
    @Transactional
    public Employee updateEmployee(Long id, EmployeeDTO dto) {
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("الموظف غير موجود"));
        
        // Validate unique employee ID if changed
        if (!employee.getEmployeeId().equals(dto.getEmployeeId())) {
            if (employeeRepository.existsByEmployeeId(dto.getEmployeeId())) {
                throw new RuntimeException("معرف الموظف موجود بالفعل");
            }
            employee.setEmployeeId(dto.getEmployeeId());
        }
        
        // Validate unique badge ID if changed
        if (dto.getBadgeId() != null && !dto.getBadgeId().equals(employee.getBadgeId())) {
            if (employeeRepository.existsByBadgeId(dto.getBadgeId())) {
                throw new RuntimeException("رقم الشارة موجود بالفعل");
            }
            employee.setBadgeId(dto.getBadgeId());
        }
        
        employee.setName(dto.getName());
        
        // Only update PIN if provided
        if (dto.getPin() != null && !dto.getPin().trim().isEmpty()) {
            employee.setPin(dto.getPin());
        }
        
        employee.setSaleUser(dto.getSaleUser() != null ? dto.getSaleUser() : false);
        employee.setReturnUser(dto.getReturnUser() != null ? dto.getReturnUser() : false);
        employee.setManagerUser(dto.getManagerUser() != null ? dto.getManagerUser() : false);
        employee.setActive(dto.getActive() != null ? dto.getActive() : true);
        
        return employeeRepository.save(employee);
    }

    /**
     * Update employee PIN only
     */
    @Transactional
    public Employee updateEmployeePin(Long id, String newPin) {
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("الموظف غير موجود"));
        
        employee.setPin(newPin);
        return employeeRepository.save(employee);
    }

    /**
     * Delete (deactivate) employee
     */
    @Transactional
    public void deactivateEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("الموظف غير موجود"));
        
        employee.setActive(false);
        employeeRepository.save(employee);
    }

    /**
     * Activate employee
     */
    @Transactional
    public Employee activateEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("الموظف غير موجود"));
        
        employee.setActive(true);
        return employeeRepository.save(employee);
    }

    /**
     * Convert Employee entity to DTO
     */
    private EmployeeDTO convertToDTO(Employee employee) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(employee.getId());
        dto.setEmployeeId(employee.getEmployeeId());
        dto.setBadgeId(employee.getBadgeId());
        dto.setName(employee.getName());
        // Don't return PIN in DTO for security
        dto.setSaleUser(employee.getSaleUser());
        dto.setReturnUser(employee.getReturnUser());
        dto.setManagerUser(employee.getManagerUser());
        dto.setActive(employee.getActive());
        return dto;
    }
}
