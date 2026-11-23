package com.sich.benefit;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sich.benefit.dto.CustomerBenefitsResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/customers/me/benefits")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Benefícios do cliente", description = "Cashback e cartão fidelidade do cliente autenticado. "
        + "Para usar o saldo, envie useCashback = true em POST /customers/me/appointments")
public class BenefitController {

    private final BenefitService benefitService;

    @GetMapping
    @Operation(summary = "Carteira do cliente: saldo de cashback, cashback a receber dos próximos agendamentos, totais ganho/usado, "
            + "regras (percentual de cashback e limite de uso por agendamento) e progresso do cartão fidelidade (ex.: 3/5)")
    public ResponseEntity<CustomerBenefitsResponse> find() {
        return ResponseEntity.ok(benefitService.forCustomer());
    }
}
