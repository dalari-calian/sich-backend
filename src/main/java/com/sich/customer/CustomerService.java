package com.sich.customer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.auth.dto.RegisterRequest;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.user.UserEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerEntity create(UserEntity user, RegisterRequest request) {
        CustomerEntity customer = new CustomerEntity();
        customer.setUser(user);
        customer.setName(request.name());
        customer.setPhone(request.phone());
        customer.setCnpjCpf(request.cnpjCpf());
        customer.setCity(request.city());
        customer.setState(request.state());
        customer.setStreet(request.street());
        customer.setComplementAdress(request.complementAdress());
        customer.setNeighborhood(request.neighborhood());
        customer.setCep(request.cep());
        customer.setNumberAdress(request.numberAdress());
        return customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public CustomerEntity findByUser(UserEntity user) {
        return customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
    }
}
