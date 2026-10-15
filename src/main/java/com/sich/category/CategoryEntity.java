package com.sich.category;

import com.sich.common.base.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Categoria de serviços cadastrada pelo admin (ex.: Unha, Cabelo). */
@Getter
@Setter
@Entity
@Table(name = "categories")
public class CategoryEntity extends AbstractEntity {

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(columnDefinition = "text")
    private String description;
}
