package com.pos.config;

import com.pos.model.*;
import com.pos.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LoyaltyRepository loyaltyRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @Override
    public void run(String... args) {
        // Only load sample data if no products exist
        if (productRepository.count() > 0) {
            return;
        }

        // Sample Products
        createProduct("001", "Coffee", "Hot brewed coffee", 3.99, 100, "Beverages", 0.08);
        createProduct("002", "Latte", "Espresso with steamed milk", 4.99, 80, "Beverages", 0.08);
        createProduct("003", "Cappuccino", "Espresso with foamed milk", 4.49, 75, "Beverages", 0.08);
        createProduct("004", "Tea", "Hot tea", 2.99, 120, "Beverages", 0.08);
        createProduct("005", "Orange Juice", "Fresh squeezed OJ", 3.49, 50, "Beverages", 0.08);
        createProduct("006", "Croissant", "Butter croissant", 2.99, 40, "Bakery", 0.05);
        createProduct("007", "Muffin", "Blueberry muffin", 3.49, 35, "Bakery", 0.05);
        createProduct("008", "Bagel", "Plain bagel", 2.49, 60, "Bakery", 0.05);
        createProduct("009", "Sandwich", "Ham & cheese sandwich", 6.99, 30, "Food", 0.08);
        createProduct("010", "Salad", "Garden salad", 7.99, 25, "Food", 0.08);
        createProduct("011", "Soup", "Soup of the day", 5.99, 20, "Food", 0.08);
        createProduct("012", "Cookie", "Chocolate chip cookie", 1.99, 100, "Snacks", 0.05);

        // Sample Loyalty: Buy 2 Coffees, get 1 Cookie free (BUY_X_GET_Y = type 1)
        Loyalty bogo1 = new Loyalty();
        bogo1.setName("Buy 2 Coffees Get 1 Cookie Free");
        bogo1.setType(1); // BUY_X_GET_Y
        bogo1.setTriggerProductIds("001");
        bogo1.setRewardProductIds("012");
        bogo1.setMinQuantity(2);
        bogo1.setRewardQuantity(1);
        bogo1.setDiscountPercent(BigDecimal.ZERO);
        bogo1.setStartDate(LocalDateTime.now().minusDays(30));
        bogo1.setEndDate(LocalDateTime.now().plusDays(60));
        bogo1.setActive(true);
        loyaltyRepository.save(bogo1);

        // Sample Loyalty: Buy any Coffee (001,002,003), get 10% off any Bakery (006,007,008) (DISCOUNT = type 0)
        Loyalty disc1 = new Loyalty();
        disc1.setName("Coffee + Bakery 10% Off");
        disc1.setType(0); // DISCOUNT
        disc1.setTriggerProductIds("001,002,003");
        disc1.setRewardProductIds("006,007,008");
        disc1.setMinQuantity(1);
        disc1.setRewardQuantity(1);
        disc1.setDiscountPercent(new BigDecimal("10"));
        disc1.setStartDate(LocalDateTime.now().minusDays(30));
        disc1.setEndDate(LocalDateTime.now().plusDays(60));
        disc1.setActive(true);
        loyaltyRepository.save(disc1);

        // Sample Promotions
        Promotion promo1 = new Promotion();
        promo1.setName("10% Off Food");
        promo1.setDescription("10% discount on all food items");
        promo1.setDiscountType(Promotion.DiscountType.PERCENTAGE);
        promo1.setDiscountValue(new BigDecimal("10"));
        promo1.setCategory("Food");
        promo1.setStartDate(LocalDateTime.now().minusDays(30));
        promo1.setEndDate(LocalDateTime.now().plusDays(60));
        promo1.setActive(true);
        promotionRepository.save(promo1);

        Promotion promo2 = new Promotion();
        promo2.setName("$0.50 Off Cookies");
        promo2.setDescription("50 cents off each cookie");
        promo2.setDiscountType(Promotion.DiscountType.FIXED_AMOUNT);
        promo2.setDiscountValue(new BigDecimal("0.50"));
        promo2.setProductBarcode("012");
        promo2.setStartDate(LocalDateTime.now().minusDays(30));
        promo2.setEndDate(LocalDateTime.now().plusDays(60));
        promo2.setActive(true);
        promotionRepository.save(promo2);

        System.out.println("Sample data loaded successfully!");
    }

    private void createProduct(String barcode, String name, String description, 
                               double price, int stock, String category, double taxRate) {
        Product product = new Product();
        product.setBarcode(barcode);
        product.setName(name);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setCategory(category);
        product.setTaxRate(new BigDecimal(taxRate));
        product.setActive(true);
        productRepository.save(product);
    }
}
