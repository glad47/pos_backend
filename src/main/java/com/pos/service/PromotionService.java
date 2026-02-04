package com.pos.service;

import com.pos.model.Promotion;
import com.pos.repository.PromotionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PromotionService {

    @Autowired
    private PromotionRepository promotionRepository;

    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAll();
    }

    public List<Promotion> getActivePromotions() {
        return promotionRepository.findActivePromotions(LocalDateTime.now());
    }

    public List<Promotion> getApplicablePromotions(String barcode, String category) {
        return promotionRepository.findApplicablePromotions(barcode, category, LocalDateTime.now());
    }

    public Promotion savePromotion(Promotion promotion) {
        return promotionRepository.save(promotion);
    }

    public void deletePromotion(Long id) {
        promotionRepository.findById(id).ifPresent(promotion -> {
            promotion.setActive(false);
            promotionRepository.save(promotion);
        });
    }
}
