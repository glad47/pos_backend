package com.pos.controller;

import com.pos.dto.CloseSessionDTO;
import com.pos.dto.OpenSessionDTO;
import com.pos.model.PosSession;
import com.pos.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/open")
    public ResponseEntity<PosSession> openSession(@RequestBody OpenSessionDTO dto) {
        PosSession session = sessionService.openSession(dto);
        return ResponseEntity.ok(session);
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
