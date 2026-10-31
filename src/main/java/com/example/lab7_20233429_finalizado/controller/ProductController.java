package com.example.lab7_20233429_finalizado.controller;

import com.example.lab7_20233429_finalizado.entity.Product;
import com.example.lab7_20233429_finalizado.repository.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/product")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // 1. LISTAR PRODUCTOS DEL CATALOGO
    @GetMapping({"", "/"})
    public String listar(Model model) {
        model.addAttribute("listaProductos", productRepository.findAll());
        return "product/list";
    }

    // 2. NUEVO PRODUCTO (Reutiliza vista "product/form")
    @GetMapping("/new")
    public String nuevo(@ModelAttribute("product") Product product) {
        return "product/form";
    }

    // 3. EDITAR PRODUCTO (Reutiliza la misma vista "product/form")
    @GetMapping("/edit")
    public String editar(@ModelAttribute("product") Product product,
                         @RequestParam("id") Integer id,
                         Model model) {
        Optional<Product> opt = productRepository.findById(id);
        if (opt.isPresent()) {
            model.addAttribute("product", opt.get());
            return "product/form";
        }
        return "redirect:/product";
    }

    // 4. GUARDAR PRODUCTO CON @ModelAttribute, @Valid y BindingResult
    // NOTA DE IMPLEMENTACION (Pregunta 3): Para gestionar el caso en que se escriban letras en los campos numericos
    // (precio y stock), se implemento la alternativa de personalizar los mensajes de error de conversion (typeMismatch)
    // mediante el archivo src/main/resources/messages.properties (typeMismatch.product.price y typeMismatch.product.stock).
    @PostMapping("/save")
    public String guardar(@ModelAttribute("product") @Valid Product product,
                          BindingResult bindingResult,
                          RedirectAttributes attr) {

        // Validar que el nombre del producto no este duplicado en la BD
        if (product.getName() != null && !product.getName().isBlank()) {
            Optional<Product> existente = productRepository.findByNameIgnoreCase(product.getName().trim());
            if (existente.isPresent()) {
                if (product.getId() == null || !existente.get().getId().equals(product.getId())) {
                    bindingResult.rejectValue("name", "error.product",
                            "Ya existe un producto registrado con este nombre");
                }
            }
        }

        // Ante errores de validacion o de conversion (typeMismatch), volver al formulario con los datos ingresados
        if (bindingResult.hasErrors()) {
            return "product/form";
        }

        product.setName(product.getName().trim());
        productRepository.save(product);
        attr.addFlashAttribute("msg", "Producto guardado correctamente");
        return "redirect:/product";
    }

    // 5. ELIMINAR PRODUCTO
    @GetMapping("/delete")
    public String eliminar(@RequestParam("id") Integer id, RedirectAttributes attr) {
        try {
            productRepository.deleteById(id);
            attr.addFlashAttribute("msg", "Producto eliminado correctamente");
        } catch (Exception e) {
            attr.addFlashAttribute("error", "No se puede eliminar el producto porque está incluido en pedidos registrados");
        }
        return "redirect:/product";
    }
}
