package com.sich.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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

import com.sich.appointment.dto.AppointmentItemResponse;
import com.sich.appointment.dto.AppointmentResponse;
import com.sich.appointment.dto.CustomerAppointmentRequest;
import com.sich.appointment.dto.ProviderAppointmentRequest;
import com.sich.common.exception.BusinessRuleException;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.common.exception.ScheduleConflictException;
import com.sich.common.util.TimeRange;
import com.sich.customer.CustomerEntity;
import com.sich.customer.CustomerService;
import com.sich.product.ProductEntity;
import com.sich.product.ProductService;
import com.sich.provider.ProviderEntity;
import com.sich.provider.ProviderService;
import com.sich.user.UserEntity;
import com.sich.user.UserService;
import com.sich.workinghours.WorkingHoursService;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    /** Segunda-feira, 19/10/2026, 09:07. */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 19, 9, 7);
    private static final LocalDate TUESDAY = LocalDate.of(2026, 10, 20);
    private static final List<Long> PRODUCT_IDS = List.of(1L, 2L);

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ProductService productService;

    @Mock
    private WorkingHoursService workingHoursService;

    @Mock
    private ProviderService providerService;

    @Mock
    private CustomerService customerService;

    @Mock
    private UserService userService;

    private final Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);

    private AppointmentService appointmentService;

    private UserEntity user;
    private ProviderEntity provider;
    private CustomerEntity customer;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentService(appointmentRepository, productService, workingHoursService,
                providerService, customerService, userService, clock);
        user = new UserEntity();
        user.setId(1L);
        provider = new ProviderEntity();
        provider.setId(10L);
        provider.setName("Ana Beatriz");
        customer = new CustomerEntity();
        customer.setId(40L);
        customer.setName("Júlia Prado");
    }

    private void authenticatedAsProvider() {
        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);
        when(providerService.findByUser(user)).thenReturn(provider);
    }

    private void authenticatedAsCustomer() {
        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);
        when(customerService.findByUser(user)).thenReturn(customer);
    }

    private static ProductEntity product(Long id, String name, String price, int minutes) {
        ProductEntity product = new ProductEntity();
        product.setId(id);
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setDurationMinutes(minutes);
        return product;
    }

    private void productsFound() {
        when(productService.findActiveForBooking(10L, PRODUCT_IDS)).thenReturn(List.of(
                product(1L, "Esmaltação em gel", "50.00", 60),
                product(2L, "Manicure", "30.00", 30)));
    }

    private void savesAppointment() {
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void providerHasNoConflict(LocalDateTime start, LocalDateTime end) {
        when(appointmentRepository.existsProviderOverlap(10L, AppointmentStatus.SCHEDULED, start, end)).thenReturn(false);
    }

    private void workingHoursOnTuesday() {
        when(workingHoursService.findIntervals(10L, TUESDAY)).thenReturn(List.of(
                new TimeRange(TUESDAY.atTime(8, 0), TUESDAY.atTime(12, 0)),
                new TimeRange(TUESDAY.atTime(13, 0), TUESDAY.atTime(19, 0))));
    }

    private static ProviderAppointmentRequest providerRequest(LocalDateTime startAt, Long customerId, String customerName) {
        return new ProviderAppointmentRequest(startAt, PRODUCT_IDS, "  Cliente prefere esmalte nude  ", customerId, customerName);
    }

    private CustomerAppointmentRequest customerRequest(LocalDateTime startAt) {
        return new CustomerAppointmentRequest(10L, startAt, PRODUCT_IDS, "Prefiro esmalte nude");
    }

    private AppointmentEntity appointment(AppointmentStatus status, LocalDateTime startAt) {
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setId(87L);
        appointment.setProvider(provider);
        appointment.setCustomer(customer);
        appointment.setStartAt(startAt);
        appointment.setEndAt(startAt.plusMinutes(90));
        appointment.setTotalMinutes(90);
        appointment.setTotalPrice(new BigDecimal("80.00"));
        appointment.setStatus(status);
        appointment.setOrigin(AppointmentOrigin.CUSTOMER);
        return appointment;
    }

    // --- agendamento pelo prestador -----------------------------------------------

    @Test
    void createByProvider_shouldCalculateEndAndTotalForRegisteredCustomer() {
        authenticatedAsProvider();
        when(customerService.findById(40L)).thenReturn(customer);
        productsFound();
        providerHasNoConflict(TUESDAY.atTime(15, 0), TUESDAY.atTime(16, 30));
        savesAppointment();

        AppointmentResponse result = appointmentService.createByProvider(providerRequest(TUESDAY.atTime(15, 0), 40L, null));

        assertThat(result.startAt()).isEqualTo(TUESDAY.atTime(15, 0));
        assertThat(result.endAt()).isEqualTo(TUESDAY.atTime(16, 30));
        assertThat(result.duration()).isEqualTo("01:30");
        assertThat(result.durationMinutes()).isEqualTo(90);
        assertThat(result.totalPrice()).isEqualByComparingTo("80.00");
        assertThat(result.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(result.origin()).isEqualTo(AppointmentOrigin.PROVIDER);
        assertThat(result.notes()).isEqualTo("Cliente prefere esmalte nude");
        assertThat(result.providerId()).isEqualTo(10L);
        assertThat(result.providerName()).isEqualTo("Ana Beatriz");
        assertThat(result.customerId()).isEqualTo(40L);
        assertThat(result.customerName()).isEqualTo("Júlia Prado");
        assertThat(result.items()).containsExactly(
                new AppointmentItemResponse(1L, "Esmaltação em gel", new BigDecimal("50.00"), "01:00", 60),
                new AppointmentItemResponse(2L, "Manicure", new BigDecimal("30.00"), "00:30", 30));
        verify(providerService).lockById(10L);
        verify(appointmentRepository).save(org.mockito.ArgumentMatchers.<AppointmentEntity>argThat(saved ->
                saved.getItems().get(0).getPosition() == 0 && saved.getItems().get(1).getPosition() == 1
                        && saved.getItems().get(0).getAppointment() == saved));
    }

    @Test
    void createByProvider_shouldAllowCustomerWithoutRegistrationAndOutsideWorkingHours() {
        authenticatedAsProvider();
        productsFound();
        providerHasNoConflict(TUESDAY.atTime(19, 30), TUESDAY.atTime(21, 0));
        savesAppointment();

        AppointmentResponse result = appointmentService.createByProvider(
                new ProviderAppointmentRequest(TUESDAY.atTime(19, 30), PRODUCT_IDS, "   ", null, "  Maria  "));

        assertThat(result.customerId()).isNull();
        assertThat(result.customerName()).isEqualTo("Maria");
        assertThat(result.notes()).isNull();
        verify(customerService, never()).findById(any());
        verify(workingHoursService, never()).findIntervals(any(), any());
        verify(appointmentRepository, never()).existsCustomerOverlap(any(), any(), any(), any());
    }

    @Test
    void createByProvider_shouldThrow_whenNoCustomerIsInformed() {
        authenticatedAsProvider();

        assertThatThrownBy(() -> appointmentService.createByProvider(providerRequest(TUESDAY.atTime(15, 0), null, " ")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Informe o cliente cadastrado ou o nome do cliente");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void createByProvider_shouldThrow_whenRegisteredCustomerNotFound() {
        authenticatedAsProvider();
        when(customerService.findById(40L)).thenThrow(new ResourceNotFoundException("Cliente não encontrado"));

        assertThatThrownBy(() -> appointmentService.createByProvider(providerRequest(TUESDAY.atTime(15, 0), 40L, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void createByProvider_shouldThrowConflict_whenTimeIsTaken() {
        authenticatedAsProvider();
        when(customerService.findById(40L)).thenReturn(customer);
        productsFound();
        when(appointmentRepository.existsProviderOverlap(10L, AppointmentStatus.SCHEDULED,
                TUESDAY.atTime(15, 0), TUESDAY.atTime(16, 30))).thenReturn(true);

        assertThatThrownBy(() -> appointmentService.createByProvider(providerRequest(TUESDAY.atTime(15, 0), 40L, null)))
                .isInstanceOf(ScheduleConflictException.class)
                .hasMessage("Esse horário já está ocupado na agenda do prestador");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void createByProvider_shouldThrow_whenAppointmentEndsOnNextDay() {
        authenticatedAsProvider();
        when(customerService.findById(40L)).thenReturn(customer);
        productsFound();

        assertThatThrownBy(() -> appointmentService.createByProvider(providerRequest(TUESDAY.atTime(23, 0), 40L, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O agendamento deve começar e terminar no mesmo dia");
        verify(providerService, never()).lockById(any());
    }

    // --- agendamento pelo cliente ----------------------------------------------------

    @Test
    void createByCustomer_shouldBookInsideWorkingHours() {
        authenticatedAsCustomer();
        when(providerService.findById(10L)).thenReturn(provider);
        productsFound();
        workingHoursOnTuesday();
        providerHasNoConflict(TUESDAY.atTime(10, 30), TUESDAY.atTime(12, 0));
        when(appointmentRepository.existsCustomerOverlap(40L, AppointmentStatus.SCHEDULED,
                TUESDAY.atTime(10, 30), TUESDAY.atTime(12, 0))).thenReturn(false);
        savesAppointment();

        AppointmentResponse result = appointmentService.createByCustomer(customerRequest(TUESDAY.atTime(10, 30)));

        assertThat(result.endAt()).isEqualTo(TUESDAY.atTime(12, 0));
        assertThat(result.origin()).isEqualTo(AppointmentOrigin.CUSTOMER);
        assertThat(result.customerId()).isEqualTo(40L);
        assertThat(result.notes()).isEqualTo("Prefiro esmalte nude");
        verify(providerService).lockById(10L);
    }

    @Test
    void createByCustomer_shouldThrow_whenServicesDoNotFitInWorkingInterval() {
        authenticatedAsCustomer();
        when(providerService.findById(10L)).thenReturn(provider);
        productsFound();
        workingHoursOnTuesday();

        assertThatThrownBy(() -> appointmentService.createByCustomer(customerRequest(TUESDAY.atTime(11, 0))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O horário escolhido está fora do expediente do prestador");
        verify(providerService, never()).lockById(any());
    }

    @Test
    void createByCustomer_shouldThrow_whenCustomerHasAnotherAppointmentAtSameTime() {
        authenticatedAsCustomer();
        when(providerService.findById(10L)).thenReturn(provider);
        productsFound();
        workingHoursOnTuesday();
        providerHasNoConflict(TUESDAY.atTime(15, 0), TUESDAY.atTime(16, 30));
        when(appointmentRepository.existsCustomerOverlap(40L, AppointmentStatus.SCHEDULED,
                TUESDAY.atTime(15, 0), TUESDAY.atTime(16, 30))).thenReturn(true);

        assertThatThrownBy(() -> appointmentService.createByCustomer(customerRequest(TUESDAY.atTime(15, 0))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Você já tem um agendamento nesse horário");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void createByCustomer_shouldThrowConflict_whenTimeIsTaken() {
        authenticatedAsCustomer();
        when(providerService.findById(10L)).thenReturn(provider);
        productsFound();
        workingHoursOnTuesday();
        when(appointmentRepository.existsProviderOverlap(10L, AppointmentStatus.SCHEDULED,
                TUESDAY.atTime(15, 0), TUESDAY.atTime(16, 30))).thenReturn(true);

        assertThatThrownBy(() -> appointmentService.createByCustomer(customerRequest(TUESDAY.atTime(15, 0))))
                .isInstanceOf(ScheduleConflictException.class);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void createByCustomer_shouldThrow_whenStartIsNotMultipleOf15Minutes() {
        authenticatedAsCustomer();
        when(providerService.findById(10L)).thenReturn(provider);
        productsFound();

        assertThatThrownBy(() -> appointmentService.createByCustomer(customerRequest(TUESDAY.atTime(15, 10))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O horário de início deve ser múltiplo de 15 minutos (ex.: 08:00, 08:15)");
    }

    @Test
    void createByCustomer_shouldThrow_whenStartIsInThePast() {
        authenticatedAsCustomer();
        when(providerService.findById(10L)).thenReturn(provider);
        productsFound();

        assertThatThrownBy(() -> appointmentService.createByCustomer(customerRequest(NOW.withMinute(0))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Não é possível agendar em um horário que já passou");
    }

    @Test
    void createByCustomer_shouldThrow_whenStartIsBeyondMaximumDaysAhead() {
        authenticatedAsCustomer();
        when(providerService.findById(10L)).thenReturn(provider);
        productsFound();
        LocalDateTime tooFar = NOW.toLocalDate().plusDays(AppointmentService.MAX_DAYS_AHEAD + 1).atTime(10, 0);

        assertThatThrownBy(() -> appointmentService.createByCustomer(customerRequest(tooFar)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("É possível agendar com até 60 dias de antecedência");
    }

    @Test
    void createByCustomer_shouldThrow_whenProviderNotFound() {
        when(providerService.findById(10L)).thenThrow(new ResourceNotFoundException("Prestador não encontrado"));

        assertThatThrownBy(() -> appointmentService.createByCustomer(customerRequest(TUESDAY.atTime(15, 0))))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(appointmentRepository, never()).save(any());
    }

    // --- cancelamento ------------------------------------------------------------------

    @Test
    void cancelByProvider_shouldCancelAndRecordWhoCanceled() {
        authenticatedAsProvider();
        when(appointmentRepository.findByIdAndProviderId(87L, 10L))
                .thenReturn(Optional.of(appointment(AppointmentStatus.SCHEDULED, TUESDAY.atTime(15, 0))));
        savesAppointment();

        AppointmentResponse result = appointmentService.cancelByProvider(87L);

        assertThat(result.status()).isEqualTo(AppointmentStatus.CANCELED);
        assertThat(result.canceledBy()).isEqualTo(AppointmentOrigin.PROVIDER);
        assertThat(result.canceledAt()).isEqualTo(clock.instant());
    }

    @Test
    void cancelByCustomer_shouldCancelOwnAppointment() {
        authenticatedAsCustomer();
        when(appointmentRepository.findByIdAndCustomerId(87L, 40L))
                .thenReturn(Optional.of(appointment(AppointmentStatus.SCHEDULED, TUESDAY.atTime(15, 0))));
        savesAppointment();

        AppointmentResponse result = appointmentService.cancelByCustomer(87L);

        assertThat(result.status()).isEqualTo(AppointmentStatus.CANCELED);
        assertThat(result.canceledBy()).isEqualTo(AppointmentOrigin.CUSTOMER);
    }

    @Test
    void cancelByProvider_shouldThrow_whenAppointmentBelongsToAnotherProvider() {
        authenticatedAsProvider();
        when(appointmentRepository.findByIdAndProviderId(87L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.cancelByProvider(87L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Agendamento não encontrado");
    }

    @Test
    void cancelByCustomer_shouldThrow_whenAppointmentBelongsToAnotherCustomer() {
        authenticatedAsCustomer();
        when(appointmentRepository.findByIdAndCustomerId(87L, 40L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.cancelByCustomer(87L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancel_shouldThrow_whenAlreadyCanceled() {
        authenticatedAsProvider();
        when(appointmentRepository.findByIdAndProviderId(87L, 10L))
                .thenReturn(Optional.of(appointment(AppointmentStatus.CANCELED, TUESDAY.atTime(15, 0))));

        assertThatThrownBy(() -> appointmentService.cancelByProvider(87L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Esse agendamento já foi cancelado");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void cancel_shouldThrow_whenAppointmentAlreadyStarted() {
        authenticatedAsProvider();
        when(appointmentRepository.findByIdAndProviderId(87L, 10L))
                .thenReturn(Optional.of(appointment(AppointmentStatus.SCHEDULED, NOW.withMinute(0))));

        assertThatThrownBy(() -> appointmentService.cancelByProvider(87L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Não é possível cancelar um agendamento que já começou");
    }

    // --- listagens ----------------------------------------------------------------------

    @Test
    void findForProvider_shouldListAppointmentsStartingBetweenDates() {
        authenticatedAsProvider();
        when(appointmentRepository.findByProviderIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
                10L, TUESDAY.atStartOfDay(), TUESDAY.plusDays(7).atStartOfDay()))
                .thenReturn(List.of(appointment(AppointmentStatus.SCHEDULED, TUESDAY.atTime(15, 0))));

        List<AppointmentResponse> result = appointmentService.findForProvider(TUESDAY, TUESDAY.plusDays(6));

        assertThat(result).extracting(AppointmentResponse::id).containsExactly(87L);
    }

    @Test
    void findForCustomer_shouldListAppointmentsOfAuthenticatedCustomer() {
        authenticatedAsCustomer();
        when(appointmentRepository.findByCustomerIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
                40L, TUESDAY.atStartOfDay(), TUESDAY.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(appointment(AppointmentStatus.SCHEDULED, TUESDAY.atTime(15, 0))));

        List<AppointmentResponse> result = appointmentService.findForCustomer(TUESDAY, TUESDAY);

        assertThat(result).extracting(AppointmentResponse::customerName).containsExactly("Júlia Prado");
    }

    @Test
    void findForProvider_shouldThrow_whenEndDateIsBeforeStartDate() {
        assertThatThrownBy(() -> appointmentService.findForProvider(TUESDAY, TUESDAY.minusDays(1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("A data final deve ser igual ou posterior à data inicial");
    }

    @Test
    void findForCustomer_shouldThrow_whenPeriodIsTooLong() {
        assertThatThrownBy(() -> appointmentService.findForCustomer(TUESDAY, TUESDAY.plusDays(AppointmentService.MAX_LIST_DAYS)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O período consultado deve ter no máximo 92 dias");
    }
}
