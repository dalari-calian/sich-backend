package com.sich.favorite;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.customer.CustomerEntity;
import com.sich.customer.CustomerService;
import com.sich.provider.ProviderEntity;
import com.sich.provider.ProviderService;
import com.sich.provider.dto.ProviderSummaryResponse;
import com.sich.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * Prestadores favoritos do cliente autenticado. Favoritar e desfavoritar são
 * idempotentes: repetir a chamada não dá erro nem duplica o registro.
 */
@Service
@RequiredArgsConstructor
public class FavoriteProviderService {

    private final FavoriteProviderRepository favoriteProviderRepository;
    private final ProviderService providerService;
    private final CustomerService customerService;
    private final UserService userService;

    /** Cards dos favoritos, do favoritado mais recentemente para o mais antigo (consultas em lote). */
    @Transactional(readOnly = true)
    public List<ProviderSummaryResponse> findAll() {
        List<Long> providerIds = favoriteProviderRepository.findProviderIdsByCustomerId(currentCustomer().getId());
        if (providerIds.isEmpty()) {
            return List.of();
        }
        return providerService.findSummariesByIds(providerIds);
    }

    /** Favorita o prestador (404 se ele não existir) e devolve o card dele já com {@code favorite = true}. */
    @Transactional
    public ProviderSummaryResponse add(Long providerId) {
        CustomerEntity customer = currentCustomer();
        ProviderEntity provider = providerService.findById(providerId);
        if (!favoriteProviderRepository.existsByCustomerIdAndProviderId(customer.getId(), provider.getId())) {
            FavoriteProviderEntity favorite = new FavoriteProviderEntity();
            favorite.setCustomer(customer);
            favorite.setProvider(provider);
            favoriteProviderRepository.save(favorite);
        }
        return providerService.findSummaryById(provider.getId());
    }

    /** Remove o prestador dos favoritos; se ele não era favorito, não faz nada. */
    @Transactional
    public void remove(Long providerId) {
        favoriteProviderRepository.findByCustomerIdAndProviderId(currentCustomer().getId(), providerId)
                .ifPresent(favoriteProviderRepository::delete);
    }

    private CustomerEntity currentCustomer() {
        return customerService.findByUser(userService.getCurrentlyAuthenticatedUser());
    }
}
