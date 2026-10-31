package com.example.lab7_20233429_finalizado.controller;

import com.example.lab7_20233429_finalizado.entity.Customer;
import com.example.lab7_20233429_finalizado.entity.Invoice;
import com.example.lab7_20233429_finalizado.entity.InvoiceDetail;
import com.example.lab7_20233429_finalizado.entity.Product;
import com.example.lab7_20233429_finalizado.repository.CustomerRepository;
import com.example.lab7_20233429_finalizado.repository.InvoiceDetailRepository;
import com.example.lab7_20233429_finalizado.repository.InvoiceRepository;
import com.example.lab7_20233429_finalizado.repository.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/invoice")
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceDetailRepository invoiceDetailRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public InvoiceController(InvoiceRepository invoiceRepository,
                             InvoiceDetailRepository invoiceDetailRepository,
                             CustomerRepository customerRepository,
                             ProductRepository productRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceDetailRepository = invoiceDetailRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    // 1. LISTADO DE PEDIDOS
    @GetMapping({"", "/"})
    public String listar(Model model) {
        model.addAttribute("listaPedidos", invoiceRepository.findAll());
        return "invoice/list";
    }

    // 2. FORMULARIO PARA REGISTRAR UN PEDIDO
    @GetMapping("/new")
    public String nuevo(@ModelAttribute("invoice") Invoice invoice, Model model) {
        invoice.setType("BOLETA");
        invoice.setDate(LocalDate.now());

        List<Product> productos = productRepository.findAll();
        model.addAttribute("listaClientes", customerRepository.findAll());
        model.addAttribute("listaProductos", productos);

        List<String> cantidades = new ArrayList<>();
        for (int i = 0; i < productos.size(); i++) {
            cantidades.add("0");
        }
        model.addAttribute("cantidades", cantidades);

        return "invoice/form";
    }

    // 3. REGISTRAR PEDIDO CON VALIDACIONES POR CAMPO Y MENSAJES GLOBALES
    @PostMapping("/save")
    public String guardar(@ModelAttribute("invoice") @Valid Invoice invoice,
                          BindingResult bindingResult,
                          @RequestParam(value = "productIds", required = false) List<Integer> productIds,
                          @RequestParam(value = "quantities", required = false) List<String> quantitiesStr,
                          Model model,
                          RedirectAttributes attr) {

        // --- A) VALIDAR CLIENTE Y REGLA CRUZADA (TIPO COMPROBANTE vs DOCUMENTO CLIENTE) ---
        if (invoice.getCustomer() == null || invoice.getCustomer().getId() == null) {
            bindingResult.rejectValue("customer", "error.invoice", "Debe seleccionar un cliente");
        } else {
            Optional<Customer> optCust = customerRepository.findById(invoice.getCustomer().getId());
            if (optCust.isEmpty()) {
                bindingResult.rejectValue("customer", "error.invoice", "El cliente seleccionado no es válido");
            } else {
                Customer cliente = optCust.get();
                invoice.setCustomer(cliente);

                // Regla global que depende de varios campos:
                // Factura solo puede emitirse a un cliente con RUC; Boleta solo a un cliente con DNI
                if ("FACTURA".equalsIgnoreCase(invoice.getType()) && !"RUC".equalsIgnoreCase(cliente.getDocumentType())) {
                    bindingResult.reject("error.incompatibilidad",
                            "Incompatibilidad: Una Factura solo puede emitirse a un cliente con RUC");
                } else if ("BOLETA".equalsIgnoreCase(invoice.getType()) && !"DNI".equalsIgnoreCase(cliente.getDocumentType())) {
                    bindingResult.reject("error.incompatibilidad",
                            "Incompatibilidad: Una Boleta solo puede emitirse a un cliente con DNI");
                }
            }
        }

        // --- B) PROCESAR PRODUCTOS, VALIDAR CANTIDADES, REPETICION Y STOCK ---
        List<InvoiceDetail> detallesValidos = new ArrayList<>();
        List<String> cantidadesParaVista = new ArrayList<>();
        Set<Integer> productosIncluidos = new HashSet<>();
        boolean tieneAlMenosUnProducto = false;

        if (productIds != null && quantitiesStr != null && productIds.size() == quantitiesStr.size()) {
            for (int i = 0; i < productIds.size(); i++) {
                Integer prodId = productIds.get(i);
                String cantTexto = quantitiesStr.get(i) != null ? quantitiesStr.get(i).trim() : "0";
                cantidadesParaVista.add(cantTexto.isEmpty() ? "0" : cantTexto);

                if (!cantTexto.isEmpty() && !"0".equals(cantTexto)) {
                    int cantidad;
                    try {
                        cantidad = Integer.parseInt(cantTexto);
                    } catch (NumberFormatException e) {
                        bindingResult.reject("error.cantidad",
                                "La cantidad ingresada para los productos debe ser un número entero válido");
                        continue;
                    }

                    // La cantidad de cada producto incluido debe ser un entero mayor que cero
                    if (cantidad < 0) {
                        bindingResult.reject("error.cantidad",
                                "La cantidad solicitada de un producto debe ser un entero mayor que cero");
                        continue;
                    }

                    if (cantidad > 0) {
                        tieneAlMenosUnProducto = true;

                        // No se puede repetir el mismo producto dentro de un pedido
                        if (!productosIncluidos.add(prodId)) {
                            bindingResult.reject("error.duplicado",
                                    "No se puede repetir el mismo producto dentro de un pedido");
                            continue;
                        }

                        Optional<Product> optProd = productRepository.findById(prodId);
                        if (optProd.isPresent()) {
                            Product prod = optProd.get();

                            // La cantidad no puede superar el stock disponible
                            if (cantidad > prod.getStock()) {
                                bindingResult.reject("error.stock",
                                        "La cantidad solicitada para '" + prod.getName()
                                                + "' (" + cantidad + ") supera el stock disponible (" + prod.getStock() + ")");
                            } else {
                                // Calculo en Java: subtotal = cantidad * precio del producto
                                InvoiceDetail det = new InvoiceDetail();
                                det.setInvoice(invoice);
                                det.setProduct(prod);
                                det.setQuantity(cantidad);
                                det.setPrice(prod.getPrice());
                                det.setSubtotal(cantidad * prod.getPrice());
                                detallesValidos.add(det);
                            }
                        }
                    }
                }
            }
        }

        // Debe incluirse al menos un producto
        if (!tieneAlMenosUnProducto) {
            bindingResult.reject("error.sinProductos",
                    "Debe incluirse al menos un producto con cantidad mayor que cero en el pedido");
        }

        // --- C) SI HAY ERRORES DE VALIDACION O DE BINDING, NO REGISTRAR EL PEDIDO NI SUS DETALLES ---
        if (bindingResult.hasErrors()) {
            // Al volver al formulario, conservamos la seleccion del cliente, tipo, fecha y cantidades
            model.addAttribute("listaClientes", customerRepository.findAll());
            model.addAttribute("listaProductos", productRepository.findAll());
            model.addAttribute("cantidades", cantidadesParaVista);
            return "invoice/form";
        }

        // --- D) GUARDAR EL PEDIDO, SUS DETALLES Y ACTUALIZAR EL STOCK ---
        Invoice pedidoGuardado = invoiceRepository.save(invoice);

        for (InvoiceDetail det : detallesValidos) {
            det.setInvoice(pedidoGuardado);
            invoiceDetailRepository.save(det);

            Product p = det.getProduct();
            p.setStock(p.getStock() - det.getQuantity());
            productRepository.save(p);
        }

        attr.addFlashAttribute("msg", "Pedido registrado exitosamente");
        return "redirect:/invoice";
    }
}
