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
import com.sich.product.dto.ProductTreeResponse;
import com.sich.product.dto.ProductUpdateRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Produtos", description = "Catálogo de produtos/serviços organizado em árvore")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cadastra um produto. Sem parentId cria um produto raiz; com parentId cria um filho. A SKU é gerada automaticamente. Apenas ADMIN")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza o nome e a descrição de um produto (a SKU e o pai não mudam). Apenas ADMIN")
    public ResponseEntity<ProductResponse> update(
            @Parameter(description = "Id do produto") @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Exclui um produto. Só é permitido quando ele não possui filhos. Apenas ADMIN")
    public ResponseEntity<Void> delete(@Parameter(description = "Id do produto") @PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Lista os produtos raiz (primeiro nível da árvore)")
    public ResponseEntity<List<ProductResponse>> findRoots() {
        return ResponseEntity.ok(productService.findRoots());
    }

    @GetMapping("/tree")
    @Operation(summary = "Retorna a árvore completa de produtos, com os filhos aninhados")
    public ResponseEntity<List<ProductTreeResponse>> findTree() {
        return ResponseEntity.ok(productService.findTree());
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Busca um produto pela SKU")
    public ResponseEntity<ProductResponse> findBySku(
            @Parameter(description = "SKU do produto", example = "1.01") @PathVariable String sku) {
        return ResponseEntity.ok(productService.findBySku(sku));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um produto pelo id")
    public ResponseEntity<ProductResponse> findById(@Parameter(description = "Id do produto") @PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Lista os filhos diretos de um produto")
    public ResponseEntity<List<ProductResponse>> findChildren(
            @Parameter(description = "Id do produto") @PathVariable Long id) {
        return ResponseEntity.ok(productService.findChildren(id));
    }

    @GetMapping("/{id}/parent")
    @Operation(summary = "Retorna o pai direto de um produto (404 se for raiz)")
    public ResponseEntity<ProductResponse> findParent(@Parameter(description = "Id do produto") @PathVariable Long id) {
        return ResponseEntity.ok(productService.findParent(id));
    }

    @GetMapping("/{id}/ancestors")
    @Operation(summary = "Lista o caminho da raiz até o pai direto do produto (breadcrumb)")
    public ResponseEntity<List<ProductResponse>> findAncestors(
            @Parameter(description = "Id do produto") @PathVariable Long id) {
        return ResponseEntity.ok(productService.findAncestors(id));
    }

    @GetMapping("/{id}/tree")
    @Operation(summary = "Retorna a subárvore a partir de um produto, com todos os descendentes aninhados")
    public ResponseEntity<ProductTreeResponse> findSubtree(
            @Parameter(description = "Id do produto") @PathVariable Long id) {
        return ResponseEntity.ok(productService.findSubtree(id));
    }
}
