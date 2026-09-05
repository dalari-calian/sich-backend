package com.sich.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sich.common.base.AbstractEntity;
import com.sich.common.enums.UserType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Entity
@Table(name = "users")
public class UserEntity extends AbstractEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @ToString.Exclude
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "usertype", nullable = false)
    private UserType userType;

    @Column(nullable = false)
    private boolean active = true;
}
