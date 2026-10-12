package com.sich.providerproduct;

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

import com.sich.common.exception.ResourceAlreadyExistsException;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.product.ProductEntity;
import com.sich.product.ProductService;
import com.sich.provider.ProviderEntity;
import com.sich.provider.ProviderService;
import com.sich.providerproduct.dto.ProviderProductRequest;
import com.sich.providerproduct.dto.ProviderProductResponse;
import com.sich.providerproduct.dto.ProviderProductUpdateRequest;
import com.sich.user.UserEntity;
import com.sich.user.UserService;

@ExtendWith(MockitoExtension.class)
class ProviderProductServiceTest {

    @Mock
    private ProviderProductRepository providerProductRepository;

    @Mock
    private ProductService productService;

    @Mock
    private ProviderService providerService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ProviderProductService providerProductService;

    private ProviderEntity provider;
    private ProductEntity product;

    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        provider = new ProviderEntity();
        provider.setId(10L);
        provider.setUser(user);
        product = new ProductEntity();
        product.setId(5L);
        product.setName("Unha");
        product.setSku("1");
        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);
        when(providerService.findByUser(user)).thenReturn(provider);
    }

    private ProviderProductEntity providerProduct(Long id) {
        ProviderProductEntity providerProduct = new ProviderProductEntity();
        providerProduct.setId(id);
        providerProduct.setProvider(provider);
        providerProduct.setProduct(product);
        providerProduct.setDescription("Cutilagem e esmaltação");
        providerProduct.setPrice(new BigDecimal("50.00"));
        return providerProduct;
    }

    @Test
    void create_shouldLinkProductToAuthenticatedProvider() {
        when(productService.findEntityById(5L)).thenReturn(product);
        when(providerProductRepository.existsByProviderIdAndProductId(10L, 5L)).thenReturn(false);
        when(providerProductRepository.save(any(ProviderProductEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProviderProductResponse result = providerProductService.create(
                new ProviderProductRequest(5L, "Cutilagem, lixamento e esmaltação", new BigDecimal("80.00")));

        assertThat(result.productId()).isEqualTo(5L);
        assertThat(result.productName()).isEqualTo("Unha");
        assertThat(result.productSku()).isEqualTo("1");
        assertThat(result.description()).isEqualTo("Cutilagem, lixamento e esmaltação");
        assertThat(result.price()).isEqualByComparingTo("80.00");
        verify(providerProductRepository).save(any(ProviderProductEntity.class));
    }

    @Test
    void create_shouldThrow_whenProviderAlreadyProvidesProduct() {
        when(productService.findEntityById(5L)).thenReturn(product);
        when(providerProductRepository.existsByProviderIdAndProductId(10L, 5L)).thenReturn(true);

        assertThatThrownBy(() -> providerProductService.create(
                new ProviderProductRequest(5L, null, new BigDecimal("80.00"))))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage("Você já presta este produto");
        verify(providerProductRepository, never()).save(any());
    }

    @Test
    void create_shouldThrow_whenProductNotFound() {
        when(productService.findEntityById(99L)).thenThrow(new ResourceNotFoundException("Produto não encontrado"));

        assertThatThrownBy(() -> providerProductService.create(
                new ProviderProductRequest(99L, null, new BigDecimal("80.00"))))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(providerProductRepository, never()).save(any());
    }

    @Test
    void update_shouldChangeDescriptionAndPrice() {
        when(providerProductRepository.findByIdAndProviderId(1L, 10L)).thenReturn(Optional.of(providerProduct(1L)));
        when(providerProductRepository.save(any(ProviderProductEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProviderProductResponse result = providerProductService.update(
                1L, new ProviderProductUpdateRequest("Nova descrição", new BigDecimal("95.50")));

        assertThat(result.description()).isEqualTo("Nova descrição");
        assertThat(result.price()).isEqualByComparingTo("95.50");
        assertThat(result.productId()).isEqualTo(5L);
    }

    @Test
    void update_shouldThrow_whenNotOwnedByProvider() {
        when(providerProductRepository.findByIdAndProviderId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> providerProductService.update(
                1L, new ProviderProductUpdateRequest(null, BigDecimal.TEN)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Produto do prestador não encontrado");
        verify(providerProductRepository, never()).save(any());
    }

    @Test
    void delete_shouldRemoveOwnedProduct() {
        ProviderProductEntity providerProduct = providerProduct(1L);
        when(providerProductRepository.findByIdAndProviderId(1L, 10L)).thenReturn(Optional.of(providerProduct));

        providerProductService.delete(1L);

        verify(providerProductRepository).delete(providerProduct);
    }

    @Test
    void delete_shouldThrow_whenNotOwnedByProvider() {
        when(providerProductRepository.findByIdAndProviderId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> providerProductService.delete(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(providerProductRepository, never()).delete(any());
    }

    @Test
    void findById_shouldReturnOwnedProduct() {
        when(providerProductRepository.findByIdAndProviderId(1L, 10L)).thenReturn(Optional.of(providerProduct(1L)));

        ProviderProductResponse result = providerProductService.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.description()).isEqualTo("Cutilagem e esmaltação");
    }

    @Test
    void findAll_shouldReturnProductsOfAuthenticatedProvider() {
        when(providerProductRepository.findByProviderIdOrderByIdAsc(10L))
                .thenReturn(List.of(providerProduct(1L), providerProduct(2L)));

        List<ProviderProductResponse> result = providerProductService.findAll();

        assertThat(result).extracting(ProviderProductResponse::id).containsExactly(1L, 2L);
    }
}
