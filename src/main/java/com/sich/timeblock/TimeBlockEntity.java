package com.sich.timeblock;

import java.time.LocalDateTime;

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
 * Período {@code [startAt, endAt)} em que o prestador não atende (folga, compromisso,
 * férias). Ninguém agenda sobre um bloqueio. Pode durar vários dias.
 */
@Getter
@Setter
@Entity
@Table(name = "providertimeblocks")
public class TimeBlockEntity extends AbstractEntity {

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "providerid", nullable = false)
    private ProviderEntity provider;

    @Column(name = "startat", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "endat", nullable = false)
    private LocalDateTime endAt;

    @Column(length = 200)
    private String reason;
}
