package com.sich.providerproduct;

import java.math.BigDecimal;

import com.sich.common.base.AbstractEntity;
import com.sich.product.ProductEntity;
import com.sich.provider.ProviderEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Produto do catálogo que o prestador selecionou para prestar, com a descrição e o
 * preço informados por ele. Cada produto aparece no máximo uma vez por prestador.
 */
@Getter
@Setter
@Entity
@Table(name = "providerproducts", uniqueConstraints = @UniqueConstraint(
        name = "uqproviderproduct", columnNames = { "providerid", "productid" }))
public class ProviderProductEntity extends AbstractEntity {

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "providerid", nullable = false)
    private ProviderEntity provider;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "productid", nullable = false)
    private ProductEntity product;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}
