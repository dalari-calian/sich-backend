package com.sich.appointment;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.sich.appointment.dto.AppointmentResponse;
import com.sich.appointment.dto.CustomerAppointmentRequest;
import com.sich.appointment.dto.ProviderAppointmentRequest;
import com.sich.common.dto.PageResponse;
import com.sich.common.exception.BusinessRuleException;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.common.exception.ScheduleConflictException;
import com.sich.common.util.TimeGrid;
import com.sich.common.util.TimeRange;
import com.sich.customer.CustomerEntity;
import com.sich.customer.CustomerService;
import com.sich.product.ProductEntity;
import com.sich.product.ProductService;
import com.sich.provider.ProviderEntity;
import com.sich.provider.ProviderService;
import com.sich.user.UserService;
import com.sich.workinghours.WorkingHoursService;

import lombok.RequiredArgsConstructor;

/**
 * Agendamentos feitos pelo prestador (na própria agenda) e pelo cliente (na agenda de
 * um prestador). Os dois fluxos passam pelas mesmas validações em {@link #book}.
 * O horário final e o valor total são sempre calculados aqui, a partir dos produtos.
 */
@Service
@RequiredArgsConstructor
public class AppointmentService {

    /** Até quantos dias à frente é possível agendar. */
    public static final int MAX_DAYS_AHEAD = 60;

    /** Maior período (em dias) consultado de uma vez nas listagens da agenda. */
    public static final int MAX_LIST_DAYS = 92;

    /** Maior página aceita nos históricos. */
    public static final int MAX_PAGE_SIZE = 50;

    private final AppointmentRepository appointmentRepository;
    private final ProductService productService;
    private final WorkingHoursService workingHoursService;
    private final ProviderService providerService;
    private final CustomerService customerService;
    private final UserService userService;
    private final Clock clock;

    /**
     * O prestador reserva um horário na própria agenda para um cliente cadastrado
     * ({@code customerId}) ou sem cadastro ({@code customerName}). Pode ser fora do
     * expediente (encaixe), mas nunca sobre outro agendamento.
     */
    @Transactional
    public AppointmentResponse createByProvider(ProviderAppointmentRequest request) {
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setProvider(currentProvider());
        appointment.setOrigin(AppointmentOrigin.PROVIDER);
        if (request.customerId() != null) {
            appointment.setCustomer(customerService.findById(request.customerId()));
        } else if (StringUtils.hasText(request.customerName())) {
            appointment.setCustomerName(request.customerName().trim());
        } else {
            throw new BusinessRuleException("Informe o cliente cadastrado ou o nome do cliente");
        }
        appointment.setStartAt(request.startAt());
        appointment.setNotes(normalizeNotes(request.notes()));
        return book(appointment, request.productIds());
    }

    /** O cliente autenticado reserva um horário dentro do expediente do prestador. */
    @Transactional
    public AppointmentResponse createByCustomer(CustomerAppointmentRequest request) {
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setProvider(providerService.findById(request.providerId()));
        appointment.setCustomer(currentCustomer());
        appointment.setOrigin(AppointmentOrigin.CUSTOMER);
        appointment.setStartAt(request.startAt());
        appointment.setNotes(normalizeNotes(request.notes()));
        return book(appointment, request.productIds());
    }

