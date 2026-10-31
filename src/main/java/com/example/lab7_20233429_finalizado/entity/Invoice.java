package com.example.lab7_20233429_finalizado.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoice")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // El tipo debe ser Factura o Boleta
    @NotBlank(message = "El tipo de comprobante es obligatorio")
    @Pattern(regexp = "^(BOLETA|FACTURA)$", message = "El tipo de comprobante debe ser Factura o Boleta")
    @Column(name = "type", nullable = false, length = 10)
    private String type;

    // La fecha es obligatoria y no puede ser futura (@PastOrPresent)
    @NotNull(message = "La fecha es obligatoria")
    @PastOrPresent(message = "La fecha no puede ser futura")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "date", nullable = false)
    private LocalDate date;

    // Debe seleccionarse un cliente
    @NotNull(message = "Debe seleccionar un cliente")
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // Relacion OneToMany con las lineas de detalle del pedido
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<InvoiceDetail> details = new ArrayList<>();

    // Calculo en Java del total del pedido como la suma de subtotales
    public Double getTotal() {
        double suma = 0.0;
        if (details != null) {
            for (InvoiceDetail det : details) {
                if (det.getSubtotal() != null) {
                    suma += det.getSubtotal();
                }
            }
        }
        return suma;
    }

    // Getters y Setters (Sin Lombok)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public List<InvoiceDetail> getDetails() {
        return details;
    }

    public void setDetails(List<InvoiceDetail> details) {
        this.details = details;
    }
}
