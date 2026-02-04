package com.pos.controller;

import com.pos.model.Promotion;
import com.pos.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@CrossOrigin(origins = "*")
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    @GetMapping
    public List<Promotion> getAllPromotions() {
        return promotionService.getAllPromotions();
    }

    @GetMapping("/active")
    public List<Promotion> getActivePromotions() {
        return promotionService.getActivePromotions();
    }

    @PostMapping
    public Promotion createPromotion(@RequestBody Promotion promotion) {
        return promotionService.savePromotion(promotion);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Promotion> updatePromotion(@PathVariable Long id, @RequestBody Promotion promotion) {
        promotion.setId(id);
        return ResponseEntity.ok(promotionService.savePromotion(promotion));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.ok().build();
    }
}
