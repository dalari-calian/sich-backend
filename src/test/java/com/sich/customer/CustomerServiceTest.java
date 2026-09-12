package com.sich.customer;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sich.common.exception.ResourceNotFoundException;
import com.sich.user.UserEntity;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void create_shouldPersistCustomerLinkedToUser() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerEntity result = customerService.create(user, "John Doe", "11999999999", "12345678900", "São Paulo", "SP", "Rua A", "Apto 1");

        assertThat(result.getUser()).isSameAs(user);
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getPhone()).isEqualTo("11999999999");
        assertThat(result.getCnpjCpf()).isEqualTo("12345678900");
        verify(customerRepository).save(any(CustomerEntity.class));
    }

    @Test
    void findByUser_shouldReturnCustomer_whenFound() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        CustomerEntity customer = new CustomerEntity();
        customer.setUser(user);
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(customer));

        CustomerEntity result = customerService.findByUser(user);

        assertThat(result).isSameAs(customer);
    }

    @Test
    void findByUser_shouldThrow_whenNotFound() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findByUser(user))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cliente não encontrado");
    }
}
