package com.sich.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sich.customer.dto.CustomerSummaryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/providers/me/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROVIDER')")
@Tag(name = "Clientes para agendamento", description = "Localiza um cliente cadastrado para o prestador agendar em nome dele")
public class ProviderCustomerController {

    private final CustomerService customerService;

    @GetMapping("/lookup")
    @Operation(summary = "Encontra um cliente pelo email ou telefone exatos (não há busca por nome, para não expor a lista de clientes)")
    public ResponseEntity<CustomerSummaryResponse> lookup(
            @Parameter(description = "Email do cliente") @RequestParam(required = false) String email,
            @Parameter(description = "Telefone do cliente, igual ao cadastrado") @RequestParam(required = false) String phone) {
        return ResponseEntity.ok(CustomerSummaryResponse.of(customerService.lookup(email, phone)));
    }
}
