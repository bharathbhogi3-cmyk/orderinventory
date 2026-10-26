package com.apple.supply.orderinventory.repository;

import com.apple.supply.orderinventory.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :productId")
    Optional<Product> findByIdForUpdate(
            @Param("productId") Long productId);

    Page<Product> findByCategory(
            String category, Pageable pageable);

    Page<Product> findByActive(
            Boolean active, Pageable pageable);

    Page<Product> findByCategoryAndActive(
            String category, Boolean active, Pageable pageable);
}