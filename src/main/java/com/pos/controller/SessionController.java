package com.pos.controller;

import com.pos.dto.CloseSessionDTO;
import com.pos.dto.OpenSessionDTO;
import com.pos.model.PosSession;
import com.pos.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(origins = "*")
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @GetMapping
    public List<PosSession> getAllSessions() {
        return sessionService.getAllSessions();
    }

    @GetMapping("/open")
    public List<PosSession> getOpenSessions() {
        return sessionService.getOpenSessions();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PosSession> getSessionById(@PathVariable Long id) {
        return sessionService.getSessionById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Check if cashier has an active session
     * GET /api/sessions/check/{cashierName}
     * Returns: { "hasActiveSession": true/false, "session": {...} or null }
     */
    @GetMapping("/check/{cashierName}")
    public ResponseEntity<Map<String, Object>> checkActiveSession(@PathVariable String cashierName) {
        Map<String, Object> response = new HashMap<>();
        
        boolean hasActive = sessionService.hasActiveSession(cashierName);
        response.put("hasActiveSession", hasActive);
        
        if (hasActive) {
            sessionService.getActiveSession(cashierName).ifPresent(session -> {
                response.put("session", session);
            });
        } else {
            response.put("session", null);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get active session for cashier
     * GET /api/sessions/active/{cashierName}
     * Returns: PosSession or 404
     */
    @GetMapping("/active/{cashierName}")
    public ResponseEntity<PosSession> getActiveSession(@PathVariable String cashierName) {
        return sessionService.getActiveSession(cashierName)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Open new session or return existing active session
     * POST /api/sessions/open
     * Body: { "cashierName": "...", "openingCash": 0 }
     * Returns: PosSession (new or existing)
     */
    @PostMapping("/open")
    public ResponseEntity<?> openSession(@RequestBody OpenSessionDTO dto) {
        try {
            PosSession session = sessionService.openSession(dto);
            
            // Check if this was an existing session
            boolean isExisting = sessionService.hasActiveSession(dto.getCashierName());
            
            Map<String, Object> response = new HashMap<>();
            response.put("session", session);
            response.put("isExistingSession", isExisting);
            response.put("message", isExisting ? 
                "Continuing existing active session" : 
                "New session opened successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<PosSession> closeSession(@PathVariable Long id, @RequestBody CloseSessionDTO dto) {
        try {
            PosSession session = sessionService.closeSession(id, dto);
            return ResponseEntity.ok(session);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
