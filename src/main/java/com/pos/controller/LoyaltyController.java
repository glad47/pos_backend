package com.pos.controller;

import com.pos.model.Loyalty;
import com.pos.service.LoyaltyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/loyalty")
@CrossOrigin(origins = "*")
public class LoyaltyController {

    @Autowired
    private LoyaltyService loyaltyService;

    @GetMapping
    public List<Loyalty> getAllLoyalties() {
        return loyaltyService.getAllLoyalties();
    }

    @GetMapping("/active")
    public List<Loyalty> getActiveLoyalties() {
        return loyaltyService.getActiveLoyalties();
    }

    @PostMapping
    public Loyalty createLoyalty(@RequestBody Loyalty loyalty) {
        return loyaltyService.saveLoyalty(loyalty);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Loyalty> updateLoyalty(@PathVariable Long id, @RequestBody Loyalty loyalty) {
        loyalty.setId(id);
        return ResponseEntity.ok(loyaltyService.saveLoyalty(loyalty));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoyalty(@PathVariable Long id) {
        loyaltyService.deleteLoyalty(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/import")
    public ResponseEntity<?> importLoyalties(@RequestParam("file") MultipartFile file) {
        try {
            List<Loyalty> imported = loyaltyService.importFromExcel(file);
            return ResponseEntity.ok(imported);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error importing loyalties: " + e.getMessage());
        }
    }
}
