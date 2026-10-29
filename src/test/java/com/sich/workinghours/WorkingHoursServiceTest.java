package com.sich.workinghours;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sich.common.exception.BusinessRuleException;
import com.sich.common.util.TimeRange;
import com.sich.provider.ProviderEntity;
import com.sich.provider.ProviderService;
import com.sich.user.UserEntity;
import com.sich.user.UserService;
import com.sich.workinghours.dto.WorkingHoursRequest;
import com.sich.workinghours.dto.WorkingIntervalRequest;
import com.sich.workinghours.dto.WorkingIntervalResponse;

@ExtendWith(MockitoExtension.class)
class WorkingHoursServiceTest {

    @Mock
    private WorkingHoursRepository workingHoursRepository;

    @Mock
    private ProviderService providerService;

    @Mock
    private UserService userService;

    @InjectMocks
    private WorkingHoursService workingHoursService;

    private ProviderEntity authenticatedProvider() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        ProviderEntity provider = new ProviderEntity();
        provider.setId(10L);
        provider.setUser(user);
        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);
        when(providerService.findByUser(user)).thenReturn(provider);
        return provider;
    }

    private static WorkingHoursEntity interval(int weekday, String start, String end) {
        WorkingHoursEntity interval = new WorkingHoursEntity();
        interval.setWeekday(weekday);
        interval.setStartTime(LocalTime.parse(start));
        interval.setEndTime(LocalTime.parse(end));
        return interval;
    }

    private static WorkingHoursRequest week(WorkingIntervalRequest... intervals) {
        return new WorkingHoursRequest(List.of(intervals));
    }

    @Test
    void replaceWeek_shouldReplaceIntervalsSortedByDayAndStart() {
        ProviderEntity provider = authenticatedProvider();
        when(workingHoursRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<WorkingIntervalResponse> result = workingHoursService.replaceWeek(week(
                new WorkingIntervalRequest(2, "08:00", "12:00"),
                new WorkingIntervalRequest(1, "13:00", "19:00"),
                new WorkingIntervalRequest(1, "08:00", "12:00")));

        assertThat(result).containsExactly(
                new WorkingIntervalResponse(1, "08:00", "12:00"),
                new WorkingIntervalResponse(1, "13:00", "19:00"),
                new WorkingIntervalResponse(2, "08:00", "12:00"));
        InOrder order = inOrder(workingHoursRepository);
        order.verify(workingHoursRepository).deleteByProviderId(10L);
        order.verify(workingHoursRepository).saveAll(anyList());
        verify(workingHoursRepository).saveAll(org.mockito.ArgumentMatchers.<List<WorkingHoursEntity>>argThat(
                saved -> saved.stream().allMatch(entity -> entity.getProvider() == provider)));
    }

    @Test
    void replaceWeek_shouldAllowTouchingIntervalsAndSameHoursOnDifferentDays() {
        authenticatedProvider();
        when(workingHoursRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<WorkingIntervalResponse> result = workingHoursService.replaceWeek(week(
                new WorkingIntervalRequest(1, "08:00", "12:00"),
                new WorkingIntervalRequest(1, "12:00", "18:00"),
                new WorkingIntervalRequest(3, "08:00", "12:00")));

        assertThat(result).hasSize(3);
    }

    @Test
    void replaceWeek_shouldClearWeek_whenNoIntervals() {
        authenticatedProvider();
        when(workingHoursRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(workingHoursService.replaceWeek(week())).isEmpty();
        verify(workingHoursRepository).deleteByProviderId(10L);
    }

    @Test
    void replaceWeek_shouldThrow_whenTimeIsNotMultipleOf15Minutes() {
        authenticatedProvider();

        assertThatThrownBy(() -> workingHoursService.replaceWeek(week(new WorkingIntervalRequest(1, "08:10", "12:00"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Os horários do expediente devem ser múltiplos de 15 minutos");
        assertThatThrownBy(() -> workingHoursService.replaceWeek(week(new WorkingIntervalRequest(1, "08:00", "12:05"))))
                .isInstanceOf(BusinessRuleException.class);
        verify(workingHoursRepository, never()).deleteByProviderId(any());
    }

    @Test
    void replaceWeek_shouldThrow_whenStartIsNotBeforeEnd() {
        authenticatedProvider();

        assertThatThrownBy(() -> workingHoursService.replaceWeek(week(new WorkingIntervalRequest(1, "12:00", "12:00"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O início do intervalo deve ser antes do fim");
        assertThatThrownBy(() -> workingHoursService.replaceWeek(week(new WorkingIntervalRequest(1, "13:00", "12:00"))))
                .isInstanceOf(BusinessRuleException.class);
        verify(workingHoursRepository, never()).saveAll(anyList());
    }

    @Test
    void replaceWeek_shouldThrow_whenIntervalsOverlapOnSameDay() {
        authenticatedProvider();

        assertThatThrownBy(() -> workingHoursService.replaceWeek(week(
                new WorkingIntervalRequest(1, "08:00", "12:00"),
                new WorkingIntervalRequest(1, "11:45", "18:00"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Os intervalos do expediente não podem se sobrepor no mesmo dia");
        verify(workingHoursRepository, never()).deleteByProviderId(any());
    }

    @Test
    void findAll_shouldReturnIntervalsOfAuthenticatedProvider() {
        authenticatedProvider();
        when(workingHoursRepository.findByProviderIdOrderByWeekdayAscStartTimeAsc(10L))
                .thenReturn(List.of(interval(1, "08:00", "12:00"), interval(1, "13:00", "19:00")));

        assertThat(workingHoursService.findAll()).containsExactly(
                new WorkingIntervalResponse(1, "08:00", "12:00"),
                new WorkingIntervalResponse(1, "13:00", "19:00"));
    }

    @Test
    void workingMinutesBetween_shouldSumIntervalsOfEachDayInPeriod() {
        when(workingHoursRepository.findByProviderIdOrderByWeekdayAscStartTimeAsc(10L)).thenReturn(List.of(
                interval(1, "08:00", "12:00"),
                interval(1, "13:00", "19:00"),
                interval(2, "09:00", "10:30")));
        LocalDate monday = LocalDate.of(2026, 10, 19);

        assertThat(workingHoursService.workingMinutesBetween(10L, monday, monday)).isEqualTo(600);
        assertThat(workingHoursService.workingMinutesBetween(10L, monday, monday.plusDays(6))).isEqualTo(690);
        assertThat(workingHoursService.workingMinutesBetween(10L, monday, monday.plusDays(7))).isEqualTo(1290);
        assertThat(workingHoursService.workingMinutesBetween(10L, monday.plusDays(2), monday.plusDays(6))).isZero();
    }

    @Test
    void findIntervals_shouldReturnIntervalsOfWeekdayOnDate() {
        LocalDate tuesday = LocalDate.of(2026, 10, 20);
        when(workingHoursRepository.findByProviderIdAndWeekdayOrderByStartTimeAsc(10L, tuesday.getDayOfWeek().getValue()))
                .thenReturn(List.of(interval(2, "08:00", "12:00")));

        List<TimeRange> result = workingHoursService.findIntervals(10L, tuesday);

        assertThat(result).containsExactly(
                new TimeRange(LocalDateTime.of(2026, 10, 20, 8, 0), LocalDateTime.of(2026, 10, 20, 12, 0)));
    }
}
