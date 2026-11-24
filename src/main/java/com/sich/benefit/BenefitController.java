package com.sich.benefit;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sich.benefit.dto.CashbackMovementResponse;
import com.sich.benefit.dto.CustomerBenefitsResponse;
import com.sich.common.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @GetMapping("/history")
    @Operation(summary = "Extrato paginado da carteira: agendamentos que geraram ou usaram cashback, do mais recente para o mais antigo, "
            + "com a situação (CREDITED = já no saldo, PENDING = entra quando o atendimento terminar, CANCELED = não entra e o "
            + "desconto voltou). Os bônus do cartão fidelidade estão em loyalty.bonusEarnedTotal")
    public ResponseEntity<PageResponse<CashbackMovementResponse>> history(
            @Parameter(description = "Página, a partir de 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página (1 a 50)") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(benefitService.history(page, size));
    }
}
