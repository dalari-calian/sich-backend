package com.sich.product;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sich.product.dto.ProductRequest;
import com.sich.product.dto.ProductResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/providers/me/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROVIDER')")
@Tag(name = "Produtos do prestador", description = "Produtos/serviços cadastrados pelo prestador autenticado")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Cadastra um produto/serviço do prestador (categoria, nome, descrição, preço e tempo médio HH:MM)")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um produto/serviço do prestador")
    public ResponseEntity<ProductResponse> update(
            @Parameter(description = "Id do produto") @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um produto/serviço do prestador")
    public ResponseEntity<Void> delete(@Parameter(description = "Id do produto") @PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Lista os produtos/serviços do prestador autenticado, ordenados por nome")
    public ResponseEntity<List<ProductResponse>> findAll() {
        return ResponseEntity.ok(productService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um produto/serviço do prestador autenticado")
    public ResponseEntity<ProductResponse> findById(@Parameter(description = "Id do produto") @PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }
}
