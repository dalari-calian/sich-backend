package com.sich.product;

import com.sich.common.base.AbstractEntity;

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
 * Item do catálogo de produtos/serviços, organizado em árvore: cada registro aponta
 * para o pai no nível acima ({@code parentid}); {@code parentid} nulo = item raiz.
 */
@Getter
@Setter
@Entity
@Table(name = "products")
public class ProductEntity extends AbstractEntity {

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentid")
    private ProductEntity parent;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;
}
