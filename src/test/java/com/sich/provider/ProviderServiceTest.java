package com.sich.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sich.auth.dto.RegisterRequest;
import com.sich.common.enums.UserType;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.user.UserEntity;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

    @Mock
    private ProviderRepository providerRepository;

    @InjectMocks
    private ProviderService providerService;

    @Test
    void create_shouldPersistProviderLinkedToUser() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        RegisterRequest request = new RegisterRequest(
                "John Doe", "john@doe.com", "11999999999", "12345678900", "password123", UserType.PROVIDER,
                "São Paulo", "SP", "Rua A", "Apto 1", "Centro", "01310-100", "123");
        when(providerRepository.save(any(ProviderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProviderEntity result = providerService.create(user, request);

        assertThat(result.getUser()).isSameAs(user);
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getPhone()).isEqualTo("11999999999");
        assertThat(result.getCnpjCpf()).isEqualTo("12345678900");
        assertThat(result.getNeighborhood()).isEqualTo("Centro");
        assertThat(result.getCep()).isEqualTo("01310100");
        assertThat(result.getNumberAdress()).isEqualTo("123");
        verify(providerRepository).save(any(ProviderEntity.class));
    }

    @Test
    void findByUser_shouldReturnProvider_whenFound() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        ProviderEntity provider = new ProviderEntity();
        provider.setUser(user);
        when(providerRepository.findByUserId(1L)).thenReturn(Optional.of(provider));

        ProviderEntity result = providerService.findByUser(user);

        assertThat(result).isSameAs(provider);
    }

    @Test
    void findByUser_shouldThrow_whenNotFound() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        when(providerRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> providerService.findByUser(user))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Prestador não encontrado");
    }
}
