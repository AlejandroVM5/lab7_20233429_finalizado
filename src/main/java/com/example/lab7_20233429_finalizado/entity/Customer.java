package com.example.lab7_20233429_finalizado.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // El nombre es obligatorio
    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 100, message = "El nombre no debe exceder los 100 caracteres")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // El tipo de documento es obligatorio y solo puede ser DNI o RUC
    @NotBlank(message = "El tipo de documento es obligatorio")
    @Pattern(regexp = "^(DNI|RUC)$", message = "El tipo de documento solo puede ser DNI o RUC")
    @Column(name = "document_type", nullable = false, length = 10)
    private String documentType;

    // El numero de documento es obligatorio y unico
    @NotBlank(message = "El número de documento es obligatorio")
    @Column(name = "document", nullable = false, unique = true, length = 11)
    private String document;

    // Getters y Setters (Sin Lombok)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }
}
