package com.sich.appointment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import com.sich.common.base.AbstractEntity;
import com.sich.customer.CustomerEntity;
import com.sich.provider.ProviderEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Horário reservado na agenda do prestador. Os horários são o relógio de parede do
 * prestador (sem fuso) e o período é semiaberto: {@code [startat, endat)}.
 */
@Getter
@Setter
@Entity
@Table(name = "appointments")
public class AppointmentEntity extends AbstractEntity {

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "providerid", nullable = false)
    private ProviderEntity provider;

    /** Cliente cadastrado; nulo quando o prestador agenda para alguém sem cadastro. */
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerid")
    private CustomerEntity customer;

    /** Nome do cliente sem cadastro (preenchido apenas quando {@code customer} é nulo). */
    @Column(name = "customername", length = 120)
    private String customerName;

    @Column(name = "startat", nullable = false)
    private LocalDateTime startAt;

    /** Calculado pelo backend: início + soma das durações dos serviços. */
    @Column(name = "endat", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "totalminutes", nullable = false)
    private Integer totalMinutes;

    @Column(name = "totalprice", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(length = 400)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentOrigin origin;

    @Column(name = "canceledat")
    private Instant canceledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "canceledby", length = 20)
    private AppointmentOrigin canceledBy;

    /** Em listagens paginadas, os itens de até 50 agendamentos são buscados numa consulta só. */
    @ToString.Exclude
    @BatchSize(size = 50)
    @OrderBy("position")
    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AppointmentItemEntity> items = new ArrayList<>();
}
