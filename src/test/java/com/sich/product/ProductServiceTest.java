package com.sich.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sich.common.exception.BusinessRuleException;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.product.dto.ProductRequest;
import com.sich.product.dto.ProductResponse;
import com.sich.product.dto.ProductTreeResponse;
import com.sich.product.dto.ProductUpdateRequest;
import com.sich.providerproduct.ProviderProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProviderProductRepository providerProductRepository;

    @InjectMocks
    private ProductService productService;

    private static ProductEntity product(Long id, String sku, ProductEntity parent) {
        ProductEntity product = new ProductEntity();
        product.setId(id);
        product.setSku(sku);
        product.setName("Produto " + sku);
        product.setDescription("Descrição " + sku);
        product.setParent(parent);
        return product;
    }

    private void mockSaveReturningArgument() {
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void create_shouldGenerateSkuOne_whenFirstRoot() {
        when(productRepository.findByParentIsNull()).thenReturn(List.of());
        mockSaveReturningArgument();

        ProductResponse result = productService.create(new ProductRequest("Unha", "Serviços de unha", null));

        assertThat(result.sku()).isEqualTo("1");
        assertThat(result.parentId()).isNull();
        assertThat(result.name()).isEqualTo("Unha");
        assertThat(result.description()).isEqualTo("Serviços de unha");
        verify(productRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void create_shouldGenerateNextRootSku_fromHighestExistingRoot() {
        when(productRepository.findByParentIsNull())
                .thenReturn(List.of(product(1L, "1", null), product(2L, "3", null)));
        mockSaveReturningArgument();

        ProductResponse result = productService.create(new ProductRequest("Cabelo", null, null));

        assertThat(result.sku()).isEqualTo("4");
    }

    @Test
    void create_shouldSaveValuesExactlyAsReceived() {
        when(productRepository.findByParentIsNull()).thenReturn(List.of());
        mockSaveReturningArgument();

        ProductResponse result = productService.create(new ProductRequest("  unha GEL ", "  desc ", null));

        assertThat(result.name()).isEqualTo("  unha GEL ");
        assertThat(result.description()).isEqualTo("  desc ");
    }

    @Test
    void create_shouldGenerateFirstChildSku_whenParentHasNoChildren() {
        ProductEntity parent = product(1L, "1", null);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(parent));
        when(productRepository.findByParentId(1L)).thenReturn(List.of());
        mockSaveReturningArgument();

        ProductResponse result = productService.create(new ProductRequest("Gel", null, 1L));

        assertThat(result.sku()).isEqualTo("1.01");
        assertThat(result.parentId()).isEqualTo(1L);
    }

    @Test
    void create_shouldGenerateNextChildSku_fromHighestSibling() {
        ProductEntity root = product(1L, "1", null);
        ProductEntity parent = product(2L, "1.01", root);
        when(productRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(parent));
        when(productRepository.findByParentId(2L))
                .thenReturn(List.of(product(3L, "1.01.01", parent), product(4L, "1.01.03", parent)));
        mockSaveReturningArgument();

        ProductResponse result = productService.create(new ProductRequest("Alongamento", null, 2L));

        assertThat(result.sku()).isEqualTo("1.01.04");
        assertThat(result.parentId()).isEqualTo(2L);
    }

    @Test
    void create_shouldThrow_whenParentNotFound() {
        when(productRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(new ProductRequest("Gel", null, 99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Produto pai não encontrado");
        verify(productRepository, never()).save(any());
    }

    @Test
    void update_shouldChangeNameAndDescriptionKeepingSkuAndParent() {
        ProductEntity root = product(1L, "1", null);
        ProductEntity product = product(2L, "1.01", root);
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        mockSaveReturningArgument();

        ProductResponse result = productService.update(2L, new ProductUpdateRequest("Gel novo", "Nova descrição"));

        assertThat(result.name()).isEqualTo("Gel novo");
        assertThat(result.description()).isEqualTo("Nova descrição");
        assertThat(result.sku()).isEqualTo("1.01");
        assertThat(result.parentId()).isEqualTo(1L);
    }

    @Test
    void update_shouldThrow_whenNotFound() {
        when(productRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(9L, new ProductUpdateRequest("x", null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Produto não encontrado");
    }

    @Test
    void delete_shouldRemove_whenProductHasNoChildren() {
        ProductEntity product = product(1L, "1", null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsByParentId(1L)).thenReturn(false);

        productService.delete(1L);

        verify(productRepository).delete(product);
    }

    @Test
    void delete_shouldThrow_whenProductHasChildren() {
        ProductEntity product = product(1L, "1", null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsByParentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> productService.delete(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Não é possível excluir um produto que possui filhos");
        verify(productRepository, never()).delete(any());
    }

    @Test
    void delete_shouldThrow_whenProductIsProvidedByAnyProvider() {
        ProductEntity product = product(1L, "1", null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsByParentId(1L)).thenReturn(false);
        when(providerProductRepository.existsByProductId(1L)).thenReturn(true);

        assertThatThrownBy(() -> productService.delete(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Não é possível excluir um produto prestado por algum prestador");
        verify(productRepository, never()).delete(any());
    }

    @Test
    void findEntityById_shouldReturnEntity() {
        ProductEntity product = product(1L, "1", null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThat(productService.findEntityById(1L)).isSameAs(product);
    }

    @Test
    void delete_shouldThrow_whenNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).delete(any());
    }

    @Test
    void findById_shouldReturnProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L, "1", null)));

        ProductResponse result = productService.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.sku()).isEqualTo("1");
    }

    @Test
    void findBySku_shouldReturnProduct_whenFound() {
        when(productRepository.findBySku("1.01")).thenReturn(Optional.of(product(2L, "1.01", product(1L, "1", null))));

        ProductResponse result = productService.findBySku("1.01");

        assertThat(result.id()).isEqualTo(2L);
    }

    @Test
    void findBySku_shouldThrow_whenNotFound() {
        when(productRepository.findBySku("9")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findBySku("9"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Produto não encontrado");
    }

    @Test
    void findRoots_shouldReturnRootProducts() {
        when(productRepository.findByParentIsNull())
                .thenReturn(List.of(product(1L, "1", null), product(2L, "2", null)));

        List<ProductResponse> result = productService.findRoots();

        assertThat(result).extracting(ProductResponse::sku).containsExactly("1", "2");
    }

    @Test
    void findRoots_shouldSortSkuNumerically() {
        when(productRepository.findByParentIsNull())
                .thenReturn(List.of(product(10L, "10", null), product(2L, "2", null), product(1L, "1", null)));

        List<ProductResponse> result = productService.findRoots();

        assertThat(result).extracting(ProductResponse::sku).containsExactly("1", "2", "10");
    }

    @Test
    void findTree_shouldBuildCorrectly_whenInputIsUnordered() {
        ProductEntity unha = product(1L, "1", null);
        ProductEntity gel = product(2L, "1.99", unha);
        ProductEntity fibra = product(3L, "1.100", unha);
        ProductEntity alongamento = product(4L, "1.99.01", gel);
        when(productRepository.findAll()).thenReturn(List.of(alongamento, fibra, gel, unha));

        List<ProductTreeResponse> result = productService.findTree();

        assertThat(result).extracting(ProductTreeResponse::sku).containsExactly("1");
        assertThat(result.get(0).children()).extracting(ProductTreeResponse::sku).containsExactly("1.99", "1.100");
        assertThat(result.get(0).children().get(0).children())
                .extracting(ProductTreeResponse::sku).containsExactly("1.99.01");
    }

    @Test
    void findChildren_shouldReturnDirectChildren() {
        ProductEntity root = product(1L, "1", null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(root));
        when(productRepository.findByParentId(1L))
                .thenReturn(List.of(product(2L, "1.01", root), product(3L, "1.02", root)));

        List<ProductResponse> result = productService.findChildren(1L);

        assertThat(result).extracting(ProductResponse::sku).containsExactly("1.01", "1.02");
    }

    @Test
    void findChildren_shouldThrow_whenProductNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findChildren(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).findByParentId(any());
    }

    @Test
    void findParent_shouldReturnParent() {
        ProductEntity root = product(1L, "1", null);
        when(productRepository.findById(2L)).thenReturn(Optional.of(product(2L, "1.01", root)));

        ProductResponse result = productService.findParent(2L);

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void findParent_shouldThrow_whenProductIsRoot() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L, "1", null)));

        assertThatThrownBy(() -> productService.findParent(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Produto raiz não possui pai");
    }

    @Test
    void findAncestors_shouldReturnEmpty_whenProductIsRoot() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L, "1", null)));

        List<ProductResponse> result = productService.findAncestors(1L);

        assertThat(result).isEmpty();
        verify(productRepository, never()).findBySkuIn(anyCollection());
    }

    @Test
    void findAncestors_shouldQueryEverySkuPrefix() {
        ProductEntity root = product(1L, "1", null);
        ProductEntity middle = product(2L, "1.01", root);
        when(productRepository.findById(3L)).thenReturn(Optional.of(product(3L, "1.01.02", middle)));
        when(productRepository.findBySkuIn(List.of("1", "1.01"))).thenReturn(List.of(root, middle));

        List<ProductResponse> result = productService.findAncestors(3L);

        assertThat(result).extracting(ProductResponse::sku).containsExactly("1", "1.01");
    }

    @Test
    void findTree_shouldNestChildrenUnderParents() {
        ProductEntity unha = product(1L, "1", null);
        ProductEntity gel = product(2L, "1.01", unha);
        ProductEntity alongamento = product(3L, "1.01.01", gel);
        ProductEntity cutilagem = product(4L, "1.02", unha);
        ProductEntity cabelo = product(5L, "2", null);
        when(productRepository.findAll())
                .thenReturn(List.of(unha, gel, alongamento, cutilagem, cabelo));

        List<ProductTreeResponse> result = productService.findTree();

        assertThat(result).extracting(ProductTreeResponse::sku).containsExactly("1", "2");
        assertThat(result.get(0).children()).extracting(ProductTreeResponse::sku).containsExactly("1.01", "1.02");
        assertThat(result.get(0).children().get(0).children())
                .extracting(ProductTreeResponse::sku).containsExactly("1.01.01");
        assertThat(result.get(1).children()).isEmpty();
    }

    @Test
    void findSubtree_shouldReturnProductWithAllDescendants() {
        ProductEntity unha = product(1L, "1", null);
        ProductEntity gel = product(2L, "1.01", unha);
        ProductEntity alongamento = product(3L, "1.01.01", gel);
        when(productRepository.findById(2L)).thenReturn(Optional.of(gel));
        when(productRepository.findBySkuStartingWith("1.01.")).thenReturn(List.of(alongamento));

        ProductTreeResponse result = productService.findSubtree(2L);

        assertThat(result.sku()).isEqualTo("1.01");
        assertThat(result.children()).extracting(ProductTreeResponse::sku).containsExactly("1.01.01");
    }

    @Test
    void findSubtree_shouldThrow_whenProductNotFound() {
        when(productRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findSubtree(2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
