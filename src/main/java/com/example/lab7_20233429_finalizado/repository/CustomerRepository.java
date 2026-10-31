package com.example.lab7_20233429_finalizado.repository;

import com.example.lab7_20233429_finalizado.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    // Buscar por numero de documento para verificar que sea unico
    Optional<Customer> findByDocument(String document);
}
