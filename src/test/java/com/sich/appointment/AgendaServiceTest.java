package com.sich.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sich.appointment.dto.AgendaDayResponse;
import com.sich.appointment.dto.AgendaIntervalResponse;
import com.sich.common.exception.BusinessRuleException;
import com.sich.common.util.TimeRange;
import com.sich.provider.ProviderEntity;
import com.sich.provider.ProviderService;
import com.sich.timeblock.TimeBlockEntity;
import com.sich.timeblock.TimeBlockService;
import com.sich.timeblock.dto.TimeBlockResponse;
import com.sich.user.UserEntity;
import com.sich.user.UserService;
import com.sich.workinghours.WorkingHoursService;

@ExtendWith(MockitoExtension.class)
class AgendaServiceTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 19);
    private static final LocalDate TUESDAY = MONDAY.plusDays(1);
    private static final LocalDate WEDNESDAY = MONDAY.plusDays(2);

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private WorkingHoursService workingHoursService;

    @Mock
    private TimeBlockService timeBlockService;

    @Mock
    private ProviderService providerService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AgendaService agendaService;

    private final ProviderEntity provider = new ProviderEntity();

    private void authenticatedAsProvider() {
        UserEntity user = new UserEntity();
        provider.setId(10L);
        provider.setName("Ana Beatriz");
        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);
        when(providerService.findByUser(user)).thenReturn(provider);
    }

    private AppointmentEntity appointment(Long id, LocalDateTime start, int minutes) {
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setId(id);
        appointment.setProvider(provider);
        appointment.setCustomerName("Marina R.");
        appointment.setStartAt(start);
        appointment.setEndAt(start.plusMinutes(minutes));
        appointment.setTotalMinutes(minutes);
        appointment.setTotalPrice(BigDecimal.TEN);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setOrigin(AppointmentOrigin.PROVIDER);
        return appointment;
    }

    private static TimeBlockEntity block(Long id, LocalDateTime start, LocalDateTime end) {
        TimeBlockEntity block = new TimeBlockEntity();
        block.setId(id);
        block.setStartAt(start);
        block.setEndAt(end);
        return block;
    }

    @Test
    void forProvider_shouldBuildOneDayPerDateWithWorkingHoursBreaksBlocksAndAppointments() {
        authenticatedAsProvider();
        when(workingHoursService.findIntervalsBetween(10L, MONDAY, WEDNESDAY)).thenReturn(List.of(
                new TimeRange(MONDAY.atTime(8, 0), MONDAY.atTime(12, 0)),
                new TimeRange(MONDAY.atTime(13, 0), MONDAY.atTime(19, 0)),
                new TimeRange(TUESDAY.atTime(9, 0), TUESDAY.atTime(17, 0))));
        when(appointmentRepository.findByProviderIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
                10L, MONDAY.atStartOfDay(), WEDNESDAY.plusDays(1).atStartOfDay())).thenReturn(List.of(
                appointment(1L, MONDAY.atTime(9, 0), 60),
                appointment(2L, MONDAY.atTime(13, 0), 90),
                appointment(3L, TUESDAY.atTime(10, 0), 30)));
        // Começa na terça à tarde e vai até quarta de manhã: aparece nos dois dias.
        TimeBlockEntity block = block(7L, TUESDAY.atTime(15, 0), WEDNESDAY.atTime(12, 0));
        when(timeBlockService.findOverlapping(10L, MONDAY.atStartOfDay(), WEDNESDAY.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(block));

        List<AgendaDayResponse> result = agendaService.forProvider(MONDAY, WEDNESDAY);

        assertThat(result).extracting(AgendaDayResponse::date).containsExactly(MONDAY, TUESDAY, WEDNESDAY);
        assertThat(result).extracting(AgendaDayResponse::weekday).containsExactly(1, 2, 3);

        AgendaDayResponse monday = result.get(0);
        assertThat(monday.workingHours()).containsExactly(
                new AgendaIntervalResponse("08:00", "12:00"), new AgendaIntervalResponse("13:00", "19:00"));
        assertThat(monday.breaks()).containsExactly(new AgendaIntervalResponse("12:00", "13:00"));
        assertThat(monday.blocks()).isEmpty();
        assertThat(monday.appointments()).extracting(appointment -> appointment.id()).containsExactly(1L, 2L);

        AgendaDayResponse tuesday = result.get(1);
        assertThat(tuesday.breaks()).isEmpty();
        assertThat(tuesday.blocks()).containsExactly(TimeBlockResponse.of(block));
        assertThat(tuesday.appointments()).extracting(appointment -> appointment.id()).containsExactly(3L);

        AgendaDayResponse wednesday = result.get(2);
        assertThat(wednesday.workingHours()).isEmpty();
        assertThat(wednesday.blocks()).containsExactly(TimeBlockResponse.of(block));
        assertThat(wednesday.appointments()).isEmpty();
    }

    @Test
    void forProvider_shouldThrow_whenPeriodIsInvalid() {
        assertThatThrownBy(() -> agendaService.forProvider(MONDAY, MONDAY.minusDays(1)))
                .isInstanceOf(BusinessRuleException.class);
        verify(workingHoursService, never()).findIntervalsBetween(any(), any(), any());
    }

    @Test
    void breaks_shouldReturnGapsBetweenIntervalsOnly() {
        assertThat(AgendaService.breaks(List.of(
                new TimeRange(MONDAY.atTime(8, 0), MONDAY.atTime(12, 0)),
                new TimeRange(MONDAY.atTime(12, 0), MONDAY.atTime(14, 0)),
                new TimeRange(MONDAY.atTime(15, 0), MONDAY.atTime(18, 0)))))
                .containsExactly(new TimeRange(MONDAY.atTime(14, 0), MONDAY.atTime(15, 0)));
        assertThat(AgendaService.breaks(List.of())).isEmpty();
    }
}
