package com.sich.benefit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.appointment.AppointmentEntity;
import com.sich.appointment.AppointmentRepository;
import com.sich.appointment.AppointmentService;
import com.sich.appointment.AppointmentStatus;
import com.sich.benefit.dto.CashbackMovementResponse;
import com.sich.benefit.dto.CustomerBenefitsResponse;
import com.sich.benefit.dto.LoyaltyResponse;
import com.sich.common.dto.PageResponse;
import com.sich.customer.CustomerService;
import com.sich.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * Cashback e cartão fidelidade do cliente, calculados a partir dos agendamentos (sem
 * extrato próprio), com consultas agregadas:
 * <ul>
 * <li>cada agendamento de cliente cadastrado guarda o cashback que vai gerar
 * ({@code cashbackearned}) e o que usou como desconto ({@code cashbackused});</li>
 * <li>o cashback ganho entra no saldo quando o atendimento termina sem ser cancelado;</li>
 * <li>a cada {@code loyaltyGoal} atendimentos realizados entra um bônus de {@code loyaltyBonus};</li>
 * <li>o desconto usado sai do saldo na hora de agendar e volta se o agendamento for cancelado.</li>
 * </ul>
 * Como atendimento que já começou não pode ser cancelado, o saldo nunca fica negativo.
 */
@Service
@RequiredArgsConstructor
public class BenefitService {

    private final AppointmentRepository appointmentRepository;
    private final CustomerService customerService;
    private final UserService userService;
    private final BenefitProperties properties;
    private final Clock clock;

    /** Carteira do cliente autenticado: saldo, cashback a receber e progresso do cartão fidelidade. */
    @Transactional(readOnly = true)
    public CustomerBenefitsResponse forCustomer() {
        Long customerId = customerService.findByUser(userService.getCurrentlyAuthenticatedUser()).getId();
        LocalDateTime now = LocalDateTime.now(clock);
        Wallet wallet = wallet(customerId, now);
        BigDecimal pending = orZero(appointmentRepository.sumCashbackEarnedUpcoming(customerId, AppointmentStatus.SCHEDULED, now));
        int goal = loyaltyGoal();
        int progress = (int) (wallet.completedAppointments() % goal);
        return new CustomerBenefitsResponse(
                wallet.balance(),
                pending,
                wallet.earned(),
                wallet.used(),
                properties.getCashbackRate(),
                properties.getMaxCashbackUsageRate(),
                new LoyaltyResponse(
                        goal,
                        progress,
                        goal - progress,
                        wallet.completedCards(),
                        wallet.completedAppointments(),
                        money(properties.getLoyaltyBonus()),
                        wallet.bonus()));
    }

    /**
     * Extrato da carteira do cliente autenticado: agendamentos que geraram ou usaram
     * cashback, do mais recente para o mais antigo (página a partir de 0, tamanho de 1
     * a {@value AppointmentService#MAX_PAGE_SIZE}).
     */
    @Transactional(readOnly = true)
    public PageResponse<CashbackMovementResponse> history(int page, int size) {
        Long customerId = customerService.findByUser(userService.getCurrentlyAuthenticatedUser()).getId();
        LocalDateTime now = LocalDateTime.now(clock);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, AppointmentService.MAX_PAGE_SIZE));
        return PageResponse.of(appointmentRepository.findCashbackMovements(customerId, pageable),
                appointment -> new CashbackMovementResponse(
                        appointment.getId(),
                        appointment.getStartAt(),
                        appointment.getProvider().getId(),
                        appointment.getProvider().getName(),
                        appointment.getTotalPrice(),
                        appointment.getCashbackUsed(),
                        appointment.getCashbackEarned(),
                        statusOf(appointment, now)));
    }

    private static CashbackStatus statusOf(AppointmentEntity appointment, LocalDateTime now) {
        if (appointment.getStatus() == AppointmentStatus.CANCELED) {
            return CashbackStatus.CANCELED;
        }
        return appointment.getEndAt().isAfter(now) ? CashbackStatus.PENDING : CashbackStatus.CREDITED;
    }

    /** Saldo de cashback disponível do cliente agora. */
    @Transactional(readOnly = true)
    public BigDecimal availableBalance(Long customerId) {
        return wallet(customerId, LocalDateTime.now(clock)).balance();
    }

    /** Maior desconto com cashback permitido para um agendamento de {@code total} (arredondado para baixo). */
    public BigDecimal maxUsable(BigDecimal total) {
        return total.multiply(properties.getMaxCashbackUsageRate()).setScale(2, RoundingMode.DOWN);
    }

    /** Cashback gerado ao pagar {@code amountPaid} (arredondado para baixo). */
    public BigDecimal earnedOn(BigDecimal amountPaid) {
        return amountPaid.multiply(properties.getCashbackRate()).setScale(2, RoundingMode.DOWN);
    }

    private Wallet wallet(Long customerId, LocalDateTime now) {
        BigDecimal earnedFinished = orZero(
                appointmentRepository.sumCashbackEarnedFinished(customerId, AppointmentStatus.SCHEDULED, now));
        BigDecimal used = orZero(appointmentRepository.sumCashbackUsed(customerId, AppointmentStatus.SCHEDULED));
        long completed = appointmentRepository.countByCustomerIdAndStatusAndEndAtLessThanEqual(
                customerId, AppointmentStatus.SCHEDULED, now);
        long cards = completed / loyaltyGoal();
        BigDecimal bonus = money(properties.getLoyaltyBonus().multiply(BigDecimal.valueOf(cards)));
        BigDecimal earned = earnedFinished.add(bonus);
        return new Wallet(earned, used, bonus, completed, cards, earned.subtract(used).max(BigDecimal.ZERO));
    }

    /** Meta do cartão fidelidade (nunca menor que 1, para não dividir por zero com configuração errada). */
    private int loyaltyGoal() {
        return Math.max(1, properties.getLoyaltyGoal());
    }

    private static BigDecimal orZero(BigDecimal value) {
        return money(value == null ? BigDecimal.ZERO : value);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.DOWN);
    }

    private record Wallet(BigDecimal earned, BigDecimal used, BigDecimal bonus, long completedAppointments,
            long completedCards, BigDecimal balance) {
    }
}
