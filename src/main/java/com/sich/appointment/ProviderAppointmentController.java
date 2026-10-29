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

import com.sich.appointment.dto.AgendaSummaryResponse;
import com.sich.appointment.dto.AppointmentResponse;
import com.sich.appointment.dto.AvailabilityResponse;
import com.sich.appointment.dto.ProviderAppointmentRequest;
import com.sich.common.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/providers/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROVIDER')")
@Tag(name = "Agenda do prestador", description = "Agendamentos na agenda do prestador autenticado")
public class ProviderAppointmentController {

    private final AppointmentService appointmentService;
    private final AvailabilityService availabilityService;
    private final AppointmentSummaryService appointmentSummaryService;

    @GetMapping("/appointments")
    @Operation(summary = "Lista os agendamentos (inclusive cancelados) que começam entre as datas, por horário")
    public ResponseEntity<List<AppointmentResponse>> findAll(
            @Parameter(description = "Data inicial (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Data final, inclusive (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(appointmentService.findForProvider(from, to));
    }

    @GetMapping("/appointments/summary")
    @Operation(summary = "Resumo da agenda entre as datas: confirmados, cancelados, minutos de expediente, agendados e livres, e receita prevista "
            + "(cards da agenda, atendimentos de hoje e ocupação da semana)")
    public ResponseEntity<AgendaSummaryResponse> summary(
            @Parameter(description = "Data inicial (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Data final, inclusive (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(appointmentSummaryService.forProvider(from, to));
    }

    @GetMapping("/appointments/history")
    @Operation(summary = "Histórico paginado: agendamentos que já começaram, do mais recente para o mais antigo")
    public ResponseEntity<PageResponse<AppointmentResponse>> history(
            @Parameter(description = "Página, a partir de 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página (1 a 50)") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(appointmentService.historyForProvider(page, size));
    }

    @GetMapping("/appointments/{id}")
    @Operation(summary = "Detalhe de um agendamento da própria agenda")
    public ResponseEntity<AppointmentResponse> findById(@Parameter(description = "Id do agendamento") @PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.findByIdForProvider(id));
    }

    @PostMapping("/appointments")
    @Operation(summary = "Reserva um horário na própria agenda para um cliente (cadastrado ou só pelo nome); o fim é calculado pelos serviços")
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody ProviderAppointmentRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(appointmentService.createByProvider(request));
    }

    @PatchMapping("/appointments/{id}/cancel")
    @Operation(summary = "Cancela um agendamento da própria agenda e libera o horário")
    public ResponseEntity<AppointmentResponse> cancel(@Parameter(description = "Id do agendamento") @PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.cancelByProvider(id));
    }

    @GetMapping("/availability")
    @Operation(summary = "Horários de início livres no próprio expediente para os serviços escolhidos")
    public ResponseEntity<AvailabilityResponse> availability(
            @Parameter(description = "Data (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Ids dos serviços, separados por vírgula") @RequestParam List<Long> productIds) {
        return ResponseEntity.ok(availabilityService.forCurrentProvider(date, productIds));
    }
}
