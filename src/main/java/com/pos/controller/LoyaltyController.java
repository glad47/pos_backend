package com.pos.controller;

import com.pos.model.Loyalty;
import com.pos.service.LoyaltyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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

    /**
     * GET /api/loyalty/all
     * Returns all loyalty programs in a grouped format suitable for sync.
     * Groups by odoo_program_id with all eligible barcodes listed.
     * This mirrors the product sync pattern (/api/products/all).
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllForSync() {
        try {
            List<Loyalty> all = loyaltyService.getAllLoyalties();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", all,
                "count", all.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
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
            String filename = file.getOriginalFilename();
            List<Loyalty> imported;
            if (filename != null && filename.toLowerCase().endsWith(".csv")) {
                imported = loyaltyService.importFromCsv(file);
            } else {
                imported = loyaltyService.importFromExcel(file);
            }
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "imported", imported.size(),
                "data", imported
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Error importing loyalties: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/loyalty/sync
     * Bulk upsert loyalty programs from sync service.
     * Accepts a list of loyalty program objects and upserts by odoo_program_id.
     */
    @PostMapping("/sync")
    public ResponseEntity<?> syncLoyalties(@RequestBody List<Loyalty> loyalties) {
        try {
            List<Loyalty> synced = loyaltyService.bulkUpsert(loyalties);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "synced", synced.size(),
                "data", synced
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
}
