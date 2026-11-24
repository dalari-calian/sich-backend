package com.sich.profile;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
import lombok.ToString;

/**
 * Foto de perfil do usuário (cliente, prestador ou admin), salva como data URL em
 * base64. Fica numa tabela própria para a imagem não ser carregada junto com o
 * usuário em toda requisição autenticada.
 */
@Getter
@Setter
@Entity
@Table(name = "userphotos")
public class UserPhotoEntity extends AbstractEntity {

    @ToString.Exclude
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "userid", nullable = false, unique = true)
    private UserEntity user;

    @ToString.Exclude
    @Column(nullable = false, columnDefinition = "text")
    private String image;
}
