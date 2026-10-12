package com.sich.admin;

import com.sich.common.base.AbstractEntity;
import com.sich.user.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Perfil do administrador. Não é criado pela API: o registro em {@code users}
 * (com {@code usertype = 'ADMIN'}) e o registro aqui são inseridos direto no banco.
 */
@Getter
@Setter
@Entity
@Table(name = "admins")
public class AdminEntity extends AbstractEntity {

    @Column(nullable = false)
    private String name;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "userid", nullable = false, unique = true)
    private UserEntity user;
}
