package com.sich.appointment;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sich.appointment.dto.AppointmentResponse;
import com.sich.appointment.dto.CustomerAppointmentRequest;
import com.sich.appointment.dto.CustomerAppointmentSummaryResponse;
import com.sich.common.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/customers/me/appointments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Agenda do cliente", description = "Agendamentos do cliente autenticado")
public class CustomerAppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentSummaryService appointmentSummaryService;

    @GetMapping
    @Operation(summary = "Lista os agendamentos do cliente (inclusive os marcados pelo prestador) que começam entre as datas")
    public ResponseEntity<List<AppointmentResponse>> findAll(
            @Parameter(description = "Data inicial (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Data final, inclusive (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(appointmentService.findForCustomer(from, to));
    }

    @GetMapping("/summary")
    @Operation(summary = "Contadores do cliente: atendimentos realizados, próximos e cancelados")
    public ResponseEntity<CustomerAppointmentSummaryResponse> summary() {
        return ResponseEntity.ok(appointmentSummaryService.forCustomer());
    }

    @GetMapping("/history")
    @Operation(summary = "Histórico paginado: agendamentos que já começaram, do mais recente para o mais antigo")
    public ResponseEntity<PageResponse<AppointmentResponse>> history(
            @Parameter(description = "Página, a partir de 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página (1 a 50)") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(appointmentService.historyForCustomer(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um agendamento do cliente")
    public ResponseEntity<AppointmentResponse> findById(@Parameter(description = "Id do agendamento") @PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.findByIdForCustomer(id));
    }

    @PostMapping
    @Operation(summary = "Agenda serviços com um prestador; o fim é calculado pelos serviços e precisa caber no expediente")
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody CustomerAppointmentRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(appointmentService.createByCustomer(request));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancela um agendamento do cliente e libera o horário")
    public ResponseEntity<AppointmentResponse> cancel(@Parameter(description = "Id do agendamento") @PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.cancelByCustomer(id));
    }
}
