package com.sich.provider;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sich.appointment.AvailabilityService;
import com.sich.appointment.dto.AvailabilityResponse;
import com.sich.product.ProductService;
import com.sich.product.dto.ProductResponse;
import com.sich.provider.dto.ProviderSummaryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Consultas de prestadores abertas a qualquer usuário autenticado (ex.: cliente escolhendo onde agendar). */
@RestController
@RequestMapping("/providers")
@RequiredArgsConstructor
@Tag(name = "Prestadores", description = "Busca de prestadores, serviços ativos e horários livres")
public class PublicProviderController {

    private final ProviderService providerService;
    private final ProductService productService;
    private final AvailabilityService availabilityService;

    @GetMapping
    @Operation(summary = "Busca prestadores pelo nome e, opcionalmente, por categoria (com produto ativo nela)")
    public ResponseEntity<List<ProviderSummaryResponse>> search(
            @Parameter(description = "Parte do nome do prestador") @RequestParam(required = false) String name,
            @Parameter(description = "Id da categoria") @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(providerService.search(name, categoryId));
    }

    @GetMapping("/{providerId}/products")
    @Operation(summary = "Lista os produtos/serviços ativos do prestador, com as fotos")
    public ResponseEntity<List<ProductResponse>> products(@Parameter(description = "Id do prestador") @PathVariable Long providerId) {
        return ResponseEntity.ok(productService.findActiveByProvider(providerService.findById(providerId).getId()));
    }

    @GetMapping("/{providerId}/availability")
    @Operation(summary = "Horários de início livres do prestador na data para os serviços escolhidos")
    public ResponseEntity<AvailabilityResponse> availability(
            @Parameter(description = "Id do prestador") @PathVariable Long providerId,
            @Parameter(description = "Data (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Ids dos serviços, separados por vírgula") @RequestParam List<Long> productIds) {
        return ResponseEntity.ok(availabilityService.forProvider(providerId, date, productIds));
    }
}
