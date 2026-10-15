package com.sich.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sich.common.exception.BusinessRuleException;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.product.dto.ProductImageRequest;
import com.sich.product.dto.ProductImageResponse;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    private static final String PNG = "data:image/png;base64,"
            + Base64.getEncoder().encodeToString(new byte[] { 1, 2, 3 });

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductImageService productImageService;

    private ProductEntity product;

    @BeforeEach
    void setUp() {
        product = new ProductEntity();
        product.setId(1L);
    }

    private ProductImageEntity image(Long id) {
        ProductImageEntity image = new ProductImageEntity();
        image.setId(id);
        image.setProduct(product);
        image.setImage(PNG);
        return image;
    }

    private void mockOwnedProductWithImages(long count) {
        when(productService.findOwned(1L)).thenReturn(product);
        when(productImageRepository.countByProductId(1L)).thenReturn(count);
    }

    @Test
    void create_shouldSaveImageAsReceived() {
        mockOwnedProductWithImages(4);
        when(productImageRepository.save(any(ProductImageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductImageResponse result = productImageService.create(1L, new ProductImageRequest(PNG));

        assertThat(result.image()).isEqualTo(PNG);
        verify(productImageRepository).save(any(ProductImageEntity.class));
    }

    @Test
    void create_shouldAcceptJpegAndWebp() {
        mockOwnedProductWithImages(0);
        when(productImageRepository.save(any(ProductImageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        String payload = Base64.getEncoder().encodeToString(new byte[] { 9 });

        assertThat(productImageService.create(1L, new ProductImageRequest("data:image/jpeg;base64," + payload)))
                .isNotNull();
        assertThat(productImageService.create(1L, new ProductImageRequest("data:image/webp;base64," + payload)))
                .isNotNull();
    }

    @Test
    void create_shouldThrow_whenProductAlreadyHasFiveImages() {
        mockOwnedProductWithImages(5);

        assertThatThrownBy(() -> productImageService.create(1L, new ProductImageRequest(PNG)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O produto já possui o máximo de 5 imagens");
        verify(productImageRepository, never()).save(any());
    }

    @Test
    void create_shouldThrow_whenNotDataUrlOfAcceptedType() {
        mockOwnedProductWithImages(0);

        assertThatThrownBy(() -> productImageService.create(1L, new ProductImageRequest("data:image/gif;base64,AQID")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("A imagem deve estar no formato data:image/(png|jpeg|webp);base64,...");
        assertThatThrownBy(() -> productImageService.create(1L, new ProductImageRequest("AQID")))
                .isInstanceOf(BusinessRuleException.class);
        verify(productImageRepository, never()).save(any());
    }

    @Test
    void create_shouldThrow_whenBase64IsInvalid() {
        mockOwnedProductWithImages(0);

        assertThatThrownBy(() -> productImageService.create(1L, new ProductImageRequest("data:image/png;base64,@@@")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O conteúdo base64 da imagem é inválido");
    }

    @Test
    void create_shouldThrow_whenImageIsEmpty() {
        mockOwnedProductWithImages(0);

        assertThatThrownBy(() -> productImageService.create(1L, new ProductImageRequest("data:image/png;base64,")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("A imagem está vazia");
    }

    @Test
    void create_shouldThrow_whenImageExceedsTwoMegabytes() {
        mockOwnedProductWithImages(0);
        String tooBig = "data:image/png;base64,"
                + Base64.getEncoder().encodeToString(new byte[ProductImageService.MAX_IMAGE_BYTES + 1]);

        assertThatThrownBy(() -> productImageService.create(1L, new ProductImageRequest(tooBig)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("A imagem deve ter no máximo 2 MB");
    }

    @Test
    void create_shouldThrow_whenProductNotOwned() {
        when(productService.findOwned(1L)).thenThrow(new ResourceNotFoundException("Produto não encontrado"));

        assertThatThrownBy(() -> productImageService.create(1L, new ProductImageRequest(PNG)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productImageRepository, never()).save(any());
    }

    @Test
    void delete_shouldRemoveImageOfOwnedProduct() {
        ProductImageEntity image = image(7L);
        when(productService.findOwned(1L)).thenReturn(product);
        when(productImageRepository.findByIdAndProductId(7L, 1L)).thenReturn(Optional.of(image));

        productImageService.delete(1L, 7L);

        verify(productImageRepository).delete(image);
    }

    @Test
    void delete_shouldThrow_whenImageNotInProduct() {
        when(productService.findOwned(1L)).thenReturn(product);
        when(productImageRepository.findByIdAndProductId(7L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productImageService.delete(1L, 7L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Imagem não encontrada");
        verify(productImageRepository, never()).delete(any());
    }

    @Test
    void findAll_shouldReturnImagesOfOwnedProduct() {
        when(productService.findOwned(1L)).thenReturn(product);
        when(productImageRepository.findByProductIdOrderByIdAsc(1L)).thenReturn(List.of(image(7L), image(8L)));

        List<ProductImageResponse> result = productImageService.findAll(1L);

        assertThat(result).extracting(ProductImageResponse::id).containsExactly(7L, 8L);
    }
}
