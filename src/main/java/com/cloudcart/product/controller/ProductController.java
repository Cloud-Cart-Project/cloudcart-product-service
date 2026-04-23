package com.cloudcart.product.controller;

import com.cloudcart.product.entity.Product;
import com.cloudcart.product.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<Product> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category) {
        if (search != null && !search.isEmpty()) {
            return productRepository.findByNameContainingIgnoreCase(search);
        } else if (category != null && !category.isEmpty()) {
            return productRepository.findByCategory(category);
        }
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return productRepository.save(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setName(productDetails.getName());
                    product.setDescription(productDetails.getDescription());
                    product.setCategory(productDetails.getCategory());
                    product.setPrice(productDetails.getPrice());
                    product.setStock(productDetails.getStock());
                    product.setImageUrl(productDetails.getImageUrl());
                    return ResponseEntity.ok(productRepository.save(product));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(product -> {
                    productRepository.delete(product);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<?> updateStock(@PathVariable Long id, @RequestBody Map<String, Integer> payload) {
        if (!payload.containsKey("quantity")) {
            return ResponseEntity.badRequest().body("Missing 'quantity' in payload");
        }

        Integer diff = payload.get("quantity");
        
        // 1. Check if product exists
        if (!productRepository.existsById(id)) {
            return ResponseEntity.status(404).body("Product not found");
        }

        // 2. Attempt thread-safe update
        int updatedRows = productRepository.updateStockSafely(id, diff);
        
        if (updatedRows == 0) {
            // Since we know it exists, updatedRows=0 means stock condition failed
            return ResponseEntity.status(409).body("Insufficient stock");
        }
        
        // 3. Return the updated product
        return ResponseEntity.ok(productRepository.findById(id).orElse(null));
    }
}
