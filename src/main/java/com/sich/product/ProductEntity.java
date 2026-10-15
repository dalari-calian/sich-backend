package com.sich.product;

import java.math.BigDecimal;

import com.sich.category.CategoryEntity;
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
 * Produto/serviço cadastrado pelo prestador. Pertence a um único prestador e a uma
 * categoria cadastrada pelo admin.
 */
@Getter
@Setter
@Entity
@Table(name = "products")
public class ProductEntity extends AbstractEntity {

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "providerid", nullable = false)
    private ProviderEntity provider;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoryid", nullable = false)
    private CategoryEntity category;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Tempo médio do serviço em minutos (na tela é exibido como HH:MM). */
    @Column(name = "durationminutes", nullable = false)
    private Integer durationMinutes;
}
