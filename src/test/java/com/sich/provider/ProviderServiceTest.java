package com.sich.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sich.auth.dto.RegisterRequest;
import com.sich.common.enums.UserType;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.provider.dto.ProviderSummaryResponse;
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

    private static ProviderEntity provider(Long id, String name) {
        ProviderEntity provider = new ProviderEntity();
        provider.setId(id);
        provider.setName(name);
        provider.setCity("São Paulo");
        provider.setState("SP");
        provider.setNeighborhood("Jardins");
        return provider;
    }

    @Test
    void findById_shouldReturnProvider_whenFound() {
        ProviderEntity provider = provider(10L, "Ana Beatriz");
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));

        assertThat(providerService.findById(10L)).isSameAs(provider);
    }

    @Test
    void findById_shouldThrow_whenNotFound() {
        when(providerRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> providerService.findById(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Prestador não encontrado");
    }

    @Test
    void lockById_shouldReturnLockedProvider() {
        ProviderEntity provider = provider(10L, "Ana Beatriz");
        when(providerRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(provider));

        assertThat(providerService.lockById(10L)).isSameAs(provider);
    }

    @Test
    void lockById_shouldThrow_whenNotFound() {
        when(providerRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> providerService.lockById(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void search_shouldFilterOnlyByName_whenNoCategory() {
        when(providerRepository.findByNameContainingIgnoreCaseOrderByNameAsc("ana"))
                .thenReturn(List.of(provider(10L, "Ana Beatriz")));

        List<ProviderSummaryResponse> result = providerService.search("  ana ", null);

        assertThat(result).containsExactly(new ProviderSummaryResponse(10L, "Ana Beatriz", "São Paulo", "SP", "Jardins"));
    }

    @Test
    void search_shouldListAll_whenNameIsMissing() {
        when(providerRepository.findByNameContainingIgnoreCaseOrderByNameAsc("")).thenReturn(List.of());

        assertThat(providerService.search(null, null)).isEmpty();
    }

    @Test
    void search_shouldFilterByCategory_whenInformed() {
        when(providerRepository.searchByNameAndCategory("", 3L)).thenReturn(List.of(provider(10L, "Ana Beatriz")));

        List<ProviderSummaryResponse> result = providerService.search(null, 3L);

        assertThat(result).extracting(ProviderSummaryResponse::id).containsExactly(10L);
        verify(providerRepository, never()).findByNameContainingIgnoreCaseOrderByNameAsc(any());
    }
}
