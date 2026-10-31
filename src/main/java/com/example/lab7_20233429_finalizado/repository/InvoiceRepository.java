package com.example.lab7_20233429_finalizado.repository;

import com.example.lab7_20233429_finalizado.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
}
