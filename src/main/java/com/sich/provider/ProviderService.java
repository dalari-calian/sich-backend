package com.sich.provider;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.auth.dto.RegisterRequest;
import com.sich.common.enums.UserType;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.favorite.FavoriteProviderRepository;
import com.sich.product.ProductRepository;
import com.sich.product.ProviderCategoryName;
import com.sich.product.ProviderMinPrice;
import com.sich.profile.UserPhotoService;
import com.sich.provider.dto.ProviderSummaryResponse;
import com.sich.user.UserEntity;
import com.sich.user.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProviderService {

    private final ProviderRepository providerRepository;
    private final ProductRepository productRepository;
    private final UserPhotoService userPhotoService;
    private final FavoriteProviderRepository favoriteProviderRepository;
    private final UserService userService;

    @Transactional
    public ProviderEntity create(UserEntity user, RegisterRequest request) {
        ProviderEntity provider = new ProviderEntity();
        provider.setUser(user);
        provider.setName(request.name());
        provider.setPhone(request.phone());
        provider.setCnpjCpf(request.cnpjCpf());
        provider.setCity(request.city());
        provider.setState(request.state());
        provider.setStreet(request.street());
        provider.setComplementAdress(request.complementAdress());
        provider.setNeighborhood(request.neighborhood());
        provider.setCep(request.cep());
        provider.setNumberAdress(request.numberAdress());
        return providerRepository.save(provider);
    }

    @Transactional(readOnly = true)
    public ProviderEntity findByUser(UserEntity user) {
        return providerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Prestador não encontrado"));
    }

    @Transactional(readOnly = true)
    public ProviderEntity findById(Long id) {
        return providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prestador não encontrado"));
    }

    /** Trava o prestador até o fim da transação corrente (deve ser chamado dentro de uma transação de escrita). */
    @Transactional
    public ProviderEntity lockById(Long id) {
        return providerRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prestador não encontrado"));
    }

    /**
     * Busca prestadores com produto ativo pelo nome do prestador ou de um produto
     * (parcial, sem diferenciar maiúsculas) e, opcionalmente, por categorias. O menor
     * preço, as categorias, as fotos e os favoritos de todos os encontrados vêm em uma
     * consulta cada, nunca uma por prestador.
     */
    @Transactional(readOnly = true)
    public List<ProviderSummaryResponse> search(String text, List<Long> categoryIds) {
        String term = "%" + (text == null ? "" : text.trim().toLowerCase()) + "%";
        boolean anyCategory = categoryIds == null || categoryIds.isEmpty();
        return toSummaries(providerRepository.search(term, anyCategory, anyCategory ? List.of(0L) : categoryIds));
    }

    @Transactional(readOnly = true)
    public ProviderSummaryResponse findSummaryById(Long id) {
        return toSummaries(List.of(findById(id))).get(0);
    }

    /** Cards dos prestadores na mesma ordem dos ids; ids inexistentes são ignorados. */
    @Transactional(readOnly = true)
    public List<ProviderSummaryResponse> findSummariesByIds(List<Long> ids) {
        Map<Long, ProviderEntity> byId = providerRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ProviderEntity::getId, provider -> provider));
        return toSummaries(ids.stream().filter(byId::containsKey).map(byId::get).toList());
    }

    private List<ProviderSummaryResponse> toSummaries(List<ProviderEntity> providers) {
        if (providers.isEmpty()) {
            return List.of();
        }
        List<Long> ids = providers.stream().map(ProviderEntity::getId).toList();
        Map<Long, BigDecimal> minPrices = productRepository.findMinActivePriceByProviderIds(ids).stream()
                .collect(Collectors.toMap(ProviderMinPrice::providerId, ProviderMinPrice::minPrice));
        Map<Long, List<String>> categories = productRepository.findActiveCategoryNamesByProviderIds(ids).stream()
                .collect(Collectors.groupingBy(ProviderCategoryName::providerId,
                        Collectors.mapping(ProviderCategoryName::categoryName, Collectors.toList())));
        Map<Long, String> photos = userPhotoService.findByProviderIds(ids);
        Set<Long> favorites = favoriteIdsOfCurrentCustomer(ids);
        return providers.stream()
                .map(provider -> ProviderSummaryResponse.of(provider, minPrices.get(provider.getId()),
                        categories.getOrDefault(provider.getId(), List.of()), photos.get(provider.getId()),
                        favorites.contains(provider.getId())))
                .toList();
    }

    /** Quais dos prestadores o cliente autenticado favoritou; vazio para quem não é cliente. */
    private Set<Long> favoriteIdsOfCurrentCustomer(List<Long> providerIds) {
        UserEntity user = userService.getCurrentlyAuthenticatedUser();
        if (user.getUserType() != UserType.CUSTOMER) {
            return Set.of();
        }
        return new HashSet<>(favoriteProviderRepository.findFavoriteProviderIds(user.getId(), providerIds));
    }
}
