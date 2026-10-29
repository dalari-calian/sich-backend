package com.sich.workinghours;

import java.time.LocalTime;

import com.sich.common.base.AbstractEntity;
import com.sich.provider.ProviderEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Um intervalo de trabalho do prestador em um dia da semana. O almoço não é salvo:
 * é o espaço entre dois intervalos do mesmo dia (ex.: 08:00–12:00 e 13:00–19:00).
 * Dia sem intervalos é folga.
 */
@Getter
@Setter
@Entity
@Table(name = "providerworkinghours")
public class WorkingHoursEntity extends AbstractEntity {

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "providerid", nullable = false)
    private ProviderEntity provider;

    /** Dia da semana como no {@link java.time.DayOfWeek}: 1 = segunda ... 7 = domingo. */
    @Column(nullable = false)
    private Integer weekday;

    @Column(name = "starttime", nullable = false)
    private LocalTime startTime;

    @Column(name = "endtime", nullable = false)
    private LocalTime endTime;
}
