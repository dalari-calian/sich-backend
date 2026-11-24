package com.sich.favorite;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.sich.common.base.AbstractEntity;
import com.sich.customer.CustomerEntity;
import com.sich.provider.ProviderEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** Prestador favoritado por um cliente (um registro por par cliente/prestador). */
@Getter
@Setter
@Entity
@Table(name = "favoriteproviders",
        uniqueConstraints = @UniqueConstraint(name = "favoriteproviders_customer_provider_uq",
                columnNames = { "customerid", "providerid" }))
public class FavoriteProviderEntity extends AbstractEntity {

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "customerid", nullable = false)
    private CustomerEntity customer;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "providerid", nullable = false)
    private ProviderEntity provider;
}
