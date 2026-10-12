package com.sich.providerproduct;

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

import com.sich.providerproduct.dto.ProviderProductRequest;
import com.sich.providerproduct.dto.ProviderProductResponse;
import com.sich.providerproduct.dto.ProviderProductUpdateRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/providers/me/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROVIDER')")
@Tag(name = "Produtos do prestador", description = "Produtos do catálogo que o prestador autenticado presta, com descrição e preço")
public class ProviderProductController {

    private final ProviderProductService providerProductService;

    @PostMapping
    @Operation(summary = "Adiciona um produto do catálogo aos produtos que o prestador presta, com descrição e preço")
    public ResponseEntity<ProviderProductResponse> create(@Valid @RequestBody ProviderProductRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(providerProductService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza a descrição e o preço de um produto prestado")
    public ResponseEntity<ProviderProductResponse> update(
            @Parameter(description = "Id do produto do prestador") @PathVariable Long id,
            @Valid @RequestBody ProviderProductUpdateRequest request) {
        return ResponseEntity.ok(providerProductService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um produto dos produtos que o prestador presta")
    public ResponseEntity<Void> delete(@Parameter(description = "Id do produto do prestador") @PathVariable Long id) {
        providerProductService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Lista os produtos que o prestador autenticado presta")
    public ResponseEntity<List<ProviderProductResponse>> findAll() {
        return ResponseEntity.ok(providerProductService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um produto prestado pelo prestador autenticado")
    public ResponseEntity<ProviderProductResponse> findById(
            @Parameter(description = "Id do produto do prestador") @PathVariable Long id) {
        return ResponseEntity.ok(providerProductService.findById(id));
    }
}
