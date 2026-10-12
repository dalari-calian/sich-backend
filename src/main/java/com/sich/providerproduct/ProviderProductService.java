package com.sich.providerproduct;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.common.exception.ResourceAlreadyExistsException;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.product.ProductEntity;
import com.sich.product.ProductService;
import com.sich.provider.ProviderEntity;
import com.sich.provider.ProviderService;
import com.sich.providerproduct.dto.ProviderProductRequest;
import com.sich.providerproduct.dto.ProviderProductResponse;
import com.sich.providerproduct.dto.ProviderProductUpdateRequest;
import com.sich.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * Produtos que o prestador autenticado presta. O prestador é sempre resolvido pelo
 * token, nunca pelo corpo da requisição, então cada prestador só enxerga e altera
 * os próprios registros.
 */
@Service
@RequiredArgsConstructor
public class ProviderProductService {

    private final ProviderProductRepository providerProductRepository;
    private final ProductService productService;
    private final ProviderService providerService;
    private final UserService userService;

    @Transactional
    public ProviderProductResponse create(ProviderProductRequest request) {
        ProviderEntity provider = currentProvider();
        ProductEntity product = productService.findEntityById(request.productId());
        if (providerProductRepository.existsByProviderIdAndProductId(provider.getId(), product.getId())) {
            throw new ResourceAlreadyExistsException("Você já presta este produto");
        }
        ProviderProductEntity providerProduct = new ProviderProductEntity();
        providerProduct.setProvider(provider);
        providerProduct.setProduct(product);
        providerProduct.setDescription(request.description());
        providerProduct.setPrice(request.price());
        return ProviderProductResponse.of(providerProductRepository.save(providerProduct));
    }

    @Transactional
    public ProviderProductResponse update(Long id, ProviderProductUpdateRequest request) {
        ProviderProductEntity providerProduct = findOwned(id);
        providerProduct.setDescription(request.description());
        providerProduct.setPrice(request.price());
        return ProviderProductResponse.of(providerProductRepository.save(providerProduct));
    }

    @Transactional
    public void delete(Long id) {
        providerProductRepository.delete(findOwned(id));
    }

    @Transactional(readOnly = true)
    public ProviderProductResponse findById(Long id) {
        return ProviderProductResponse.of(findOwned(id));
    }

    @Transactional(readOnly = true)
    public List<ProviderProductResponse> findAll() {
        return providerProductRepository.findByProviderIdOrderByIdAsc(currentProvider().getId()).stream()
                .map(ProviderProductResponse::of)
                .toList();
    }

    private ProviderEntity currentProvider() {
        return providerService.findByUser(userService.getCurrentlyAuthenticatedUser());
    }

    private ProviderProductEntity findOwned(Long id) {
        return providerProductRepository.findByIdAndProviderId(id, currentProvider().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto do prestador não encontrado"));
    }
}
