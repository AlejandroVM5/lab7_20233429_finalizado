package com.example.lab7_20233429_finalizado.repository;

import com.example.lab7_20233429_finalizado.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    // Buscar producto por nombre para validar que no se duplique
    Optional<Product> findByNameIgnoreCase(String name);
}
