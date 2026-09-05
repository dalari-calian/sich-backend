package com.sich.customer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.user.UserEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerEntity create(UserEntity user, String name, String phone, String cnpjCpf) {
        CustomerEntity customer = new CustomerEntity();
        customer.setUser(user);
        customer.setName(name);
        customer.setPhone(phone);
        customer.setCnpjCpf(cnpjCpf);
        return customerRepository.save(customer);
    }
}
