package com.sich.customer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.common.exception.ResourceNotFoundException;
import com.sich.user.UserEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerEntity create(UserEntity user, String name, String phone, String cnpjCpf, String city, String state, String street, String complementAdress) {
        CustomerEntity customer = new CustomerEntity();
        customer.setUser(user);
        customer.setName(name);
        customer.setPhone(phone);
        customer.setCnpjCpf(cnpjCpf);
        customer.setCity(city);
        customer.setState(state);
        customer.setStreet(street);
        customer.setComplementAdress(complementAdress);
        return customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public CustomerEntity findByUser(UserEntity user) {
        return customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
    }
}
