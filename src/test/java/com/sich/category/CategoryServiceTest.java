package com.sich.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.sich.category.dto.CategoryRequest;
import com.sich.category.dto.CategoryResponse;
import com.sich.common.exception.BusinessRuleException;
import com.sich.common.exception.ResourceAlreadyExistsException;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.product.ProductRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    private static CategoryEntity category(Long id, String name) {
        CategoryEntity category = new CategoryEntity();
        category.setId(id);
        category.setName(name);
        category.setDescription("Descrição " + name);
        return category;
    }

    @Test
    void create_shouldPersistCategoryWithValuesAsReceived() {
        when(categoryRepository.existsByName(" Unha ")).thenReturn(false);
        when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse result = categoryService.create(new CategoryRequest(" Unha ", "Manicure e pedicure"));

        assertThat(result.name()).isEqualTo(" Unha ");
        assertThat(result.description()).isEqualTo("Manicure e pedicure");
    }

    @Test
    void create_shouldThrow_whenNameAlreadyExists() {
        when(categoryRepository.existsByName("Unha")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(new CategoryRequest("Unha", null)))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage("Já existe uma categoria com esse nome");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_shouldChangeNameAndDescription() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Unha")));
        when(categoryRepository.existsByNameAndIdNot("Unhas", 1L)).thenReturn(false);
        when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse result = categoryService.update(1L, new CategoryRequest("Unhas", "Nova descrição"));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Unhas");
        assertThat(result.description()).isEqualTo("Nova descrição");
    }

    @Test
    void update_shouldThrow_whenNameUsedByAnotherCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Unha")));
        when(categoryRepository.existsByNameAndIdNot("Cabelo", 1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.update(1L, new CategoryRequest("Cabelo", null)))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_shouldThrow_whenNotFound() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update(1L, new CategoryRequest("Unha", null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Categoria não encontrada");
    }

    @Test
    void delete_shouldRemove_whenCategoryHasNoProducts() {
        CategoryEntity category = category(1L, "Unha");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.delete(1L);

        verify(categoryRepository).delete(category);
    }

    @Test
    void delete_shouldThrow_whenCategoryHasProducts() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Unha")));
        when(productRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Não é possível excluir uma categoria que possui produtos");
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void findById_shouldReturnCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Unha")));

        assertThat(categoryService.findById(1L).name()).isEqualTo("Unha");
    }

    @Test
    void findAll_shouldReturnCategoriesOrderedByRepository() {
        when(categoryRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of(category(2L, "Cabelo"), category(1L, "Unha")));

        List<CategoryResponse> result = categoryService.findAll();

        assertThat(result).extracting(CategoryResponse::name).containsExactly("Cabelo", "Unha");
    }

    @Test
    void findEntityById_shouldReturnEntity() {
        CategoryEntity category = category(1L, "Unha");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThat(categoryService.findEntityById(1L)).isSameAs(category);
    }
}
