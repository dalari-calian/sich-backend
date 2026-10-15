package com.sich.product;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
 * Foto de um produto (até 5 por produto), salva como data URL em base64
 * ({@code data:image/png;base64,...}). Apagada em cascata junto com o produto.
 */
@Getter
@Setter
@Entity
@Table(name = "productimages")
public class ProductImageEntity extends AbstractEntity {

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "productid", nullable = false)
    private ProductEntity product;

    @ToString.Exclude
    @Column(nullable = false, columnDefinition = "text")
    private String image;
}
