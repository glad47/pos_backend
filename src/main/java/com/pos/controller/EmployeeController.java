package com.pos.controller;

import com.pos.dto.EmployeeDTO;
import com.pos.dto.EmployeeLoginDTO;
import com.pos.model.Employee;
import com.pos.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "*")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    /**
     * Authenticate employee
     * POST /api/employees/login
     * Body: { "employeeId": "041339718160", "pin": "2007" }
     * Returns: { "success": true/false, "message": "...", "employee": {...} }
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody EmployeeLoginDTO loginDTO) {
        Map<String, Object> response = employeeService.authenticate(loginDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all active employees
     * GET /api/employees/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<Employee>> getActiveEmployees() {
        return ResponseEntity.ok(employeeService.getAllActiveEmployees());
    }

    /**
     * Get all employees (for management)
     * GET /api/employees
     */
    @GetMapping
    public ResponseEntity<List<Employee>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    /**
     * Get employee by ID
     * GET /api/employees/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
        return employeeService.getEmployeeById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new employee
     * POST /api/employees
     * Body: EmployeeDTO
     */
    @PostMapping
    public ResponseEntity<?> createEmployee(@RequestBody EmployeeDTO dto) {
        try {
            Employee employee = employeeService.createEmployee(dto);
            return ResponseEntity.ok(employee);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Update employee
     * PUT /api/employees/{id}
     * Body: EmployeeDTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEmployee(@PathVariable Long id, @RequestBody EmployeeDTO dto) {
        try {
            Employee employee = employeeService.updateEmployee(id, dto);
            return ResponseEntity.ok(employee);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Update employee PIN
     * PUT /api/employees/{id}/pin
     * Body: { "pin": "1234" }
     */
    @PutMapping("/{id}/pin")
    public ResponseEntity<?> updatePin(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String newPin = body.get("pin");
            if (newPin == null || newPin.trim().isEmpty()) {
                throw new RuntimeException("الرقم السري مطلوب");
            }
            Employee employee = employeeService.updateEmployeePin(id, newPin);
            return ResponseEntity.ok(employee);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Deactivate employee
     * DELETE /api/employees/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivateEmployee(@PathVariable Long id) {
        try {
            employeeService.deactivateEmployee(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "تم تعطيل الموظف بنجاح");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Activate employee
     * PUT /api/employees/{id}/activate
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<?> activateEmployee(@PathVariable Long id) {
        try {
            Employee employee = employeeService.activateEmployee(id);
            return ResponseEntity.ok(employee);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
