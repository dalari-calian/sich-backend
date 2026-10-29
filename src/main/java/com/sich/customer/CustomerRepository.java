package com.sich.customer;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {

    Optional<CustomerEntity> findByUserId(Long userId);

    Optional<CustomerEntity> findByUserEmailIgnoreCase(String email);

    Optional<CustomerEntity> findFirstByPhone(String phone);
}
