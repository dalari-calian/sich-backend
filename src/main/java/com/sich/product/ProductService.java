package com.sich.product;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.category.CategoryService;
import com.sich.common.exception.BusinessRuleException;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.common.util.DurationFormat;
import com.sich.product.dto.ProductImageResponse;
import com.sich.product.dto.ProductRequest;
import com.sich.product.dto.ProductResponse;
import com.sich.product.dto.ProductStatusRequest;
import com.sich.provider.ProviderEntity;
import com.sich.provider.ProviderService;
import com.sich.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * Produtos/serviços do prestador autenticado. O prestador é sempre resolvido pelo
 * token, nunca pelo corpo da requisição, então cada prestador só enxerga e altera
 * os próprios produtos.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryService categoryService;
    private final ProviderService providerService;
    private final UserService userService;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        ProductEntity product = new ProductEntity();
        product.setProvider(currentProvider());
        apply(product, request);
        return ProductResponse.of(productRepository.save(product), List.of());
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        ProductEntity product = findOwned(id);
        apply(product, request);
        return withImages(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateStatus(Long id, ProductStatusRequest request) {
        ProductEntity product = findOwned(id);
        product.setActive(request.active());
        return withImages(productRepository.save(product));
    }

    /** Exclui o produto; as imagens dele são apagadas em cascata pelo banco. */
    @Transactional
    public void delete(Long id) {
        productRepository.delete(findOwned(id));
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return withImages(findOwned(id));
    }

    /** Lista os produtos com as imagens, buscando as imagens de todos em uma única consulta. */
    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        List<ProductEntity> products = productRepository.findByProviderIdOrderByNameAsc(currentProvider().getId());
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ProductImageResponse>> imagesByProduct = productImageRepository
                .findByProductIdInOrderByIdAsc(products.stream().map(ProductEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(image -> image.getProduct().getId(),
                        Collectors.mapping(ProductImageResponse::of, Collectors.toList())));
        return products.stream()
                .map(product -> ProductResponse.of(product, imagesByProduct.getOrDefault(product.getId(), List.of())))
                .toList();
    }

    private ProductResponse withImages(ProductEntity product) {
        List<ProductImageResponse> images = productImageRepository.findByProductIdOrderByIdAsc(product.getId()).stream()
                .map(ProductImageResponse::of)
                .toList();
        return ProductResponse.of(product, images);
    }

    private void apply(ProductEntity product, ProductRequest request) {
        int durationMinutes = DurationFormat.toMinutes(request.duration());
        if (durationMinutes == 0) {
            throw new BusinessRuleException("O tempo médio do serviço deve ser maior que 00:00");
        }
        product.setCategory(categoryService.findEntityById(request.categoryId()));
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setDurationMinutes(durationMinutes);
    }

    private ProviderEntity currentProvider() {
        return providerService.findByUser(userService.getCurrentlyAuthenticatedUser());
    }

    /** Produto do prestador autenticado; 404 se não existir ou for de outro prestador. */
    @Transactional(readOnly = true)
    public ProductEntity findOwned(Long id) {
        return productRepository.findByIdAndProviderId(id, currentProvider().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
    }
}
