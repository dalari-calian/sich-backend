package com.sich.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sich.category.CategoryEntity;
import com.sich.category.CategoryService;
import com.sich.common.exception.BusinessRuleException;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.product.dto.ProductImageResponse;
import com.sich.product.dto.ProductResponse;
import com.sich.provider.ProviderService;
import com.sich.user.UserService;

/** Consultas de produtos usadas pela agenda, que recebem o prestador por id (não pelo token). */
@ExtendWith(MockitoExtension.class)
class ProductBookingServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ProviderService providerService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ProductService productService;

    private static ProductEntity product(Long id, String name, boolean active) {
        CategoryEntity category = new CategoryEntity();
        category.setId(3L);
        category.setName("Unha");
        ProductEntity product = new ProductEntity();
        product.setId(id);
        product.setCategory(category);
        product.setName(name);
        product.setPrice(new BigDecimal("50.00"));
        product.setDurationMinutes(60);
        product.setActive(active);
        return product;
    }

    @Test
    void findActiveForBooking_shouldReturnProductsInRequestedOrder() {
        ProductEntity gel = product(1L, "Esmaltação em gel", true);
        ProductEntity manicure = product(2L, "Manicure", true);
        when(productRepository.findByIdInAndProviderId(List.of(2L, 1L), 10L)).thenReturn(List.of(gel, manicure));

        List<ProductEntity> result = productService.findActiveForBooking(10L, List.of(2L, 1L));

        assertThat(result).containsExactly(manicure, gel);
    }

    @Test
    void findActiveForBooking_shouldThrow_whenNoProductIsChosen() {
        assertThatThrownBy(() -> productService.findActiveForBooking(10L, List.of()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Escolha ao menos um serviço");
        verify(productRepository, never()).findByIdInAndProviderId(any(), any());
    }

    @Test
    void findActiveForBooking_shouldThrow_whenProductIsRepeated() {
        assertThatThrownBy(() -> productService.findActiveForBooking(10L, List.of(1L, 1L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O mesmo serviço não pode ser escolhido mais de uma vez");
    }

    @Test
    void findActiveForBooking_shouldThrow_whenProductBelongsToAnotherProvider() {
        when(productRepository.findByIdInAndProviderId(List.of(1L, 2L), 10L))
                .thenReturn(List.of(product(1L, "Esmaltação em gel", true)));

        assertThatThrownBy(() -> productService.findActiveForBooking(10L, List.of(1L, 2L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Produto não encontrado");
    }

    @Test
    void findActiveForBooking_shouldThrow_whenProductIsInactive() {
        when(productRepository.findByIdInAndProviderId(List.of(1L, 2L), 10L))
                .thenReturn(List.of(product(1L, "Esmaltação em gel", true), product(2L, "Manicure", false)));

        assertThatThrownBy(() -> productService.findActiveForBooking(10L, List.of(1L, 2L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O serviço Manicure está inativo");
    }

    @Test
    void findActiveByProvider_shouldReturnActiveProductsWithImages() {
        ProductEntity gel = product(1L, "Esmaltação em gel", true);
        ProductImageEntity image = new ProductImageEntity();
        image.setId(7L);
        image.setProduct(gel);
        image.setImage("data:image/png;base64,AQID");
        when(productRepository.findByProviderIdAndActiveTrueOrderByNameAsc(10L)).thenReturn(List.of(gel));
        when(productImageRepository.findByProductIdInOrderByIdAsc(List.of(1L))).thenReturn(List.of(image));

        List<ProductResponse> result = productService.findActiveByProvider(10L);

        assertThat(result).extracting(ProductResponse::id).containsExactly(1L);
        assertThat(result.get(0).images()).extracting(ProductImageResponse::id).containsExactly(7L);
    }

    @Test
    void findActiveByProvider_shouldReturnEmpty_whenProviderHasNoActiveProducts() {
        when(productRepository.findByProviderIdAndActiveTrueOrderByNameAsc(10L)).thenReturn(List.of());

        assertThat(productService.findActiveByProvider(10L)).isEmpty();
        verify(productImageRepository, never()).findByProductIdInOrderByIdAsc(any());
    }
}
