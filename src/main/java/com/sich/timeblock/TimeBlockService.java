package com.sich.timeblock;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.sich.appointment.AppointmentRepository;
import com.sich.appointment.AppointmentService;
import com.sich.appointment.AppointmentStatus;
import com.sich.common.exception.BusinessRuleException;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.common.exception.ScheduleConflictException;
import com.sich.common.util.TimeGrid;
import com.sich.provider.ProviderEntity;
import com.sich.provider.ProviderService;
import com.sich.timeblock.dto.TimeBlockRequest;
import com.sich.timeblock.dto.TimeBlockResponse;
import com.sich.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * Bloqueios da agenda do prestador autenticado. Um bloqueio não pode sobrepor
 * agendamentos marcados nem outro bloqueio; enquanto existir, ninguém agenda no período.
 */
@Service
@RequiredArgsConstructor
public class TimeBlockService {

    private final TimeBlockRepository timeBlockRepository;
    private final AppointmentRepository appointmentRepository;
    private final ProviderService providerService;
    private final UserService userService;
    private final Clock clock;

    @Transactional
    public TimeBlockResponse create(TimeBlockRequest request) {
        ProviderEntity provider = currentProvider();
        LocalDateTime startAt = request.startAt();
        LocalDateTime endAt = request.endAt();
        validatePeriod(startAt, endAt);

        providerService.lockById(provider.getId());
        if (timeBlockRepository.existsOverlap(provider.getId(), startAt, endAt)) {
            throw new BusinessRuleException("Já existe um bloqueio nesse período");
        }
        if (appointmentRepository.existsProviderOverlap(provider.getId(), AppointmentStatus.SCHEDULED, startAt, endAt)) {
            throw new ScheduleConflictException("Existem agendamentos nesse período; cancele-os antes de bloquear");
        }

        TimeBlockEntity block = new TimeBlockEntity();
        block.setProvider(provider);
        block.setStartAt(startAt);
        block.setEndAt(endAt);
        block.setReason(StringUtils.hasText(request.reason()) ? request.reason().trim() : null);
        return TimeBlockResponse.of(timeBlockRepository.save(block));
    }

    /** Bloqueios do prestador autenticado que encostam nos dias {@code from} a {@code to}. */
    @Transactional(readOnly = true)
    public List<TimeBlockResponse> findAll(LocalDate from, LocalDate to) {
        AppointmentService.validatePeriod(from, to);
        return findOverlapping(currentProvider().getId(), from.atStartOfDay(), to.plusDays(1).atStartOfDay()).stream()
                .map(TimeBlockResponse::of)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        TimeBlockEntity block = timeBlockRepository.findByIdAndProviderId(id, currentProvider().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bloqueio não encontrado"));
        timeBlockRepository.delete(block);
    }

    /** Bloqueios do prestador que encostam no período {@code [from, to)}, por início. */
    @Transactional(readOnly = true)
    public List<TimeBlockEntity> findOverlapping(Long providerId, LocalDateTime from, LocalDateTime to) {
        return timeBlockRepository.findOverlapping(providerId, from, to);
    }

    @Transactional(readOnly = true)
    public boolean isBlocked(Long providerId, LocalDateTime from, LocalDateTime to) {
        return timeBlockRepository.existsOverlap(providerId, from, to);
    }

    private void validatePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (!TimeGrid.isAligned(startAt.toLocalTime()) || !TimeGrid.isAligned(endAt.toLocalTime())) {
            throw new BusinessRuleException("O início e o fim do bloqueio devem ser múltiplos de 15 minutos");
        }
        if (!startAt.isBefore(endAt)) {
            throw new BusinessRuleException("O início do bloqueio deve ser antes do fim");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (startAt.isBefore(now)) {
            throw new BusinessRuleException("Não é possível bloquear um horário que já passou");
        }
        if (startAt.toLocalDate().isAfter(now.toLocalDate().plusDays(AppointmentService.MAX_DAYS_AHEAD))) {
            throw new BusinessRuleException("É possível bloquear com até " + AppointmentService.MAX_DAYS_AHEAD
                    + " dias de antecedência");
        }
        if (ChronoUnit.DAYS.between(startAt, endAt) >= AppointmentService.MAX_LIST_DAYS) {
            throw new BusinessRuleException("O bloqueio deve durar no máximo " + AppointmentService.MAX_LIST_DAYS + " dias");
        }
    }

    private ProviderEntity currentProvider() {
        return providerService.findByUser(userService.getCurrentlyAuthenticatedUser());
    }
}