    @Transactional
    public AppointmentResponse cancelByProvider(Long id) {
        AppointmentEntity appointment = appointmentRepository.findByIdAndProviderId(id, currentProvider().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));
        return cancel(appointment, AppointmentOrigin.PROVIDER);
    }

    @Transactional
    public AppointmentResponse cancelByCustomer(Long id) {
        AppointmentEntity appointment = appointmentRepository.findByIdAndCustomerId(id, currentCustomer().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));
        return cancel(appointment, AppointmentOrigin.CUSTOMER);
    }

    /** Agenda do prestador autenticado, com cancelados, dos dias {@code from} a {@code to}. */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> findForProvider(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        return appointmentRepository.findByProviderIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
                currentProvider().getId(), from.atStartOfDay(), to.plusDays(1).atStartOfDay()).stream()
                .map(AppointmentResponse::of)
                .toList();
    }

    /** Agenda do cliente autenticado (inclusive o que o prestador marcou para ele), dos dias {@code from} a {@code to}. */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> findForCustomer(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        return appointmentRepository.findByCustomerIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
                currentCustomer().getId(), from.atStartOfDay(), to.plusDays(1).atStartOfDay()).stream()
                .map(AppointmentResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse findByIdForProvider(Long id) {
        return AppointmentResponse.of(appointmentRepository.findByIdAndProviderId(id, currentProvider().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado")));
    }

    @Transactional(readOnly = true)
    public AppointmentResponse findByIdForCustomer(Long id) {
        return AppointmentResponse.of(appointmentRepository.findByIdAndCustomerId(id, currentCustomer().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado")));
    }

    /** Agendamentos do prestador que já começaram (inclusive cancelados), do mais recente para o mais antigo. */
    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> historyForProvider(int page, int size) {
        return PageResponse.of(appointmentRepository.findByProviderIdAndStartAtLessThanOrderByStartAtDesc(
                currentProvider().getId(), LocalDateTime.now(clock), pageable(page, size)), AppointmentResponse::of);
    }

    /** Agendamentos do cliente que já começaram (inclusive cancelados), do mais recente para o mais antigo. */
    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> historyForCustomer(int page, int size) {
        return PageResponse.of(appointmentRepository.findByCustomerIdAndStartAtLessThanOrderByStartAtDesc(
                currentCustomer().getId(), LocalDateTime.now(clock), pageable(page, size)), AppointmentResponse::of);
    }

    /** Página a partir de 0 e tamanho entre 1 e {@value #MAX_PAGE_SIZE}. */
    static Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
    }

    private AppointmentResponse book(AppointmentEntity appointment, List<Long> productIds) {
        Long providerId = appointment.getProvider().getId();
        List<ProductEntity> products = productService.findActiveForBooking(providerId, productIds);
        LocalDateTime startAt = appointment.getStartAt();
        validateStart(startAt);

        int totalMinutes = products.stream().mapToInt(ProductEntity::getDurationMinutes).sum();
        LocalDateTime endAt = startAt.plusMinutes(totalMinutes);
        if (!endAt.toLocalDate().equals(startAt.toLocalDate())) {
            throw new BusinessRuleException("O agendamento deve começar e terminar no mesmo dia");
        }
        TimeRange period = new TimeRange(startAt, endAt);
        boolean byCustomer = appointment.getOrigin() == AppointmentOrigin.CUSTOMER;
        if (byCustomer && workingHoursService.findIntervals(providerId, startAt.toLocalDate()).stream()
                .noneMatch(interval -> interval.contains(period))) {
            throw new BusinessRuleException("O horário escolhido está fora do expediente do prestador");
        }

        providerService.lockById(providerId);
        if (appointmentRepository.existsProviderOverlap(providerId, AppointmentStatus.SCHEDULED, startAt, endAt)) {
            throw new ScheduleConflictException("Esse horário já está ocupado na agenda do prestador");
        }
        if (byCustomer && appointmentRepository.existsCustomerOverlap(appointment.getCustomer().getId(),
                AppointmentStatus.SCHEDULED, startAt, endAt)) {
            throw new BusinessRuleException("Você já tem um agendamento nesse horário");
        }

        appointment.setEndAt(endAt);
        appointment.setTotalMinutes(totalMinutes);
        appointment.setTotalPrice(products.stream().map(ProductEntity::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add));
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        for (int position = 0; position < products.size(); position++) {
            appointment.getItems().add(item(appointment, products.get(position), position));
        }
        return AppointmentResponse.of(appointmentRepository.save(appointment));
    }

    private void validateStart(LocalDateTime startAt) {
        if (!TimeGrid.isAligned(startAt.toLocalTime())) {
            throw new BusinessRuleException("O horário de início deve ser múltiplo de 15 minutos (ex.: 08:00, 08:15)");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (startAt.isBefore(now)) {
            throw new BusinessRuleException("Não é possível agendar em um horário que já passou");
        }
        if (startAt.toLocalDate().isAfter(now.toLocalDate().plusDays(MAX_DAYS_AHEAD))) {
            throw new BusinessRuleException("É possível agendar com até " + MAX_DAYS_AHEAD + " dias de antecedência");
        }
    }

    private AppointmentResponse cancel(AppointmentEntity appointment, AppointmentOrigin canceledBy) {
        if (appointment.getStatus() == AppointmentStatus.CANCELED) {
            throw new BusinessRuleException("Esse agendamento já foi cancelado");
        }
        if (appointment.getStartAt().isBefore(LocalDateTime.now(clock))) {
            throw new BusinessRuleException("Não é possível cancelar um agendamento que já começou");
        }
        appointment.setStatus(AppointmentStatus.CANCELED);
        appointment.setCanceledAt(clock.instant());
        appointment.setCanceledBy(canceledBy);
        return AppointmentResponse.of(appointmentRepository.save(appointment));
    }

    static void validatePeriod(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new BusinessRuleException("A data final deve ser igual ou posterior à data inicial");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_LIST_DAYS) {
            throw new BusinessRuleException("O período consultado deve ter no máximo " + MAX_LIST_DAYS + " dias");
        }
    }

    private static AppointmentItemEntity item(AppointmentEntity appointment, ProductEntity product, int position) {
        AppointmentItemEntity item = new AppointmentItemEntity();
        item.setAppointment(appointment);
        item.setProduct(product);
        item.setProductName(product.getName());
        item.setPrice(product.getPrice());
        item.setDurationMinutes(product.getDurationMinutes());
        item.setPosition(position);
        return item;
    }

    private static String normalizeNotes(String notes) {
        return StringUtils.hasText(notes) ? notes.trim() : null;
    }

    private ProviderEntity currentProvider() {
        return providerService.findByUser(userService.getCurrentlyAuthenticatedUser());
    }

    private CustomerEntity currentCustomer() {
        return customerService.findByUser(userService.getCurrentlyAuthenticatedUser());
    }
}
