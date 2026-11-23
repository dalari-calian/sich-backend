package com.sich.benefit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sich.appointment.AppointmentRepository;
import com.sich.appointment.AppointmentStatus;
import com.sich.benefit.dto.CustomerBenefitsResponse;
import com.sich.customer.CustomerEntity;
import com.sich.customer.CustomerService;
import com.sich.user.UserEntity;
import com.sich.user.UserService;

@ExtendWith(MockitoExtension.class)
class BenefitServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 23, 10, 0);
    private static final AppointmentStatus SCHEDULED = AppointmentStatus.SCHEDULED;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private CustomerService customerService;

    @Mock
    private UserService userService;

    private final BenefitProperties properties = new BenefitProperties();

    private BenefitService benefitService;

    @BeforeEach
    void setUp() {
        benefitService = new BenefitService(appointmentRepository, customerService, userService, properties,
                Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE));
    }

    private void authenticatedAsCustomer() {
        UserEntity user = new UserEntity();
        CustomerEntity customer = new CustomerEntity();
        customer.setId(40L);
        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);
        when(customerService.findByUser(user)).thenReturn(customer);
    }

    private void wallet(String earnedFinished, String used, long completed) {
        when(appointmentRepository.sumCashbackEarnedFinished(40L, SCHEDULED, NOW))
                .thenReturn(earnedFinished == null ? null : new BigDecimal(earnedFinished));
        when(appointmentRepository.sumCashbackUsed(40L, SCHEDULED)).thenReturn(used == null ? null : new BigDecimal(used));
        when(appointmentRepository.countByCustomerIdAndStatusAndEndAtLessThanEqual(40L, SCHEDULED, NOW)).thenReturn(completed);
    }

    @Test
    void forCustomer_shouldCombineCashbackLoyaltyBonusAndUsage() {
        authenticatedAsCustomer();
        wallet("12.50", "4.00", 13);
        when(appointmentRepository.sumCashbackEarnedUpcoming(40L, SCHEDULED, NOW)).thenReturn(new BigDecimal("3.00"));

        CustomerBenefitsResponse result = benefitService.forCustomer();

        // 13 atendimentos = 2 cartões completos (2 × R$ 10) e 3/5 no cartão atual.
        assertThat(result.cashbackEarnedTotal()).isEqualByComparingTo("32.50");
        assertThat(result.cashbackUsedTotal()).isEqualByComparingTo("4.00");
        assertThat(result.cashbackBalance()).isEqualByComparingTo("28.50");
        assertThat(result.cashbackPending()).isEqualByComparingTo("3.00");
        assertThat(result.cashbackRate()).isEqualByComparingTo("0.05");
        assertThat(result.maxCashbackUsageRate()).isEqualByComparingTo("0.20");
        assertThat(result.loyalty().goal()).isEqualTo(5);
        assertThat(result.loyalty().progress()).isEqualTo(3);
        assertThat(result.loyalty().remaining()).isEqualTo(2);
        assertThat(result.loyalty().completedCards()).isEqualTo(2);
        assertThat(result.loyalty().completedAppointments()).isEqualTo(13);
        assertThat(result.loyalty().bonusPerCard()).isEqualByComparingTo("10.00");
        assertThat(result.loyalty().bonusEarnedTotal()).isEqualByComparingTo("20.00");
    }

    @Test
    void forCustomer_shouldReturnZeros_whenCustomerHasNoAppointments() {
        authenticatedAsCustomer();
        wallet(null, null, 0);
        when(appointmentRepository.sumCashbackEarnedUpcoming(40L, SCHEDULED, NOW)).thenReturn(null);

        CustomerBenefitsResponse result = benefitService.forCustomer();

        assertThat(result.cashbackBalance()).isEqualByComparingTo("0");
        assertThat(result.cashbackPending()).isEqualByComparingTo("0");
        assertThat(result.loyalty().progress()).isZero();
        assertThat(result.loyalty().remaining()).isEqualTo(5);
        assertThat(result.loyalty().completedCards()).isZero();
    }

    @Test
    void forCustomer_shouldTreatInvalidLoyaltyGoalAsOne() {
        properties.setLoyaltyGoal(0);
        authenticatedAsCustomer();
        wallet(null, null, 2);
        when(appointmentRepository.sumCashbackEarnedUpcoming(40L, SCHEDULED, NOW)).thenReturn(null);

        CustomerBenefitsResponse result = benefitService.forCustomer();

        assertThat(result.loyalty().goal()).isEqualTo(1);
        assertThat(result.loyalty().completedCards()).isEqualTo(2);
    }

    @Test
    void availableBalance_shouldNeverBeNegative() {
        wallet("1.00", "5.00", 0);

        assertThat(benefitService.availableBalance(40L)).isEqualByComparingTo("0");
    }

    @Test
    void availableBalance_shouldBeEarnedPlusBonusMinusUsed() {
        wallet("7.35", "2.00", 5);

        assertThat(benefitService.availableBalance(40L)).isEqualByComparingTo("15.35");
    }

    @Test
    void maxUsable_shouldBeTwentyPercentRoundedDown() {
        assertThat(benefitService.maxUsable(new BigDecimal("80.00"))).isEqualByComparingTo("16.00");
        assertThat(benefitService.maxUsable(new BigDecimal("33.33"))).isEqualByComparingTo("6.66");
    }

    @Test
    void earnedOn_shouldBeFivePercentRoundedDown() {
        assertThat(benefitService.earnedOn(new BigDecimal("64.00"))).isEqualByComparingTo("3.20");
        assertThat(benefitService.earnedOn(new BigDecimal("74.50"))).isEqualByComparingTo("3.72");
    }
}
