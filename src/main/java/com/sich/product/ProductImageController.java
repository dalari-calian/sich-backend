package com.sich.product;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sich.product.dto.ProductImageRequest;
import com.sich.product.dto.ProductImageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/providers/me/products/{productId}/images")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROVIDER')")
@Tag(name = "Imagens do produto", description = "Fotos (até 5, em base64) dos produtos do prestador autenticado")
public class ProductImageController {

    private final ProductImageService productImageService;

    @PostMapping
    @Operation(summary = "Adiciona uma foto ao produto (data URL base64 png/jpeg/webp, até 2 MB; máximo 5 por produto)")
    public ResponseEntity<ProductImageResponse> create(
            @Parameter(description = "Id do produto") @PathVariable Long productId,
            @Valid @RequestBody ProductImageRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productImageService.create(productId, request));
    }

    @GetMapping
    @Operation(summary = "Lista as fotos do produto")
    public ResponseEntity<List<ProductImageResponse>> findAll(
            @Parameter(description = "Id do produto") @PathVariable Long productId) {
        return ResponseEntity.ok(productImageService.findAll(productId));
    }

    @DeleteMapping("/{imageId}")
    @Operation(summary = "Remove uma foto do produto")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Id do produto") @PathVariable Long productId,
            @Parameter(description = "Id da imagem") @PathVariable Long imageId) {
        productImageService.delete(productId, imageId);
        return ResponseEntity.noContent().build();
    }
}
