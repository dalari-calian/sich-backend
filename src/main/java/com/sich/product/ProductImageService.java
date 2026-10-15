package com.sich.product;

import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.common.exception.BusinessRuleException;
import com.sich.common.exception.ResourceNotFoundException;
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

    private static final Pattern DATA_URL_PREFIX = Pattern.compile("^data:image/(png|jpeg|webp);base64,");

    private final ProductImageRepository productImageRepository;
    private final ProductService productService;

    @Transactional
    public ProductImageResponse create(Long productId, ProductImageRequest request) {
        ProductEntity product = productService.findOwned(productId);
        if (productImageRepository.countByProductId(product.getId()) >= MAX_IMAGES_PER_PRODUCT) {
            throw new BusinessRuleException("O produto já possui o máximo de " + MAX_IMAGES_PER_PRODUCT + " imagens");
        }
        validate(request.image());
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

    /** Aceita apenas data URL de png/jpeg/webp com base64 válido e no máximo 2 MB decodificados. */
    private void validate(String image) {
        Matcher prefix = DATA_URL_PREFIX.matcher(image);
        if (!prefix.lookingAt()) {
            throw new BusinessRuleException(
                    "A imagem deve estar no formato data:image/(png|jpeg|webp);base64,...");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(image.substring(prefix.end()));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("O conteúdo base64 da imagem é inválido");
        }
        if (bytes.length == 0) {
            throw new BusinessRuleException("A imagem está vazia");
        }
        if (bytes.length > MAX_IMAGE_BYTES) {
            throw new BusinessRuleException("A imagem deve ter no máximo 2 MB");
        }
    }
}
