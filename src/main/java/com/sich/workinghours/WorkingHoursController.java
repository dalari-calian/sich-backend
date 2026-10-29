package com.sich.workinghours;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sich.workinghours.dto.WorkingHoursRequest;
import com.sich.workinghours.dto.WorkingIntervalResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/providers/me/working-hours")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROVIDER')")
@Tag(name = "Expediente do prestador", description = "Horários em que o prestador atende, por dia da semana")
public class WorkingHoursController {

    private final WorkingHoursService workingHoursService;

    @GetMapping
    @Operation(summary = "Lista os intervalos de trabalho da semana, por dia e início")
    public ResponseEntity<List<WorkingIntervalResponse>> findAll() {
        return ResponseEntity.ok(workingHoursService.findAll());
    }

    @PutMapping
    @Operation(summary = "Substitui o expediente da semana (ex.: segunda 08:00–12:00 e 13:00–19:00 para almoço das 12h às 13h)")
    public ResponseEntity<List<WorkingIntervalResponse>> replaceWeek(@Valid @RequestBody WorkingHoursRequest request) {
        return ResponseEntity.ok(workingHoursService.replaceWeek(request));
    }
}
