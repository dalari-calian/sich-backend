package com.sich.customer;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {

    Optional<CustomerEntity> findByUserId(Long userId);

    Optional<CustomerEntity> findByUserEmailIgnoreCase(String email);

    Optional<CustomerEntity> findFirstByPhone(String phone);

    /** Trava a linha do cliente até o fim da transação: o saldo de cashback dele é usado por um agendamento de cada vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CustomerEntity c where c.id = :id")
    Optional<CustomerEntity> findByIdForUpdate(@Param("id") Long id);
}
