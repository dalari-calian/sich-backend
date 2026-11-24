package com.sich.financial;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sich.financial.dto.FinancialSummaryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/providers/me/financial")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROVIDER')")
@Tag(name = "Financeiro do prestador", description = "Faturamento do prestador autenticado (tela Financeiro)")
public class FinancialController {

    private final FinancialService financialService;

    @GetMapping
    @Operation(summary = "Financeiro dos agendamentos que começam entre as datas (até 366 dias): faturamento realizado "
            + "(bruto, desconto de cashback, líquido e ticket médio), receita prevista, valor cancelado, faturamento por dia "
            + "(gráfico) e os 10 serviços que mais faturaram. A lista de atendimentos é o histórico /providers/me/appointments/history")
    public ResponseEntity<FinancialSummaryResponse> summary(
            @Parameter(description = "Data inicial (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Data final, inclusive (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(financialService.forProvider(from, to));
    }
}
