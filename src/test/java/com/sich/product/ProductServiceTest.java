package com.sich.product;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sich.category.CategoryEntity;
import com.sich.category.CategoryService;
import com.sich.common.exception.BusinessRuleException;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.product.dto.ProductRequest;
import com.sich.product.dto.ProductImageResponse;
import com.sich.product.dto.ProductResponse;
import com.sich.product.dto.ProductStatusRequest;
import com.sich.provider.ProviderEntity;
import com.sich.provider.ProviderService;
import com.sich.user.UserEntity;
import com.sich.user.UserService;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

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

    private ProviderEntity provider;
    private CategoryEntity category;

    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        provider = new ProviderEntity();
        provider.setId(10L);
        provider.setUser(user);
        category = new CategoryEntity();
        category.setId(3L);
        category.setName("Unha");
        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);
        when(providerService.findByUser(user)).thenReturn(provider);
    }

    private ProductEntity product(Long id) {
        ProductEntity product = new ProductEntity();
        product.setId(id);
        product.setProvider(provider);
        product.setCategory(category);
        product.setName("Alongamento em gel");
        product.setDescription("Aplicação do gel");
        product.setPrice(new BigDecimal("120.00"));
        product.setDurationMinutes(90);
        return product;
    }

    private static final String IMAGE = "data:image/png;base64,AQID";

    private static ProductImageEntity image(Long id, ProductEntity product) {
        ProductImageEntity image = new ProductImageEntity();
        image.setId(id);
        image.setProduct(product);
        image.setImage(IMAGE);
        return image;
    }

    private static ProductRequest request(String duration) {
        return new ProductRequest(3L, "Alongamento em gel", "Aplicação do gel", new BigDecimal("120.00"), duration);
    }

    @Test
    void create_shouldPersistProductOfAuthenticatedProvider() {
        when(categoryService.findEntityById(3L)).thenReturn(category);
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse result = productService.create(request("01:30"));

        assertThat(result.categoryId()).isEqualTo(3L);
        assertThat(result.categoryName()).isEqualTo("Unha");
        assertThat(result.name()).isEqualTo("Alongamento em gel");
        assertThat(result.description()).isEqualTo("Aplicação do gel");
        assertThat(result.price()).isEqualByComparingTo("120.00");
        assertThat(result.durationMinutes()).isEqualTo(90);
        assertThat(result.duration()).isEqualTo("01:30");
        assertThat(result.active()).isTrue();
        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    void updateStatus_shouldInactivateAndReactivateOwnedProduct() {
        ProductEntity product = product(1L);
        when(productRepository.findByIdAndProviderId(1L, 10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(productService.updateStatus(1L, new ProductStatusRequest(false)).active()).isFalse();
        assertThat(productService.updateStatus(1L, new ProductStatusRequest(true)).active()).isTrue();
    }

    @Test
    void updateStatus_shouldThrow_whenProductNotOwnedByProvider() {
        when(productRepository.findByIdAndProviderId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateStatus(1L, new ProductStatusRequest(false)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void findOwned_shouldReturnEntityOfAuthenticatedProvider() {
        ProductEntity product = product(1L);
        when(productRepository.findByIdAndProviderId(1L, 10L)).thenReturn(Optional.of(product));

        assertThat(productService.findOwned(1L)).isSameAs(product);
    }

    @Test
    void create_shouldAcceptMaximumDuration() {
        when(categoryService.findEntityById(3L)).thenReturn(category);
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse result = productService.create(request("99:59"));

        assertThat(result.durationMinutes()).isEqualTo(5999);
        assertThat(result.duration()).isEqualTo("99:59");
    }

    @Test
    void create_shouldThrow_whenDurationIsZero() {
        assertThatThrownBy(() -> productService.create(request("00:00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O tempo médio do serviço deve ser maior que 00:00");
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_shouldThrow_whenCategoryNotFound() {
        when(categoryService.findEntityById(3L)).thenThrow(new ResourceNotFoundException("Categoria não encontrada"));

        assertThatThrownBy(() -> productService.create(request("01:00")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Categoria não encontrada");
        verify(productRepository, never()).save(any());
    }

    @Test
    void update_shouldChangeAllFieldsOfOwnedProduct() {
        CategoryEntity otherCategory = new CategoryEntity();
        otherCategory.setId(4L);
        otherCategory.setName("Cabelo");
        when(productRepository.findByIdAndProviderId(1L, 10L)).thenReturn(Optional.of(product(1L)));
        when(categoryService.findEntityById(4L)).thenReturn(otherCategory);
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse result = productService.update(1L,
                new ProductRequest(4L, "Corte", "Corte feminino", new BigDecimal("75.50"), "0:45"));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.categoryName()).isEqualTo("Cabelo");
        assertThat(result.name()).isEqualTo("Corte");
        assertThat(result.price()).isEqualByComparingTo("75.50");
        assertThat(result.duration()).isEqualTo("00:45");
    }

    @Test
    void update_shouldThrow_whenProductNotOwnedByProvider() {
        when(productRepository.findByIdAndProviderId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(1L, request("01:00")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Produto não encontrado");
        verify(productRepository, never()).save(any());
    }

    @Test
    void delete_shouldRemoveOwnedProduct() {
        ProductEntity product = product(1L);
        when(productRepository.findByIdAndProviderId(1L, 10L)).thenReturn(Optional.of(product));

        productService.delete(1L);

        verify(productRepository).delete(product);
    }

    @Test
    void delete_shouldThrow_whenProductNotOwnedByProvider() {
        when(productRepository.findByIdAndProviderId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).delete(any());
    }

    @Test
    void findById_shouldReturnOwnedProductWithImages() {
        ProductEntity product = product(1L);
        when(productRepository.findByIdAndProviderId(1L, 10L)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByIdAsc(1L))
                .thenReturn(List.of(image(7L, product), image(8L, product)));

        ProductResponse result = productService.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.duration()).isEqualTo("01:30");
        assertThat(result.images()).extracting(ProductImageResponse::id).containsExactly(7L, 8L);
        assertThat(result.images().get(0).image()).isEqualTo(IMAGE);
    }

    @Test
    void update_shouldReturnImagesOfProduct() {
        ProductEntity product = product(1L);
        when(productRepository.findByIdAndProviderId(1L, 10L)).thenReturn(Optional.of(product));
        when(categoryService.findEntityById(3L)).thenReturn(category);
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productImageRepository.findByProductIdOrderByIdAsc(1L)).thenReturn(List.of(image(7L, product)));

        ProductResponse result = productService.update(1L, request("01:00"));

        assertThat(result.images()).extracting(ProductImageResponse::id).containsExactly(7L);
    }

    @Test
    void updateStatus_shouldReturnImagesOfProduct() {
        ProductEntity product = product(1L);
        when(productRepository.findByIdAndProviderId(1L, 10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productImageRepository.findByProductIdOrderByIdAsc(1L)).thenReturn(List.of(image(7L, product)));

        ProductResponse result = productService.updateStatus(1L, new ProductStatusRequest(false));

        assertThat(result.images()).extracting(ProductImageResponse::id).containsExactly(7L);
    }

    @Test
    void create_shouldReturnEmptyImagesWithoutQueryingThem() {
        when(categoryService.findEntityById(3L)).thenReturn(category);
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse result = productService.create(request("01:00"));

        assertThat(result.images()).isEmpty();
        verify(productImageRepository, never()).findByProductIdOrderByIdAsc(any());
    }

    @Test
    void findAll_shouldReturnEachProductWithItsOwnImagesUsingOneQuery() {
        ProductEntity first = product(1L);
        ProductEntity second = product(2L);
        ProductEntity third = product(3L);
        when(productRepository.findByProviderIdOrderByNameAsc(10L)).thenReturn(List.of(first, second, third));
        when(productImageRepository.findByProductIdInOrderByIdAsc(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(image(7L, first), image(8L, second), image(9L, first)));

        List<ProductResponse> result = productService.findAll();

        assertThat(result).extracting(ProductResponse::id).containsExactly(1L, 2L, 3L);
        assertThat(result.get(0).images()).extracting(ProductImageResponse::id).containsExactly(7L, 9L);
        assertThat(result.get(1).images()).extracting(ProductImageResponse::id).containsExactly(8L);
        assertThat(result.get(2).images()).isEmpty();
        verify(productImageRepository, never()).findByProductIdOrderByIdAsc(any());
    }

    @Test
    void findAll_shouldReturnEmptyWithoutQueryingImages_whenProviderHasNoProducts() {
        when(productRepository.findByProviderIdOrderByNameAsc(10L)).thenReturn(List.of());

        assertThat(productService.findAll()).isEmpty();
        verify(productImageRepository, never()).findByProductIdInOrderByIdAsc(any());
    }
}
