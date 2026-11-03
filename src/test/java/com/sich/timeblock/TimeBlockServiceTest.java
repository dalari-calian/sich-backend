package com.sich.timeblock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sich.appointment.AppointmentRepository;
import com.sich.appointment.AppointmentService;
import com.sich.appointment.AppointmentStatus;
import com.sich.common.exception.BusinessRuleException;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.common.exception.ScheduleConflictException;
import com.sich.provider.ProviderEntity;
import com.sich.provider.ProviderService;
import com.sich.timeblock.dto.TimeBlockRequest;
import com.sich.timeblock.dto.TimeBlockResponse;
import com.sich.user.UserEntity;
import com.sich.user.UserService;

@ExtendWith(MockitoExtension.class)
class TimeBlockServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    /** Segunda-feira, 19/10/2026, 09:07. */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 19, 9, 7);
    private static final LocalDate TUESDAY = LocalDate.of(2026, 10, 20);

    @Mock
    private TimeBlockRepository timeBlockRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ProviderService providerService;

    @Mock
    private UserService userService;

    private TimeBlockService timeBlockService;

    private ProviderEntity provider;

    @BeforeEach
    void setUp() {
        timeBlockService = new TimeBlockService(timeBlockRepository, appointmentRepository, providerService, userService,
                Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE));
        provider = new ProviderEntity();
        provider.setId(10L);
    }

    private void authenticatedAsProvider() {
        UserEntity user = new UserEntity();
        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);
        when(providerService.findByUser(user)).thenReturn(provider);
    }

    private static TimeBlockEntity block(Long id, LocalDateTime start, LocalDateTime end, String reason) {
        TimeBlockEntity block = new TimeBlockEntity();
        block.setId(id);
        block.setStartAt(start);
        block.setEndAt(end);
        block.setReason(reason);
        return block;
    }

    @Test
    void create_shouldSaveBlock_whenPeriodIsFree() {
        authenticatedAsProvider();
        LocalDateTime start = TUESDAY.atTime(14, 0);
        LocalDateTime end = TUESDAY.atTime(16, 0);
        when(timeBlockRepository.save(any(TimeBlockEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TimeBlockResponse result = timeBlockService.create(new TimeBlockRequest(start, end, "  Consulta médica  "));

        assertThat(result).isEqualTo(new TimeBlockResponse(null, start, end, "Consulta médica"));
        verify(providerService).lockById(10L);
        verify(timeBlockRepository).save(org.mockito.ArgumentMatchers.<TimeBlockEntity>argThat(
                saved -> saved.getProvider() == provider));
    }

    @Test
    void create_shouldAllowSeveralDaysAndBlankReason() {
        authenticatedAsProvider();
        when(timeBlockRepository.save(any(TimeBlockEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TimeBlockResponse result = timeBlockService.create(
                new TimeBlockRequest(TUESDAY.atStartOfDay(), TUESDAY.plusDays(7).atStartOfDay(), " "));

        assertThat(result.reason()).isNull();
    }

    @Test
    void create_shouldThrow_whenTimesAreOutOfGrid() {
        authenticatedAsProvider();

        assertThatThrownBy(() -> timeBlockService.create(new TimeBlockRequest(TUESDAY.atTime(14, 10), TUESDAY.atTime(15, 0), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O início e o fim do bloqueio devem ser múltiplos de 15 minutos");
        assertThatThrownBy(() -> timeBlockService.create(new TimeBlockRequest(TUESDAY.atTime(14, 0), TUESDAY.atTime(15, 5), null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(providerService, never()).lockById(any());
    }

    @Test
    void create_shouldThrow_whenStartIsNotBeforeEnd() {
        authenticatedAsProvider();

        assertThatThrownBy(() -> timeBlockService.create(new TimeBlockRequest(TUESDAY.atTime(14, 0), TUESDAY.atTime(14, 0), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O início do bloqueio deve ser antes do fim");
    }

    @Test
    void create_shouldThrow_whenStartIsInThePast() {
        authenticatedAsProvider();

        assertThatThrownBy(() -> timeBlockService.create(new TimeBlockRequest(NOW.toLocalDate().atTime(9, 0),
                NOW.toLocalDate().atTime(10, 0), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Não é possível bloquear um horário que já passou");
    }

    @Test
    void create_shouldThrow_whenStartIsBeyondMaximumDaysAhead() {
        authenticatedAsProvider();
        LocalDate tooFar = NOW.toLocalDate().plusDays(AppointmentService.MAX_DAYS_AHEAD + 1);

        assertThatThrownBy(() -> timeBlockService.create(new TimeBlockRequest(tooFar.atTime(8, 0), tooFar.atTime(9, 0), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("É possível bloquear com até 60 dias de antecedência");
    }

    @Test
    void create_shouldThrow_whenBlockIsTooLong() {
        authenticatedAsProvider();

        assertThatThrownBy(() -> timeBlockService.create(new TimeBlockRequest(TUESDAY.atStartOfDay(),
                TUESDAY.plusDays(AppointmentService.MAX_LIST_DAYS).atStartOfDay(), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O bloqueio deve durar no máximo 92 dias");
    }

    @Test
    void create_shouldThrow_whenAnotherBlockOverlaps() {
        authenticatedAsProvider();
        when(timeBlockRepository.existsOverlap(10L, TUESDAY.atTime(14, 0), TUESDAY.atTime(16, 0))).thenReturn(true);

        assertThatThrownBy(() -> timeBlockService.create(new TimeBlockRequest(TUESDAY.atTime(14, 0), TUESDAY.atTime(16, 0), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Já existe um bloqueio nesse período");
        verify(timeBlockRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowConflict_whenScheduledAppointmentOverlaps() {
        authenticatedAsProvider();
        when(appointmentRepository.existsProviderOverlap(10L, AppointmentStatus.SCHEDULED,
                TUESDAY.atTime(14, 0), TUESDAY.atTime(16, 0))).thenReturn(true);

        assertThatThrownBy(() -> timeBlockService.create(new TimeBlockRequest(TUESDAY.atTime(14, 0), TUESDAY.atTime(16, 0), null)))
                .isInstanceOf(ScheduleConflictException.class)
                .hasMessage("Existem agendamentos nesse período; cancele-os antes de bloquear");
        verify(timeBlockRepository, never()).save(any());
    }

    @Test
    void findAll_shouldReturnBlocksTouchingPeriod() {
        authenticatedAsProvider();
        TimeBlockEntity block = block(5L, TUESDAY.atTime(14, 0), TUESDAY.atTime(16, 0), "Consulta");
        when(timeBlockRepository.findOverlapping(10L, TUESDAY.atStartOfDay(), TUESDAY.plusDays(7).atStartOfDay()))
                .thenReturn(List.of(block));

        assertThat(timeBlockService.findAll(TUESDAY, TUESDAY.plusDays(6)))
                .containsExactly(new TimeBlockResponse(5L, TUESDAY.atTime(14, 0), TUESDAY.atTime(16, 0), "Consulta"));
    }

    @Test
    void findAll_shouldThrow_whenPeriodIsInvalid() {
        assertThatThrownBy(() -> timeBlockService.findAll(TUESDAY, TUESDAY.minusDays(1)))
                .isInstanceOf(BusinessRuleException.class);
        verify(timeBlockRepository, never()).findOverlapping(any(), any(), any());
    }

    @Test
    void delete_shouldRemoveOwnBlock() {
        authenticatedAsProvider();
        TimeBlockEntity block = block(5L, TUESDAY.atTime(14, 0), TUESDAY.atTime(16, 0), null);
        when(timeBlockRepository.findByIdAndProviderId(5L, 10L)).thenReturn(Optional.of(block));

        timeBlockService.delete(5L);

        verify(timeBlockRepository).delete(block);
    }

    @Test
    void delete_shouldThrow_whenBlockIsNotFromProvider() {
        authenticatedAsProvider();
        when(timeBlockRepository.findByIdAndProviderId(5L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timeBlockService.delete(5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Bloqueio não encontrado");
        verify(timeBlockRepository, never()).delete(any());
    }

    @Test
    void isBlocked_shouldAskRepositoryForOverlap() {
        when(timeBlockRepository.existsOverlap(10L, TUESDAY.atTime(14, 0), TUESDAY.atTime(15, 0))).thenReturn(true);

        assertThat(timeBlockService.isBlocked(10L, TUESDAY.atTime(14, 0), TUESDAY.atTime(15, 0))).isTrue();
    }
}
