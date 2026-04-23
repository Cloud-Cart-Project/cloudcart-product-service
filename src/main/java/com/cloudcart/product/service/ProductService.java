package com.cloudcart.product.service;

import com.cloudcart.product.entity.Product;
import com.cloudcart.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Product Service — Business logic + Redis cache management.
 *
 * Cache names:
 *   "products"  → lists (all products or per-category)
 *   "product"   → single product by id
 *
 * Eviction policy:
 *   Any mutation (create, update, delete, stock change) evicts ALL "products" lists
 *   and the specific "product" entry so the admin dashboard always shows fresh data.
 *
 * Future readiness:
 *   Replace @CacheEvict with event publishing (e.g., ProductUpdatedEvent) when
 *   a message broker is introduced, without touching the controller layer.
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // ─── READ OPERATIONS (cached) ────────────────────────────────────────────

    @Cacheable(value = "products", key = "'all'")
    public List<Product> getAllProducts() {
        log.debug("Cache MISS products:all — loading from DB");
        return productRepository.findAll();
    }

    @Cacheable(value = "products", key = "'category:' + #category")
    public List<Product> getProductsByCategory(String category) {
        log.debug("Cache MISS products:category:{} — loading from DB", category);
        return productRepository.findByCategory(category);
    }

    @Cacheable(value = "products", key = "'search:' + #search")
    public List<Product> searchProducts(String search) {
        log.debug("Cache MISS products:search:{} — loading from DB", search);
        return productRepository.findByNameContainingIgnoreCase(search);
    }

    @Cacheable(value = "product", key = "#id")
    public Optional<Product> getProductById(Long id) {
        log.debug("Cache MISS product:{} — loading from DB", id);
        return productRepository.findById(id);
    }

    // ─── WRITE OPERATIONS (invalidate cache) ─────────────────────────────────

    @CacheEvict(value = "products", allEntries = true)
    public Product createProduct(Product product) {
        Product saved = productRepository.save(product);
        log.info("Cache EVICT products:all — new product created id={}", saved.getId());
        return saved;
    }

    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "products", allEntries = true)
    })
    public Optional<Product> updateProduct(Long id, Product productDetails) {
        return productRepository.findById(id).map(product -> {
            product.setName(productDetails.getName());
            product.setDescription(productDetails.getDescription());
            product.setCategory(productDetails.getCategory());
            product.setPrice(productDetails.getPrice());
            product.setStock(productDetails.getStock());
            product.setImageUrl(productDetails.getImageUrl());
            Product saved = productRepository.save(product);
            log.info("Cache EVICT product:{} + products:all — product updated", id);
            return saved;
        });
    }

    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "products", allEntries = true)
    })
    public boolean deleteProduct(Long id) {
        return productRepository.findById(id).map(product -> {
            productRepository.delete(product);
            log.info("Cache EVICT product:{} + products:all — product deleted", id);
            return true;
        }).orElse(false);
    }

    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "products", allEntries = true)
    })
    public StockUpdateResult updateStock(Long id, Integer quantity) {
        if (!productRepository.existsById(id)) {
            return StockUpdateResult.NOT_FOUND;
        }
        int updatedRows = productRepository.updateStockSafely(id, quantity);
        if (updatedRows == 0) {
            return StockUpdateResult.INSUFFICIENT_STOCK;
        }
        log.info("Cache EVICT product:{} + products:all — stock updated by {}", id, quantity);
        return StockUpdateResult.SUCCESS;
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public enum StockUpdateResult {
        SUCCESS, NOT_FOUND, INSUFFICIENT_STOCK
    }
}
