package com.sich.timeblock;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sich.timeblock.dto.TimeBlockRequest;
import com.sich.timeblock.dto.TimeBlockResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/providers/me/time-blocks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROVIDER')")
@Tag(name = "Bloqueios da agenda", description = "Períodos em que o prestador não atende e ninguém pode agendar")
public class TimeBlockController {

    private final TimeBlockService timeBlockService;

    @GetMapping
    @Operation(summary = "Lista os bloqueios que encostam nos dias informados, por início")
    public ResponseEntity<List<TimeBlockResponse>> findAll(
            @Parameter(description = "Data inicial (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Data final, inclusive (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(timeBlockService.findAll(from, to));
    }

    @PostMapping
    @Operation(summary = "Bloqueia um período da agenda; recusa se houver agendamento marcado (409) ou outro bloqueio (422) nele")
    public ResponseEntity<TimeBlockResponse> create(@Valid @RequestBody TimeBlockRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(timeBlockService.create(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um bloqueio e libera o período")
    public ResponseEntity<Void> delete(@Parameter(description = "Id do bloqueio") @PathVariable Long id) {
        timeBlockService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
