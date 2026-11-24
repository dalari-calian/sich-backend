package com.sich.product;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.common.exception.BusinessRuleException;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.common.util.ImageDataUrl;
import com.sich.product.dto.ProductImageRequest;
import com.sich.product.dto.ProductImageResponse;

import lombok.RequiredArgsConstructor;

/**
 * Fotos dos produtos do prestador autenticado. A posse do produto é validada pelo
 * {@link ProductService#findOwned(Long)}, então um prestador só mexe nas fotos dos
 * próprios produtos.
 */
@Service
@RequiredArgsConstructor
public class ProductImageService {

    static final int MAX_IMAGES_PER_PRODUCT = 5;
    static final int MAX_IMAGE_BYTES = 2 * 1024 * 1024;

    private final ProductImageRepository productImageRepository;
    private final ProductService productService;

    @Transactional
    public ProductImageResponse create(Long productId, ProductImageRequest request) {
        ProductEntity product = productService.findOwned(productId);
        if (productImageRepository.countByProductId(product.getId()) >= MAX_IMAGES_PER_PRODUCT) {
            throw new BusinessRuleException("O produto já possui o máximo de " + MAX_IMAGES_PER_PRODUCT + " imagens");
        }
        ImageDataUrl.validate(request.image(), MAX_IMAGE_BYTES);
        ProductImageEntity image = new ProductImageEntity();
        image.setProduct(product);
        image.setImage(request.image());
        return ProductImageResponse.of(productImageRepository.save(image));
    }

    @Transactional
    public void delete(Long productId, Long imageId) {
        ProductEntity product = productService.findOwned(productId);
        ProductImageEntity image = productImageRepository.findByIdAndProductId(imageId, product.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Imagem não encontrada"));
        productImageRepository.delete(image);
    }

    @Transactional(readOnly = true)
    public List<ProductImageResponse> findAll(Long productId) {
        ProductEntity product = productService.findOwned(productId);
        return productImageRepository.findByProductIdOrderByIdAsc(product.getId()).stream()
                .map(ProductImageResponse::of)
                .toList();
    }
}
