package com.sich.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import com.sich.product.ProductRepository;
import com.sich.product.ProviderCategoryName;
import com.sich.product.ProviderMinPrice;
import com.sich.provider.dto.ProviderSummaryResponse;
import com.sich.user.UserEntity;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private ProductRepository productRepository;

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
    void search_shouldReturnEachProviderWithMinPriceAndCategoriesUsingTwoQueries() {
        when(providerRepository.search("%ana%", true, List.of(0L)))
                .thenReturn(List.of(provider(10L, "Ana Beatriz"), provider(11L, "Ana Clara")));
        when(productRepository.findMinActivePriceByProviderIds(List.of(10L, 11L)))
                .thenReturn(List.of(new ProviderMinPrice(10L, new BigDecimal("95.00"))));
        when(productRepository.findActiveCategoryNamesByProviderIds(List.of(10L, 11L))).thenReturn(List.of(
                new ProviderCategoryName(10L, "Manicure"),
                new ProviderCategoryName(10L, "Pedicure"),
                new ProviderCategoryName(11L, "Manicure")));

        List<ProviderSummaryResponse> result = providerService.search("  ANA ", null);

        assertThat(result).containsExactly(
                new ProviderSummaryResponse(10L, "Ana Beatriz", "São Paulo", "SP", "Jardins",
                        new BigDecimal("95.00"), List.of("Manicure", "Pedicure")),
                new ProviderSummaryResponse(11L, "Ana Clara", "São Paulo", "SP", "Jardins",
                        null, List.of("Manicure")));
    }

    @Test
    void search_shouldFilterByCategories_whenInformed() {
        when(providerRepository.search("%%", false, List.of(3L, 4L))).thenReturn(List.of(provider(10L, "Ana Beatriz")));
        when(productRepository.findMinActivePriceByProviderIds(List.of(10L))).thenReturn(List.of());
        when(productRepository.findActiveCategoryNamesByProviderIds(List.of(10L))).thenReturn(List.of());

        List<ProviderSummaryResponse> result = providerService.search(null, List.of(3L, 4L));

        assertThat(result).extracting(ProviderSummaryResponse::categories).containsExactly(List.of());
    }

    @Test
    void search_shouldReturnEmptyWithoutAggregating_whenNothingFound() {
        when(providerRepository.search("%x%", true, List.of(0L))).thenReturn(List.of());

        assertThat(providerService.search("x", List.of())).isEmpty();
        verify(productRepository, never()).findMinActivePriceByProviderIds(any());
    }

    @Test
    void findSummaryById_shouldReturnCardOfProvider() {
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider(10L, "Ana Beatriz")));
        when(productRepository.findMinActivePriceByProviderIds(List.of(10L)))
                .thenReturn(List.of(new ProviderMinPrice(10L, new BigDecimal("50.00"))));
        when(productRepository.findActiveCategoryNamesByProviderIds(List.of(10L)))
                .thenReturn(List.of(new ProviderCategoryName(10L, "Manicure")));

        ProviderSummaryResponse result = providerService.findSummaryById(10L);

        assertThat(result.minPrice()).isEqualByComparingTo("50.00");
        assertThat(result.categories()).containsExactly("Manicure");
    }
}
