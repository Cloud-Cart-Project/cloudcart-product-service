package com.cloudcart.product.controller;

import com.cloudcart.product.entity.Product;
import com.cloudcart.product.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ─── READ ──────────────────────────────────────────────────────────────────

    @GetMapping
    public List<Product> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category) {
        if (search != null && !search.isEmpty()) {
            return productService.searchProducts(search);
        } else if (category != null && !category.isEmpty()) {
            return productService.getProductsByCategory(category);
        }
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── WRITE (all evict cache via ProductService) ────────────────────────────

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return productService.createProduct(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
        return productService.updateProduct(id, productDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        boolean deleted = productService.deleteProduct(id);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<?> updateStock(@PathVariable Long id, @RequestBody Map<String, Integer> payload) {
        if (!payload.containsKey("quantity")) {
            return ResponseEntity.badRequest().body("Missing 'quantity' in payload");
        }

        ProductService.StockUpdateResult result = productService.updateStock(id, payload.get("quantity"));

        return switch (result) {
            case SUCCESS -> ResponseEntity.ok(productService.findById(id).orElse(null));
            case NOT_FOUND -> ResponseEntity.status(404).body("Product not found");
            case INSUFFICIENT_STOCK -> ResponseEntity.status(409).body("Insufficient stock");
        };
    }
}
