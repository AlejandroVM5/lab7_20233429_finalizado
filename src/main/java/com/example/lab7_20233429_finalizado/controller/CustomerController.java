package com.example.lab7_20233429_finalizado.controller;

import com.example.lab7_20233429_finalizado.entity.Customer;
import com.example.lab7_20233429_finalizado.repository.CustomerRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // 1. LISTAR CLIENTES
    @GetMapping({"", "/"})
    public String listar(Model model) {
        model.addAttribute("listaClientes", customerRepository.findAll());
        return "customer/list";
    }

    // 2. NUEVO CLIENTE (Reutiliza vista "customer/form")
    @GetMapping("/new")
    public String nuevo(@ModelAttribute("customer") Customer customer) {
        customer.setDocumentType("DNI");
        return "customer/form";
    }

    // 3. EDITAR CLIENTE (Reutiliza la misma vista "customer/form")
    @GetMapping("/edit")
    public String editar(@ModelAttribute("customer") Customer customer,
                         @RequestParam("id") Integer id,
                         Model model) {
        Optional<Customer> opt = customerRepository.findById(id);
        if (opt.isPresent()) {
            model.addAttribute("customer", opt.get());
            return "customer/form";
        }
        return "redirect:/customer";
    }

    // 4. GUARDAR CLIENTE CON @ModelAttribute, @Valid y BindingResult
    @PostMapping("/save")
    public String guardar(@ModelAttribute("customer") @Valid Customer customer,
                          BindingResult bindingResult,
                          RedirectAttributes attr) {

        if (customer.getDocument() != null && !customer.getDocument().isBlank()
                && customer.getDocumentType() != null) {
            String doc = customer.getDocument().trim();

            // Validar que DNI contenga exactamente 8 digitos y RUC exactamente 11 digitos
            if ("DNI".equals(customer.getDocumentType()) && !doc.matches("\\d{8}")) {
                bindingResult.rejectValue("document", "error.customer",
                        "El DNI debe contener exactamente 8 dígitos numéricos");
            } else if ("RUC".equals(customer.getDocumentType()) && !doc.matches("\\d{11}")) {
                bindingResult.rejectValue("document", "error.customer",
                        "El RUC debe contener exactamente 11 dígitos numéricos");
            }

            // Validar que el numero de documento sea unico en la BD
            Optional<Customer> existente = customerRepository.findByDocument(doc);
            if (existente.isPresent()) {
                if (customer.getId() == null || !existente.get().getId().equals(customer.getId())) {
                    bindingResult.rejectValue("document", "error.customer",
                            "El número de documento ya se encuentra registrado en el sistema");
                }
            }
        }

        // Si existen errores, volver al formulario conservando los datos introducidos
        if (bindingResult.hasErrors()) {
            return "customer/form";
        }

        customer.setName(customer.getName().trim());
        customer.setDocument(customer.getDocument().trim());
        customerRepository.save(customer);
        attr.addFlashAttribute("msg", "Cliente guardado correctamente");
        return "redirect:/customer";
    }

    // 5. ELIMINAR CLIENTE
    @GetMapping("/delete")
    public String eliminar(@RequestParam("id") Integer id, RedirectAttributes attr) {
        try {
            customerRepository.deleteById(id);
            attr.addFlashAttribute("msg", "Cliente eliminado correctamente");
        } catch (Exception e) {
            attr.addFlashAttribute("error", "No se puede eliminar el cliente porque tiene pedidos registrados");
        }
        return "redirect:/customer";
    }
}
