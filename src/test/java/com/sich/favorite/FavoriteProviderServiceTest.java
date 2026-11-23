package com.sich.favorite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sich.common.exception.ResourceNotFoundException;
import com.sich.customer.CustomerEntity;
import com.sich.customer.CustomerService;
import com.sich.provider.ProviderEntity;
import com.sich.provider.ProviderService;
import com.sich.provider.dto.ProviderSummaryResponse;
import com.sich.user.UserEntity;
import com.sich.user.UserService;

@ExtendWith(MockitoExtension.class)
class FavoriteProviderServiceTest {

    @Mock
    private FavoriteProviderRepository favoriteProviderRepository;

    @Mock
    private ProviderService providerService;

    @Mock
    private CustomerService customerService;

    @Mock
    private UserService userService;

    @InjectMocks
    private FavoriteProviderService favoriteProviderService;

    private CustomerEntity customer;
    private ProviderEntity provider;

    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        customer = new CustomerEntity();
        customer.setId(40L);
        provider = new ProviderEntity();
        provider.setId(10L);
        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);
        when(customerService.findByUser(user)).thenReturn(customer);
    }

    private static ProviderSummaryResponse summary(Long id, boolean favorite) {
        return new ProviderSummaryResponse(id, "Ana Beatriz", "São Paulo", "SP", "Jardins",
                new BigDecimal("50.00"), List.of("Manicure"), null, favorite);
    }

    @Test
    void findAll_shouldReturnCardsInFavoriteOrder() {
        when(favoriteProviderRepository.findProviderIdsByCustomerId(40L)).thenReturn(List.of(11L, 10L));
        List<ProviderSummaryResponse> cards = List.of(summary(11L, true), summary(10L, true));
        when(providerService.findSummariesByIds(List.of(11L, 10L))).thenReturn(cards);

        assertThat(favoriteProviderService.findAll()).isEqualTo(cards);
    }

    @Test
    void findAll_shouldReturnEmptyWithoutLoadingCards_whenCustomerHasNoFavorites() {
        when(favoriteProviderRepository.findProviderIdsByCustomerId(40L)).thenReturn(List.of());

        assertThat(favoriteProviderService.findAll()).isEmpty();
        verify(providerService, never()).findSummariesByIds(any());
    }

    @Test
    void add_shouldSaveFavoriteAndReturnCard() {
        when(providerService.findById(10L)).thenReturn(provider);
        when(favoriteProviderRepository.existsByCustomerIdAndProviderId(40L, 10L)).thenReturn(false);
        when(providerService.findSummaryById(10L)).thenReturn(summary(10L, true));

        ProviderSummaryResponse result = favoriteProviderService.add(10L);

        ArgumentCaptor<FavoriteProviderEntity> saved = ArgumentCaptor.forClass(FavoriteProviderEntity.class);
        verify(favoriteProviderRepository).save(saved.capture());
        assertThat(saved.getValue().getCustomer()).isSameAs(customer);
        assertThat(saved.getValue().getProvider()).isSameAs(provider);
        assertThat(result.favorite()).isTrue();
    }

    @Test
    void add_shouldNotDuplicate_whenProviderIsAlreadyFavorite() {
        when(providerService.findById(10L)).thenReturn(provider);
        when(favoriteProviderRepository.existsByCustomerIdAndProviderId(40L, 10L)).thenReturn(true);
        when(providerService.findSummaryById(10L)).thenReturn(summary(10L, true));

        favoriteProviderService.add(10L);

        verify(favoriteProviderRepository, never()).save(any());
    }

    @Test
    void add_shouldThrow_whenProviderDoesNotExist() {
        when(providerService.findById(99L)).thenThrow(new ResourceNotFoundException("Prestador não encontrado"));

        assertThatThrownBy(() -> favoriteProviderService.add(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(favoriteProviderRepository, never()).save(any());
    }

    @Test
    void remove_shouldDeleteFavorite_whenPresent() {
        FavoriteProviderEntity favorite = new FavoriteProviderEntity();
        when(favoriteProviderRepository.findByCustomerIdAndProviderId(40L, 10L)).thenReturn(Optional.of(favorite));

        favoriteProviderService.remove(10L);

        verify(favoriteProviderRepository).delete(favorite);
    }

    @Test
    void remove_shouldDoNothing_whenProviderIsNotFavorite() {
        when(favoriteProviderRepository.findByCustomerIdAndProviderId(40L, 10L)).thenReturn(Optional.empty());

        favoriteProviderService.remove(10L);

        verify(favoriteProviderRepository, never()).delete(any());
    }
}
